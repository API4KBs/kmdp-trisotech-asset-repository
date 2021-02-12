/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors;

import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTAssetRepositoryConfig.TTWParams.DEFAULT_VERSION_TAG;
import static edu.mayo.kmdp.trisotechwrapper.TrisotechWrapper.applyTimestampToVersion;
import static edu.mayo.kmdp.util.JSonUtil.writeJsonAsString;
import static edu.mayo.kmdp.util.JaxbUtil.unmarshall;
import static edu.mayo.kmdp.util.Util.isNotEmpty;
import static edu.mayo.kmdp.util.XMLUtil.asAttributeStream;
import static edu.mayo.kmdp.util.XMLUtil.asElementStream;
import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.Captures;
import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.Defines;
import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.In_Terms_Of;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.stream.Collectors.toList;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newId;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newVersionId;
import static org.omg.spec.api4kp._20200801.id.VersionIdentifier.toSemVer;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.defaultSurrogateUUID;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.randomAssetId;
import static org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype._20200801.DependencyType.Imports;
import static org.omg.spec.api4kp._20200801.taxonomy.iso639_2_languagecode._20190201.Language.English;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeartifactcategory._2020_01_20.KnowledgeArtifactCategory.Software;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries.Assessment_Predictive_And_Inferential_Models;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries.Plans_Processes_Pathways_And_Protocol_Definitions;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Care_Process_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Computable_Decision_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Decision_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.JSON;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.CMMN_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.DMN_1_2;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.asEnum;
import static org.omg.spec.api4kp._20200801.taxonomy.krserialization.KnowledgeRepresentationLanguageSerializationSeries.CMMN_1_1_XML_Syntax;
import static org.omg.spec.api4kp._20200801.taxonomy.krserialization.KnowledgeRepresentationLanguageSerializationSeries.DMN_1_2_XML_Syntax;
import static org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatusSeries.Draft;
import static org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatusSeries.Final_Draft;
import static org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatusSeries.Published;

import com.github.zafarkhaja.semver.Version;
import edu.mayo.kmdp.kdcaci.knew.trisotech.IdentityMapper;
import edu.mayo.kmdp.kdcaci.knew.trisotech.NamespaceManager;
import edu.mayo.kmdp.kdcaci.knew.trisotech.TTAssetRepositoryConfig;
import edu.mayo.kmdp.trisotechwrapper.TrisotechWrapper;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.util.StreamUtil;
import edu.mayo.kmdp.util.XPathUtil;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.surrogate.Annotation;
import org.omg.spec.api4kp._20200801.surrogate.Dependency;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.Link;
import org.omg.spec.api4kp._20200801.surrogate.Publication;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.omg.spec.api4kp._20200801.taxonomy.krserialization.KnowledgeRepresentationLanguageSerializationSeries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Extract the data from the woven (by the Weaver) document to create KnowledgeAsset from model
 * data.
 */
@Component
public class TrisotechIntrospectionStrategy {

  private static final Logger logger = LoggerFactory.getLogger(TrisotechIntrospectionStrategy.class);

  @Autowired
  TrisotechWrapper client;

  @Autowired
  IdentityMapper mapper;

  @Autowired
  NamespaceManager names;

  @Autowired(required = false)
  TTAssetRepositoryConfig config;

  public static final String CMMN_DEFINITIONS = "//cmmn:definitions";
  public static final String DMN_DEFINITIONS = "//dmn:definitions";
  private static final String SEMANTIC_EXTENSION_ELEMENTS = "semantic:extensionElements";
  private static final String EXTENSION_ELEMENTS = "extensionElements";

  private final XPathUtil xPathUtil = new XPathUtil();

  public TrisotechIntrospectionStrategy() {
    //
  }

  @PostConstruct
  void init() {
    if (config == null) {
      config = new TTAssetRepositoryConfig();
    }
  }


