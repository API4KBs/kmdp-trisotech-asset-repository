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
import static edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.BPMMetadataHelper.dependencyRel;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.BPMMetadataHelper.detectRepLanguage;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.BPMMetadataHelper.extractAnnotations;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.BPMMetadataHelper.getDeclaredAssetTypes;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.BPMMetadataHelper.getDefaultAssetType;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.BPMMetadataHelper.getDefaultRepresentation;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.BPMMetadataHelper.tryAddKommunicatorArtifact;
import static edu.mayo.kmdp.trisotechwrapper.config.TTConstants.ASSETS_PREFIX;
import static edu.mayo.kmdp.trisotechwrapper.config.TTNotations.OPENAPI_JSON;
import static edu.mayo.kmdp.util.JSonUtil.writeJsonAsString;
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
import static org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries.Clinical_Inference_Rule;
import static org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries.Clinical_Rule;
import static org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries.Depends_On;
import static org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries.Is_Supplemented_By;
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
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.ReSTful_Service_Specification;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.JSON;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.asEnum;
import static org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatusSeries.Draft;
import static org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatusSeries.Final_Draft;
import static org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatusSeries.Published;
import static org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatusSeries.Unpublished;

import edu.mayo.kmdp.registry.Registry;
import edu.mayo.kmdp.trisotechwrapper.TTAPIAdapter;
import edu.mayo.kmdp.trisotechwrapper.components.NamespaceManager;
import edu.mayo.kmdp.trisotechwrapper.components.SemanticModelInfo;
import edu.mayo.kmdp.trisotechwrapper.components.redactors.Redactor;
import edu.mayo.kmdp.trisotechwrapper.components.weavers.Weaver;
import edu.mayo.kmdp.trisotechwrapper.config.TTWEnvironmentConfiguration;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechPublicationStates;
import edu.mayo.kmdp.util.Util;
import edu.mayo.kmdp.util.XPathUtil;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.omg.spec.api4kp._20200801.id.IdentifierConstants;
import org.omg.spec.api4kp._20200801.id.Pointer;
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
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Default implementation of @{@link ModelIntrospector}
 */
public class BPMModelIntrospector implements ModelIntrospector {

  /**
   * Logger
   */
  private static final Logger logger = LoggerFactory
      .getLogger(BPMModelIntrospector.class);

  /**
   * {@link TTAPIAdapter}, used to resolve model/model dependencies
   */
  @Nonnull
  protected final TTAPIAdapter client;

  /**
   * {@link NamespaceManager}, used to map internal IDs to enterprise IDs
   */
  @Nonnull
  protected final NamespaceManager names;

  /**
   * Environment configuration
   */
  @Nonnull
  protected final TTWEnvironmentConfiguration config;

  /**
   * Optional HrefBuilder, used for {@link KnowledgeArtifact#setLocator(URI)} URLs
   */
  @Nullable
  protected final KARSHrefBuilder hrefBuilder;

  /**
   * Utility used to extract elements from a Model document
   */
  @Nonnull
  private final XPathUtil xPathUtil = new XPathUtil();

  public BPMModelIntrospector(
      @Nonnull TTWEnvironmentConfiguration config,
      @Nonnull NamespaceManager names,
      @Nonnull TTAPIAdapter client,
      @Nullable KARSHrefBuilder hrefBuilder) {
    this.config = config;
    this.client = client;
    this.names = names;
    this.hrefBuilder = hrefBuilder;
  }


