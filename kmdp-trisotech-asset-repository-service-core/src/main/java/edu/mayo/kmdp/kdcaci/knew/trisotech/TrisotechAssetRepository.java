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
package edu.mayo.kmdp.kdcaci.knew.trisotech;

import static edu.mayo.kmdp.kdcaci.knew.trisotech.components.TTContentNegotiationHelper.negotiateHTML;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.BPMMetadataHelper.dependencyRel;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.BPMMetadataHelper.getDeclaredAssetTypes;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.BPMMetadataHelper.getDefaultAssetType;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.BPMMetadataHelper.getDefaultRepresentation;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.DocumentHelper.extractAssetIdFromDocument;
import static edu.mayo.kmdp.registry.Registry.BASE_UUID_URN_URI;
import static edu.mayo.kmdp.trisotechwrapper.TTWrapper.matchesVersion;
import static edu.mayo.kmdp.trisotechwrapper.config.TTNotations.getXmlMimeTypeByAssetType;
import static edu.mayo.kmdp.trisotechwrapper.config.TTWConfigParamsDef.ASSET_ID_ATTRIBUTE;
import static edu.mayo.kmdp.trisotechwrapper.config.TTWConfigParamsDef.DEFAULT_VERSION_TAG;
import static edu.mayo.kmdp.util.JenaUtil.objA;
import static edu.mayo.kmdp.util.Util.isEmpty;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.ofAst;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.AbstractCompositeCarrier.ofMixedAnonymousComposite;
import static org.omg.spec.api4kp._20200801.AbstractCompositeCarrier.ofUniformAnonymousComposite;
import static org.omg.spec.api4kp._20200801.Answer.failed;
import static org.omg.spec.api4kp._20200801.Answer.succeed;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newId;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newKey;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newVersionId;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.timedSemverComparator;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.defaultArtifactId;
import static org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries.Care_Process_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries.Clinical_Calculation_Rule;
import static org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries.Clinical_Case_Management_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries.Clinical_Contextualization_Rule;
import static org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries.Clinical_Decision_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries.Clinical_Eligibility_Rule;
import static org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries.Clinical_Enrollment_Rule;
import static org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries.Clinical_Guidance_Rule;
import static org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries.Clinical_Inference_Rule;
import static org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries.Clinical_Rule;
import static org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries.Cognitive_Care_Process_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Case_Management_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Computable_Decision_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Decision_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Formal_Ontology;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Lexicon;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Naturalistic_Decision_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Protocol;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.ReSTful_Service_Specification;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Semantic_Decision_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.TXT;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;
import static org.omg.spec.api4kp._20200801.taxonomy.krserialization.KnowledgeRepresentationLanguageSerializationSeries.Turtle;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Encoded_Knowledge_Expression;

import edu.mayo.kmdp.kdcaci.knew.trisotech.components.EphemeralAssetFabricator;
import edu.mayo.kmdp.kdcaci.knew.trisotech.components.PlanDefinitionEphemeralAssetFabricator;
import edu.mayo.kmdp.kdcaci.knew.trisotech.components.TTContentNegotiationHelper;
import edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.DefaultMetadataIntrospector;
import edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.MetadataIntrospector;
import edu.mayo.kmdp.language.parsers.rdf.JenaRdfParser;
import edu.mayo.kmdp.trisotechwrapper.TTAPIAdapter;
import edu.mayo.kmdp.trisotechwrapper.TTWrapper;
import edu.mayo.kmdp.trisotechwrapper.components.DefaultNamespaceManager;
import edu.mayo.kmdp.trisotechwrapper.components.NamespaceManager;
import edu.mayo.kmdp.trisotechwrapper.components.SemanticModelInfo;
import edu.mayo.kmdp.trisotechwrapper.components.redactors.TTRedactor;
import edu.mayo.kmdp.trisotechwrapper.components.weavers.DomainSemanticsWeaver;
import edu.mayo.kmdp.trisotechwrapper.config.TTWConfigParamsDef;
import edu.mayo.kmdp.trisotechwrapper.config.TTWEnvironmentConfiguration;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.util.XMLUtil;
import java.net.URI;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.KnowledgeAssetCatalogApi;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.KnowledgeAssetRepositoryApi;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.server.KnowledgeAssetCatalogApiInternal;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.server.KnowledgeAssetRepositoryApiInternal;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.TransxionApiInternal;
import org.omg.spec.api4kp._20200801.id.KeyIdentifier;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.CompositeKnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.KPServer;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.services.repository.KnowledgeAssetCatalog;
import org.omg.spec.api4kp._20200801.services.repository.asset.ConfigurableKnowledgeAssetCatalog;
import org.omg.spec.api4kp._20200801.services.repository.asset.KARSHrefBuilder;
import org.omg.spec.api4kp._20200801.services.repository.asset.KARSHrefBuilder.HrefType;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.SurrogateHelper;
import org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries;
import org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyType;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetType;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries;
import org.omg.spec.api4kp._20200801.terms.ConceptTerm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

/**
 * This class implements the Knowledge Asset API and wraps that API around the Trisotech API to
 * return Trisotech data in a Knowledge Asset compatible way.
 */