  public KnowledgeAsset extractXML(Document dox, TrisotechFileInfo meta) {
    Optional<ResourceIdentifier> tryAssetID;
    if (mapper.isLatest(meta)) {
      // this only works when trying to process the LATEST version
      // since the Enteprise Graph does not contain historical information
      tryAssetID = mapper.resolveModelToCurrentAssetId(meta.getId())
      .or(() -> mapper.extractAssetIdFromDocument(dox));
    } else {
      tryAssetID = mapper.extractAssetIdFromDocument(dox);
    }

    if (tryAssetID.isEmpty()) {
      logger.warn("Model {} does not have an Asset ID - providing a random one", meta.getId());
    }
    ResourceIdentifier assetID = tryAssetID
        .orElse(randomAssetId(names.getAssetNamespace()));

    if (logger.isDebugEnabled()) {
      logger.debug("assetID: {} : {}", assetID.getResourceId(), assetID.getVersionTag() );
    }

    return extractXML(dox, meta, assetID);
  }

  private KnowledgeAsset extractXML(Document woven, TrisotechFileInfo model,
      ResourceIdentifier assetID) {

    List<Annotation> annotations = extractAnnotations(woven);
    KnowledgeAssetCategorySeries formalCategory;

    KnowledgeAssetTypeSeries formalType;
    KnowledgeAsset surr;

    KnowledgeRepresentationLanguageSerializationSeries syntax;
    Publication lifecycle = getPublication(model);
    // Identifiers
    Optional<String> docId = getArtifactID(woven);
    if (docId.isEmpty()) {
      // error out. Can't proceed w/o Artifact -- How did we get this far?
      throw new IllegalStateException("Failed to have artifact in Document");
    }

    logger.debug("docId: {}", docId);

    Date modelDate = Date.from(Instant.parse(model.getUpdated()));
    String artifactTag = docId.get();
    String artifactVersionTag = model.getVersion() == null
        ? applyTimestampToVersion(config.getTyped(DEFAULT_VERSION_TAG, String.class), modelDate.getTime())
        : applyTimestampToVersion(toSemVer(model.getVersion()), modelDate.getTime());

    // for the surrogate, want the version of the artifact
    ResourceIdentifier artifactID =
        newVersionId(URI.create(artifactTag),artifactVersionTag)
            .withEstablishedOn(modelDate);

    // artifact<->artifact relation
    List<ResourceIdentifier> theTargetArtifactId = getArtifactImports(docId.get(), model);
      logger.debug("theTargetArtifactId: {}", theTargetArtifactId);

    // asset<->asset relations
    // assets are derived from the artifact relations
    List<ResourceIdentifier> theTargetAssetId;
    if (mapper.isLatest(model.getId(),model.getVersion())) {
       theTargetAssetId = mapper.getLatestAssetDependencies(docId.get());
    } else {
      theTargetAssetId = theTargetArtifactId
          .stream()
          .map(aid -> mapper.getAssetIdForHistoricalArtifact(aid))
          .flatMap(StreamUtil::trimStream)
          .collect(Collectors.toList());
    }

    // get the language for the document to set the appropriate values
    SyntacticRepresentation synRep = getRepLanguage(woven, false)
        .orElseThrow(() -> new IllegalStateException("Invalid language detected"));
    switch (asEnum(synRep.getLanguage())) {
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
        throw new IllegalStateException(
            "Invalid Language detected." + synRep.getLanguage());
    }

    // towards the ideal
    surr = new org.omg.spec.api4kp._20200801.surrogate.resources.KnowledgeAsset()
        .withAssetId(assetID)
        .withName(model.getName())
        .withFormalCategory(formalCategory)
        .withFormalType(formalType)
        // only restrict to published assets
        .withLifecycle(lifecycle)
//         TODO: Follow-up w/Davide on this CAO
        // Some work needed to infer the dependencies
        .withLinks(getRelatedAssets(theTargetAssetId)) // asset - asset relation/dependency
        .withCarriers(new KnowledgeArtifact()
            .withArtifactId(artifactID)
            .withName(model.getName())
            .withLifecycle(lifecycle)
            .withLocalization(English)
            .withExpressionCategory(Software)
            .withRepresentation(synRep)
            .withMimeType(codedRep(synRep))
            .withLinks( // artifact - artifact relation/dependency
                getRelatedArtifacts(theTargetArtifactId))
        )
        .withSurrogate(
            new KnowledgeArtifact()
                .withArtifactId(newId(
                    names.getArtifactNamespace(),
                    defaultSurrogateUUID(assetID, Knowledge_Asset_Surrogate_2_0),
                    toSemVer(artifactVersionTag)))
                .withRepresentation(rep(Knowledge_Asset_Surrogate_2_0, JSON))
                .withMimeType(codedRep(Knowledge_Asset_Surrogate_2_0, JSON))
        );

    // Annotations
    addSemanticAnnotations(surr, annotations);

    if (logger.isDebugEnabled()) {
      logger.debug(
          "surrogate in JSON format: {} ", writeJsonAsString(surr).orElse("n/a"));
    }
    return surr;
  }