  /**
   * Generates a {@link KnowledgeAsset} Surrogate from the introspection of a BPM+ model, combined
   * with the information in its corresponding Trisotech internal manifest, assuming that the model
   * is a manifestation of the Asset with the given ID.
   * <p>
   * Note that, at this point, the model document has already been standardized using the
   * {@link Redactor}, and (re)annotated using the {@link Weaver}
   *
   * @return a KnowledgeAsset surrogate with metadata for that model
   */
  @Nonnull
  public Optional<KnowledgeAsset> introspectAsModel(
      @Nonnull ResourceIdentifier assetId,
      @Nonnull Map<SemanticModelInfo, Document> carriers) {

    var carrierMetadata = buildCarrierMetadata(assetId, carriers);

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
        .withLifecycle(getAssetPublicationStatus(carrierMetadata))
        .withLinks(mergeSortedLinks(
            mergeSortedLinks(
                toAssetRelationships(linkedAssets),
                gatherRelatedServices(carriers.keySet())),
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

    return Optional.of(surr);
  }


  /**
   * Infers the Asset/Asset dependencies implied by specific uses of the modeling elements in an
   * Asset's carriers
   *
   * @param carriers the Manifest/Model Map of the Asset's carriers
   * @return a Collection of Asset/Asset dependencies
   * @see #getOtherDependencies(Document, KnowledgeRepresentationLanguage)
   */
  @Nonnull
  protected Collection<Link> getOtherDependencies(
      @Nonnull final Map<SemanticModelInfo, Document> carriers) {
    return carriers.entrySet().stream()
        .flatMap(e -> detectRepLanguage(e.getKey())
            .map(lang -> getOtherDependencies(e.getValue(), lang).stream())
            .orElseGet(Stream::empty))
        .distinct()
        .collect(toList());
  }

  /**
   * Aggregates the Publication Statuses of an Asset's Carriers, to infer the Asset's publication
   * status. Considers the Asset Published, unless all Carriers are Unpublished
   *
   * @param carrierMetadata the metadata for the Carriers
   * @return Unpublished if all Artifacts are Unpublished, Published otherwise
   */
  @Nonnull
  protected Publication getAssetPublicationStatus(
      @Nonnull final List<KnowledgeArtifact> carrierMetadata) {
    var allUnpublished = carrierMetadata.stream()
        .map(KnowledgeArtifact::getLifecycle)
        .allMatch(s -> Unpublished.sameAs(s.getPublicationStatus()));
    return new Publication()
        .withPublicationStatus(allUnpublished ? Unpublished : Published);
  }

  /**
   * Extracts all the semantic Annotations from an Asset's Carriers
   *
   * @param carriers the Manifest/Model Map of the Asset's carriers
   * @return the Annotations on the Models
   * @see BPMMetadataHelper#extractAnnotations(Element)
   */
  @Nonnull
  protected List<Annotation> gatherAnnotations(
      @Nonnull final Map<SemanticModelInfo, Document> carriers) {
    return carriers.values().stream()
        .flatMap(dox -> extractAnnotations(dox.getDocumentElement()))
        .distinct()
        .collect(Collectors.toCollection(LinkedList::new));
  }

  /**
   * Builds the {@link KnowledgeArtifact} descriptor for the {@link KnowledgeAsset} Surrogate
   * created by this introspector
   *
   * @param assetId the ID of the Asset
   * @return the {@link KnowledgeArtifact} that describes this Surrogate ('self')
   */
  protected KnowledgeArtifact buildSurrogateSelf(
      ResourceIdentifier assetId) {
    var surr = new KnowledgeArtifact()
        .withArtifactId(newId(
            names.getArtifactNamespace(),
            defaultSurrogateUUID(assetId, Knowledge_Asset_Surrogate_2_0),
            toSemVer(assetId.getVersionTag())))
        .withName("(( Metadata Record - Self ))")
        .withRepresentation(rep(Knowledge_Asset_Surrogate_2_0, JSON))
        .withMimeType(codedRep(Knowledge_Asset_Surrogate_2_0, JSON));
    tryLocateDefaultSurrogate(assetId)
        .ifPresent(surr::withLocator);
    return surr;
  }

  /**
   * Gathers all of an Asset's types, as inferred from or asserted in that Asset's Carriers
   *
   * @param carriers the Manifest/Model Map of the Asset's carriers
   * @return the distinct {@link KnowledgeAssetType} from all the Carriers
   * @see BPMMetadataHelper#getDeclaredAssetTypes(SemanticModelInfo, Supplier)
   */
  @Nonnull
  protected List<KnowledgeAssetType> gatherFormalTypes(
      @Nonnull final Map<SemanticModelInfo, Document> carriers) {
    return carriers.keySet().stream()
        .flatMap(manifest -> getDeclaredAssetTypes(manifest,
            () -> getDefaultAssetType(manifest.getMimetype())).stream())
        .distinct()
        .collect(toList());
  }

  /**
   * Builds the {@link KnowledgeArtifact} metadata for all of an Asset's Carrier
   *
   * @param assetID  the ID of the Asset
   * @param carriers the Manifest/Model Map of the Asset's carriers
   * @return the {@link KnowledgeArtifact} metadata for all the Carrier variants
   * @see #buildArtifact(ResourceIdentifier, SemanticModelInfo)
   */
  @Nonnull
  protected List<KnowledgeArtifact> buildCarrierMetadata(
      @Nonnull final ResourceIdentifier assetID,
      @Nonnull final Map<SemanticModelInfo, Document> carriers) {
    return carriers.entrySet().stream()
        .flatMap(e -> buildArtifact(assetID, e.getKey()))
        .collect(toList());
  }

  /**
   * Builds {@link KnowledgeArtifact} metadata for an Asset's Carrier
   * <p>
   * Creates a {@link KnowledgeArtifact} for the Model, as well as its Kommunicator variant
   *
   * @param assetID  the ID of the Asset
   * @param manifest the Manifest of the carrier model, plus the Kommunicator variant, if
   *                 detectable
   * @return the Carrier variants
   */
  @Nonnull
  protected Stream<KnowledgeArtifact> buildArtifact(
      @Nonnull final ResourceIdentifier assetID,
      @Nonnull final SemanticModelInfo manifest) {
    // Publication Status
    var lifecycle = getArtifactPublicationStatus(manifest);
    // Identifiers
    var artifactId = names.modelToArtifactId(manifest);

    // get the language for the document to set the appropriate values
    var synRep = getDefaultRepresentation(manifest)
        .orElseThrow(() -> new IllegalStateException("Invalid language detected"));

    var artifactDependencies = Stream.concat(
            toArtifactRelationships(gatherArtifactImports(manifest)).stream(),
            gatherRelatedServices(Set.of(manifest)).stream())
        .collect(Collectors.toList());

    var model = buildDefaultCarrierMetadata(
        assetID,
        artifactId,
        manifest.getName().trim(),
        lifecycle,
        synRep,
        artifactDependencies);

    var preview = tryAddKommunicatorArtifact(
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
  @Nonnull
  protected KnowledgeArtifact buildDefaultCarrierMetadata(
      @Nonnull final ResourceIdentifier assetId,
      @Nonnull final ResourceIdentifier artifactId,
      @Nonnull final String name,
      @Nonnull final Publication lifecycle,
      @Nonnull final SyntacticRepresentation synRep,
      @Nonnull final List<Link> artifactRelationships) {
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
  @Nonnull
  protected Optional<URI> tryLocateDefaultArtifact(
      @Nonnull final ResourceIdentifier assetID) {
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

  /**
   * Maps the asset ID to the default location of the canonical Surrogate (ie the one built by this
   * component) on this Asset Repository server, if deployed as a web service.
   * <p>
   * Maps to [base URL]/cat/assets/{assetId}/versions/{assetVersion}/surrogate/content
   * <p>
   * FIXME: there is no surrogate-oriented counterpart of hrefBuilder#getAssetDefaultContent yet
   *
   * @param assetID the asset ID.
   * @return the URL, as a URI, if able to determine
   */
  @Nonnull
  protected Optional<URI> tryLocateDefaultSurrogate(
      @Nonnull final ResourceIdentifier assetID) {
    return tryLocateDefaultArtifact(assetID)
        .map(URI::toString)
        .map(s -> s.replace("/carrier/", "/surrogate/"))
        .map(URI::create);
  }



  /* ----------------------------------------------------------------------------------------- */

  /**
   * Determines the {@link KnowledgeAssetCategory} for a set of {@link KnowledgeAssetType}s, based
   * on the hierarchies defined in the API4KP ontology.
   *
   * @param assetId     the Asset for which to infer the category
   * @param formalTypes the {@link KnowledgeAssetType}s
   * @return the broader, parent {@link KnowledgeAssetCategory}
   */
  @Nullable
  protected KnowledgeAssetCategorySeries inferFormalCategory(
      @Nonnull final ResourceIdentifier assetId,
      @Nonnull final List<KnowledgeAssetType> formalTypes) {
    var categories = formalTypes.stream()
        .distinct()
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
  @Nonnull
  protected KnowledgeAssetCategorySeries inferFormalCategory(
      @Nonnull final KnowledgeAssetType formalType) {
    if (isA(formalType, Clinical_Rule)) {
      return Rules_Policies_And_Guidelines;
    }
    if (isA(formalType, Case_Management_Model) || isA(formalType, Clinical_Case_Management_Model)) {
      return Plans_Processes_Pathways_And_Protocol_Definitions;
    }
    if (isA(formalType, Decision_Model) || isA(formalType, Clinical_Decision_Model)
        || isA(formalType, Clinical_Inference_Rule)) {
      return Assessment_Predictive_And_Inferential_Models;
    }
    if (isA(formalType, Protocol) || isA(formalType, Care_Process_Model)) {
      return Plans_Processes_Pathways_And_Protocol_Definitions;
    }
    if (isA(formalType, Lexicon) || isA(formalType, Formal_Ontology)) {
      return Terminology_Ontology_And_Assertional_KBs;
    }
    if (isA(formalType, ReSTful_Service_Specification)) {
      return Rules_Policies_And_Guidelines;
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
  protected boolean isA(
      @Nonnull final KnowledgeAssetType subType,
      @Nonnull final KnowledgeAssetType superType) {
    return subType.sameTermAs(superType)
        || Arrays.stream(subType.getAncestors()).anyMatch(t -> t.sameTermAs(superType));
  }

  /* ----------------------------------------------------------------------------------------- */

  /**
   * Gathers the Asset/Service Asset relationships for a given Model, across the Carriers of an
   * Asset
   *
   * @param manifests the Manifest of the source Model
   * @return the IDs of the Service Assets that the Models expose
   */
  @Nonnull
  protected Collection<Link> gatherRelatedServices(
      @Nonnull final Set<SemanticModelInfo> manifests) {
    return manifests.stream()
        .flatMap(info -> info.getExposedServices().stream())
        .flatMap(svcKey -> client.getMetadataByAssetId(svcKey.getUuid(), svcKey.getVersionTag()))
        .map(info -> names.assetKeyToId(info.getServiceKey())
            .withName(info.getName()))
        .map(svc -> new Dependency()
            .withHref(svc)
            .withRel(dependencyRel(OPENAPI_JSON.getMimeType())))
        .collect(toList());
  }

  /**
   * Gathers the Artifact/Artifact import relationships for a given model
   *
   * @param manifest the Manifest of the source Model
   * @return the IDs of the Artifacts that the Model depends on
   */
  @Nonnull
  protected List<Pointer> gatherArtifactImports(
      @Nonnull final SemanticModelInfo manifest) {
    return manifest.getModelDependencies().stream()
        .flatMap(modelUri -> client.getMetadataByModelId(modelUri).stream())
        .map(mf -> names.modelToArtifactId(mf)
            .toInnerPointer()
            .withMimeType(mf.getMimetype()))
        .collect(toList());
  }


  /**
   * Gathers the Asset/Asset dependency relationships for a given model, as inferred from the
   * Carriers of the source Asset. Considers both direct and reverse relationships
   *
   * @param carriers the Manifests of the Models that carry the given Asset
   * @return the IDs of the Assets that the source Asset depends on, grouped by a flag that denotes
   * direct dependencies (true) vs reverse dependencies (false)
   */
  @Nonnull
  protected Map<Boolean, List<Pointer>> gatherAssetImports(
      @Nonnull final Set<SemanticModelInfo> carriers) {
    var direct = gatherAssetImports(carriers, SemanticModelInfo::getModelDependencies);
    var reverse = gatherAssetImports(carriers, SemanticModelInfo::getReverseModelDependencies);
    return Map.of(
        Boolean.TRUE, direct,
        Boolean.FALSE, reverse);
  }


  /**
   * Gathers the Asset/Asset dependency relationships for a given model, as inferred from the
   * Carriers of the source Asset, for a given mapping between the model manifest and its
   * dependencies
   *
   * @param carriers           the Manifests of the Models that carry the given Asset
   * @param dependencySelector the mapping between a Model manifest and its asset dependencies
   * @return the IDs of the Assets that the source Asset depends on
   */
  @Nonnull
  private List<Pointer> gatherAssetImports(
      @Nonnull final Set<SemanticModelInfo> carriers,
      @Nonnull final Function<SemanticModelInfo, Set<String>> dependencySelector) {
    return carriers.stream()
        .flatMap(info -> dependencySelector.apply(info).stream()
            .flatMap(modelRef -> client.getMetadataByModelId(modelRef)
                .filter(ref -> ref.getAssetKey() != null)
                .map(ref ->
                    names.assetKeyToId(ref.getAssetKey())
                        .toInnerPointer()
                        .withName(ref.getName())
                        .withMimeType(ref.getMimetype()))
                .stream()))
        .distinct()
        .collect(toList());
  }

  /**
   * Creates a collection of Dependency {@link Link} for a set of Artifact/Artifact dependencies
   *
   * @param importedArtifacts The list of artifact IDs for the imports to the current version of the
   *                          model being processed
   * @return Links to the given Artifacts, as Imports
   */
  @Nonnull
  protected Collection<Link> toArtifactRelationships(
      @Nonnull final List<Pointer> importedArtifacts) {
    return importedArtifacts.stream()
        .filter(Objects::nonNull)
        .map(ptr -> new Dependency()
            .withRel(dependencyRel(ptr.getMimeType()))
            .withHref(ptr))
        .collect(toList());
  }

  /**
   * Creates a collection of Dependency {@link Link} for a set of Asset/Asset dependencies
   * <p>
   * Similar to the relatedArtifacts, related Assets are the asset identifiers for the artifacts
   * that are imported or used (dependency) of the current processed artifact. Want to retain the
   * identifiers.
   *
   * @param importedAssets the list of assets this a Model depends on
   * @return Links to the given Assets, as Imports
   */
  @Nonnull
  protected Collection<Link> toAssetRelationships(
      @Nonnull final Map<Boolean, List<Pointer>> importedAssets) {
    var direct = importedAssets.getOrDefault(Boolean.TRUE, Collections.emptyList()).stream()
        .map(ptr ->
            new Dependency()
                .withRel(dependencyRel(ptr.getMimeType()))
                .withHref(ptr));
    var reverse = importedAssets.getOrDefault(Boolean.FALSE, Collections.emptyList()).stream()
        .map(ptr ->
            new Dependency()
                .withRel(Is_Supplemented_By)
                .withHref(ptr));
    return Stream.concat(direct, reverse).collect(toList());
  }


  /**
   * Combines two collections of related links, sorting by relationship type
   *
   * @param first  the first collection
   * @param second the second collection
   * @return a merged, sorted collection
   */
  @Nonnull
  protected Collection<Link> mergeSortedLinks(
      @Nonnull final Collection<Link> first,
      @Nonnull final Collection<Link> second) {
    return Stream.concat(
            first.stream(),
            second.stream())
        .sorted(comparing(l -> l.getHref().getTag()))
        .collect(Collectors.toList());
  }

  /**
   * Detects Asset/Asset dependencies that are not implied by the modeling language, but rather by
   * the ad-hoc use of modeling elements.
   * <p>
   * Supports Assets linked through dmn:KnowledgeSource#locationURI (semi-standard) and
   * cmmn:CaseFileItems with CMIS type (non-standard)
   *
   * @param woven    the Model Document (after processing by the {@link Weaver})
   * @param language the Model Language (supports DMN 1.2 and CMMN 1.1)
   * @return any Asset dependencies detected in the Model elements
   */
  @Nonnull
  protected List<Link> getOtherDependencies(
      @Nonnull final Document woven,
      @Nonnull final KnowledgeRepresentationLanguage language) {
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
        .filter(str -> Registry.isGlobalIdentifier(str)
            || str.startsWith(names.getAssetNamespace().toString())
            || str.startsWith(ASSETS_PREFIX))
        .map(this::normalizeQualifiedName)
        .map(URI::create)
        .map(SemanticIdentifier::newVersionId)
        .map(id -> {
          if (id.getVersionId() == null) {
            String separator = Registry.isGlobalIdentifier(id.getResourceId()) ? ":" : "/";
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
      case DMN_1_2:
      case BPMN_2_0:
      default:
        linkElements = Stream.empty();
    }
    linkElements
        .map(n -> n.getTextContent().trim().split(" "))
        .filter(s -> DependencyTypeSeries.resolveTag(s[0]).isPresent())
        .map(s -> new Dependency()
            .withRel(DependencyTypeSeries.resolveTag(s[0]).orElse(Depends_On))
            .withHref(SemanticIdentifier.newVersionId(URI.create(s[1]))))
        .forEach(links::add);

    return links;
  }

  /**
   * Rewrites non-standard Asset IDs into versioned URIs
   * <p>
   * Replace namespace prefixes, and adds a default version if absent
   *
   * @param id the ID to be rewritten
   * @return the ID, as a normalized and versioned URI string
   */
  @Nonnull
  protected String normalizeQualifiedName(
      @Nonnull final String id) {
    if (id.startsWith(ASSETS_PREFIX)) {
      String str = id.substring(ASSETS_PREFIX.length());
      if (Registry.isHttpIdentifier(names.getAssetNamespace()) && str.indexOf(':') >= 0) {
        str = str.replace(":", IdentifierConstants.VERSIONS);
      }
      return names.getAssetNamespace() + str;
    } else {
      return id;
    }
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
  @Nonnull
  protected Publication getArtifactPublicationStatus(
      @Nonnull final TrisotechFileInfo manifest) {
    Publication lifecycle = new Publication();
    var state = TrisotechPublicationStates.parse(manifest.getState());
    switch (state) {
      case PUBLISHED:
        lifecycle.withPublicationStatus(Published);
        break;
      case DRAFT:
        lifecycle.withPublicationStatus(Draft);
        break;
      case PENDING_APPROVAL:
        lifecycle.withPublicationStatus(Final_Draft);
        break;
      case UNPUBLISHED:
        lifecycle.withPublicationStatus(Unpublished);
        break;
      default:
        throw new IllegalStateException("Unrecognized state " + manifest.getState());
    }
    logger.trace("lifecycle = {}", lifecycle.getPublicationStatus());

    return lifecycle;
  }
}