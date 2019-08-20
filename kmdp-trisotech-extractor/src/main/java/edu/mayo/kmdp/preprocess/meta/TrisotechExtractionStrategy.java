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

import static edu.mayo.kmdp.util.XMLUtil.asAttributeStream;
import static edu.mayo.kmdp.util.XPathUtil.xList;
import static edu.mayo.kmdp.util.XPathUtil.xNode;
import static edu.mayo.kmdp.util.XPathUtil.xString;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassetcategory._20190801.KnowledgeAssetCategory.Assessment_Predictive_And_Inferential_Models;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassetcategory._20190801.KnowledgeAssetCategory.Plans_Processes_Pathways_And_Protocol_Definitions;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassettype._20190801.KnowledgeAssetType.Care_Process_Model;
import static edu.mayo.ontology.taxonomies.kao.knowledgeassettype._20190801.KnowledgeAssetType.Decision_Model;
import static edu.mayo.ontology.taxonomies.krlanguage._20190801.KnowledgeRepresentationLanguage.CMMN_1_1;
import static edu.mayo.ontology.taxonomies.krlanguage._20190801.KnowledgeRepresentationLanguage.DMN_1_2;
import static edu.mayo.ontology.taxonomies.krserialization._20190801.KnowledgeRepresentationLanguageSerialization.CMMN_1_1_XML_Syntax;
import static edu.mayo.ontology.taxonomies.krserialization._20190801.KnowledgeRepresentationLanguageSerialization.DMN_1_2_XML_Syntax;

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
import edu.mayo.ontology.taxonomies.iso639_2_languagecodes._20190201.Language;
import edu.mayo.ontology.taxonomies.kao.knowledgeartifactcategory.KnowledgeArtifactCategory;
import edu.mayo.ontology.taxonomies.kao.knowledgeassetcategory._20190801.KnowledgeAssetCategory;
import edu.mayo.ontology.taxonomies.kao.knowledgeassettype._20190801.KnowledgeAssetType;
import edu.mayo.ontology.taxonomies.kao.publicationstatus._2014_02_01.PublicationStatus;
import edu.mayo.ontology.taxonomies.kao.rel.dependencyreltype._20190801.DependencyType;
import edu.mayo.ontology.taxonomies.kmdo.annotationreltype._20190801.AnnotationRelType;
import edu.mayo.ontology.taxonomies.krformat._20190801.SerializationFormat;
import edu.mayo.ontology.taxonomies.krlanguage._20190801.KnowledgeRepresentationLanguage;
import edu.mayo.ontology.taxonomies.krserialization._20190801.KnowledgeRepresentationLanguageSerialization;
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
import org.w3c.dom.Document;
import org.w3c.dom.Node;

// TODO: FIXME CAO

/**
 * Extract the data from the woven (by the Weaver) document to create KnowledgeAsset from model data.
 */
public class TrisotechExtractionStrategy implements ExtractionStrategy {

  public static final String CMMN_DEFINITIONS = "//cmmn:definitions";
  public static final String DMN_DEFINITIONS = "//dmn:definitions";
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

    List<Annotation> annotations = extractAnnotations(dox);
    KnowledgeAssetCategory formalCategory;

    KnowledgeAssetType formalType;
    KnowledgeAsset surr = null;

    KnowledgeRepresentationLanguageSerialization syntax;
    // Identifiers
    Optional<String> docId = getArtifactID(dox, meta);
    if(!docId.isPresent()) {
      // error out. Can't proceed w/o Artifact -- How did we get this far?
      throw new IllegalStateException("Failed to have artifact in Document");
    }

    System.out.println("docId: " + docId);

    Optional<URIIdentifier> assetID = getAssetID(dox);
    // TODO: Should processing fail if no assetID? CAO
    // TODO: should CMMN have assetID? should, currently does not, so this fails if don't check for present CAO
    System.out.println("assetID: " + (assetID.isPresent() ? assetID.get() : Optional.empty()));

    // for the surrogate, want the version of the artifact
    String artifactUrl = String
        .format("%s/versions/%s", docId.get(), mapper.getVersion(docId.get()).orElseGet(null));
    URIIdentifier artifactId = new URIIdentifier()
        .withUri(URI.create(artifactUrl)); // TODO: what to do if not present? CAO
    // Carrier needs the name
    Optional<String> artifactName = mapper.getArtifactName(docId.get());
    if(!artifactName.isPresent()) {
      throw new IllegalStateException("Failed to have artifact name.");
    }

    // TODO: what is this? CAO -- get all the artifactId; get the imports; put as relationship at artifact level -- should be able to query from IDMapper
    // artifact<->artifact relation
    Set<Resource> theTargetArtifactId = mapper.getArtifactImports(docId);
    if (null != theTargetArtifactId) {
      System.out.println("theTargetArtifactId: " + theTargetArtifactId.toString());
    } else {
      System.out.println("theTargetArtifactId is null");
    }