  private List<ResourceIdentifier> getArtifactImports(String docId, TrisotechFileInfo model) {
    // if dealing with the latest of the model, return the latest of the imports
    List<ResourceIdentifier> imports;
    if (mapper.isLatest(model.getId(), model.getVersion())) {
      imports = mapper.getLatestArtifactDependencies(docId);
    } else {
      imports = getImportVersions(docId, model);
    }
    return imports != null ? imports : Collections.emptyList();
  }

  /**
   * Need to get the correct versions of the dependent artifacts for this artifact. It is possible
   * the latest artifact by date may not be the latest artifact by version. Need to get the latest
   * version
   *
   * @param docId the id used to query from Trisotech
   * @param model the latest model information
   * @return the list of ResourceIdentifier for the dependencies
   */
  private List<ResourceIdentifier> getImportVersions(String docId, TrisotechFileInfo model) {
    List<ResourceIdentifier> dependencies = new ArrayList<>();
    // need to find the dependency artifact versions that map to this artifact version
    // using this algorithm:
    // map to the 'latest' version of the dependency that is not timestamped later then the next
    // version of this artifact
    logger.debug(
        "current artifactVersion: {} {} {} ",
        model.getName(), model.getVersion(), model.getUpdated());
    // getTrisotechModelVersions returns all versions of the model EXCEPT the latest
    List<TrisotechFileInfo> artifactModelVersions
        = client.getModelVersions(model.getId(), model.getMimetype());
    // get next version
    TrisotechFileInfo nextArtifactVersion = null;
    Date artifactDate = Date.from(Instant.parse(model.getUpdated()));
    Date nextVersionDate = null;
    for (TrisotechFileInfo tfi : artifactModelVersions) {
      // compare timestamp
      // Need to compare version too? yes, per e-mail exchange w/Davide 04/21
      nextVersionDate = Date.from(Instant.parse(tfi.getUpdated()));
      if (nextVersionDate.after(artifactDate) && tfi.getVersion() != null &&
          (Version.valueOf(tfi.getVersion()).greaterThan(Version.valueOf(model.getVersion())))) {
        nextArtifactVersion = tfi;
        break;
      }
    }
    // the latest version is NOT included in the list of versions;
    // if next is null, it needs to be set to latest
    if (null == nextArtifactVersion) {
      nextArtifactVersion = client.getLatestModelFileInfo(model.getId()).orElse(null);
      if (null != nextArtifactVersion) {
        nextVersionDate = Date.from(Instant.parse(nextArtifactVersion.getUpdated()));
      }
    }

    logger.debug("nextArtifactVersion: {} {} {} ", nextArtifactVersion.getName(),
        nextArtifactVersion.getVersion(), nextArtifactVersion.getUpdated());
    logger.debug("nextVersionDate: {}", nextVersionDate);
    // get versions of the imported artifacts
    List<ResourceIdentifier> artifactImports = mapper.getLatestArtifactDependencies(docId);
    for (ResourceIdentifier ri : artifactImports) {
      logger
          .debug("have resourceIdentifier from artifactImports: {} ", ri.getVersionId());
      String importedModelID = mapper.resolveModelId(ri.getTag()).orElseThrow();
      List<TrisotechFileInfo> importVersions =
          client.getModelVersions(importedModelID,
              mapper.getMimetype(importedModelID));
      // will need to use convertInternalId to get the KMDP resourceId to return
      // use the tag of the artifact with the version and timestamp found to match
      if (importVersions.isEmpty()) {
        return dependencies;
      }
      TrisotechFileInfo matchVersion = findVersionMatch(importVersions, artifactDate,
          nextVersionDate);
      if (null != matchVersion) { // shouldn't happen
        dependencies.add(names.rewriteInternalId(ri.getTag(),
            matchVersion.getVersion(),
            matchVersion.getUpdated()));
      }
    }
    return dependencies;
  }

