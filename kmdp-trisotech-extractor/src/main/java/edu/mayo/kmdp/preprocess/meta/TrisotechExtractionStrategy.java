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
import edu.mayo.kmdp.metadata.v2.surrogate.ComputableKnowledgeArtifact;
import edu.mayo.kmdp.metadata.v2.surrogate.Dependency;
import edu.mayo.kmdp.metadata.v2.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.metadata.v2.surrogate.Link;
import edu.mayo.kmdp.metadata.v2.surrogate.Publication;
import edu.mayo.kmdp.metadata.v2.surrogate.annotations.Annotation;
import edu.mayo.kmdp.preprocess.NotLatestVersionException;
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
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
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
    Optional<ResourceIdentifier> assetID = getAssetID(meta.getId()); // getAssetID(dox);
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
    List<ResourceIdentifier> theTargetArtifactId = mapper.getArtifactImports(docId.get());
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

  private Collection<Link> getRelatedArtifacts(List<ResourceIdentifier> theTargetArtifactId) {
    return theTargetArtifactId.stream()
        // TODO: Do something different for null id? means related artifact was not published
        //  log warning was already noted in gathering of related artifacts CAO
        .filter(Objects::nonNull)
        .map(resourceIdentifier ->
            new Dependency().withRel(DependencyTypeSeries.Imports)
                .withHref(resourceIdentifier))
        .collect(Collectors.toList());

  }

  private Collection<Link> getRelatedAssets(List<ResourceIdentifier> theTargetAssetId) {
    return theTargetAssetId.stream()
        .map(resourceIdentifier ->
            new Dependency()
                .withRel(DependencyTypeSeries.Depends_On)
                .withHref(resourceIdentifier))
        .collect(Collectors.toList());
  }


  // TODO: Is this needed? Yes (eventually) -- need example models to work from CAO
  // 02/10/2020: pulls annotations up; may not be needed - terminology
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

  public ResourceIdentifier convertInternalId(String internalId, String versionTag) {
    return mapper.convertInternalId(internalId, versionTag);
  }

  @Override
  public Optional<ResourceIdentifier> getAssetID(String fileId) {
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
  public Optional<ResourceIdentifier> extractAssetID(String internalFileId) {
    return mapper.getAssetId(internalFileId);
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

}
