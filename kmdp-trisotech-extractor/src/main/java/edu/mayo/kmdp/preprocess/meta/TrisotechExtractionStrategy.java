/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package edu.mayo.kmdp.preprocess.meta;

import static edu.mayo.kmdp.util.XMLUtil.asAttributeStream;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassetcategory.KnowledgeAssetCategorySeries.Assessment_Predictive_And_Inferential_Models;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassetcategory.KnowledgeAssetCategorySeries.Plans_Processes_Pathways_And_Protocol_Definitions;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassettype.KnowledgeAssetTypeSeries.Care_Process_Model;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassettype.KnowledgeAssetTypeSeries.Decision_Model;
import static edu.mayo.ontology.taxonomies.kmdo.annotationreltype.AnnotationRelTypeSeries.In_Terms_Of;
import static edu.mayo.ontology.taxonomies.krformat.SerializationFormatSeries.XML_1_1;
import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.CMMN_1_1;
import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.DMN_1_2;
import static edu.mayo.ontology.taxonomies.krserialization.KnowledgeRepresentationLanguageSerializationSeries.CMMN_1_1_XML_Syntax;
import static edu.mayo.ontology.taxonomies.krserialization.KnowledgeRepresentationLanguageSerializationSeries.DMN_1_2_XML_Syntax;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.zafarkhaja.semver.Version;
import edu.mayo.kmdp.metadata.v2.surrogate.ComputableKnowledgeArtifact;
import edu.mayo.kmdp.metadata.v2.surrogate.Dependency;
import edu.mayo.kmdp.metadata.v2.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.metadata.v2.surrogate.Link;
import edu.mayo.kmdp.metadata.v2.surrogate.Publication;
import edu.mayo.kmdp.metadata.v2.surrogate.annotations.Annotation;
import edu.mayo.kmdp.preprocess.NotLatestVersionException;
import edu.mayo.kmdp.trisotechwrapper.TrisotechWrapper;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.util.JSonUtil;
import edu.mayo.kmdp.util.JaxbUtil;
import edu.mayo.kmdp.util.XMLUtil;
import edu.mayo.kmdp.util.XPathUtil;
import edu.mayo.ontology.taxonomies.iso639_2_languagecodes.LanguageSeries;
import edu.mayo.ontology.taxonomies.kao.knowledgeartifactcategory.KnowledgeArtifactCategory;
import edu.mayo.ontology.taxonomies.kao.knowledgeassetcategory.KnowledgeAssetCategorySeries;
import edu.mayo.ontology.taxonomies.kao.knowledgeassettype.KnowledgeAssetTypeSeries;
import edu.mayo.ontology.taxonomies.kao.publicationstatus.PublicationStatusSeries;
import edu.mayo.ontology.taxonomies.kao.rel.dependencyreltype.DependencyTypeSeries;
import edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries;
import edu.mayo.ontology.taxonomies.krserialization.KnowledgeRepresentationLanguageSerializationSeries;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.jena.shared.NotFoundException;
import org.omg.spec.api4kp._1_0.id.ResourceIdentifier;
import org.omg.spec.api4kp._1_0.id.SemanticIdentifier;
import org.omg.spec.api4kp._1_0.services.SyntacticRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Extract the data from the woven (by the Weaver) document to create KnowledgeAsset from model data.
 */
@Component
public class TrisotechExtractionStrategy implements ExtractionStrategy {

  public static final String CMMN_DEFINITIONS = "//cmmn:definitions";
  public static final String DMN_DEFINITIONS = "//dmn:definitions";
  private static final String SEMANTIC_EXTENSION_ELEMENTS = "semantic:extensionElements";
  private IdentityMapper mapper;
  private XPathUtil xPathUtil;
  private static final Logger logger = LoggerFactory.getLogger(TrisotechExtractionStrategy.class);

  public TrisotechExtractionStrategy() {
    xPathUtil = new XPathUtil();
  }

