/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.mayo.kmdp.preprocess.meta;

import static edu.mayo.kmdp.preprocess.meta.Weaver.CLINICALKNOWLEDGEMANAGEMENT_MAYO_ARTIFACTS_BASE_URI;
import static edu.mayo.kmdp.util.XMLUtil.asAttributeStream;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassetcategory.KnowledgeAssetCategorySeries.Assessment_Predictive_And_Inferential_Models;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassetcategory.KnowledgeAssetCategorySeries.Plans_Processes_Pathways_And_Protocol_Definitions;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassettype.KnowledgeAssetTypeSeries.Care_Process_Model;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassettype.KnowledgeAssetTypeSeries.Decision_Model;
import static edu.mayo.ontology.taxonomies.krformat.SerializationFormatSeries.XML_1_1;
import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.CMMN_1_1;
import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.DMN_1_2;
import static edu.mayo.ontology.taxonomies.krserialization.KnowledgeRepresentationLanguageSerializationSeries.CMMN_1_1_XML_Syntax;
import static edu.mayo.ontology.taxonomies.krserialization.KnowledgeRepresentationLanguageSerializationSeries.DMN_1_2_XML_Syntax;

import com.fasterxml.jackson.databind.JsonNode;
import edu.mayo.kmdp.SurrogateBuilder;
import edu.mayo.kmdp.SurrogateHelper;
import edu.mayo.kmdp.id.helper.DatatypeHelper;
import edu.mayo.kmdp.metadata.annotations.Annotation;
import edu.mayo.kmdp.metadata.annotations.BasicAnnotation;
import edu.mayo.kmdp.metadata.annotations.SimpleAnnotation;
import edu.mayo.kmdp.metadata.surrogate.Association;
import edu.mayo.kmdp.metadata.surrogate.ComputableKnowledgeArtifact;
import edu.mayo.kmdp.metadata.surrogate.Dependency;
import edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.metadata.surrogate.Publication;
import edu.mayo.kmdp.metadata.surrogate.Representation;
import edu.mayo.kmdp.preprocess.NotLatestVersionException;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.util.JSonUtil;
import edu.mayo.kmdp.util.XMLUtil;
import edu.mayo.kmdp.util.XPathUtil;
import edu.mayo.ontology.taxonomies.iso639_2_languagecodes.LanguageSeries;
import edu.mayo.ontology.taxonomies.kao.knowledgeartifactcategory.KnowledgeArtifactCategory;
import edu.mayo.ontology.taxonomies.kao.knowledgeassetcategory.KnowledgeAssetCategorySeries;
import edu.mayo.ontology.taxonomies.kao.knowledgeassettype.KnowledgeAssetTypeSeries;
import edu.mayo.ontology.taxonomies.kao.publicationstatus.PublicationStatusSeries;
import edu.mayo.ontology.taxonomies.kao.rel.dependencyreltype.DependencyTypeSeries;
import edu.mayo.ontology.taxonomies.kmdo.annotationreltype.AnnotationRelTypeSeries;
import edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries;
import edu.mayo.ontology.taxonomies.krserialization.KnowledgeRepresentationLanguageSerializationSeries;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.jena.rdf.model.Resource;
import org.omg.spec.api4kp._1_0.identifiers.URIIdentifier;
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
  private IdentityMapper mapper;
  private XPathUtil xPathUtil;
  private static final Logger logger = LoggerFactory.getLogger(TrisotechExtractionStrategy.class);

  public TrisotechExtractionStrategy() {
    xPathUtil = new XPathUtil();
  }

  @Override
  public IdentityMapper getMapper() {
    return mapper;
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

    List<Annotation> annotations = extractAnnotations(dox);
    KnowledgeAssetCategorySeries formalCategory;

    KnowledgeAssetTypeSeries formalType;
    KnowledgeAsset surr = null;

    KnowledgeRepresentationLanguageSerializationSeries syntax;
    Publication lifecycle = getPublication(meta);
    // Identifiers
    Optional<String> docId = getArtifactID(dox, meta);
    if (!docId.isPresent()) {
      // error out. Can't proceed w/o Artifact -- How did we get this far?
      throw new IllegalStateException("Failed to have artifact in Document");
    }

    logger.debug("docId: {}", docId);

    Optional<URIIdentifier> assetID = getAssetID(dox);
    // TODO: Should processing fail if no assetID? CAO
    if(logger.isDebugEnabled()) {
      logger.debug("assetID: {}", assetID.isPresent() ? assetID.get() : Optional.empty());
    }
    // for the surrogate, want the version of the artifact
    URIIdentifier artifactId = DatatypeHelper.uri(docId.get(), meta.getVersion());

    // artifact<->artifact relation
    Set<Resource> theTargetArtifactId = mapper.getArtifactImports(docId.get());
    if (null != theTargetArtifactId) {
      logger.debug("theTargetArtifactId: {}", theTargetArtifactId);
    } else {
      logger.debug("theTargetArtifactId is null");
    }
    List<URIIdentifier> theTargetAssetId = mapper.getAssetRelations(docId.get());

    // get the language for the document to set the appropriate values
    Optional<Representation> rep = getRepLanguage(dox, false);
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

    // towards the ideal as below
    surr = new edu.mayo.kmdp.metadata.surrogate.resources.KnowledgeAsset()
        .withAssetId(
            (assetID.isPresent() ? assetID.get() : null)) // TODO: what to do if not present? CAO
        // TODO: Discuss with Davide, this is the fileInfo name; shouldn't there be some asset name? CAO
        .withName(meta.getName())
        .withTitle(meta.getName())
        .withFormalCategory(formalCategory)
        .withFormalType(formalType)
        .withSubject(annotations)
        // only restrict to published assets
        .withLifecycle(lifecycle)
        // TODO: Follow-up w/Davide on this CAO
        //        // Some work needed to infer the dependencies
        .withRelated(getRelatedAssets(theTargetAssetId)) // asset - asset relation/dependency
        .withCarriers(new ComputableKnowledgeArtifact()
                .withArtifactId(artifactId)
                .withName(meta.getName())
                .withLocalization(LanguageSeries.English)
                .withExpressionCategory(KnowledgeArtifactCategory.Software)
                .withRepresentation(new Representation()
                        .withLanguage(rep.get().getLanguage())  // DMN_1_2 or CMMN_1_1)
                        .withFormat(XML_1_1)
//                                    .withLexicon(Lexicon.PCV) // TODO: this compiles now, but is it accurate? CAO
                        .withSerialization(syntax) // DMN_1_2_XML_Syntax or CMMN_1_1_XML_Syntax)
                )
                .withRelated( // artifact - artifact relation/dependency
                    getRelatedArtifacts(theTargetArtifactId))
        )
        .withName(meta.getName()); // TODO: might want '(DMN)' / '(CMMN)' here

//
//    // TODO: Needed? yes CAO Is it? annotations are added above in .withSubject
//    // Annotations
    addSemanticAnnotations(surr, annotations);
//
//    // TODO: Needed? yes Maybe not anymore due to mapper code CAO
//    // Dependencies TODO: [asset -> asset ] CAO
//    resolveDependencies(surr, dox)

    logger.debug("surrogate in JSON format: " + new String(JSonUtil.writeJson(surr).get().toByteArray()));

    return surr;
  }

  private Publication getPublication(TrisotechFileInfo meta) {
    Publication lifecycle = new Publication();
    // TODO: FAIL if not? shouldn't have made it this far if not CAO
    if (Optional.ofNullable(meta.getState()).isPresent()) {
      switch (meta.getState()) {
        case "Published":
          lifecycle.withPublicationStatus(PublicationStatusSeries.resolve(PublicationStatusSeries.Published).get());
          break;
        case "Draft":
          lifecycle.withPublicationStatus(PublicationStatusSeries.resolve(PublicationStatusSeries.Draft).get());
          break;
        case "Pending Approval":
          lifecycle.withPublicationStatus(PublicationStatusSeries.resolve(PublicationStatusSeries.Final_Draft).get());
          break;
        default: // TODO: ??? error? CAO
          break;
      }
    }
    logger.debug("lifecycle = {}", lifecycle.getPublicationStatus());

    return lifecycle;
  }

  private Collection<Association> getRelatedArtifacts(Set<Resource> theTargetArtifactId) {
    List<KnowledgeAsset> knowledgeAssets = new ArrayList<>();

    // TODO: rework this once confirm the logic is correct CAO
    if (null != theTargetArtifactId) {
      for (Resource resource : theTargetArtifactId) {
        KnowledgeAsset knowledgeAsset = null;
        // handle try/catch with URIs first
        knowledgeAsset = new KnowledgeAsset().withAssetId(
            // TODO: Is this right? Should be KnowledgeResource? KnowledgeAsset ISA KnowledgeResource CAO
            new URIIdentifier()
                .withUri(URI.create(convertInternalId(resource
                    .getURI(), null))))
            .withName(resource
                .getLocalName()); // TODO: Ask Davide - better name? tgt must have name to pass marshal CAO
        knowledgeAssets.add(knowledgeAsset);
      }
    }
    // TODO: Is this right? Should be KnowledgeResource; KR is abstract; KnowledgeAsset ISA KnowledgeResource; is it the right one? CAO
    return knowledgeAssets.stream().map(ka ->
        new Dependency().withRel(DependencyTypeSeries.Imports)
            .withTgt(ka))
        .collect(Collectors.toList());

  }

  private Collection<Association> getRelatedAssets(List<URIIdentifier> theTargetAssetId) {
    return theTargetAssetId.stream()
        .map(uriIdentifier ->
            new Dependency()
                .withRel(DependencyTypeSeries.Depends_On)
                .withTgt(new KnowledgeAsset().withAssetId(uriIdentifier)
                    .withName(uriIdentifier
                        .toString()))) // TODO: Ask Davide -- is something else expected here as name value? have to have name to pass SAXParser CAO
        .collect(Collectors.toList());
  }


  // TODO: Is this needed? Yes -- need example models to work from CAO
  protected void addSemanticAnnotations(KnowledgeAsset surr, List<Annotation> annotations) {
//    annotations.stream()
//        .filter(ann -> ann.getRel().equals(KnownAttributes.CAPTURES.asConcept())
//            || ann.getRel().equals(AnnotationRelType.Defines.asConcept())
//            || ann.getRel().equals(AnnotationRelType.In_Terms_Of.asConcept()))
//        .forEach(surr::withSubject);

    // TODO: Needed? Am not finding use of Computable_Decision_Model in test models CAO
//    if (surr.getType().contains(KnowledgeAssetType.Computable_Decision_Model)
//            && annotations.stream().anyMatch((ann) -> ann.getRel().equals(KnownAttributes.CAPTURES.asConcept()))) {
//      surr.withType(KnowledgeAssetType.Operational_Concept_Defintion);
//
//      annotations.stream()
//              .filter((ann) -> ann.getRel().equals(KnownAttributes.CAPTURES.asConcept()))
//              .forEach((ann) -> surr.withSubject(new SimpleAnnotation().withRel(AssetVocabulary.DEFINES.asConcept())
//                      .withExpr(((SimpleAnnotation) ann).getExpr())));
//      annotations.stream()
//              .filter((ann) -> ann.getRel().equals(KnownAttributes.CAPTURES.asConcept()))
//              .map((ann) -> ((SimpleAnnotation) ann).getExpr().getTag())
//              .map(ClinicalSituation::resolve)
//              .filter(Optional::isPresent)
//              .map(Optional::get)
//              .map(this::getDatatypeModel)
//              .collect(Collectors.toSet())
//              .forEach((typeCode) -> surr.withRelated(new Dependency().withRel(DependencyType.Effectuates)
//                      .withTgt(buildFhir2Datatype(typeCode))));
//    }
  }


  // used to pull out the annotation values from the woven dox
  private List<Annotation> extractAnnotations(Document dox) {
    List<Annotation> annos = new LinkedList<>();

    // TODO: Maybe extract more annotations, other than the 'document' level ones?
    annos.addAll(XMLUtil.asElementStream(
        dox.getDocumentElement().getElementsByTagName("semantic:extensionElements"))
        .filter(Objects::nonNull)
        .filter(el -> el.getLocalName().equals("extensionElements"))
        .flatMap(el -> XMLUtil.asElementStream(el.getChildNodes()))
        .map(SurrogateHelper::unmarshallAnnotation)
        .map(SurrogateHelper::rootToFragment)
        .collect(Collectors.toList()));

    if (annos.stream()
        .filter(SimpleAnnotation.class::isInstance)
        .map(SimpleAnnotation.class::cast)
        .anyMatch(ann -> KnowledgeAssetTypeSeries.Computable_Decision_Model.getTag()
            .equals(ann.getExpr().getTag()))) {
      // this is a DMN decision model
      List<Node> itemDefs = asAttributeStream(xPathUtil.xList(dox, "//semantic:inputData/@name"))
          // TODO: Needed?  CAO
//					.map( in -> xNode( dox, "//dmn:itemDefinition[@name='"+ in.getValue()+"']" ) ) CAO
          .collect(Collectors.toList());
      for (Node itemDef : itemDefs) {
        List<Annotation> inputAnnos = XMLUtil.asElementStream(itemDef.getChildNodes())
            .filter(Objects::nonNull)
            .filter(el -> el.getLocalName().equals("semantic:extensionElements"))
            .flatMap(el -> XMLUtil.asElementStream(el.getChildNodes()))
            .map(SurrogateHelper::unmarshallAnnotation)
            .collect(Collectors.toList());
        if (inputAnnos.isEmpty() || inputAnnos.size() > 2) {
          throw new IllegalStateException("Missing or duplicated input concept");
        }

        SimpleAnnotation inputAnno = inputAnnos.stream()
//                .filter(ann -> KnownAttributes.CAPTURES.asConcept().equals(ann.getRel())) // TODO: Needed? yes needed, but need better example files, no sample files have CAPTURES CAO
            .map(SimpleAnnotation.class::cast)
            .map(sa -> new SimpleAnnotation().withRel(AnnotationRelTypeSeries.In_Terms_Of.asConcept())
                .withExpr(sa.getExpr()))
            .collect(Collectors.toList()).get(0);
        annos.add(inputAnno);
      }
    }

    logger.debug("end of extractAnnotations; have annos size: {} ", annos.size());
    return annos;
  }

  /**
   * Need the Trisotech path converted to KMDP path and underscores removed
   * TODO: move to utility class? The other place this happens is Weaver CAO
   *
   * @param internalId the Trisotech internal id for the model
   * @return the KMDP-ified internal id
   */
  public String convertInternalId(String internalId, String versionTag) {
    String id = internalId.substring(internalId.lastIndexOf('/') + 1).replace("_", "");
    if (null == versionTag) {
      return CLINICALKNOWLEDGEMANAGEMENT_MAYO_ARTIFACTS_BASE_URI + id;
    } else {
      return CLINICALKNOWLEDGEMANAGEMENT_MAYO_ARTIFACTS_BASE_URI + id + "/versions/" + versionTag;
    }
  }

  @Override
  public Optional<URIIdentifier> getAssetID(Document dox) {
    return getIDAnnotationValue(dox)
        .map(DatatypeHelper::toVersionIdentifier)
        .map(versionIdentifier -> SurrogateBuilder
            .id(versionIdentifier.getTag(), versionIdentifier.getVersion()));
  }

  @Override
  public Optional<URIIdentifier> getAssetID(String fileId) {
    return mapper.getAssetId(fileId);
  }

  public Optional<URI> getEnterpriseAssetIdForAsset(UUID assetId) {
    return mapper.getEnterpriseAssetIdForAsset(assetId);
  }

  public URI getEnterpriseAssetIdForAssetVersionId(URI enterpriseAssetVersionId) {
    return mapper.getEnterpriseAssetIdForAssetVersionId(enterpriseAssetVersionId);
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
  public String getArtifactID(URIIdentifier id, boolean any) throws NotLatestVersionException {
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


  protected Optional<String> getIDAnnotationValue(Document dox) {
    logger.debug("in getIDAnnotationValue...");
    // long form
    List<Annotation> annotations = extractAnnotations(dox);
    for (Annotation annotation : annotations) {
      logger.debug("annotation: {}", annotation);
      if (null != annotation.getRel() && annotation.getRel()
          .equals(KnownAttributes.ASSET_IDENTIFIER.asConcept())) {
        logger.debug("annotation.getRel: {}", annotation.getRel());
        logger.debug("ASSET_IDENTIFIER asConcept: {}",
                KnownAttributes.ASSET_IDENTIFIER.asConcept());
        logger.debug("class: {}", annotation.getClass());
        logger.debug("is BasicAnnotation: {}",
                annotation.getClass().isInstance(BasicAnnotation.class));
        logger.debug("isAssignableFrom BasicAnnotation: {}", annotation.getClass()
                .isAssignableFrom(BasicAnnotation.class));
        logger.debug("expr: {}", ((BasicAnnotation) annotation).getExpr());
      }

    }
    return extractAnnotations(dox).stream()
        .filter(ann -> ann.getRel()!=null)
        .filter(ann -> ann.getRel().equals(KnownAttributes.ASSET_IDENTIFIER.asConcept()))
        .filter(BasicAnnotation.class::isInstance)
        .map(BasicAnnotation.class::cast)
        .map(BasicAnnotation::getExpr)
        .map(URI::toString)
        .findAny();
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
  public Optional<Representation> getRepLanguage(Document dox, boolean concrete) {

    if (xPathUtil.xNode(dox, CMMN_DEFINITIONS) != null) {
      return Optional.of(new Representation()
          .withLanguage(CMMN_1_1)
          .withFormat(concrete ? XML_1_1 : null));
    }
    if (xPathUtil.xNode(dox, DMN_DEFINITIONS) != null) {
      return Optional.of(new Representation()
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

  @Override
  public URIIdentifier extractAssetID(Document dox) {
    Optional<URIIdentifier> resId = getAssetID(dox);
    return resId
        .orElseThrow(IllegalStateException::new); // TODO: better return value if not existent? CAO
  }

}