  /**
   * When looking for matches for imported versions, need to identify the correct version for THIS
   * version of the artifact. Finding the correct version relies on knowing what the *next* version
   * of this artifact is.
   *
   * @param importVersions  versions of the imported models for this model
   * @param artifactDate    the date for this model
   * @param nextVersionDate the date for the next version of this model
   * @return the TrisotechFileInfo for the version of the import that is correct for THIS version of
   * the model
   */
  private TrisotechFileInfo findVersionMatch(List<TrisotechFileInfo> importVersions,
      Date artifactDate, Date nextVersionDate) {
    // as loop through the dependency versions, need to keep track of the previous one
    // as artifact version will depend on the dependency version that is
    // JUST BEFORE the next version of the artifact
    TrisotechFileInfo prevVersion;
    TrisotechFileInfo thisVersion = null;
    TrisotechFileInfo matchVersion = null;
    for (TrisotechFileInfo tfi : importVersions) {
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
      if ((depDate.after(artifactDate)) &&
          ((depDate.before(nextVersionDate)) ||
              (depDate.after(nextVersionDate)))) {
        matchVersion = prevVersion;
      }
      if (matchVersion != null) {
        break; // for loop
      }
      thisVersion = tfi;
    }
    if (null != matchVersion) {
      return matchVersion;
    } else {
      return thisVersion;
    }
  }

  private Publication getPublication(TrisotechFileInfo meta) {
    Publication lifecycle = new Publication();
    if (isNotEmpty(meta.getState())) {
      switch (meta.getState()) {
        case "Published":
          lifecycle.withPublicationStatus(Published);
          break;
        case "Draft":
          lifecycle.withPublicationStatus(Draft);
          break;
        case "Pending Approval":
          lifecycle.withPublicationStatus(Final_Draft);
          break;
        default:
          throw new IllegalStateException("Unrecognized state " + meta.getState());
      }
    } else {
      // NOTE: This should NOT happen in production, but can happen when we are testing models and downloading manually
      // either way, don't want to leave lifecycle empty, so default to Draft (per e-mail w/Davide 1/24/2020)
      lifecycle.withPublicationStatus(Draft);
    }
    logger.debug("lifecycle = {}", lifecycle.getPublicationStatus());

    return lifecycle;
  }

  /**
   * create the information to be returned identifying the artifacts that are imported by the
   * artifact being processed.
   *
   * @param theTargetArtifactId The list of artifact IDs for the imports to the current version of
   *                            the model being processed
   * @return The Link information for the imports; only keeping the identifier
   */
  private Collection<Link> getRelatedArtifacts(List<ResourceIdentifier> theTargetArtifactId) {
    return theTargetArtifactId.stream()
        // TODO: Do something different for null id? means related artifact was not published
        //  log warning was already noted in gathering of related artifacts CAO
        .filter(Objects::nonNull)
        .map(resourceIdentifier -> new Dependency().withRel(Imports)
            .withHref(resourceIdentifier))
        .collect(toList());

  }

  /**
   * Similar to the relatedArtifacts, related Assets are the asset identifiers for the artifacts
   * that are imported or used (dependency) of the current processed artifact. Want to retain the
   * identifiers.
   *
   * @param theTargetAssetId the list of assets this model depends on
   * @return the link information for the assets; only keeping the identifier
   */
  private Collection<Link> getRelatedAssets(List<ResourceIdentifier> theTargetAssetId) {
    return theTargetAssetId.stream()
        .map(resourceIdentifier ->
            new Dependency()
                .withRel(Imports)
                .withHref(resourceIdentifier))
        .collect(toList());
  }


