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

import static edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.BPMMetadataHelper.addSemanticAnnotations;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.BPMMetadataHelper.detectRepLanguage;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.BPMMetadataHelper.extractAnnotations;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.BPMMetadataHelper.getDeclaredAssetTypes;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.BPMMetadataHelper.getDefaultAssetType;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.BPMMetadataHelper.getRepLanguage;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.BPMMetadataHelper.tryAddKommunicatorLink;
import static edu.mayo.kmdp.util.DateTimeUtil.parseDateTime;
import static edu.mayo.kmdp.util.JSonUtil.writeJsonAsString;
import static edu.mayo.kmdp.util.Util.isNotEmpty;
import static edu.mayo.kmdp.util.XMLUtil.asAttributeStream;
import static edu.mayo.kmdp.util.XMLUtil.asElementStream;
import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.id.IdentifierConstants.VERSION_ZERO_SNAPSHOT;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newId;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newVersionId;
import static org.omg.spec.api4kp._20200801.id.VersionIdentifier.toSemVer;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.defaultSurrogateUUID;
import static org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries.Care_Process_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries.Clinical_Case_Management_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries.Clinical_Decision_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries.Clinical_Rule;
import static org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries.Depends_On;
import static org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries.Imports;
import static org.omg.spec.api4kp._20200801.taxonomy.iso639_2_languagecode._20190201.Language.English;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeartifactcategory._2020_01_20.KnowledgeArtifactCategory.Software;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries.Assessment_Predictive_And_Inferential_Models;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries.Plans_Processes_Pathways_And_Protocol_Definitions;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries.Rules_Policies_And_Guidelines;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries.Terminology_Ontology_And_Assertional_KBs;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Case_Management_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Decision_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Formal_Ontology;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Lexicon;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Protocol;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.JSON;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.asEnum;
import static org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatusSeries.Draft;
import static org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatusSeries.Final_Draft;
import static org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatusSeries.Published;
import static org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatusSeries.Unpublished;

import edu.mayo.kmdp.trisotechwrapper.TTAPIAdapter;
import edu.mayo.kmdp.trisotechwrapper.components.NamespaceManager;
import edu.mayo.kmdp.trisotechwrapper.components.SemanticModelInfo;
import edu.mayo.kmdp.trisotechwrapper.components.redactors.Redactor;
import edu.mayo.kmdp.trisotechwrapper.components.weavers.Weaver;
import edu.mayo.kmdp.trisotechwrapper.config.TTWEnvironmentConfiguration;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.util.Util;
import edu.mayo.kmdp.util.XPathUtil;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import org.omg.spec.api4kp._20200801.id.IdentifierConstants;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.services.repository.asset.KARSHrefBuilder;
import org.omg.spec.api4kp._20200801.surrogate.Annotation;
import org.omg.spec.api4kp._20200801.surrogate.Dependency;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.Link;
import org.omg.spec.api4kp._20200801.surrogate.Publication;
import org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategory;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetType;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Extract the data from the woven (by the Weaver) document to create KnowledgeAsset from model
 * data.
 */
@Component
public class BPMModelIntrospector {

  private static final Logger logger = LoggerFactory
      .getLogger(BPMModelIntrospector.class);

  @Autowired
  TTAPIAdapter client;

  @Autowired
  NamespaceManager names;

  @Autowired(required = false)
  TTWEnvironmentConfiguration config;

  @Autowired(required = false)
  KARSHrefBuilder hrefBuilder;

  private final XPathUtil xPathUtil = new XPathUtil();

  public BPMModelIntrospector() {
    //
  }

  public BPMModelIntrospector(TTAPIAdapter client, NamespaceManager names) {
    this.client = client;
    this.names = names;
  }

  @PostConstruct
  void init() {
    if (config == null) {
      config = new TTWEnvironmentConfiguration();
    }
  }


