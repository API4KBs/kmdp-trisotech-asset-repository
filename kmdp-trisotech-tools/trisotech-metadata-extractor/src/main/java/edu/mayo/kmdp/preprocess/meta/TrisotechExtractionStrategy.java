/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.mayo.kmdp.preprocess.meta;

import edu.mayo.kmdp.preprocess.meta.KnownAttributes;
//import ca.uhn.fhir.model.dstu2.resource.DataElement;
import com.fasterxml.jackson.databind.JsonNode;
import edu.mayo.kmdp.SurrogateHelper;
import edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset;
//import edu.mayo.kmdp.dataconcepts.FHIR2DataConceptGenerator;
import edu.mayo.kmdp.id.helper.DatatypeHelper;
import edu.mayo.kmdp.metadata.annotations.Annotation;
import edu.mayo.kmdp.metadata.annotations.BasicAnnotation;
import edu.mayo.kmdp.metadata.annotations.SimpleAnnotation;
import edu.mayo.kmdp.metadata.surrogate.*;
import edu.mayo.kmdp.registry.Registry;
import edu.mayo.ontology.taxonomies.clinicalsituations.ClinicalSituation;
import edu.mayo.ontology.taxonomies.iso639_1_languagecodes._20170801.Language;
import edu.mayo.ontology.taxonomies.kao.knowledgeartifactcategory.KnowledgeArtifactCategory;
import edu.mayo.ontology.taxonomies.kao.publicationstatus._2014_02_01.PublicationStatus;
import edu.mayo.ontology.taxonomies.kmdo.annotationreltype._20180601.AnnotationRelType;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.ontology.taxonomies.kao.knowledgeassetcategory._1_0.KnowledgeAssetCategory;
import edu.mayo.ontology.taxonomies.kao.knowledgeassettype._1_0.KnowledgeAssetType;
import edu.mayo.ontology.taxonomies.kao.rel.dependencyreltype._20190801.DependencyType;
import edu.mayo.ontology.taxonomies.krformat._2018._08.SerializationFormat;
import edu.mayo.ontology.taxonomies.krlanguage._2018._08.KnowledgeRepresentationLanguage;
import edu.mayo.kmdp.util.JSonUtil;
import edu.mayo.kmdp.util.XMLUtil;
import edu.mayo.ontology.taxonomies.krserialization._2018._08.KnowledgeRepresentationLanguageSerialization;
import edu.mayo.ontology.taxonomies.lexicon._2018._08.Lexicon;
import org.apache.commons.lang3.StringUtils;
import org.omg.spec.api4kp._1_0.identifiers.URIIdentifier;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

import static edu.mayo.kmdp.id.helper.DatatypeHelper.uri;
import static edu.mayo.kmdp.util.XMLUtil.asAttributeStream;
import static edu.mayo.kmdp.util.XMLUtil.asElementStream;
import static edu.mayo.kmdp.util.XPathUtil.*;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassetcategory._1_0.KnowledgeAssetCategory.Assessment_Predictive_And_Inferential_Models;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassetcategory._1_0.KnowledgeAssetCategory.Plans_Processes_Pathways_And_Protocol_Definitions;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassettype._1_0.KnowledgeAssetType.Cognitive_Care_Process_Model;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassettype._1_0.KnowledgeAssetType.Semantic_Decision_Model;
import static edu.mayo.ontology.taxonomies.krlanguage._2018._08.KnowledgeRepresentationLanguage.CMMN_1_1;
import static edu.mayo.ontology.taxonomies.krlanguage._2018._08.KnowledgeRepresentationLanguage.DMN_1_2;
import static edu.mayo.ontology.taxonomies.krserialization._2018._08.KnowledgeRepresentationLanguageSerialization.CMMN_1_1_XML_Syntax;
import static edu.mayo.ontology.taxonomies.krserialization._2018._08.KnowledgeRepresentationLanguageSerialization.DMN_1_2_XML_Syntax;


// TODO: FIXME CAO

/**
 * Extract the data from the woven (by the Weaver) document to create KnowledgeAsset from model data.
 */
public class TrisotechExtractionStrategy implements ExtractionStrategy {

  private IdentityMapper mapper;

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

//    KnowledgeAsset surr = newSurrogate();
    List<Annotation> annotations = extractAnnotations(dox);
    KnowledgeAssetCategory formalCategory;