  public void setMapper(IdentityMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public KnowledgeAsset extractXML(Document dox, JsonNode meta) {

    TrisotechFileInfo info = meta != null
        ? JSonUtil.parseJson(meta.get("file"), TrisotechFileInfo.class)
        .orElse(new TrisotechFileInfo())
        : new TrisotechFileInfo();

    return extractXML(dox, info);

  }

  @Override
  public KnowledgeAsset extractXML(Document dox, TrisotechFileInfo meta) {
    Optional<ResourceIdentifier> assetID = getAssetID(meta.getId());
    if (logger.isDebugEnabled()) {
      logger.debug("assetID: {}", assetID.isPresent() ? assetID.get() : Optional.empty());
    }
    // Should processing fail if no assetID? don't continue processing; provide warning
    if(!assetID.isPresent()) {
      logger.warn("Asset ID is missing from model. Halting processing for {} ", meta.getName());
      return null; // TODO: better return value? empty?
    }

    return extractXML(dox, meta, assetID.get());
  }

  @Override
  public KnowledgeAsset extractXML(Document woven, TrisotechFileInfo model,
      ResourceIdentifier assetID) {

    List<Annotation> annotations = extractAnnotations(woven);
    KnowledgeAssetCategorySeries formalCategory;

    KnowledgeAssetTypeSeries formalType;
    KnowledgeAsset surr;

    KnowledgeRepresentationLanguageSerializationSeries syntax;
    Publication lifecycle = getPublication(model);
    // Identifiers
    Optional<String> docId = getArtifactID(woven, model);
    if (!docId.isPresent()) {
      // error out. Can't proceed w/o Artifact -- How did we get this far?
      throw new IllegalStateException("Failed to have artifact in Document");
    }

    logger.debug("docId: {}", docId);

    // for the surrogate, want the version of the artifact
    Date modelDate = Date.from(Instant.parse(model.getUpdated()));
    ResourceIdentifier artifactId = SemanticIdentifier.newVersionId(URI.create(docId.get()))
        .withVersionTag(model.getVersion() + "+" + modelDate.getTime())
        .withEstablishedOn(modelDate);

    // artifact<->artifact relation
    List<ResourceIdentifier> theTargetArtifactId = getArtifactImports(docId.get(), model);
    if (null != theTargetArtifactId) {
      logger.debug("theTargetArtifactId: {}", theTargetArtifactId);
    } else {
      logger.debug("theTargetArtifactId is null");
    }

    // asset<->asset relations
    // assets are derived from the artifact relations
    List<ResourceIdentifier> theTargetAssetId = mapper.getAssetRelations(docId.get());

    // get the language for the document to set the appropriate values
    Optional<SyntacticRepresentation> rep = getRepLanguage(woven, false);
    if (rep.isPresent()) {
      switch (rep.get().getLanguage().asEnum()) {
        case DMN_1_2:
          formalCategory = Assessment_Predictive_And_Inferential_Models;
          // default value
          formalType = Decision_Model;
          syntax = DMN_1_2_XML_Syntax;
          break;
        case CMMN_1_1:
          formalCategory = Plans_Processes_Pathways_And_Protocol_Definitions;
          // default value, may be specified differently in the file
          formalType = Care_Process_Model;
          syntax = CMMN_1_1_XML_Syntax;
          break;
        default:
          throw new IllegalStateException("Invalid Language detected." + rep.get().getLanguage());
      }
    } else {
      throw new IllegalStateException(
          "Invalid Language detected." + rep); // TODO: better error for here? CAO
    }

    // towards the ideal
    surr = new edu.mayo.kmdp.metadata.v2.surrogate.resources.KnowledgeAsset()
        .withAssetId(assetID)
        .withName(model.getName())
        .withFormalCategory(formalCategory)
        .withFormalType(formalType)
        // only restrict to published assets
        .withLifecycle(lifecycle)
//         TODO: Follow-up w/Davide on this CAO
        // Some work needed to infer the dependencies
        .withLinks(getRelatedAssets(theTargetAssetId)) // asset - asset relation/dependency
        .withCarriers(new ComputableKnowledgeArtifact()
                .withArtifactId(artifactId)
                .withName(model.getName())
                .withLifecycle(lifecycle)
                .withLocalization(LanguageSeries.English)
                .withExpressionCategory(KnowledgeArtifactCategory.Software)
                .withRepresentation(new SyntacticRepresentation()
                        .withLanguage(rep.get().getLanguage())  // DMN_1_2 or CMMN_1_1)
                        .withFormat(XML_1_1)
//                                    .withLexicon(Lexicon.PCV) // TODO: this compiles now, but is it accurate? CAO
                        .withSerialization(syntax) // DMN_1_2_XML_Syntax or CMMN_1_1_XML_Syntax)
                )
                .withLinks( // artifact - artifact relation/dependency
                    getRelatedArtifacts(theTargetArtifactId))
        )
        .withName(model.getName()); // TODO: might want '(DMN)' / '(CMMN)' here

//
//    // TODO: Needed? yes CAO Is it? annotations are added above in .withSubject [withSubject has been removed per Davide notes in surrogate]
//    // Annotations
    addSemanticAnnotations(surr, annotations);

    logger.debug(
        "surrogate in JSON format: {} ", new String(JSonUtil.writeJson(surr).get().toByteArray()));

    return surr;
  }

  private List<ResourceIdentifier> getArtifactImports(String docId, TrisotechFileInfo model) {
    // if dealing with the latest of the model, return the latest of the imports
    if(mapper.isLatest(model.getId(), model.getVersion())) {
      return mapper.getArtifactImports(docId);
    }
    return getImportVersions(docId, model);
  }

  /**
   * Need to get the correct versions of the dependent artifacts for this artifact.
   * It is possible the latest artifact by date may not be the latest artifact by version.
   * Need to get the latest version
   * @param docId the id used to query from Trisotech
   * @param model the latest model information
   * @return the list of ResourceIdentifier for the dependencies
   */
  private List<ResourceIdentifier> getImportVersions(String docId, TrisotechFileInfo model) {
    List<ResourceIdentifier> dependencies = new ArrayList<>();
    // need to find the dependency artifact versions that map to this artifact version
    // using this algorithm:
    // map to the 'latest' version of the dependency that is not timestamped later then the next
    // get versions of this artifact
    logger.debug(
        "current artifactVersion: {} {} {} ", model.getName(), model.getVersion(), model
            .getUpdated());
    // getTrisotechModelVersions returns all versions of the model EXCEPT the latest
    List<TrisotechFileInfo> artifactModelVersions = getTrisotechModelVersions(model.getId(), model.getMimetype());
    // get next version
    TrisotechFileInfo nextArtifactVersion = null;
    Date artifactDate = Date.from(Instant.parse(model.getUpdated()));
    Date nextVersionDate = null;
    for(TrisotechFileInfo tfi: artifactModelVersions) {
      // compare timestamp
      // Need to compare version too? yes, per e-mail exchange w/Davide 04/21
      nextVersionDate = Date.from(Instant.parse(tfi.getUpdated()));
      if(nextVersionDate.after(artifactDate) &&
          (Version.valueOf(tfi.getVersion()).greaterThan(Version.valueOf(model.getVersion())))) {
        nextArtifactVersion = tfi;
        break;
      }
    }
    // the latest version is NOT included in the list of versions;
    // if next is null, it needs to be set to latest
    if(null == nextArtifactVersion) {
      nextArtifactVersion = TrisotechWrapper.getLatestModelFileInfo(model.getId()).orElse(null);
      nextVersionDate = Date.from(Instant.parse(nextArtifactVersion.getUpdated()));
    }

    logger.debug("nextArtifactVersion: {} {} {} ",nextArtifactVersion.getName(),
        nextArtifactVersion.getVersion(), nextArtifactVersion.getUpdated());
    logger.debug("nextVersionDate: {}", nextVersionDate.toString());
    // get versions of the imported artifacts
    List<ResourceIdentifier> artifactImports = mapper.getArtifactImports(docId);
    for(ResourceIdentifier ri : artifactImports) {
      logger.debug("have resourceIdentifier from artifactImports: {} ", ri.getVersionId().toString());
      List<TrisotechFileInfo> importVersions = getTrisotechModelVersions(ri.getTag());
      // will need to use convertInternalId to get the KMDP resourceId to return
      // use the tag of the artifact with the version and timestamp found to match
      if(importVersions.size() == 0) {
        return dependencies;
      }
      TrisotechFileInfo matchVersion = findVersionMatch(importVersions, artifactDate, nextVersionDate);
      if(null != matchVersion) { // shouldn't happen
        dependencies.add(convertInternalId(ri.getTag(),
            matchVersion.getVersion(),
            matchVersion.getUpdated()));
      }
    }
    return dependencies;
  }

  /**
   * When looking for matches for imported versions, need to identify the correct version for THIS
   * version of the artifact.
   * Finding the correct version relies on knowing what the *next* version of this artifact is.
   *
   * @param importVersions versions of the imported models for this model
   * @param artifactDate the date for this model
   * @param nextVersionDate the date for the next version of this model
   * @return the TrisotechFileInfo for the version of the import that is correct for THIS version
   * of the model
   */
  private TrisotechFileInfo findVersionMatch(List<TrisotechFileInfo> importVersions,
      Date artifactDate, Date nextVersionDate) {
    // as loop through the dependency versions, need to keep track of the previous one
    // as artifact version will depend on the dependency version that is
    // JUST BEFORE the next version of the artifact
    TrisotechFileInfo prevVersion;
    TrisotechFileInfo thisVersion = null;
    TrisotechFileInfo matchVersion = null;
    for(TrisotechFileInfo tfi : importVersions) {
      prevVersion = thisVersion;
      logger.debug("version: {}", tfi.getVersion());
      logger.debug("updated: {}", tfi.getUpdated());
      // find the version that is a match for the artifact
      Date depDate = Date.from(Instant.parse(tfi.getUpdated()));

      logger.debug("dependency date: {} ", depDate);
      logger.debug("nextVersion compareTo depDate: {}", nextVersionDate.compareTo(depDate));
      logger.debug("artifactDate compareTo depDate: {}", artifactDate.compareTo(depDate));
      logger.debug("depDate before artifactDate? {}", depDate.before(artifactDate));
      logger.debug("depDate after artifactDate? {}", depDate.after(artifactDate));
      logger.debug("depDate before nextDate? {}", depDate.before(nextVersionDate));
      logger.debug("depDate after nextDate? {}", depDate.after(nextVersionDate));
      // will need to use convertInternalId to get the KMDP resourceId to return
      if((depDate.after(artifactDate)) &&
          ((depDate.before(nextVersionDate)) ||
              (depDate.after(nextVersionDate)))) {
        matchVersion = prevVersion;
      }
      if(matchVersion != null) {
        break; // for loop
      }
      thisVersion = tfi;
    }
    if(null != matchVersion) {
      return matchVersion;
    } else {
      return thisVersion;
    }
  }

  private Publication getPublication(TrisotechFileInfo meta) {
    Publication lifecycle = new Publication();
    // TODO: FAIL if not? shouldn't have made it this far if not CAO
    if (Optional.ofNullable(meta.getState()).isPresent()) {
      switch (meta.getState()) {
        case "Published":
          lifecycle.withPublicationStatus(
              PublicationStatusSeries.Published);
          break;
        case "Draft":
          lifecycle.withPublicationStatus(
              PublicationStatusSeries.Draft);
          break;
        case "Pending Approval":
          lifecycle.withPublicationStatus(
              PublicationStatusSeries.Final_Draft);
          break;
        default: // TODO: ??? error? CAO
          break;
      }
    } else {
      // NOTE: This should NOT happen in production, but can happen when we are testing models and downloading manually
      // either way, don't want to leave lifecycle empty, so default to Draft (per e-mail w/Davide 1/24/2020)
      lifecycle.withPublicationStatus(PublicationStatusSeries.Draft);
    }
    logger.debug("lifecycle = {}", lifecycle.getPublicationStatus());

    return lifecycle;
  }

  /**
   * create the information to be returned identifying the artifacts that are imported by
   * the artifact being processed.
   *
   * @param theTargetArtifactId The list of artifact IDs for the imports to the current version
   * of the model being processed
   * @return The Link information for the imports; only keeping the identifier
   */
  private Collection<Link> getRelatedArtifacts(List<ResourceIdentifier> theTargetArtifactId) {
    return theTargetArtifactId.stream()
        // TODO: Do something different for null id? means related artifact was not published
        //  log warning was already noted in gathering of related artifacts CAO
        .filter(Objects::nonNull)
        .map(resourceIdentifier -> new Dependency().withRel(DependencyTypeSeries.Imports)
              .withHref(resourceIdentifier))
        .collect(Collectors.toList());

  }

  /**
   * Similar to the relatedArtifacts, related Assets are the asset identifiers for the
   * artifacts that are imported or used (dependency) of the current processed artifact.
   * Want to retain the identifiers.
   *
   * @param theTargetAssetId the list of assets this model depends on
   * @return the link information for the assets; only keeping the identifier
   */
  private Collection<Link> getRelatedAssets(List<ResourceIdentifier> theTargetAssetId) {
    return theTargetAssetId.stream()
        .map(resourceIdentifier ->
            new Dependency()
                .withRel(DependencyTypeSeries.Depends_On)
                .withHref(resourceIdentifier))
        .collect(Collectors.toList());
  }


  // TODO: Is this needed? Yes (eventually) -- need example models to work from CAO
  // 02/10/2020: pulls annotations up; may not be needed - replace with terminology service
  protected void addSemanticAnnotations(KnowledgeAsset surr, List<Annotation> annotations) {
//    annotations.stream()
//        .filter(ann -> ann.getRel().equals(AnnotationRelTypeSeries.Captures.asConcept())
//            || ann.getRel().equals(AnnotationRelTypeSeries.Defines.asConcept())
//            || ann.getRel().equals(AnnotationRelTypeSeries.In_Terms_Of.asConcept()))
//        .forEach(surr::withSubject);

  }


  // used to pull out the annotation values from the woven dox
  private List<Annotation> extractAnnotations(Document dox) {
    List<Annotation> annos = new LinkedList<>();

    // TODO: Maybe extract more annotations, other than the 'document' level ones?
    annos.addAll(XMLUtil.asElementStream(
        dox.getDocumentElement().getElementsByTagName(SEMANTIC_EXTENSION_ELEMENTS))
        .filter(Objects::nonNull)
        .filter(el -> el.getLocalName().equals("extensionElements"))
        .flatMap(el -> XMLUtil.asElementStream(el.getChildNodes()))
        .map(child -> JaxbUtil.unmarshall(Annotation.class, Annotation.class, child))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList()));

    if (annos.stream()
        .filter(Annotation.class::isInstance)
        .map(Annotation.class::cast)
        .anyMatch(ann -> KnowledgeAssetTypeSeries.Computable_Decision_Model.getTag()
            .equals(ann.getRef().getTag()))) { //ann.getExpr().getTag()))) {
      // this is a DMN decision model
      List<Node> itemDefs = asAttributeStream(xPathUtil.xList(dox, "//semantic:inputData/@name"))
          // TODO: Needed?  CAO
//					.map( in -> xNode( dox, "//dmn:itemDefinition[@name='"+ in.getValue()+"']" ) ) CAO
          .collect(Collectors.toList());
      for (Node itemDef : itemDefs) {
        List<Annotation> inputAnnos = XMLUtil.asElementStream(itemDef.getChildNodes())
            .filter(Objects::nonNull)
            .filter(el -> el.getLocalName().equals(SEMANTIC_EXTENSION_ELEMENTS))
            .flatMap(el -> XMLUtil.asElementStream(el.getChildNodes()))
            .map(child -> JaxbUtil.unmarshall(Annotation.class, Annotation.class, child))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
        if (inputAnnos.isEmpty() || inputAnnos.size() > 2) {
          throw new IllegalStateException("Missing or duplicated input concept");
        }

        Annotation inputAnno = inputAnnos.stream()
//                .filter(ann -> KnownAttributes.CAPTURES.asConcept().equals(ann.getRel())) // TODO: Needed? yes needed, but need better example files, no sample files have CAPTURES CAO
            .map(Annotation.class::cast)
            .map(sa -> new Annotation()
                .withRel(In_Terms_Of.asConceptIdentifier())
                .withRef(sa.getRef())) //.getExpr()))
            .collect(Collectors.toList()).get(0);
        annos.add(inputAnno);
      }
    }

    logger.debug("end of extractAnnotations; have annos size: {} ", annos.size());
    return annos;
  }