  /**
   * Generates a {@link KnowledgeAsset} Surrogate from the introspection of a BPM+ model, combined
   * with the information in its corresponding Trisotech's internal manifest, assuming that the
   * model is a manifestation of the Asset with the given ID.
   * <p>
   * FUTURE: Currently supports DMN and CMMN models, but not BPMN
   * <p>
   * Note that, at this point, the model document has already been standardized using the
   * {@link Redactor}, and (re)annotated using the {@link Weaver}
   *
   * @return a KnowledgeAsset surrogate with metadata for that model
   */
  public KnowledgeAsset extractSurrogateFromDocument(
      Map<SemanticModelInfo, Document> carriers,
      ResourceIdentifier assetId) {

    var carrierMetadata = buildCarrierMetadata(carriers, assetId);

    var formalTypes = gatherFormalTypes(carriers);
    var formalCategory = inferFormalCategory(assetId, formalTypes);

    var annotations = gatherAnnotations(carriers);

    var name = carriers.keySet().stream()
        .findFirst().map(manifest -> manifest.getName().trim())
        .orElse(null);

    // asset<->asset relations
    // assets are derived from the artifact relations
    var linkedAssets = gatherAssetImports(carriers.keySet());

    // towards the ideal
    var surr = new org.omg.spec.api4kp._20200801.surrogate.resources.KnowledgeAsset()
        .withAssetId(assetId)
        .withName(name)
        .withFormalCategory(formalCategory)
        .withFormalType(formalTypes)
        .withLifecycle(getAssestPublicationStatus(carrierMetadata))
        .withLinks(mergeSortedLinks(
            mergeSortedLinks(
                getRelatedAssets(linkedAssets),
                getRelatedServices(carriers.keySet())),
            getOtherDependencies(carriers)))
        // carriers
        .withCarriers(carrierMetadata)
        .withSurrogate(
            buildSurrogateSelf(assetId));

    addSemanticAnnotations(surr, annotations);

    if (logger.isTraceEnabled()) {
      logger.trace(
          "surrogate in JSON format: {} ", writeJsonAsString(surr).orElse("n/a"));
    }

    return surr;
  }


  private Collection<Link> getOtherDependencies(Map<SemanticModelInfo, Document> artifacts) {
    return artifacts.entrySet().stream()
        .flatMap(e -> detectRepLanguage(e.getKey())
            .map(lang -> getOtherDependencies(e.getValue(), lang).stream())
            .orElseGet(Stream::empty))
        .distinct()
        .collect(toList());
  }

  private Publication getAssestPublicationStatus(List<KnowledgeArtifact> carrierMetadata) {
    var allUnpublished = carrierMetadata.stream()
        .map(KnowledgeArtifact::getLifecycle)
        .allMatch(s -> Unpublished.sameAs(s.getPublicationStatus()));
    return new Publication()
        .withPublicationStatus(allUnpublished ? Unpublished : Published);
  }

  private List<Annotation> gatherAnnotations(Map<SemanticModelInfo, Document> carriers) {
    return carriers.values().stream()
        .flatMap(dox -> extractAnnotations(dox.getDocumentElement()))
        .distinct()
        .collect(Collectors.toCollection(LinkedList::new));
  }

  private KnowledgeArtifact buildSurrogateSelf(ResourceIdentifier assetId) {
    return new KnowledgeArtifact()
        .withArtifactId(newId(
            names.getArtifactNamespace(),
            defaultSurrogateUUID(assetId, Knowledge_Asset_Surrogate_2_0),
            toSemVer(assetId.getVersionTag())))
        .withRepresentation(rep(Knowledge_Asset_Surrogate_2_0, JSON))
        .withMimeType(codedRep(Knowledge_Asset_Surrogate_2_0, JSON));
  }

  private List<KnowledgeAssetType> gatherFormalTypes(Map<SemanticModelInfo, Document> carriers) {
    return carriers.keySet().stream()
        .flatMap(manifest -> getDeclaredAssetTypes(manifest,
            () -> getDefaultAssetType(manifest.getMimetype())).stream())
        .distinct()
        .collect(toList());
  }