    List<URIIdentifier> theTargetAssetId = mapper.getAssetRelations(docId);

    // get the language for the document to set the appropriate values
    Optional<Representation> rep = getRepLanguage(dox, false);
    if (rep.isPresent()) {
      switch (rep.get().getLanguage()) {
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
        .withName(meta.getName())
        .withTitle(meta.getName())
        .withFormalCategory(formalCategory)
        .withFormalType(formalType)
        .withSubject(annotations)
        // only restrict to published assets
        .withLifecycle(new Publication().withPublicationStatus(PublicationStatus.Published))
        // TODO: Follow-up w/Davide on this CAO
        //        // Some work needed to infer the dependencies
        .withRelated(getRelatedAssets(theTargetAssetId)) // asset - asset relation/dependency
        .withCarriers(new ComputableKnowledgeArtifact()
            .withArtifactId(artifactId)
            .withName(artifactName.get())
            .withLocalization(Language.English)
            .withExpressionCategory(KnowledgeArtifactCategory.Software)
            .withRepresentation(new Representation()
                .withLanguage(rep.get().getLanguage())  // DMN_1_2 or CMMN_1_1)
                .withFormat(SerializationFormat.XML_1_1)
                //                    .withLexicon(Lexicon.PCV) // TODO: this doesn't compile anymore CAO
                .withSerialization(syntax) // DMN_1_2_XML_Syntax or CMMN_1_1_XML_Syntax)
            )
            .withRelated( // artifact - artifact relation/dependency
                getRelatedArtifacts(theTargetArtifactId))
        )
        .withName(meta.getName()); // TODO: might want '(DMN)' / '(CMMN)' here

//
//    // TODO: Needed? yes CAO
//    // Annotations
    addSemanticAnnotations(surr, annotations);
//
//    // TODO: Needed? yes Maybe not anymore due to mapper code CAO
//    // Dependencies TODO: [asset -> asset ] CAO
//    resolveDependencies(surr, dox);

    return surr;
  }

  private Collection<Association> getRelatedArtifacts(Set<Resource> theTargetArtifactId) {
    List<KnowledgeAsset> knowledgeAssets = new ArrayList<>();
    if (null != theTargetArtifactId) {
      for (Resource resource : theTargetArtifactId) {
        KnowledgeAsset knowledgeAsset = null;
        // handle try/catch with URIs first
        knowledgeAsset = new KnowledgeAsset().withAssetId( // TODO: Is this right? Should be KnowledgeResource? KnowledgeAsset ISA KnowledgeResource CAO
            new URIIdentifier()
                .withUri(URI.create(resource.getURI()))) // TODO: URI is the Trisotech URI -- need to convert to ckm? CAO
        .withName(resource.getLocalName()); // TODO: Ask Davide - better name? tgt must have name to pass marshal CAO
        knowledgeAssets.add(knowledgeAsset);
      }
    }
    return knowledgeAssets.stream().map(ka ->
        new Dependency().withRel(DependencyType.Imports)
            .withTgt(ka))
        .collect(Collectors.toList());

    // TODO: FIX THIS! Need to create for each relation CAO
//    new Dependency().withRel(DependencyType.Imports)
//        .withTgt(new KnowledgeAsset().withAssetId( // TODO: Is this right? Should be KnowledgeResource? KR is abstract KnowledgeAsset ISA KnowledgeResource is it the right one? CAO
//            new URIIdentifier()
//                .withUri(new URI(theTargetArtifactId.stream().findFirst().get().getURI())))
  }

  private Collection<Association> getRelatedAssets(List<URIIdentifier> theTargetAssetId) {
    return theTargetAssetId.stream()
        .map(uriIdentifier ->
            new Dependency()
                .withRel(DependencyType.Depends_On)
                .withTgt(new KnowledgeAsset().withAssetId(uriIdentifier)
                .withName(uriIdentifier.toString()))) // TODO: Ask Davide -- is something else expected here? have to have name to pass SAXParser CAO
        .collect(Collectors.toList());
//    if(null != theTargetAssetId && theTargetAssetId.size() > 0) {
//      for (URIIdentifier uri : theTargetAssetId) {
//        new Dependency()
//            .withRel(DependencyType.Depends_On)
//            .withTgt(new KnowledgeAsset().withAssetId(uri)); // nothing else
//      }
  }

//  // this is the ideal -- don't recall exactly from the conversation several weeks ago, BUT I believe the following is meant to replace the extractXML() code
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
//                .withArtifactId(artifactId) // from the document targetNamespace [subject in triples]
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

  // TODO: Is this needed anymore?  KnowledgeExpression no longer exists? CAO
  // TODO: CAO
  //  From code review: checks the file for a specific asset type instead of the default --- could return type and use above or else default
//  protected void trackRepresentationInfo(List<Annotation> annotations) {
//
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

  // TODO: Is this needed? Yes -- need example models to work from CAO
  protected void addSemanticAnnotations(KnowledgeAsset surr, List<Annotation> annotations) {
    return; // for now to shut up SonarLint CAO
  }
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

//  private String stripIdFromUri(String uri) {
//    return StringUtils.substringBefore(StringUtils.substringAfterLast(uri, "/"), ".");
//  }

  // TODO: What is this doing? is it needed anymore? yes; can help with dependency mappings
  //  Re-evaluate mapper code for support of Trisotech data and CAO
  // need to know kA has dependencies asserted; need to put in KA, but maybe not in this manner
//  private void resolveDependencies(KnowledgeAsset surr, Document dox) {
//    NodeList refs = xList(dox, "//*[@externalRef]");
//    asElementStream(refs).filter((n) -> n.hasAttribute("xmlns"))
//        .map((n) -> n.getAttribute("xmlns"))
//        .map(this::stripIdFromUri)
//        .filter(mapper::hasIdMapped)
//        .forEach((artifactId) -> {
//          mapper.getAssetId(artifactId) // TODO: artifactId or assetId??? is this supposed to be mapping artifact->asset or artifact->artifact?
//              // TODO: cont: my mapper will map artifacts->artifacts, so to get asset, need to get the file for the artifact? CAO
//              .ifPresent((assetId) -> {
//                KnowledgeAsset ka = new KnowledgeAsset();
//                ka.setAssetId(mapper.associate(surr.getAssetId(),
//                    artifactId,
//                    DependencyType.Depends_On));
//
//                surr.getRelated().add(
//                    new Dependency().withRel(DependencyType.Depends_On)
//                        .withTgt(ka));
//              });
//        });
//  }

  // TODO: Needed? Purpose? CAO
  // TODO: Pick up here -- more is needed here? missing BasicAnnotation for assetid? CAO why the filter on extensionElements?
  // used to pull out the annotation values from the woven dox
  private List<Annotation> extractAnnotations(Document dox) {
    System.out.println("in extractAnnotations...");
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
        .anyMatch(ann -> KnowledgeAssetType.Computable_Decision_Model.getTag()
            .equals(ann.getExpr().getTag()))) {
      // this is a DMN decision model
      List<Node> itemDefs = asAttributeStream(xList(dox, "//semantic:inputData/@name"))
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

        // TODO: Needed? yes CAO
        SimpleAnnotation inputAnno = inputAnnos.stream()
//                .filter(ann -> KnownAttributes.CAPTURES.asConcept().equals(ann.getRel())) // TODO: Needed? yes needed, but need better example files CAO
            .map(SimpleAnnotation.class::cast)
            .map(sa -> new SimpleAnnotation().withRel(AnnotationRelType.In_Terms_Of.asConcept())
                .withExpr(sa.getExpr()))
            .collect(Collectors.toList()).get(0);
        annos.add(inputAnno);
      }
    }