  public ResourceIdentifier convertInternalId(String internalId, String versionTag,
      String timestamp) {
    return mapper.convertInternalId(internalId, versionTag, timestamp);
  }

  @Override
  public Optional<ResourceIdentifier> getAssetID(String fileId) {
    return mapper.getAssetId(fileId);
  }

  public Optional<URI> getEnterpriseAssetIdForAsset(UUID assetId) {
    return mapper.getEnterpriseAssetIdForAsset(assetId);
  }

  public Optional<URI> getEnterpriseAssetVersionIdForAsset(UUID assetId, String versionTag,
      boolean any)
      throws NotLatestVersionException {
    return mapper.getEnterpriseAssetVersionIdForAsset(assetId, versionTag, any);
  }

  public Optional<String> getFileId(UUID assetId, boolean any) {
    return mapper.getFileId(assetId, any);
  }

  @Override
  public String getArtifactID(ResourceIdentifier id, boolean any) throws NotLatestVersionException {
    return mapper.getArtifactId(id, any);
  }

  public Optional<String> getFileId(String internalId) {
    return mapper.getFileId(internalId);
  }

  public Optional<String> getMimetype(UUID assetId) {
    return mapper.getMimetype(assetId);
  }

  public Optional<String> getMimetype(String internalId) {
    return mapper.getMimetype(internalId);
  }