    KnowledgeAssetType formalType;

    KnowledgeRepresentationLanguageSerialization syntax;
    KnowledgeResource theTargetArtifactId = null; // TODO: what is this? CAO

    // get the language for the document to set the appropriate values
    Optional<Representation> rep = getRepLanguage(dox, false);
    if ("DMN_1_2".equals(rep.get().getLanguage().toString())) {
      formalCategory = Assessment_Predictive_And_Inferential_Models;
      formalType = Semantic_Decision_Model;
      syntax = DMN_1_2_XML_Syntax;
    } else { // CMMN_1_1
      formalCategory = Plans_Processes_Pathways_And_Protocol_Definitions;
      formalType = Cognitive_Care_Process_Model;
      syntax = CMMN_1_1_XML_Syntax;
    }

    // Identifiers
    Optional<String> docId = getArtifactID(dox);
    System.out.println("docId: " + docId);
    URIIdentifier uriId = new URIIdentifier().withUri(URI.create(docId.get()));
    String versionTag = getVersionTag(dox, meta).orElse(null);
    Optional<URIIdentifier> resId = getResourceID(dox,
        docId.orElseThrow(IllegalStateException::new),
        versionTag);
    System.out.println("resId: " + resId.get().toString());


    // towards the ideal as below
    KnowledgeAsset surr = new edu.mayo.kmdp.metadata.surrogate.resources.KnowledgeAsset()
        .withAssetId(resId.get())
        .withName(meta.getName())
        .withTitle(meta.getName())
        .withFormalCategory(formalCategory)
        .withFormalType(formalType)
        .withSubject(annotations)
        // TODO: need to confirm status? should be equal meta.state CAO
        .withLifecycle(new Publication().withPublicationStatus(PublicationStatus.Published))
        // TODO: Follow-up w/Davide on this CAO
//        // Some work needed to infer the dependencies
//        .withRelated(new Dependency()
//            .withRel(DependencyType.Depends_On)
//            .withTgt(new KnowledgeAsset().withAssetId(theTargetAssetId)) // nothing else
//        )
        .withCarriers(new ComputableKnowledgeArtifact()
                .withArtifactId(uriId)
                .withLocalization(Language.English)
                .withExpressionCategory(KnowledgeArtifactCategory.Software)
                .withRepresentation(new Representation()
                    .withLanguage(rep.get().getLanguage())  // DMN_1_2 or CMMN_1_1)
                    .withFormat(SerializationFormat.XML_1_1)
                    .withLexicon(Lexicon.PCV)
                    .withSerialization(syntax) // DMN_1_2_XML_Syntax or CMMN_1_1_XML_Syntax)
                )
                .withRelated(
                    new Dependency().withRel(DependencyType.Imports)
                        .withTgt(theTargetArtifactId)
                )
            .withName("AskDavide") // TODO: what name goes on the carrier? XML won't parse w/o a name CAO
        );

    // TODO: is any of this needed, or all covered by the code above? CAO
    // TODO: is setAssetId the correct replacement for setResourceId? CAO
//    resId.ifPresent(surr::setAssetId);
//    System.out.println("assetId: " + surr.getAssetId());
//    // .withName
//    surr.setName(meta.getName());
////    resId.ifPresent(surr::setResourceId);
//
//    // TODO: Needed? CAO
////    getRepLanguage(dox, false)
////            .ifPresent((rep) -> trackRepresentationInfo(surr, rep, annotations));
//
//
//    // Descriptive Info
//    surr.setName(meta.getName());
//
//    // TODO: Needed? CAO
//    // Manifestation
//    trackArtifact(surr, docId.get(), versionTag);
//
//    // TODO: Needed? CAO
//    // Annotations
////    addSemanticAnnotations(surr, annotations);
//
//    // TODO: Needed? CAO
//    // Dependencies
//    resolveDependencies(surr, dox);

    return surr;
  }