@Component
@KPServer
public class TrisotechAssetRepository implements KnowledgeAssetCatalogApiInternal,
    KnowledgeAssetRepositoryApiInternal {

  /**
   * Logger
   */
  private static final Logger logger = LoggerFactory.getLogger(TrisotechAssetRepository.class);

  /**
   * List of supported {@link KnowledgeAssetType}
   */
  private static final List<KnowledgeAssetType> SUPPORTED_ASSET_TYPES = List.of(
      Naturalistic_Decision_Model,
      Computable_Decision_Model,
      Clinical_Decision_Model,
      Semantic_Decision_Model,
      Decision_Model,

      Clinical_Case_Management_Model,
      Care_Process_Model,
      Cognitive_Care_Process_Model,
      Case_Management_Model,

      Clinical_Eligibility_Rule,
      Clinical_Enrollment_Rule,
      Clinical_Calculation_Rule,
      Clinical_Contextualization_Rule,
      Clinical_Guidance_Rule,
      Clinical_Inference_Rule,
      Clinical_Rule,

      Lexicon,
      Formal_Ontology,

      ReSTful_Service_Specification,

      Protocol
  );

  /**
   * The DES API facade used to interact with the DES server
   */
  @Nonnull
  protected final TTAPIAdapter client;

  /**
   * The Introspector used to generate {@link KnowledgeAsset} surrogates from the analysis of the
   * BPM+ models artifacts in the DES server
   */
  @Nonnull
  protected final MetadataIntrospector extractor;

  /**
   * The {@link KARSHrefBuilder} used to map URIs to URLs relative to this server's deployment
   */
  @Nullable
  protected final KARSHrefBuilder hrefBuilder;

  /**
   * The helper used in content negotiation
   */
  @Nullable
  protected final TTContentNegotiationHelper negotiator;

  /**
   * The environment configuration
   */
  @Nonnull
  protected final TTWEnvironmentConfiguration cfg;

  /**
   * The namespace manager used to rewrite the Trisotech native URIs into platform URIs
   */
  @Nonnull
  protected final NamespaceManager names;

  @Nullable
  protected final EphemeralAssetFabricator fabricator;

  @Autowired
  public TrisotechAssetRepository(
      @Nonnull TTWEnvironmentConfiguration cfg,
      @Nullable TTAPIAdapter client,
      @Nullable MetadataIntrospector extractor,
      @Nullable KARSHrefBuilder hrefBuilder,
      @Nullable TTContentNegotiationHelper negotiator,
      @Nullable NamespaceManager names,
      @Nullable EphemeralAssetFabricator fabricator,
      @Nullable TransxionApiInternal translator) {
    //
    this.cfg = cfg;

    this.client = client != null
        ? client
        : new TTWrapper(cfg, new DomainSemanticsWeaver(this.cfg), new TTRedactor());

    this.hrefBuilder = hrefBuilder != null
        ? hrefBuilder
        : new KARSHrefBuilder(cfg);

    this.names = names != null
        ? names
        : new DefaultNamespaceManager(this.cfg);

    this.negotiator = negotiator != null
        ? negotiator
        : new TTContentNegotiationHelper(
            this.names, this.hrefBuilder, translator);

    this.extractor = extractor != null
        ? extractor
        : new DefaultMetadataIntrospector(this.cfg, this.client, this.names, this.hrefBuilder);

    this.fabricator = fabricator != null
        ? fabricator
        : new PlanDefinitionEphemeralAssetFabricator(
            this.names,
            KnowledgeAssetCatalogApi.newInstance(this),
            KnowledgeAssetRepositoryApi.newInstance(this)
        );
  }

  /**
   * @return A {@link KnowledgeAssetCatalog} that serves as a manifest of this server's capabilities
   */
  @Override
  public Answer<KnowledgeAssetCatalog> getKnowledgeAssetCatalog() {
    return Answer.of(new ConfigurableKnowledgeAssetCatalog(TTWConfigParamsDef.values())
        .withName("KMDP Trisotech DES Wrapper")
        .withId(newId(BASE_UUID_URN_URI, "TTW"))
        .withSurrogateModels(rep(Knowledge_Asset_Surrogate_2_0, XML_1_1))
        .withSupportedAssetTypes(SUPPORTED_ASSET_TYPES));
  }

  /**
   * Lists all the KnowledgeAssets, collectively carried by the Models/Artifacts in the DES server
   * <p>
   * Can filter by asset type. Sorts by date. May paginate (best effort)
   *
   * @param assetTypeTag           the type of asset to retrieve; if null, will get ALL types;
   * @param assetAnnotationTag     ignored
   * @param assetAnnotationConcept ignored
   * @param offset                 pagination, will skip
   * @param limit                  ignore -- needed if we have pagination
   * @return Pointers to available Assets
   */
  @Override
  public Answer<List<Pointer>> listKnowledgeAssets(
      @Nullable final String assetTypeTag,
      @Nullable final String assetAnnotationTag,
      @Nullable final String assetAnnotationConcept,
      @Nullable final Integer offset,
      @Nullable final Integer limit) {
    try {
      var filterType = decodeTypeFilter(assetTypeTag);

      var manifests
          // get all models
          = client.listModels(getXmlMimeTypeByAssetType(assetTypeTag))
          // sort by date
          .sorted(Comparator.comparing(SemanticModelInfo::lastUpdated))
          // map models to assets, reduce
          .flatMap(this::getAssetPointersForModel)
          .flatMap(ptr -> {
            assert fabricator != null;
            return fabricator.join(ptr);
          })
          // filter by type
          .filter(ptr -> filterType == null || Objects.equals(ptr.getType(), filterType))
          // paginate, if requested
          .skip((null == offset) ? 0 : offset)
          .limit((null == limit || limit < 0) ? Integer.MAX_VALUE : limit);

      return Answer.of(aggregateVersions(manifests));
    } catch (Exception e) {
      return Answer.failed(e);
    }
  }


  /**
   * Returns the Surrogate for the GREATEST version of a given Knowledge Asset
   * <p>
   *
   * @param assetId The *Knowledge Asset* UUID
   * @param xAccept content negotiation parameter that controls the manifestation of the returned
   *                surrogate. Supports HTML as a main variant for browser compatibility
   * @return The asset surrogate for the greatest version of the given Asset ID
   */
  @Override
  public Answer<KnowledgeAsset> getKnowledgeAsset(
      @Nonnull final UUID assetId,
      @Nullable final String xAccept) {
    try {
      // need the modelId of the model in order to query for modelInfo
      if (negotiateHTML(xAccept) && hrefBuilder != null) {
        return Answer.referTo(hrefBuilder.getRelativeURL("/surrogate"), false);
      }

      // get all Models (manifests) that carry the same Asset
      var models = client.getMetadataByGreatestAssetId(assetId)
          .collect(toList());
      if (models.isEmpty()) {
        return Answer.ofTry(Optional.empty(), newId(assetId),
            () -> "No Asset found for the given ID");
      }

      var greatestAssetId = names.modelToAssetId(models.get(0))
          .orElseGet(() -> newId(assetId, cfg.getTyped(DEFAULT_VERSION_TAG, String.class)));
      return Answer.ofTry(getSurrogateFromManifests(
          greatestAssetId,
          models));
    } catch (Exception e) {
      return Answer.failed(e);
    }
  }


  /**
   * Returns the Surrogate for the given version of a given Knowledge Asset
   * <p>
   * Note that an Asset Version is indexed and cached only if it is still carried by the latest
   * version of at least one Model. If not, this service will try to locate that version in the
   * previous version of the associated Models, at a non-trivial computational cost that grows
   * linearly with the length of the history of the Models.
   *
   * @param assetId    The *Knowledge Asset* UUID
   * @param versionTag the version tag of the Knowledge Asset
   * @param xAccept    content negotiation parameter that controls the manifestation of the returned
   *                   surrogate. Supports HTML as a main variant for browser compatibility
   * @return The asset surrogate for a given Asset ID
   */
  @Override
  public Answer<KnowledgeAsset> getKnowledgeAssetVersion(
      @Nonnull final UUID assetId,
      @Nonnull final String versionTag,
      @Nullable final String xAccept) {
    try {
      if (negotiateHTML(xAccept) && hrefBuilder != null) {
        return Answer.referTo(hrefBuilder.getRelativeURL("/surrogate"), false);
      }

      var carrierInfo = client.getMetadataByAssetId(assetId, versionTag)
          .collect(toList());
      return Answer.ofTry(
          getSurrogateFromManifests(newId(assetId, versionTag), carrierInfo));
    } catch (Exception e) {
      return Answer.failed(e);
    }
  }


  /**
   * Drops all the internal caches, and refreshes the Place/Path Index cache
   *
   * @return Success, unless Exception
   * @see TTAPIAdapter#rescan()
   */
  @Override
  public Answer<Void> clearKnowledgeAssetCatalog() {
    try {
      client.rescan();
      return succeed();
    } catch (Exception e) {
      return failed(e);
    }
  }


  /**
   * Retrieves the Artifact with the canonical representation of a given Asset version
   *
   * @param assetId    assetId of the asset
   * @param versionTag version of the asset
   * @param xAccept    'accept' MIME type
   * @return the canonical Artifact, at the binary level, wrapped in a {@link KnowledgeCarrier}
   */
  @Override
  public Answer<KnowledgeCarrier> getKnowledgeAssetVersionCanonicalCarrier(
      @Nonnull final UUID assetId,
      @Nonnull final String versionTag,
      @Nullable final String xAccept) {
    try {
      var manifest = getLatestAndGreatestAssetManifest(assetId, versionTag);
      var carrier = Answer.ofTry(manifest.flatMap(info ->
          buildCarrierFromManifest(assetId, versionTag, info)));

      if (carrier.isFailure() && fabricator != null) {
        carrier = fabricator.fabricate(assetId, versionTag);
      }

      if (negotiator != null
          && carrier.map(kc -> negotiator.needsVariant(kc, xAccept)).orElse(false)) {
        return carrier
            .flatMap(kc -> negotiator.negotiate(kc, xAccept));
      } else {
        return carrier;
      }
    } catch (Exception e) {
      return Answer.failed(e);
    }
  }

  /**
   * Retrieves the Artifact with the canonical representation of a given Asset version, raw
   * <p>
   * Note: the behavior of this method is consistent with #getKnowledgeAssetVersionCanonicalCarrier
   *
   * @param assetId    assetId of the asset
   * @param versionTag version of the asset
   * @param xAccept    'accept' MIME type
   * @return the canonical Artifact, at the binary level, wrapped in a {@link KnowledgeCarrier}
   */
  @Override
  public Answer<byte[]> getKnowledgeAssetVersionCanonicalCarrierContent(
      UUID assetId, String versionTag, String xAccept) {
    try {
      return getKnowledgeAssetVersionCanonicalCarrier(assetId, versionTag, xAccept)
          .flatOpt(KnowledgeCarrier::asBinary);
    } catch (Exception e) {
      return Answer.failed(e);
    }
  }

  /**
   * Retrieves (a copy of) a specific version of an Artifact/Model, as the carrier of a given
   * version of a Knowledge Asset.
   * <p>
   * This method enforces the Asset/Carrier relationship. To retrieve a Model directly, clients
   * should use the Knowledge Artifact Repository API.
   * <p>
   * Background: In TT, only the latest version of a model is indexed, and there is no guarantee
   * that the latest model matches the requested artifact version, and/or still carries the
   * requested version of the Asset. TT not being a long term repository, this method makes a
   * best-effort attempt to honor the request, looking up the requested artifact version (if still
   * existing), and returning content only if that version carries the desired asset version.
   *
   * @param assetId            the Asset ID
   * @param versionTag         the Asset version
   * @param artifactId         the Artifact ID
   * @param artifactVersionTag the Artifact version
   * @see TrisotechArtifactRepository#getKnowledgeArtifactVersion(String, UUID, String, Boolean)
   */
  @Override
  public Answer<KnowledgeCarrier> getKnowledgeAssetCarrierVersion(
      @Nonnull final UUID assetId,
      @Nonnull final String versionTag,
      @Nonnull final UUID artifactId,
      @Nonnull final String artifactVersionTag,
      @Nullable final String xAccept) {
    try {
      Optional<SemanticModelInfo> manifest = getCarrierInfo(assetId, versionTag, artifactId);
      if (manifest.isPresent()) {
        // The asset version is indexed: it is still 'current' in at least one latest artifact ...
        var info = manifest.get();
        if (matchesVersion(info, artifactVersionTag,
            () -> cfg.getTyped(DEFAULT_VERSION_TAG))) {
          // ... if that artifact has the requested version, return the data
          return Answer.ofTry(getCarrier(assetId, versionTag, artifactId));
        }
      }
      // ... at this point, the asset version is not in a 'latest' model.
      // Need to retrieve the historical version of the artifact,
      // and see if contains the asset version
      return getKnowledgeCarrierFromOtherVersion(
          assetId, versionTag, artifactId, artifactVersionTag);
    } catch (Exception e) {
      return Answer.failed(e);
    }
  }

  /**
   * Retrieves the canonical Surrogate for the greatest version of an Asset, in a KnowledgeCarrier
   * <p>
   * Supports minimal content negotiation between the default format, and its basic HTML
   * counterpart
   *
   * @param assetId the Asset ID
   * @param xAccept the generalized mime type
   * @return the {@link KnowledgeAsset} Surrogate, in a {@link KnowledgeCarrier}
   */
  @Override
  public Answer<KnowledgeCarrier> getKnowledgeAssetCanonicalSurrogate(
      @Nonnull final UUID assetId,
      @Nullable String xAccept) {
    try {
      var surr = getKnowledgeAsset(assetId, null)
          .map(SurrogateHelper::carry);
      if (surr.isFailure() && fabricator != null) {
        surr = fabricator.getFabricatableVersion(assetId)
            .map(vtag -> getKnowledgeAssetVersionCanonicalSurrogate(assetId, vtag, xAccept))
            .orElseGet(Answer::notFound);
      }
      return negotiateHTML(xAccept) && negotiator != null
          ? surr.flatMap(negotiator::toHtml)
          : surr;
    } catch (Exception e) {
      return Answer.failed(e);
    }
  }

  /**
   * Retrieves the canonical Carrier Artifact for the greatest version of an Asset
   *
   * @param assetId the Asset ID
   * @param xAccept the generalized mime type
   * @return the {@link KnowledgeAsset} Asset Carrier, wrapped in a {@link KnowledgeCarrier}
   */
  @Override
  public Answer<KnowledgeCarrier> getKnowledgeAssetCanonicalCarrier(
      @Nonnull final UUID assetId,
      @Nullable String xAccept) {
    try {
      var carrier = client.getMetadataByGreatestAssetId(assetId)
          .findFirst()
          .flatMap(names::modelToAssetId)
          .map(vid ->
              getKnowledgeAssetVersionCanonicalCarrier(vid.getUuid(), vid.getVersionTag(),
                  xAccept));

      if (carrier.map(Answer::isFailure).orElse(true) && fabricator != null) {
        carrier = fabricator.getFabricatableVersion(assetId)
            .map(vtag -> getKnowledgeAssetVersionCanonicalCarrier(assetId, vtag, xAccept));
      }
      return carrier.orElseGet(Answer::notFound);
    } catch (Exception e) {
      return Answer.failed(e);
    }
  }


  /**
   * Retrieves the canonical Carrier Artifact for the greatest version of an Asset
   *
   * @param assetId the Asset ID
   * @param xAccept the generalized mime type
   * @return the {@link KnowledgeAsset} Asset Carrier, in binary form
   */
  @Override
  public Answer<byte[]> getKnowledgeAssetCanonicalCarrierContent(
      @Nonnull final UUID assetId,
      @Nullable String xAccept) {
    try {
      return getKnowledgeAssetCanonicalCarrier(assetId, xAccept)
          .flatOpt(AbstractCarrier::asBinary);
    } catch (Exception e) {
      return Answer.failed(e);
    }
  }

  /**
   * Retrieves the canonical Surrogate for the given version of an Asset, in a KnowledgeCarrier
   * <p>
   * Supports minimal content negotiation between the default format, and its basic HTML
   * counterpart
   *
   * @param assetId    the Asset ID
   * @param versionTag the Asset version
   * @param xAccept    the generalized mime type
   * @return the {@link KnowledgeAsset} Surrogate, in a {@link KnowledgeCarrier}
   */
  @Override
  public Answer<KnowledgeCarrier> getKnowledgeAssetVersionCanonicalSurrogate(
      @Nonnull final UUID assetId,
      @Nonnull final String versionTag,
      @Nullable final String xAccept) {
    try {
      var surr = getKnowledgeAssetVersion(assetId, versionTag, null)
          .map(SurrogateHelper::carry);
      if (surr.isFailure() && fabricator != null) {
        surr = fabricator.fabricateSurrogate(assetId, versionTag);
      }
      return negotiateHTML(xAccept) && negotiator != null
          ? surr.flatMap(negotiator::toHtml)
          : surr;
    } catch (Exception e) {
      return Answer.failed(e);
    }
  }


  /**
   * Gathers a Composite Knowledge Artifact from (the Carrier of) a 'root' Knowledge Asset.
   * <p>
   * Traverses the Asset/Asset dependency relations, interprets their closure as the components of
   * an anonymous composite, resolves each component Asset as a carrier Artifact, and returns the
   * package thereof
   *
   * @param assetId    the Asset ID of the root Asset
   * @param versionTag the version Tag of the root Asset
   * @param xAccept    content negotiation header, to drive Artifact form preferences
   * @return a Composite Artifact that manifests the implicit Composite rooted in the given Asset
   */
  @Override
  public Answer<CompositeKnowledgeCarrier> getAnonymousCompositeKnowledgeAssetCarrier(
      @Nonnull final UUID assetId,
      @Nonnull final String versionTag,
      @Nullable final String xAccept) {
    try {
      var rootId = newId(assetId, versionTag);
      Set<KeyIdentifier> closure = getAssetClosure(rootId)
          .map(Entry::getKey)
          .collect(Collectors.toSet());

      Answer<Set<KnowledgeCarrier>> componentArtifacts = closure.stream()
          .map(comp -> getKnowledgeAssetVersionCanonicalCarrier(comp.getUuid(),
              comp.getVersionTag(),
              xAccept))
          .collect(Answer.toSet());

      return componentArtifacts
          .map(comps -> ofMixedAnonymousComposite(rootId, comps));
    } catch (Exception e) {
      return Answer.failed(e);
    }
  }

  /**
   * Gathers a Composite Knowledge Surrogate from (the Surrogates of) a 'root' Knowledge Asset.
   * <p>
   * Traverses the Asset/Asset dependency relations, interprets their closure as the components of
   * an anonymous composite, resolves each component Asset its canonical Surrogate, and returns the
   * package thereof
   *
   * @param assetId    the Asset ID of the root Asset
   * @param versionTag the version Tag of the root Asset
   * @param xAccept    content negotiation header, to drive Surrogate form preferences
   * @return a Composite Surrogate that manifests the implicit Composite rooted in the given Asset
   */
  @Override
  public Answer<CompositeKnowledgeCarrier> getAnonymousCompositeKnowledgeAssetSurrogate(
      @Nonnull final UUID assetId,
      @Nonnull final String versionTag,
      @Nullable final String xAccept) {
    try {
      var rootId = newId(assetId, versionTag);
      Set<KeyIdentifier> closure = getAssetClosure(rootId)
          .map(Entry::getKey)
          .collect(Collectors.toSet());

      Answer<Set<KnowledgeCarrier>> componentSurrogates = closure.stream()
          .map(comp -> getKnowledgeAssetVersion(comp.getUuid(),
              comp.getVersionTag(),
              xAccept)
              .map(SurrogateHelper::carry))
          .collect(Answer.toSet());

      return componentSurrogates
          .map(comps -> ofUniformAnonymousComposite(rootId, comps));
    } catch (Exception e) {
      return Answer.failed(e);
    }
  }

  /**
   * Infers the structure of an anonymous Composite Knowledge Asset, from (the ID of) a root Asset
   * and the transitive closure of that Asset's dependencies.
   *
   * @param assetId    the Asset ID of the root Asset
   * @param versionTag the version Tag of the root Asset
   * @param xAccept    content negotiation header, to drive Surrogate form preferences
   * @return a Composite Surrogate that manifests the implicit Composite rooted in the given Asset
   */
  @Override
  public Answer<KnowledgeCarrier> getAnonymousCompositeKnowledgeAssetStructure(
      @Nonnull final UUID assetId,
      @Nonnull final String versionTag,
      @Nullable final String xAccept) {
    try {
      var root = newId(assetId, versionTag);

      Model struct = ModelFactory.createDefaultModel();
      getAssetClosure(root)
          .forEach(dep -> struct.add(objA(
              names.assetKeyToId(root.asKey()).getVersionId(),
              dep.getValue().getReferentId(),
              names.assetKeyToId(dep.getKey()).getVersionId())));

      var structId = struct.listSubjects().toSet().stream()
          .map(s -> newVersionId(URI.create(s.getURI())))
          .reduce(SemanticIdentifier::hashIdentifiers)
          .orElse(null);

      return new JenaRdfParser().applyLower(
          ofAst(struct)
              .withAssetId(structId)
              .withArtifactId(defaultArtifactId(BASE_UUID_URN_URI, structId, OWL_2))
              .withRepresentation(rep(OWL_2)),
          Encoded_Knowledge_Expression,
          codedRep(OWL_2, Turtle, TXT, defaultCharset(), Encodings.DEFAULT),
          null);
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      return Answer.failed(e);
    }
  }

  /* ----------------------------------------------------------------------------------------- */


  /**
   * Decodes an assetTypeTag, used to filter Assets by formal type
   * <p>
   * The method recognizes types from the {@link KnowledgeAssetType} taxonomy, including formal and
   * clinical types. Maps the Concept to the referent URI in the (C)KAO ontology
   *
   * @param assetTypeTag the filter tag, if any
   * @return the URI of the type, as defined in the API4KP (C)KAO ontology, if any
   */
  @Nullable
  private URI decodeTypeFilter(
      @Nullable final String assetTypeTag) {
    if (isEmpty(assetTypeTag)) {
      return null;
    }
    return KnowledgeAssetTypeSeries.resolveTag(assetTypeTag)
        .or(() -> ClinicalKnowledgeAssetTypeSeries.resolveTag(assetTypeTag))
        .map(ConceptTerm::getReferentId)
        .orElse(null);
  }


  /**
   * Retrieves Pointers to all Assets declared by a given Model
   * <p>
   * In general, a Model carries one Knowledge Asset, and references 0 to many Service Assets
   *
   * @param info the Model manifest
   * @return the Asset Pointers, in a Stream
   */
  @Nonnull
  private Stream<Pointer> getAssetPointersForModel(
      @Nonnull final SemanticModelInfo info) {
    return Stream.concat(toAssetPointer(info), toServicePointer(info));
  }

  /**
   * Creates Pointers to the Service Assets exposed by a given Model
   *
   * @param info the Model manifest
   * @return the Service Asset Pointers, in a Stream
   */
  @Nonnull
  private Stream<Pointer> toServicePointer(
      @Nonnull final SemanticModelInfo info) {
    return info.getExposedServices().stream()
        .flatMap(key -> client.getMetadataByAssetId(key.getUuid(), key.getVersionTag()))
        .map(sInfo -> {
              var id = names.assetKeyToId(sInfo.getServiceKey());
              return id.toPointer()
                  .withType(getRepresentativeType(
                      sInfo.getServiceKey(), () -> ReSTful_Service_Specification))
                  .withName(sInfo.getName())
                  .withHref(tryAddHref(id));
            }
        );
  }

  /**
   * Creates Pointer to the Knowledge Asset exposed by a given Model
   * <p>
   * Note: a Model is currently assumed to carry up to one Asset
   *
   * @param info the Model manifest
   * @return the Knowledge Asset Pointers, in a Stream
   */
  @Nonnull
  private Stream<Pointer> toAssetPointer(
      @Nonnull final SemanticModelInfo info) {
    return names.modelToAssetId(info).stream()
        .map(assetId -> assetId.toPointer()
            .withType(
                getRepresentativeType(
                    assetId.asKey(),
                    () -> getDefaultAssetType(info.getMimetype())))
            .withName(info.getName())
            .withHref(tryAddHref(assetId))
        );
  }

  /**
   * Maps an Asset ID to an API URL for that Asset Version's Surrogate, if the Server is deployed
   *
   * @param assetId the Asset ID
   * @return the URL for that Asset Version Surrogate, as a URI, if able
   */
  @Nullable
  private URI tryAddHref(
      @Nonnull ResourceIdentifier assetId) {
    return hrefBuilder != null
        ? hrefBuilder.getHref(assetId, HrefType.ASSET_VERSION)
        : null;
  }

  /**
   * Chooses one Asset Type to use in an asset Pointer, even when the asset has multiple types, or
   * no types at all.
   * <p>
   * Selects one of the types, based on the ranked preference of {@link #SUPPORTED_ASSET_TYPES}
   *
   * @param assetId     the asset ID (pointer)
   * @param defaultType the type to be used if the asset does not declare a type explicitly
   * @return the ontology URI of the chosen asset type
   */
  @Nullable
  private URI getRepresentativeType(
      @Nonnull final KeyIdentifier assetId,
      @Nonnull final Supplier<KnowledgeAssetType> defaultType) {
    return client.getMetadataByAssetId(assetId.getUuid(), assetId.getVersionTag())
        .flatMap(info -> getDeclaredAssetTypes(info, defaultType).stream())
        .distinct()
        .filter(type -> type.isAnyOf(SUPPORTED_ASSET_TYPES))
        .min(Comparator.comparingInt(SUPPORTED_ASSET_TYPES::indexOf))
        .map(ConceptTerm::getReferentId)
        .orElseGet(() -> {
          logger.error("No supported asset type(s) detected for Asset {}", assetId);
          return null;
        });
  }

  /**
   * Reduces a Stream of Pointers, grouping by Series UUID, then reducing to the LATEST and GREATEST
   * version of each Series
   *
   * @param versionedPtrs the versioned Pointers
   * @return a Reduced List for the input Stream
   */
  protected List<Pointer> aggregateVersions(Stream<Pointer> versionedPtrs) {
    return versionedPtrs
        .collect(groupingBy(SemanticIdentifier::asKey))
        .values().stream()
        .map(l -> {
          l.sort(timedSemverComparator());
          return l.get(0);
        }).collect(toList());
  }

  /* ----------------------------------------------------------------------------------------- */

  /**
   * Looks up an Asset Version in a Model Version, when the Model Version is not a "latest", and
   * thus not indexed. Finds the Artifact versions that share the given version tag, and looks for
   * the Asset ID in the actual XML Model.
   * <p>
   * Note: this operation is fairly expensive, especially if the same version tag has been used with
   * multiple artifact snapshots, and should be used with care.
   *
   * @param assetId         the assetId looking for
   * @param assetVersionTag the version of the asset looking for
   * @param artifactId      the artifactID
   * @param modelVersionTag the version of the artifact
   * @return KnowledgeCarrier for the version of the artifact with the requested version of the
   * asset
   */
  private Answer<KnowledgeCarrier> getKnowledgeCarrierFromOtherVersion(
      UUID assetId,
      String assetVersionTag,
      UUID artifactId,
      String modelVersionTag) {
    var assetKey = newKey(assetId, assetVersionTag);

    var carrier = client.getVersionsMetadataByModelId(names.artifactToModelId(artifactId))
        .stream().filter(info -> Objects.equals(modelVersionTag, info.getVersion()))
        .sorted()
        // given any Model that matches the artifact ID/Version (could be more than one)
        .filter(candidate -> {
          var detectedId = client.getModel(candidate)
              .flatMap(dox ->
                  extractAssetIdFromDocument(dox, cfg.getTyped(ASSET_ID_ATTRIBUTE)));
          // does the model assert the desired asset ID/version
          return detectedId.map(id -> id.asKey().equals(assetKey)).orElse(false);
        }).findFirst()
        .flatMap(info -> buildCarrierFromManifest(assetId, assetVersionTag, info));

    return Answer.ofTry(carrier);
  }

  /* ----------------------------------------------------------------------------------------- */

  /**
   * Traverses the Asset to Asset dependencies of a given Knowledge Asset, returning a Stream of the
   * IDs of those dependencies
   *
   * @param rootAssetId the ID of the root Knowledge Asset
   * @return A Stream of IDs of those Assets that the root depends on, directly or indirectly
   */
  @Nonnull
  protected Stream<Map.Entry<KeyIdentifier, DependencyType>> getAssetClosure(
      @Nonnull final ResourceIdentifier rootAssetId) {
    return client.getMetadataByAssetId(rootAssetId.getUuid(), rootAssetId.getVersionTag())
        .flatMap(this::getAssetClosure)
        .distinct();
  }

  /**
   * Recurses on the Asset to Asset dependencies of a given Knowledge Asset, returning a Stream of
   * the IDs of those dependencies, using the internal index in the process.
   * <p>
   * The IDs are mapped to the specific dependency relationship type, assumning that the same Asset
   * will only play one type of role
   *
   * @param modelRoot the manifest of the root Knowledge Asset
   * @return A Stream of IDs of those Assets that the root depends on, directly or indirectly
   */
  @Nonnull
  protected Stream<Map.Entry<KeyIdentifier, DependencyType>> getAssetClosure(
      @Nonnull final SemanticModelInfo modelRoot) {
    return Stream.concat(
        Stream.ofNullable(modelRoot.getAssetKey())
            .map(key -> Map.entry(key, dependencyRel(modelRoot.getMimetype()))),
        modelRoot.getModelDependencies().stream()
            .flatMap(depId -> client.getMetadataByModelId(depId).stream())
            .flatMap(this::getAssetClosure));
  }


  /* ----------------------------------------------------------------------------------------- */

  /**
   * Looks up the Manifest of a given Knowledge Asset Version, conditional to the fact that the
   * Asset is carried by a Model with the given Artifact (Series) ID
   *
   * @param assetId    the ID of the Knowledge Asset
   * @param versionTag the version of the Knowledge Asset
   * @param artifactId the Artifact ID
   * @return the Manifest of the Model version carrying that Asset Version
   */
  @Nonnull
  private Optional<SemanticModelInfo> getCarrierInfo(
      @Nonnull final UUID assetId,
      @Nonnull final String versionTag,
      @Nonnull final UUID artifactId) {
    return client.getMetadataByAssetId(assetId, versionTag)
        .filter(info -> info.getId().contains(artifactId.toString()))
        .findFirst();
  }

  /**
   * Looks up the Artifact for a given Knowledge Asset Version, conditional to the fact that the
   * Asset is carried by a Model with the given Artifact (Series) ID
   *
   * @param assetId    the ID of the Knowledge Asset
   * @param versionTag the version of the Knowledge Asset
   * @param artifactId the Artifact ID
   * @return the Manifest of the Model version carrying that Asset Version
   */
  @Nonnull
  private Optional<KnowledgeCarrier> getCarrier(
      @Nonnull final UUID assetId,
      @Nonnull final String versionTag,
      @Nonnull final UUID artifactId) {
    var manifest = getCarrierInfo(assetId, versionTag, artifactId);
    return manifest.flatMap(info -> buildCarrierFromManifest(assetId, versionTag, info));
  }


  /**
   * Retrieves the Latest (by date) and Greatest (by version) version of a Model that carries the
   * given Asset version
   * <p>
   * Note: Both time and version need to be taken into account, because TT allows for the same
   * version tag to be used with different version of a Model
   *
   * @param assetId    the Asset ID
   * @param versionTag the Asset version ID
   * @return the Manifest for the latest and greatest
   */
  @Nonnull
  private Optional<SemanticModelInfo> getLatestAndGreatestAssetManifest(
      @Nonnull final UUID assetId,
      @Nonnull final String versionTag) {
    return client.getMetadataByAssetId(assetId, versionTag)
        .sorted()
        .findFirst();
  }

  /**
   * Creates a {@link KnowledgeCarrier} that wraps a Model, given that Model's manifest, assuming
   * the Model to be the Carrier of a given Asset Version
   *
   * @param assetId    the Asset ID
   * @param versionTag the Asset version ID
   * @param info       the Model Manifest
   * @return the Model for the given Manifest, as the Asset's {@link KnowledgeCarrier}
   */
  @Nonnull
  private Optional<KnowledgeCarrier> buildCarrierFromManifest(
      @Nonnull final UUID assetId,
      @Nonnull final String versionTag,
      @Nonnull final TrisotechFileInfo info) {

    return getDefaultRepresentation(info).flatMap(lang ->
        client.getModel(info).map(xml ->
            buildCarrierFromNativeModel(
                assetId,
                versionTag,
                names.modelToArtifactId(info),
                lang,
                xml)));
  }

  /**
   * Factory method
   * <p>
   * Builds a Knowledge Carrier for a given Artifact, with the given metadata
   *
   * @param assetId    the Asset ID
   * @param versionTag the Asset version tag
   * @param artifactId the full Artifact ID, with version and label
   * @param rep        the Artifact's representation metadata
   * @param dox        the Artifact, as an XML Document
   * @return the Artifact, wrapped in a {@link KnowledgeCarrier}
   */
  private KnowledgeCarrier buildCarrierFromNativeModel(
      @Nonnull final UUID assetId,
      @Nonnull final String versionTag,
      @Nonnull final ResourceIdentifier artifactId,
      @Nonnull final SyntacticRepresentation rep,
      @Nonnull final Document dox) {

    return
        AbstractCarrier.of(XMLUtil.toByteArray(dox))
            .withRepresentation(rep)
            .withArtifactId(artifactId)
            .withLabel(artifactId.getName())
            .withAssetId(newId(names.getAssetNamespace(), assetId, versionTag));

  }

  /**
   * Builds a {@link KnowledgeAsset} Surrogate, given the Manifest(s) of the Models that carry
   * representations of that Asset
   * <p>
   * Note: assumes that the manifests all refer to the same version of the Asset with the given ID,
   * which may be the GREATEST, or a specific one provided by a client
   * <p>
   * Resolves the Manifests to the actual Models, then introspects the models to generate the
   * canonical Surrogate
   *
   * @param assetId   the Asset ID
   * @param manifests the Manifests for the Models that carry that Asset
   * @return a {@link KnowledgeAsset} Surrogate for the Asset, given the carriers
   */
  @Nonnull
  private Optional<KnowledgeAsset> getSurrogateFromManifests(
      @Nonnull final ResourceIdentifier assetId,
      @Nonnull final Collection<SemanticModelInfo> manifests) {
    var models = manifests.stream()
        .collect(toMap(
            info -> info,
            client::getModel
        ));
    return getSurrogateFromCarriers(assetId, models);
  }


  /**
   * Builds a {@link KnowledgeAsset} Surrogate, from the given Model/Manifest pairs
   *
   * @param assetId  the Asset ID
   * @param carriers the Model/Manifest pairs
   * @return a {@link KnowledgeAsset} Surrogate for the Asset, given the carriers
   */
  private Optional<KnowledgeAsset> getSurrogateFromCarriers(
      @Nonnull final ResourceIdentifier assetId,
      @Nonnull final Map<SemanticModelInfo, Optional<Document>> carriers) {

    // extract data from Trisotech format to OMG format
    return extractor.introspect(assetId, carriers);
  }

}