  protected void addSemanticAnnotations(KnowledgeAsset surr, List<Annotation> annotations) {
    annotations.stream()
        .filter(ann -> Captures.sameTermAs(ann.getRel())
            || Defines.sameTermAs(ann.getRel())
            || In_Terms_Of.sameTermAs(ann.getRel()))
        .map(ann -> (Annotation) ann.copyTo(new Annotation()))
        .forEach(surr::withAnnotation);
  }


  // used to pull out the annotation values from the woven dox
  private List<Annotation> extractAnnotations(Document dox) {

    // TODO: Maybe extract more annotations, other than the 'document' level ones?
    List<Annotation> annos = asElementStream(
        dox.getDocumentElement().getElementsByTagName(SEMANTIC_EXTENSION_ELEMENTS))
        .filter(Objects::nonNull)
        .filter(el -> el.getLocalName().equals(EXTENSION_ELEMENTS))
        .flatMap(el -> asElementStream(el.getChildNodes()))
        .filter(child -> child.getLocalName().equals("annotation"))
        .map(child -> unmarshall(Annotation.class, Annotation.class, child))
        .flatMap(StreamUtil::trimStream)
        .collect(java.util.stream.Collectors.toCollection(LinkedList::new));

    if (annos.stream()
        .anyMatch(ann -> Computable_Decision_Model.getTag()
            .equals(ann.getRef().getTag()))) {

      NodeList list = xPathUtil.xList(dox, "//dmn:inputData/@name");
      List<Node> itemDefs = null;
      if(null != list) {
        itemDefs = asAttributeStream(list)
					.map( in -> xPathUtil.xNode(dox, "//dmn:inputData[@name='" + in.getValue() + "']") )
          .collect(toList());
      }

      if(null != itemDefs) {
        for (Node itemDef : itemDefs) {
          List<Annotation> inputAnnos = asElementStream(itemDef.getChildNodes())
              .filter(Objects::nonNull)
              .filter(el -> el.getLocalName().equals(EXTENSION_ELEMENTS))
              .flatMap(el -> asElementStream(el.getChildNodes()))
              .map(child -> unmarshall(Annotation.class, Annotation.class, child))
              .flatMap(StreamUtil::trimStream)
              .collect(toList());
          if (inputAnnos.isEmpty() || inputAnnos.size() > 2) {
            throw new IllegalStateException("Missing or duplicated input concept");
          }

          Annotation inputAnno = inputAnnos.stream()
              .map(Annotation.class::cast)
              .map(sa -> new Annotation()
                  .withRel(In_Terms_Of.asConceptIdentifier())
                  .withRef(sa.getRef())) //.getExpr()))
              .collect(toList()).get(0);
          annos.add(inputAnno);
        }
      }
    }

    logger.debug("end of extractAnnotations; have annos size: {} ", annos.size());
    return annos;
  }


  public Optional<String> getArtifactID(Document dox) {
    Optional<KnowledgeRepresentationLanguage> lang = detectRepLanguage(dox);

    return lang.map(l -> {
      switch (asEnum(l)) {
        case DMN_1_2:
          return xPathUtil.xString(dox, "//*/@namespace");
        case CMMN_1_1:
          return xPathUtil.xString(dox, "//*/@targetNamespace");
        default:
          return null;
      }
    });
  }

  public Optional<SyntacticRepresentation> getRepLanguage(Document dox, boolean concrete) {
    if (dox == null) {
      return Optional.empty();
    }
    if (xPathUtil.xNode(dox, CMMN_DEFINITIONS) != null) {
      return Optional.of(
          concrete ? rep(CMMN_1_1, XML_1_1, defaultCharset(), Encodings.DEFAULT) : rep(CMMN_1_1));
    }
    if (xPathUtil.xNode(dox, DMN_DEFINITIONS) != null) {
      return Optional.of(
          concrete ? rep(DMN_1_2, XML_1_1, defaultCharset(), Encodings.DEFAULT) : rep(DMN_1_2));
    }
    return Optional.empty();
  }

  public Optional<KnowledgeRepresentationLanguage> detectRepLanguage(Document dox) {
    return getRepLanguage(dox,false)
        .map(SyntacticRepresentation::getLanguage);
  }

}