//  // this is the ideal -- don't recall exactly from the conversation a couple of weeks ago, BUT I believe the following is meant to replace the extractXML() code
//  public KnowledgeAsset metadata() {
//    return new KnowledgeAsset()
//        // from the metadata / explicit manually set id
//        // these come from the modelInfo
//        .withAssetId(assetId)
//        .withName(modelName)
//        .withTitle(modelName)
//        .withDescription(maybe ?)
//        // these come from the annotations
//        .withFormalCategory(Assessment_Predictive_And_Inferential_Models or Plans_Processes_Pathways_And_Protocol_Definitions)
//        .withFormalType(Semantic_Decision_Model or Cognitive_Care_Process_Model)
//        // plug in all the annotations - only the ones of type Simple/MultiWord Annotations, i.e. the ones whose 'expr' is a ClinicalSituation
//        // or a 'focal concept'
//        .withSubject(annotationList)
//        // May still evolve
//        .withLifecycle(new Publication().withPublicationStatus(publicationStatus))
//        // Some work needed to infer the dependencies
//        .withRelated(new Dependency()
//            .withRel(DependencyType.Depends_On)
//            .withTgt(new KnowledgeAsset().withAssetId(theTargetAssetId)) // nothing else
//        )
//        .withCarriers(new ComputableKnowledgeArtifact()
//                .withArtifactId(artifactId) // from the document targetNamespace
//                .withLocalization(Language.English)
//                .withExpressionCategory(KnowledgeArtifactCategory.Software)
//
////                    .withInlined( (serialize the woven document) ) // NOT really at least for now
//                // the final URL/API call in either the KAssR or the KArtR where to get the artifact
//                // either 'assetRepo.getDefaultCarrier(ids..)' or artifactRepo.getArtifact(ids..)'
//                // may leave it out for now
//                //.withLocator( url )
//                .withRepresentation(new Representation()
//                    .withLanguage(DMN_1_2 or CMMN_1_1)
//                    .withFormat(SerializationFormat.XML_1_1)
//                    .withLexicon(Lexicon.PCV)
//                    .withSerialization(DMN_1_2_XML_Syntax or CMMN_1_1_XML_Syntax)
//                )
//                .withRelated(
//                    new Dependency().withRel(DependencyType.Imports)
//                        .withTgt(theTargtArtifactId)
//                )
//
//
//        );
//
//  }


  // TODO: Is this needed anymore?  KnowledgeExpression no longer exists CAO
//  protected void trackRepresentationInfo(KnowledgeAsset surr, Representation r, List<Annotation> annotations) {
//
//    surr.setExpression(new KnowledgeExpression()
//            .withResourceId(uri(UUID.randomUUID().toString()))
//            .withRepresentation(r));
//    // TODO: Update to DMN 1.2 when available CAO
//    switch (r.getLanguage()) {
//      case CMMN_1_1:
//        surr.getCategory().add(KnowledgeAssetCategory.Plans_Processes_Pathways_And_Protocol_Definitions);
//        break;
//      case DMN_1_2:
//        surr.getCategory().add(KnowledgeAssetCategory.Assessment_Predictive_And_Inferential_Models);
//        break;
//      default:
//    }
//
//    annotations.stream()
//            .filter((ann) -> ann.getRel().equals(KnownAttributes.TYPE.asConcept()))
//            .forEach((ann) -> {
//              if (!(ann instanceof SimpleAnnotation)) {
//                throw new IllegalStateException("Asset Type annotation: expected Simple Type, found " + ann.getClass().getName());
//              }
//              SimpleAnnotation sa = (SimpleAnnotation) ann;
//              surr.withType(KnowledgeAssetType.resolve(sa.getExpr())
//                      .orElseThrow(() -> new IllegalStateException("Unable to resolve type-safe annotation " + sa.getExpr())));
//            });
//  }

  // TODO: Is this needed? I think this information is now available in the model... CAO
//  protected void addSemanticAnnotations(KnowledgeAsset surr, List<Annotation> annotations) {
//    annotations.stream()
//            .filter((ann) -> ann.getRel().equals(KnownAttributes.CAPTURES.asConcept())
//                    || ann.getRel().equals(AssetVocabulary.DEFINES.asConcept())
//                    || ann.getRel().equals(KnownAttributes.SUBJECT.asConcept())
//                    || ann.getRel().equals(AssetVocabulary.IN_TERMS_OF.asConcept()))
//            .forEach(surr::withSubject);
//
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
//  }

  // TODO: Fix this .. if needed; per Davide FHIR2/FHIR3DataConceptGenerator are deprecated:
  //  "Just attach the concepts without the 'data definitions'" CAO
//  private String getDatatypeModel(ClinicalSituation thePC) {
//		DataElement type = FHIR2DataConceptGenerator.get( thePC );
//		return type.getElementFirstRep().getTypeFirstRep().getCode();
//    return ;
//  }

  /**
   * TODO: Move this/refactor.
   * @param dataType
   * @return
   */