  private List<KnowledgeArtifact> buildCarrierMetadata(Map<SemanticModelInfo, Document> carriers,
      ResourceIdentifier assetID) {
    return carriers.entrySet().stream()
        .flatMap(e -> buildArtifact(assetID, e.getKey()))
        .collect(toList());
  }

  private Stream<KnowledgeArtifact> buildArtifact(
      ResourceIdentifier assetID, SemanticModelInfo manifest) {
    // Publication Status
    var lifecycle = getArtifactPublicationStatus(manifest);
    // Identifiers
    var artifactId = extractArtifactId(manifest);

    // get the language for the document to set the appropriate values
    var synRep = getRepLanguage(manifest)
        .orElseThrow(() -> new IllegalStateException("Invalid language detected"));

    var artifactDependencies = Stream.concat(
        getRelatedArtifacts(getArtifactImports(manifest)).stream(),
        getRelatedServices(Set.of(manifest)).stream())
        .collect(Collectors.toList());

    var model = buildDefaultCarrierMetadata(
        assetID,
        artifactId,
        manifest.getName().trim(),
        lifecycle,
        synRep,
        artifactDependencies);

    var preview = tryAddKommunicatorLink(
        artifactId, manifest, lifecycle.getPublicationStatus(), config);

    return Stream.concat(Stream.of(model), preview.stream());
  }

  /**
   * Constructs the default KnowledgeArtifact carrier metadata object given the parameters.
   * <p>
   * The default carrier is the BPM+ model exportable, usually in standard XML, from the DES server
   *
   * @param assetId               the asset ID
   * @param artifactId            the artifact ID
   * @param name                  the model name
   * @param lifecycle             the publication status and dates
   * @param synRep                the artifact {@link SyntacticRepresentation}
   * @param artifactRelationships the artifact dependencies
   * @return a {@link KnowledgeArtifact} carrier metadata object
   */
  private KnowledgeArtifact buildDefaultCarrierMetadata(
      ResourceIdentifier assetId,
      ResourceIdentifier artifactId,
      String name,
      Publication lifecycle,
      SyntacticRepresentation synRep,
      List<Link> artifactRelationships) {
    var carrier = new KnowledgeArtifact()
        .withArtifactId(artifactId)
        .withName(name)
        .withLifecycle(lifecycle)
        .withLocalization(English)
        .withExpressionCategory(Software)
        .withRepresentation(synRep)
        .withMimeType(codedRep(synRep))
        .withLinks(artifactRelationships);
    tryLocateDefaultArtifact(assetId)
        .ifPresent(carrier::withLocator);
    return carrier;
  }

  /**
   * Maps the asset ID to the default location of the default artifact on this Asset Repository
   * server, if deployed as a web service.
   * <p>
   * Maps to [base URL]/cat/assets/{assetId}/versions/{assetVersion}/carrier/content
   *
   * @param assetID the asset ID.
   * @return the URL, as a URI, if able to determine
   */
  private Optional<URI> tryLocateDefaultArtifact(
      ResourceIdentifier assetID) {
    if (hrefBuilder == null) {
      return Optional.empty();
    }
    try {
      return Optional.of(
          hrefBuilder.getAssetDefaultContent(
              assetID.getUuid(), assetID.getVersionTag()).toURI());
    } catch (URISyntaxException e) {
      logger.warn(e.getMessage(), e);
      return Optional.empty();
    }
  }

  /* ----------------------------------------------------------------------------------------- */

  /**
   * Creates an Artifact Id for the given model.
   * <p>
   * The Artifact Id is derived from the Trisotech native model Id. At this point, the namespace has
   * already been rewritten. This method further parses the Id, combining it with the version (tag)
   * and a time stamp.
   *
   * @param manifest the Trisotech metadata
   * @return the structured Artifact Id, as a {@link ResourceIdentifier}
   */
  public ResourceIdentifier extractArtifactId(SemanticModelInfo manifest) {
    String docId = manifest.getId();
    if (docId == null) {
      // error out. Can't proceed w/o Artifact -- How did we get this far?
      throw new IllegalStateException("Failed to find artifactId in Manifest");
    }

    return names.modelToArtifactId(
        docId,
        manifest.getVersion(),
        manifest.getName(),
        parseDateTime(manifest.getUpdated()));
  }



