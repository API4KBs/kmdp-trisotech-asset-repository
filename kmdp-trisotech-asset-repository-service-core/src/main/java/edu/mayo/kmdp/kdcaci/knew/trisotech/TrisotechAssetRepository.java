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

import static edu.mayo.kmdp.kdcaci.knew.trisotech.TrisotechArtifactRepository.ALL_REPOS;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.DocumentHelper.extractAssetIdFromDocument;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.BPMMetadataHelper.getDeclaredAssetTypes;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.BPMMetadataHelper.getDefaultAssetType;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.BPMMetadataHelper.getRepLanguage;
import static edu.mayo.kmdp.registry.Registry.BASE_UUID_URN_URI;
import static edu.mayo.kmdp.trisotechwrapper.TTWrapper.matchesVersion;
import static edu.mayo.kmdp.trisotechwrapper.config.TTNotations.getXmlMimeTypeByAssetType;
import static edu.mayo.kmdp.trisotechwrapper.config.TTWConfigParamsDef.ASSET_ID_ATTRIBUTE;
import static edu.mayo.kmdp.trisotechwrapper.config.TTWConfigParamsDef.DEFAULT_VERSION_TAG;
import static edu.mayo.kmdp.util.PropertiesUtil.serializeProps;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.AbstractCompositeCarrier.ofMixedAnonymousComposite;
import static org.omg.spec.api4kp._20200801.AbstractCompositeCarrier.ofUniformAnonymousComposite;
import static org.omg.spec.api4kp._20200801.Answer.failed;
import static org.omg.spec.api4kp._20200801.Answer.notFound;
import static org.omg.spec.api4kp._20200801.Answer.succeed;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newId;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.timedSemverComparator;
import static org.omg.spec.api4kp._20200801.services.transrepresentation.ModelMIMECoder.decodeAll;
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
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Naturalistic_Decision_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Protocol;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.ReSTful_Service_Specification;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Semantic_Decision_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.HTML;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;

import edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.MetadataIntrospector;
import edu.mayo.kmdp.kdcaci.knew.trisotech.exception.NotFoundException;
import edu.mayo.kmdp.kdcaci.knew.trisotech.exception.NotLatestAssetVersionException;
import edu.mayo.kmdp.language.translators.surrogate.v2.SurrogateV2toHTMLTranslator;
import edu.mayo.kmdp.trisotechwrapper.TTAPIAdapter;
import edu.mayo.kmdp.trisotechwrapper.components.DefaultNamespaceManager;
import edu.mayo.kmdp.trisotechwrapper.components.NamespaceManager;
import edu.mayo.kmdp.trisotechwrapper.components.SemanticModelInfo;
import edu.mayo.kmdp.trisotechwrapper.config.TTWEnvironmentConfiguration;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.util.XMLUtil;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.server.KnowledgeAssetCatalogApiInternal;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.server.KnowledgeAssetRepositoryApiInternal;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.TransxionApiInternal._applyTransrepresent;
import org.omg.spec.api4kp._20200801.id.KeyIdentifier;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.CompositeKnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.KPServer;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.services.repository.KnowledgeAssetCatalog;
import org.omg.spec.api4kp._20200801.services.repository.asset.KARSHrefBuilder;
import org.omg.spec.api4kp._20200801.services.repository.asset.KARSHrefBuilder.HrefType;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.SurrogateHelper;
import org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetType;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

/**
 * A 'wrapper' class. This class implements the Knowledge Asset API and wraps that API around the
 * Trisotech API to return Trisotech data in a Knowledge Asset compatible way.
 */