//  private static edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset buildFhir2Datatype(String dataType) {
//    return new edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset()
//            .withCategory(KnowledgeAssetCategory.Structured_Information_And_Data_Capture_Models)
//            .withType(KnowledgeAssetType.Information_Model)
//            .withExpression(new KnowledgeExpression()
//                    .withResourceId(vuri("https://www.hl7.org/fhir/Datatypes#" + dataType,
//                            "https://www.hl7.org/fhir/DSTU2/Datatypes#" + dataType))
//                    .withRepresentation(new Representation()
//                            .withLanguage(KnowledgeRepresentationLanguage.FHIR_DSTU2)
//                            .withFormat(SerializationFormat.XML_1_1))
//                    .withCarrier(new KnowledgeArtifact()
//                            .withMasterLocation(URI.create("http://www.hl7.org/fhir/DSTU2/datatypes.html#" + dataType))));
//  }

  // TODO: Needed? CAO What is purpose?
  protected void trackArtifact(KnowledgeAsset surr, String docId, String versionTag) {
    KnowledgeArtifact doc = new KnowledgeArtifact();
    surr.withCarriers(doc);

    String enterpriseId = StringUtils.substringBefore(StringUtils.substringAfterLast(docId, "/"), ".");
    // TODO: is setArtifactId the correct replacement for setResourceId? CAO
    // TODO: Should this be MAYO_ARTIFACTS_BASE_URI? CAO
    doc.setArtifactId(uri(Registry.MAYO_ASSETS_BASE_URI + enterpriseId, versionTag));
//    doc.setResourceId(uri(Registry.MAYO_ASSETS_BASE_URI + enterpriseId, versionTag));

    //doc.set(MediaType.APPLICATION);
  }

  private String stripIdFromUri(String uri) {
    return StringUtils.substringBefore(StringUtils.substringAfterLast(uri, "/"), ".");
  }

  // TODO: What is this doing? is it needed anymore? CAO
  private void resolveDependencies(KnowledgeAsset surr, Document dox) {
    NodeList refs = xList(dox, "//*[@externalRef]");
    asElementStream(refs).filter((n) -> n.hasAttribute("xmlns"))
        .map((n) -> n.getAttribute("xmlns"))
        .map(this::stripIdFromUri)
        .filter(mapper::hasIdMapped)
        .forEach((artifactId) -> {
          mapper.getResourceId(artifactId)
              .ifPresent((resourceId) -> {
                KnowledgeAsset ka = new KnowledgeAsset();
                ka.setAssetId(mapper.associate(surr.getAssetId(),
                    artifactId,
                    DependencyType.Depends_On));

                surr.getRelated().add(
                    new Dependency().withRel(DependencyType.Depends_On)
                        .withTgt(ka));
              });
        });
  }

  // TODO: Needed? Purpose? CAO
  private List<Annotation> extractAnnotations(Document dox) {
    List<Annotation> annos = new LinkedList<>();


    // TODO: Maybe extract more annotations, other than the 'document' level ones?
    annos.addAll(XMLUtil.asElementStream(dox.getDocumentElement().getChildNodes())
        .filter(Objects::nonNull)
        .filter((el) -> el.getLocalName().equals("extensionElements"))
        .flatMap((el) -> XMLUtil.asElementStream(el.getChildNodes()))
        .map(SurrogateHelper::unmarshallAnnotation)
        .map(SurrogateHelper::rootToFragment)
        .collect(Collectors.toList()));

    // TODO: Needed? CAO
    if (annos.stream()
        .filter(SimpleAnnotation.class::isInstance)
        .map(SimpleAnnotation.class::cast)
        .anyMatch((ann) -> KnowledgeAssetType.Computable_Decision_Model.getTag().equals(ann.getExpr().getTag()))) {
      // this is a DMN decision model
      List<Node> itemDefs = asAttributeStream(xList(dox, "//semantic:inputData/@name"))
//					.map( (in) -> xNode( dox, "//dmn:itemDefinition[@name='"+ in.getValue()+"']" ) ) CAO
          .collect(Collectors.toList());
      for (Node itemDef : itemDefs) {
        List<Annotation> inputAnnos = XMLUtil.asElementStream(itemDef.getChildNodes())
            .filter(Objects::nonNull)
            .filter((el) -> el.getLocalName().equals("semantic:extensionElements"))
            .flatMap((el) -> XMLUtil.asElementStream(el.getChildNodes()))
            .map(SurrogateHelper::unmarshallAnnotation)
            .collect(Collectors.toList());
        if (inputAnnos.isEmpty() || inputAnnos.size() > 2) {
          throw new IllegalStateException("Missing or duplicated input concept");
        }

        // TODO: Needed? CAO
        SimpleAnnotation inputAnno = inputAnnos.stream()
//                .filter((ann) -> KnownAttributes.CAPTURES.asConcept().equals(ann.getRel())) // TODO: Needed? removed CAO
            .map(SimpleAnnotation.class::cast)
            .map((sa) -> new SimpleAnnotation().withRel(AnnotationRelType.In_Terms_Of.asConcept())
                .withExpr(sa.getExpr()))
            .collect(Collectors.toList()).get(0);
        annos.add(inputAnno);
      }
    }

    return annos;
  }


  @Override
  public Optional<URIIdentifier> getResourceID(Document dox, String artifactId, String versionTag) {
    URI uri = mapper.ensureId(getIDAnnotationValue(dox).orElse(null),
        artifactId + "/" + versionTag);
    return Optional.of(DatatypeHelper.uri(uri.toString(), versionTag));
  }

  protected Optional<String> getIDAnnotationValue(Document dox) {
    return extractAnnotations(dox).stream()
        .filter((ann) -> ann.getRel().equals(KnownAttributes.ASSET_IDENTIFIER.asConcept()))
        .filter(BasicAnnotation.class::isInstance)
        .map(BasicAnnotation.class::cast)
        .map(BasicAnnotation::getExpr)
        .map(URI::toString)
        .findAny();
  }

  @Override
  public Optional<String> getVersionTag(Document dox, TrisotechFileInfo meta) {
    String tag = null;
    if (meta != null) {
      tag = "" + meta.getVersion();
    }
    return Optional.ofNullable(tag);
  }

  @Override
  public Optional<String> getArtifactID(Document dox) {
    Optional<KnowledgeRepresentationLanguage> lang = detectRepLanguage(dox);

    return lang.map((l) -> {
      switch (l) {
        case DMN_1_2:
          return xString(dox, "//*/@namespace");
        case CMMN_1_1:
          return xString(dox, "//*/@targetNamespace");
        default:
          return null;
      }
    });
  }

  @Override
  public Optional<Representation> getRepLanguage(Document dox, boolean concrete) {
    if (xNode(dox, "//cmmn:definitions") != null) {
      return Optional.of(new Representation()
          .withLanguage(CMMN_1_1)
          .withFormat(concrete ? SerializationFormat.XML_1_1 : null));
    }
    if (xNode(dox, "//dmn:definitions") != null) {
      return Optional.of(new Representation()
          .withLanguage(DMN_1_2)
          .withFormat(concrete ? SerializationFormat.XML_1_1 : null));
    }
    return Optional.empty();
  }

  public Optional<KnowledgeRepresentationLanguage> detectRepLanguage(Document dox) {
    if (xNode(dox, "//cmmn:definitions") != null) {
      return Optional.of(CMMN_1_1);
    }
    System.out.println("xNode(dox, xpath //dmn:definitions: " + xNode(dox, "//dmn:definitions"));
    if (xNode(dox, "//dmn:definitions") != null) {
      return Optional.of(DMN_1_2);
    }
    return Optional.empty();
  }

  // TODO: Needed? CAO
  @Override
  public String getMetadataEntryNameForID(String id) {
    System.out.println("getMetadataentryNameForID will return: " +
        "Trisotech Export/model_" +
        id.substring(id.lastIndexOf('/') + 1, id.lastIndexOf('.')) +
        "/model_meta.json");
    return "Trisotech Export/model_" +
        id.substring(id.lastIndexOf('/') + 1, id.lastIndexOf('.')) +
        "/model_meta.json";
  }

  @Override
  public URIIdentifier extractAssetID(Document dox, TrisotechFileInfo meta) {
    Optional<String> docId = getArtifactID(dox);
    String head = getVersionTag(dox, meta).orElse(null);

    Optional<URIIdentifier> resId = getResourceID(dox,
        docId.orElseThrow(IllegalStateException::new),
        head);
    return resId.get();
  }


}