  /* ----------------------------------------------------------------------------------------- */

  private KnowledgeAssetCategorySeries inferFormalCategory(
      ResourceIdentifier assetId, List<KnowledgeAssetType> formalTypes) {
    var categories = formalTypes.stream()
        .map(this::inferFormalCategory)
        .collect(Collectors.toSet());
    if (categories.size() > 1) {
      logger.warn("Detected multiple categories in asset {} : {}, due to types {}",
          assetId, categories, formalTypes);
    }
    if (categories.isEmpty()) {
      logger.error("Unable to determine formal category for asset {}, given types {}",
          assetId, formalTypes);
      return null;
    }
    return categories.iterator().next();
  }

  /**
   * Determines the {@link KnowledgeAssetCategory} for a given {@link KnowledgeAssetType}, based on
   * the hierarchies defined in the API4KP ontology.
   *
   * @param formalType the {@link KnowledgeAssetType}
   * @return the broader, parent {@link KnowledgeAssetCategory}
   */
  private KnowledgeAssetCategorySeries inferFormalCategory(KnowledgeAssetType formalType) {
    if (isA(formalType, Clinical_Rule)) {
      return Rules_Policies_And_Guidelines;
    }
    if (isA(formalType, Case_Management_Model) || isA(formalType, Clinical_Case_Management_Model)) {
      return Plans_Processes_Pathways_And_Protocol_Definitions;
    }
    if (isA(formalType, Decision_Model) || isA(formalType, Clinical_Decision_Model)) {
      return Assessment_Predictive_And_Inferential_Models;
    }
    if (isA(formalType, Protocol) || isA(formalType, Care_Process_Model)) {
      return Plans_Processes_Pathways_And_Protocol_Definitions;
    }
    if (isA(formalType, Lexicon) || isA(formalType, Formal_Ontology)) {
      return Terminology_Ontology_And_Assertional_KBs;
    }
    throw new UnsupportedOperationException(
        "Unable to infer category for asset type " + formalType.getName());
  }

  /**
   * Determines whether subType is equivalent to, or subclass of, superType
   *
   * @param subType   the candidate narrower concept
   * @param superType the candidate broader concept
   * @return true if subType is narrower or equal to superType
   */
  private boolean isA(KnowledgeAssetType subType, KnowledgeAssetType superType) {
    return subType.sameTermAs(superType)
        || Arrays.stream(subType.getAncestors()).anyMatch(t -> t.sameTermAs(superType));
  }

  /* ----------------------------------------------------------------------------------------- */


  private Collection<Link> getRelatedServices(Set<SemanticModelInfo> manifest) {
    return manifest.stream()
        .flatMap(info -> info.getExposedServices().stream())
        .flatMap(svcKey -> client.getMetadataByAssetId(svcKey.getUuid(), svcKey.getVersionTag()))
        .map(info -> names.assetKeyToId(info.getServiceKey())
            .withName(info.getName()))
        .map(svc -> new Dependency()
            .withHref(svc)
            .withRel(DependencyTypeSeries.Is_Supplemented_By))
        .collect(toList());
  }