    System.out.println("end of extractAnnotations; have annos size: " + annos.size());
    return annos;
  }

  @Override
  public Optional<URIIdentifier> getAssetID(Document dox) {
    System.out.println("in getAssetID...");
    return getIDAnnotationValue(dox)
        .map(DatatypeHelper::toVersionIdentifier)
        .map(versionIdentifier -> SurrogateBuilder
            .id(versionIdentifier.getTag(), versionIdentifier.getVersion()));
  }

  @Override
  public URIIdentifier getAssetID(URI artifact) {
    return null;
  }


  public Optional<URI> getEnterpriseAssetIdForAsset(UUID assetId) {
    return mapper.getEnterpriseAssetIdForAsset(assetId);
  }

  public Optional<URI> getEnterpriseAssetVersionIdForAsset(UUID assetId, String versionTag){
    return mapper.getEnterpriseAssetVersionIdForAsset(assetId, versionTag);
  }

  public Optional<URIIdentifier> getAssetID(URIIdentifier artifactId, String versionTag)
      throws NotLatestVersionException {
    try {
      return mapper.getAssetId(artifactId, versionTag);
    } catch (NotLatestVersionException e) {
      System.out.println("message with NoArtifactVersionException: " + e.getMessage());
      throw e;
    }
    // TODO: Don't recall what the following was supposed to be for. CAO
//    mapper.getVersionedAsset(artifactId, versionTag)
//        .map(DatatypeHelper::toVersionIdentifier)
//        .map(versionIdentifier -> SurrogateBuilder.id(versionIdentifier.getTag(), versionIdentifier.getVersion()));
  }

  public Optional<String> getMimetype(UUID assetId) {
    return mapper.getMimetype(assetId);
  }

  public Optional<String> getArtifactVersion(UUID assetId) {
    return mapper.getArtifactIdVersion(assetId);
  }


  protected Optional<String> getIDAnnotationValue(Document dox) {
    System.out.println("in getIDAnnotationValue...");
    // long form
    List<Annotation> annotations = extractAnnotations(dox);
    for (Annotation annotation : annotations) {
      System.out.println("annotation: " + annotation.toString());
      if (null != annotation.getRel() && annotation.getRel()
          .equals(KnownAttributes.ASSET_IDENTIFIER.asConcept())) {
        System.out.println("annotation.getRel: " + annotation.getRel().toString());
        System.out.println(
            "ASSET_IDENTIFIER asConcept: " + KnownAttributes.ASSET_IDENTIFIER.asConcept()
                .toString());
        System.out.println("class: " + annotation.getClass().toString());
        System.out.println(
            "is BasicAnnotation: " + annotation.getClass().isInstance(BasicAnnotation.class));
        System.out.println(
            "isAssignableFrom BasicAnnotation: " + annotation.getClass()
                .isAssignableFrom(BasicAnnotation.class));
        System.out.println("expr: " + ((BasicAnnotation) annotation).getExpr().toString());
      }

    }
    return extractAnnotations(dox).stream()
        .filter(ann -> ann.getRel().equals(KnownAttributes.ASSET_IDENTIFIER.asConcept()))
        .filter(BasicAnnotation.class::isInstance)
        .map(BasicAnnotation.class::cast)
        .map(BasicAnnotation::getExpr)
        .map(URI::toString)
        .findAny();
  }

  @Override
  public Optional<String> getArtifactVersionTag(Document dox, TrisotechFileInfo meta) {
    String tag = null;
    if (meta != null) {
      tag = "" + meta.getVersion();
    }
    return Optional.ofNullable(tag);
  }

  @Override
  public Optional<String> getArtifactID(Document dox, TrisotechFileInfo meta) {
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
  public String getArtifactID(URIIdentifier assetId, String versionTag)
      throws NotLatestVersionException {
    try {
      return mapper.getArtifactId(DatatypeHelper.uri(assetId.getUri().toString(), versionTag));
    } catch (NotLatestVersionException e) {
      // error is for latest artifact version not matching asset; need to see if another version of the artifact will
      throw e;
    }
  }

  @Override
  public Optional<Representation> getRepLanguage(Document dox, boolean concrete) {
    System.out.println("xNode(dox, xpath //dmn:definitions: " + xNode(dox, "//dmn:definitions"));
    System.out.println("xNode(dox, xpath //cmmn:definitions: " + xNode(dox, "//cmmn:definitions"));

    if (xNode(dox, CMMN_DEFINITIONS) != null) {
      return Optional.of(new Representation()
          .withLanguage(CMMN_1_1)
          .withFormat(concrete ? SerializationFormat.XML_1_1 : null));
    }
//    if (xNode(dox, DMN_DEFINITIONS) != null) {
//    }
    else { // TODO: return the if check once xNode has fix for Registry.getNamespaceURIForPrefix - currently failing for DMN 1.2 CAO
//    if (xNode(dox, "//dmn:definitions") != null) {
      return Optional.of(new Representation()
          .withLanguage(DMN_1_2)
          .withFormat(concrete ? SerializationFormat.XML_1_1 : null));
    }
//    return Optional.empty();
  }

  public Optional<KnowledgeRepresentationLanguage> detectRepLanguage(Document dox) {
    System.out.println("xNode(dox, xpath //cmmn:definitions: " + xNode(dox, CMMN_DEFINITIONS));
    if (xNode(dox, CMMN_DEFINITIONS) != null) {
      return Optional.of(CMMN_1_1);
    }
//    System.out.println("xNode(dox, xpath //dmn:definitions: " + xNode(dox, DMN_DEFINITIONS));
//    if (xNode(dox, DMN_DEFINITIONS) != null) {
//    }
    else { // TODO: return to if check once xNode has fix for Registry.getNamespaceURIForPrefix - currently failing for DMN 1.2 CAO
//    System.out.println("xNode(dox, xpath //dmn:definitions: " + xNode(dox, "//dmn:definitions"));
//    if (xNode(dox, "//dmn:definitions") != null) {
      return Optional.of(DMN_1_2);
    }
//    return Optional.empty();
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
  public URIIdentifier extractAssetID(Document dox) {
    Optional<URIIdentifier> resId = getAssetID(dox);
    return resId.orElseThrow(IllegalStateException::new); // TODO: better return value if not existent? CAO
  }


}