  public Optional<String> getArtifactVersion(UUID assetId) {
    return mapper.getArtifactIdVersion(assetId);
  }

  public Optional<String> getArtifactIdUpdateTime(UUID assetId) {
    return mapper.getArtifactIdUpdateTime(assetId);
  }

  @Override
  public Optional<String> getArtifactID(Document dox, TrisotechFileInfo meta) {
    Optional<KnowledgeRepresentationLanguageSeries> lang = detectRepLanguage(dox);

    return lang.map(l -> {
      switch (l) {
        case DMN_1_2:
          return xPathUtil.xString(dox, "//*/@namespace");
        case CMMN_1_1:
          return xPathUtil.xString(dox, "//*/@targetNamespace");
        default:
          return null;
      }
    });
  }

  @Override
  public Optional<SyntacticRepresentation> getRepLanguage(Document dox, boolean concrete) {

    if (xPathUtil.xNode(dox, CMMN_DEFINITIONS) != null) {
      return Optional.of(new SyntacticRepresentation()
          .withLanguage(CMMN_1_1)
          .withFormat(concrete ? XML_1_1 : null));
    }
    if (xPathUtil.xNode(dox, DMN_DEFINITIONS) != null) {
      return Optional.of(new SyntacticRepresentation()
          .withLanguage(DMN_1_2)
          .withFormat(concrete ? XML_1_1 : null));
    }
    return Optional.empty();
  }