  private List<Link> getOtherDependencies(
      Document woven,
      KnowledgeRepresentationLanguage language) {
    Stream<Attr> assetURIs;
    switch (asEnum(language)) {
      case DMN_1_2:
        assetURIs = asAttributeStream(xPathUtil.xList(woven,
            "//dmn:knowledgeSource[not(./dmn:type='*/*')]/@locationURI"));
        break;
      case CMMN_1_1:
        assetURIs = asAttributeStream(xPathUtil.xList(woven,
            "//cmmn:caseFileItemDefinition[@definitionType='http://www.omg.org/spec/CMMN/DefinitionType/CMISDocument']/@structureRef"));
        break;
      default:
        assetURIs = Stream.empty();
    }

    List<Link> links = assetURIs
        .map(Attr::getValue)
        .filter(Util::isNotEmpty)
        // only supported URIs
        .filter(str -> str.startsWith("urn:")
            || str.startsWith(names.getAssetNamespace().toString())
            || str.startsWith("assets:"))
        .map(this::normalizeQualifiedName)
        .map(URI::create)
        .map(SemanticIdentifier::newVersionId)
        .map(id -> {
          if (id.getVersionId() == null) {
            String separator = id.getResourceId().toString().startsWith("urn") ? ":" : "/";
            return newVersionId(
                URI.create(
                    id.getNamespaceUri().toString() + separator + UUID.fromString(id.getTag())),
                VERSION_ZERO_SNAPSHOT);
          } else {
            return id;
          }
        })
        .map(id -> {
          String label = ofNullable(xPathUtil.xNode(woven,
              "//*[@locationURI='" + id + "' or @structureRef='" + id + "']/@name"))
              .map(Node::getNodeValue)
              .orElse(null);
          return id.withName(label);
        })
        .map(id -> new Dependency().withRel(Depends_On).withHref(id))
        .collect(Collectors.toList());

    Stream<Element> linkElements;
    switch (asEnum(language)) {
      case CMMN_1_1:
        linkElements = asElementStream(xPathUtil.xList(woven,
            "//cmmn:processTask/cmmn:processRefExpression"));
        break;
      default:
        linkElements = Stream.empty();
    }
    linkElements
        .map(n -> n.getTextContent().trim().split(" "))
        .filter(s -> DependencyTypeSeries.resolveTag(s[0]).isPresent())
        .map(s -> new Dependency()
            .withRel(DependencyTypeSeries.resolveTag(s[0]).get())
            .withHref(SemanticIdentifier.newVersionId(URI.create(s[1]))))
        .forEach(links::add);

    return links;
  }

  private String normalizeQualifiedName(String id) {
    if (id.startsWith("assets:")) {
      String str = id.replace("assets:", names.getAssetNamespace().toString());
      if (str.contains(":")) {
        str = str.replace(":", IdentifierConstants.VERSIONS);
      }
      return str;
    } else {
      return id;
    }
  }

  private List<ResourceIdentifier> getArtifactImports(SemanticModelInfo manifest) {
    return manifest.getModelDependencies().stream()
        .flatMap(modelUri -> client.getMetadataByModelId(modelUri).stream())
        .map(info -> names.modelToArtifactId(info))
        .collect(toList());
  }


  private List<ResourceIdentifier> gatherAssetImports(Set<SemanticModelInfo> carriers) {
    return carriers.stream()
        .flatMap(info -> info.getModelDependencies().stream()
            .flatMap(modelRef -> client.getMetadataByModelId(modelRef)
                .map(ref ->
                    names.assetKeyToId(ref.getAssetKey()).withName(ref.getName()))
                .stream()))
        .distinct()
        .collect(toList());
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


  private Collection<Link> mergeSortedLinks(Collection<Link> link1, Collection<Link> link2) {
    return Stream.concat(
            link1.stream(),
            link2.stream())
        .sorted(comparing(l -> l.getHref().getTag()))
        .collect(Collectors.toList());
  }

  /* ----------------------------------------------------------------------------------------- */

  /**
   * Converts the Trisotech internal publication status to the platform {@link Publication}
   * <p>
   * Note that the instance data reflects, strictly speaking, the publication status of the model
   * Artifact, not the carried Asset itself
   *
   * @param manifest the internal model manifest.
   * @return a {@link Publication} object, to be used as a {@link KnowledgeAsset} surrogate fragment
   */
  private Publication getArtifactPublicationStatus(TrisotechFileInfo manifest) {
    Publication lifecycle = new Publication();
    if (isNotEmpty(manifest.getState())) {
      switch (manifest.getState()) {
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
          throw new IllegalStateException("Unrecognized state " + manifest.getState());
      }
    } else {
      lifecycle.withPublicationStatus(Unpublished);
    }
    logger.trace("lifecycle = {}", lifecycle.getPublicationStatus());

    return lifecycle;
  }
}