@Component
@KPServer
public class TrisotechAssetRepository implements KnowledgeAssetCatalogApiInternal,
    KnowledgeAssetRepositoryApiInternal {

  private static final Logger logger = LoggerFactory.getLogger(TrisotechAssetRepository.class);

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

      ReSTful_Service_Specification,

      Protocol
  );

  @Autowired
  private TTAPIAdapter client;

  @Autowired
  private MetadataIntrospector extractor;

  @Autowired(required = false)
  private KARSHrefBuilder hrefBuilder;

  private final _applyTransrepresent htmlTranslator = new SurrogateV2toHTMLTranslator();

  @Autowired(required = false)
  private TTWEnvironmentConfiguration configuration;

  @Autowired
  private NamespaceManager names;

  public TrisotechAssetRepository() {
    //
  }

  @PostConstruct
  void init() {
    if (configuration == null) {
      configuration = new TTWEnvironmentConfiguration();
    }
    if (hrefBuilder == null) {
      hrefBuilder = new KARSHrefBuilder(configuration);
    }
    names = new DefaultNamespaceManager(configuration);
  }

  @Override
  public Answer<KnowledgeAssetCatalog> getKnowledgeAssetCatalog() {
    return Answer.of(new KnowledgeAssetCatalog()
        .withName("KMDP Trisotech DES Wrapper")
        .withId(newId(BASE_UUID_URN_URI, "TTW"))
        .withSurrogateModels(rep(Knowledge_Asset_Surrogate_2_0, XML_1_1))
        .withSupportedAssetTypes(SUPPORTED_ASSET_TYPES));
  }


  /**
   * Of all the versions in the series, several criteria concur to determine the LATEST, including
   * the time at which a version was created, the (partial) ordering of the version tags, and the
   * association of that version of the Asset with an Artifact in a "published" state
   *
   * @param assetId The *Knowledge Asset* UUID There should be exactly one Artifact (Model)
   *                annotated with this UUID
   * @param xAccept content negotiation parameter that controls the manifestation of the returned
   *                surrogate (not supported)
   * @return The asset surrogate for a given Asset ID
   */
  @Override
  public Answer<KnowledgeAsset> getKnowledgeAsset(UUID assetId, String xAccept) {
    // need the modelId of the model in order to query for modelInfo
    if (negotiateHTML(xAccept)) {
      return Answer.referTo(hrefBuilder.getRelativeURL("/surrogate"), false);
    }

    var models = client.getMetadataByAssetId(assetId, xAccept).collect(toList());

    if (models.isEmpty()) {
      return Answer.ofTry(Optional.empty(), newId(assetId),
          () -> "No Asset found for the given ID");
    }
    // get the modelInfo for the latest artifactVersion
    return Answer.ofTry(getKnowledgeAssetForModels(assetId, models));
  }


  @Override
  public Answer<KnowledgeAsset> getKnowledgeAssetVersion(UUID assetId,
      String versionTag, String xAccept) {
    // For assetId, find artifactId; For assetId/versionTag, is latest artifactId/version a match?
    // if not, get versions of artifactId and weave each one to get assetId/version
    if (negotiateHTML(xAccept)) {
      return Answer.referTo(hrefBuilder.getRelativeURL("/surrogate"), false);
    }

    try {
      var carrierInfo = client.getMetadataByAssetId(assetId, versionTag).collect(toList());

      return Answer.ofTry(getKnowledgeAssetForModels(assetId, carrierInfo));
    } catch (NotLatestAssetVersionException e) {
      // this can happen, but is not good practice to have different versions
      // of an asset on the same model - mostly because it's very inefficient
      logger.debug(e.getMessage());
      // check other versions of the model
      try {
        return Answer.of(findArtifactVersionForAsset(e.getModelUri(), assetId, versionTag));
      } catch (NotFoundException nfe) {
        return Answer.failed(nfe);
      }
    } catch (NotFoundException nfe) {
      return Answer.failed(nfe);
    }
  }

  /**
   * When asset version cannot be found on current artifact, need to search all versions of the
   * artifacts.
   *
   * @param internalId the internal trisotech URL for the model
   * @param assetId    the assetId looking for
   * @param versionTag the version of the asset looking for
   * @return The KnowledgeAsset for the version found
   */
  private KnowledgeAsset findArtifactVersionForAsset(String internalId, UUID assetId,
      String versionTag) throws NotFoundException {
    var currentInfo = client.getMetadataByModelId(internalId).orElseThrow();
    List<TrisotechFileInfo> modelVersions =
        client.getVersionsMetadataByModelId(internalId);

    // reverse the list so the most recent version that matches is selected
    // there can be multiple versions of the artifact that map to one version of asset
    Collections.reverse(modelVersions);
    for (TrisotechFileInfo modelVersionInfo : modelVersions) {
      // skip any that are not published
      if (null == modelVersionInfo.getVersion() && null == modelVersionInfo.getState()) {
        continue;
      }

      var versionInfo = new SemanticModelInfo(modelVersionInfo, currentInfo);
      Optional<KnowledgeAsset> surr =
          client.getModel(modelVersionInfo)
              .flatMap(dox ->
                  extractAssetIdFromDocument(dox, configuration.getTyped(ASSET_ID_ATTRIBUTE))
                      .filter(axId -> versionTag.equals(axId.getVersionTag())
                          && assetId.equals(axId.getUuid()))
                      .flatMap(
                          axId -> getKnowledgeAssetForModels(
                              assetId, Map.of(versionInfo, Optional.of(dox))))
              );

      if (surr.isPresent()) {
        return surr.get();
      }
    }
    // have gone through all versions of the artifact and not found...
    throw new NotFoundException("Artifact not found", "No model is associated to asset version",
        newId(assetId, versionTag).getVersionId());
  }

  /**
   * list of the all published assets. If assetTypeTag is available will return all published assets
   * of that type.
   *
   * @param assetTypeTag           : the type of asset to retrieve; if null, will get ALL types;
   * @param assetAnnotationTag     ignore
   * @param assetAnnotationConcept ignore
   * @param offset                 ignore -- needed if we have pagination
   * @param limit                  ignore -- needed if we have pagination
   * @return Pointers to available Assets
   */
  @Override
  public Answer<List<Pointer>> listKnowledgeAssets(String assetTypeTag, String assetAnnotationTag,
      String assetAnnotationConcept, Integer offset, Integer limit) {
    var trisotechFileInfoList
        = client.listModels(getXmlMimeTypeByAssetType(assetTypeTag));

    var stream = trisotechFileInfoList
        .skip((null == offset) ? 0 : offset)
        .limit((null == limit || limit < 0) ? Integer.MAX_VALUE : limit)
        .flatMap(this::getAssetPointersForModel)
        .filter(ptr -> matchesType(ptr, assetTypeTag));

    return Answer.of(aggregateVersions(stream));
  }

  private boolean matchesType(Pointer ptr, String assetTypeTag) {
    if (assetTypeTag == null) {
      return true;
    }
    var filterType = KnowledgeAssetTypeSeries.resolveTag(assetTypeTag)
        .or(() -> ClinicalKnowledgeAssetTypeSeries.resolveTag(assetTypeTag));
    return filterType.isPresent() &&
        filterType.get().getReferentId().equals(ptr.getType());
  }

  /**
   * Retrieves Pointers to all Assets declared by a given Model
   * <p>
   * In general, a Model carries one Knowledge Asset, and referencs 0 to many Service Assets
   *
   * @param info the Model manifest
   * @return the Asset Pointers, in a Stream
   */
  private Stream<Pointer> getAssetPointersForModel(SemanticModelInfo info) {
    // getId from fileInfo is the fileID
    try {
      var assetId = names.modelToAssetId(info);
      return assetId.map(aid ->
              Stream.concat(toAssetPointer(aid, info), toServicePointer(info)))
          .orElseGet(Stream::empty);
    } catch (IllegalStateException ise) {
      logger.error(ise.getMessage(), ise);
      return Stream.empty();
    }
  }

  private Stream<Pointer> toServicePointer(SemanticModelInfo info) {
    return info.getExposedServices().stream()
        .map(sid -> {
              var id = names.assetKeyToId(sid);
              return id.toPointer()
                  .withType(getRepresentativeType(id, () -> ReSTful_Service_Specification))
                  .withName(info.getName() + "API TODO")
                  .withHref(hrefBuilder.getHref(id, HrefType.ASSET_VERSION));
            }
        );
  }

  private Stream<Pointer> toAssetPointer(ResourceIdentifier aid, SemanticModelInfo info) {
    return Stream.of(aid.toPointer()
        .withType(getRepresentativeType(aid,
            () -> getDefaultAssetType(info.getMimetype())))
        .withName(info.getName())
        .withHref(hrefBuilder.getHref(aid, HrefType.ASSET_VERSION)));
  }

  /**
   * Chooses one Asset Type to use in an asset Pointer, even when the asset has multiple types, or
   * no types at all.
   * <p>
   * Selects one of the types, based on the ranked preference of {@link #SUPPORTED_ASSET_TYPES}
   *
   * @param id          the asset Id (pointer)
   * @param defaultType the type to be used if the asset does not declare a type explicitly
   * @return the ontology URI of the chosen asset type
   */
  private URI getRepresentativeType(ResourceIdentifier id,
      Supplier<KnowledgeAssetType> defaultType) {
    var types = client.getMetadataByAssetId(id.getUuid(), id.getVersionTag())
        .flatMap(info -> getDeclaredAssetTypes(info, defaultType).stream())
        .distinct()
        .collect(toList());

    if (types.size() == 1) {
      return types.get(0).getReferentId();
    }
    // there should always be at least one type
    var preferred = SUPPORTED_ASSET_TYPES.stream()
        .filter(type -> type.isAnyOf(types))
        .findFirst()
        .orElseThrow(() ->
            new IllegalStateException("Unsupported asset type(s) detected " + types));
    return preferred.getReferentId();
  }


  @Override
  public Answer<Void> clearKnowledgeAssetCatalog() {
    try {
      client.rescan();
      return succeed();
    } catch (Exception e) {
      return failed(e);
    }
  }

  private <T extends SemanticIdentifier> List<T> aggregateVersions(Stream<T> versionIdentifiers) {
    return versionIdentifiers
        .collect(groupingBy(SemanticIdentifier::getUuid))
        .values().stream()
        .map(l -> {
          l.sort(timedSemverComparator());
          return l.get(0);
        }).collect(toList());
  }

  /**
   * corresponds to this uri:  /cat/assets/{assetId}/versions/{versionTag}/carrier KnowledgeCarrier:
   * A Resource that wraps a Serialized, Encoded Knowledge Artifact
   *
   * @param assetId    assetId of the asset
   * @param versionTag version of the asset
   * @param extAccept  'accept' MIME type
   */
  @Override
  public Answer<KnowledgeCarrier> getKnowledgeAssetVersionCanonicalCarrier(UUID assetId,
      String versionTag, String extAccept) {
    try {
      return Answer.ofTry(getLatestAndGreatestCarrier(assetId, versionTag));
    } catch (NotLatestAssetVersionException e) {
      return getKnowledgeCarrierFromOtherVersion(assetId, versionTag, e.getModelUri(), null);
    } catch (NotFoundException e) {
      return Answer.failed(e);
    }
  }

  /**
   * Returns the raw BPM+ Model Artifact for the given Asset Id/Version
   *
   * @param assetId    the asset ID
   * @param versionTag the version tag
   * @param xAccept    content negotiation (not used)
   * @return the binary artifact
   */
  @Override
  public Answer<byte[]> getKnowledgeAssetVersionCanonicalCarrierContent(
      UUID assetId, String versionTag, String xAccept) {
    return getKnowledgeAssetVersionCanonicalCarrier(assetId, versionTag, xAccept)
        .flatOpt(KnowledgeCarrier::asBinary);
  }

  /**
   * Retrieves (a copy of) a specific version of an Artifact. That Artifact must be known to the
   * client to carry at least one expression, in some language, of the given Knowledge Asset.
   * corresponds to this uri:
   * /cat/assets/{assetId}/versions/{versionTag}/carriers/{artifactId}/versions/{artifactVersionTag}
   * only return if the assetId/version is associated with the artifactid/version provided
   *
   * @param assetId            enterprise asset ID
   * @param versionTag         version for the asset
   * @param artifactId         artifact ID
   * @param artifactVersionTag version for the artifact
   */
  @Override
  public Answer<KnowledgeCarrier> getKnowledgeAssetCarrierVersion(UUID assetId,
      String versionTag, UUID artifactId, String artifactVersionTag, String xAccept) {

    try {
      Optional<SemanticModelInfo> manifest = getCarrierInfo(assetId, versionTag, artifactId);
      if (manifest.isPresent()) {
        var info = manifest.get();
        if (matchesVersion(info, artifactVersionTag,
            () -> configuration.getTyped(DEFAULT_VERSION_TAG))) {
          return Answer.ofTry(getCarrier(assetId, versionTag, artifactId));
        } else {
          // artifactId matched, but not version; get other versions to see if one of them matches
          return getKnowledgeCarrierFromOtherVersion(
              assetId, versionTag, info.getId(), artifactVersionTag);
        }
      }
    } catch (NotLatestAssetVersionException e) {
      // something failed to be in the latest version of the artifact, so check all other artifact versions
      // need to confirm the internalId returned in the exception message matches the artifactId requested
      if (e.getModelUri().contains(artifactId.toString())) {
        return getKnowledgeCarrierFromOtherVersion(
            assetId, versionTag, e.getModelUri(), artifactVersionTag);
      } else {
        return notFound();
      }
    } catch (NotFoundException nfe) {
      return Answer.failed(nfe);
    }
    return notFound();
  }

  /**
   * Retrieves the canonical surrogate for the latest version of an asset, wrapped in a
   * KnowledgeCarrier
   * <p>
   * Supports minimal content negotiation between the default format, and the basic HTML
   * transrepresentation
   *
   * @param assetId the Asset Id
   * @param xAccept the generalized mime type
   * @return the Surrogate, in a KnowledgeCarrier
   */
  @Override
  public Answer<KnowledgeCarrier> getKnowledgeAssetCanonicalSurrogate(UUID assetId,
      String xAccept) {
    var surr = getKnowledgeAsset(assetId, null)
        .map(SurrogateHelper::carry);
    return negotiateHTML(xAccept)
        ? surr.flatMap(this::toHtml)
        : surr;
  }

  /**
   * Retrieves the canonical surrogate for the given version of an asset, wrapped in a
   * KnowledgeCarrier
   * <p>
   * Supports minimal content negotiation between the default format, and the basic HTML
   * transrepresentation
   *
   * @param assetId    the Asset Id
   * @param versionTag the Asset version tag
   * @param xAccept    the generalized mime type
   * @return the Surrogate, in a KnowledgeCarrier
   */
  @Override
  public Answer<KnowledgeCarrier> getKnowledgeAssetVersionCanonicalSurrogate(
      UUID assetId, String versionTag, String xAccept) {
    var surr = getKnowledgeAssetVersion(assetId, versionTag, null)
        .map(SurrogateHelper::carry);
    return negotiateHTML(xAccept)
        ? surr.flatMap(this::toHtml)
        : surr;
  }


  /**
   * when asset version does not match for model (artifact) version given, need to search all
   * versions of the artifacts for the assetID. NOTE: This should NOT happen in reality. New
   * artifacts should be created if the assetID changes. This will only match if all ids and tags
   * match.
   *
   * @param assetId         the assetId looking for
   * @param assetVersionTag the version of the asset looking for
   * @param modelUri        the artifactID
   * @param modelVersionTag the version of the artifactID
   * @return KnowledgeCarrier for the version of the artifact with the requested version of the
   * asset
   */
  private Answer<KnowledgeCarrier> getKnowledgeCarrierFromOtherVersion(
      UUID assetId,
      String assetVersionTag,
      String modelUri,
      String modelVersionTag) {
    List<TrisotechFileInfo> modelVersions =
        client.getVersionsMetadataByModelId(modelUri);
    // reverse the list so the most recent version that matches is selected
    // there can be multiple versions of the artifact that map to one version of asset
    Collections.reverse(modelVersions);
    for (TrisotechFileInfo model : modelVersions) {
      // skip any that are not published
      if (null == model.getVersion() && null == model.getState()) {
        continue;
      }

      Optional<Document> downloadXml = client.getModel(model);
      if (downloadXml.isEmpty()) {
        continue;
      }

      Document dox = downloadXml.get();
      // check assetId for each version
      Optional<ResourceIdentifier> assetOpt =
          extractAssetIdFromDocument(dox, configuration.getTyped(ASSET_ID_ATTRIBUTE));
      if (assetOpt.isEmpty()) {
        continue;
      }

      var rep = getRepLanguage(model.getMimetype());
      if (rep.isEmpty()) {
        continue;
      }

      ResourceIdentifier resolvedAssetId = assetOpt.get();
      if (assetId.equals(resolvedAssetId.getUuid())
          && assetVersionTag.equals(resolvedAssetId.getVersionTag())
          && (modelVersionTag == null || modelVersionTag.equals(model.getVersion()))) {

        return Answer.of(
            buildCarrierFromNativeModel(
                assetId,
                assetVersionTag,
                names.modelToArtifactId(model),
                rep.get(),
                dox));
      }
    }
    return notFound();
  }

  @Override
  public Answer<CompositeKnowledgeCarrier> getAnonymousCompositeKnowledgeAssetCarrier(UUID assetId,
      String versionTag, String xAccept) {
    var rootId = newId(assetId, versionTag);
    Set<KeyIdentifier> closure = getAssetClosure(rootId)
        .collect(Collectors.toSet());

    Answer<Set<KnowledgeCarrier>> componentArtifacts = closure.stream()
        .map(comp -> getKnowledgeAssetVersionCanonicalCarrier(comp.getUuid(),
            comp.getVersionTag(),
            xAccept))
        .collect(Answer.toSet());

    return componentArtifacts
        .map(comps -> ofMixedAnonymousComposite(rootId, comps));
  }

  @Override
  public Answer<CompositeKnowledgeCarrier> getAnonymousCompositeKnowledgeAssetSurrogate(
      UUID assetId, String versionTag, String xAccept) {
    var rootId = newId(assetId, versionTag);
    Set<KeyIdentifier> closure = getAssetClosure(rootId)
        .collect(Collectors.toSet());

    Answer<Set<KnowledgeCarrier>> componentSurrogates = closure.stream()
        .map(comp -> getKnowledgeAssetVersion(comp.getUuid(),
            comp.getVersionTag(),
            xAccept)
            .map(SurrogateHelper::carry))
        .collect(Answer.toSet());

    return componentSurrogates
        .map(comps -> ofUniformAnonymousComposite(rootId, comps));
  }

  private Stream<KeyIdentifier> getAssetClosure(ResourceIdentifier assetRoot) {
    return client.getMetadataByAssetId(assetRoot.getUuid(), assetRoot.getVersionTag())
        .flatMap(this::getAssetClosure);
  }

  private Stream<KeyIdentifier> getAssetClosure(SemanticModelInfo modelRoot) {
    return Stream.concat(
        Stream.ofNullable(modelRoot.getAssetKey()),
        modelRoot.getModelDependencies().stream()
            .flatMap(depId -> client.getMetadataByModelId(depId).stream())
            .flatMap(this::getAssetClosure));
  }


  private Optional<SemanticModelInfo> getCarrierInfo(
      UUID assetId, String versionTag, UUID artifactId) {
    return client.getMetadataByAssetId(assetId, versionTag)
        .filter(info -> info.getId().contains(artifactId.toString()))
        .findFirst();
  }

  private Optional<KnowledgeCarrier> getCarrier(
      UUID assetId, String versionTag, UUID artifactId)
      throws NotFoundException, NotLatestAssetVersionException {
    var manifest = getCarrierInfo(assetId, versionTag, artifactId);
    return manifest.flatMap(info -> getCarrierByModel(assetId, versionTag, info));
  }


  private Optional<KnowledgeCarrier> getLatestAndGreatestCarrier(
      UUID assetId, String versionTag)
      throws NotFoundException, NotLatestAssetVersionException {
    var manifest = client.getMetadataByAssetId(assetId, versionTag)
        .sorted()
        .findFirst();
    return manifest.flatMap(info -> getCarrierByModel(assetId, versionTag, info));
  }

  private Optional<KnowledgeCarrier> getCarrierByModel(
      UUID assetId, String versionTag, SemanticModelInfo info)
      throws NotFoundException, NotLatestAssetVersionException {

    return
        getRepLanguage(info).flatMap(lang ->
            client.getModel(info).map(xml ->
                buildCarrierFromNativeModel(
                    assetId,
                    versionTag,
                    names.modelToArtifactId(info.getId(), info.getVersion(), info.getName()),
                    lang,
                    xml)));
  }

  private KnowledgeCarrier buildCarrierFromNativeModel(
      UUID assetId, String versionTag,
      ResourceIdentifier artifactId,
      SyntacticRepresentation rep,
      Document dox) {

    return
        AbstractCarrier.of(XMLUtil.toByteArray(dox))
            .withRepresentation(rep)
            .withArtifactId(artifactId)
            .withLabel(artifactId.getName())
            .withAssetId(newId(names.getAssetNamespace(), assetId, versionTag));

  }


  private Optional<KnowledgeAsset> getKnowledgeAssetForModels(
      UUID assetId,
      Collection<SemanticModelInfo> modelInfos) {
    var models = modelInfos.stream()
        .collect(toMap(
            info -> info,
            info -> client.getModel(info)
        ));
    return getKnowledgeAssetForModels(assetId, models);
  }

  private Optional<KnowledgeAsset> getKnowledgeAssetForModels(
      UUID assetId,
      Map<SemanticModelInfo, Optional<Document>> modelInfos) {

    // extract data from Trisotech format to OMG format
    return extractor.introspect(assetId, modelInfos);
  }

  private boolean negotiateHTML(String xAccept) {
    return decodeAll(xAccept).stream().findFirst()
        .filter(wr -> HTML.sameAs(wr.getRep().getLanguage()))
        .isPresent();
  }

  /**
   * Converts a Surrogate, wrapped in a KnowledgeCarrier, to its HTML variant
   * <p>
   * Redirects the Asset namespace base URI to this server, making the links in the HTML more
   * navigable. Note that this redirect is a best effort operation, which is not guaranteed.
   *
   * @param surrogateCarrier the KnowledgeAsset, in a KnowledgeCarrier
   * @return the KnowledgeAsset HTML variant, in a KnowledgeCarrier, wrapped by Answer
   */
  private Answer<KnowledgeCarrier> toHtml(KnowledgeCarrier surrogateCarrier) {
    String xCfg = null;
    try {
      Properties props = new Properties();
      var host = URI.create(hrefBuilder.getHost());
      var ns = names.getAssetNamespace();
      var redirect = new URI(host.getScheme(), null, host.getHost(), host.getPort(),
          host.getPath() + "/cat" + ns.getPath(), null, null);
      props.put(ns.toString(), redirect.toString());

      var ns2 = names.getArtifactNamespace();
      var redirect2 = new URI(host.getScheme(), null, host.getHost(), host.getPort(),
          host.getPath() + "/repos/" + ALL_REPOS + ns2.getPath(), null, null);
      props.put(ns2.toString(), redirect2.toString());
      xCfg = serializeProps(props);
    } catch (Exception e) {
      // fall back to not rewriting the URIs/URLs
    }
    return htmlTranslator.applyTransrepresent(surrogateCarrier, codedRep(HTML), xCfg);
  }

}