  public Optional<KnowledgeRepresentationLanguageSeries> detectRepLanguage(Document dox) {
    if (xPathUtil.xNode(dox, CMMN_DEFINITIONS) != null) {
      return Optional.of(CMMN_1_1);
    }
    if (xPathUtil.xNode(dox, DMN_DEFINITIONS) != null) {
      return Optional.of(DMN_1_2);
    }
    return Optional.empty();
  }

  /**
   * Trisotech model versions can be retrieved through a specific call. The versions will NOT include
   * the latest version of the model, but all other versions.
   *
   * @param internalId the internalId is the id used by Trisotech internally to identify the model.
   * The internal id is the URI. It is used to get the appropriate query parameter values.
   * @return the TrisotechFileInfo list of all but the current version of the model requested
   */
  public List<TrisotechFileInfo> getTrisotechModelVersions(String internalId) {
    // need fileId as trisotech APIs work on fileId
    Optional<String> fileId = getFileId(internalId);
    // need mimetype to get the correct URL to download XML
    Optional<String> mimeType = getMimetype(internalId);
    if (!fileId.isPresent() || !mimeType.isPresent()) {
      // TODO: throw exception or just return NOT_FOUND? CAO
      throw new NotFoundException("Error finding fileId or mimetype for internalid " + internalId);
    }
    // need to get all versions for the file
    return getTrisotechModelVersions(fileId.get(), mimeType.get());
  }

  public List<TrisotechFileInfo> getTrisotechModelVersions(String fileId, String mimeType) {
    // need to get all versions for the file
    return TrisotechWrapper
        .getModelVersions(fileId, mimeType);
  }

}
