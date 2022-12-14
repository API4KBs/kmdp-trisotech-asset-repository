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

import static edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.TrisotechMetadataHelper.getDefaultAssetType;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.TrisotechServiceIntrospectionStrategy.mintServiceName;
import static edu.mayo.kmdp.registry.Registry.BASE_UUID_URN_URI;
import static edu.mayo.kmdp.trisotechwrapper.config.TrisotechApiUrls.getXmlMimeTypeByAssetType;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.Answer.conflict;
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
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.ReSTful_Service_Specification;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Semantic_Decision_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.HTML;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;

import edu.mayo.kmdp.kdcaci.knew.trisotech.TTAssetRepositoryConfig.TTWParams;
import edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.MetadataIntrospector;
import edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.TrisotechMetadataHelper;
import edu.mayo.kmdp.kdcaci.knew.trisotech.components.redactors.Redactor;
import edu.mayo.kmdp.kdcaci.knew.trisotech.components.weavers.Weaver;
import edu.mayo.kmdp.kdcaci.knew.trisotech.exception.NotFoundException;
import edu.mayo.kmdp.kdcaci.knew.trisotech.exception.NotLatestAssetVersionException;
import edu.mayo.kmdp.language.translators.surrogate.v2.SurrogateV2toHTMLTranslator;
import edu.mayo.kmdp.trisotechwrapper.TrisotechWrapper;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.util.XMLUtil;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import org.apache.http.HttpException;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.server.KnowledgeAssetCatalogApiInternal;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.server.KnowledgeAssetRepositoryApiInternal;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.TransxionApiInternal._applyTransrepresent;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
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
import org.omg.spec.api4kp._20200801.terms.ConceptTerm;
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

  @Autowired
  private TrisotechWrapper client;

  @Autowired
  private IdentityMapper mapper;

  @Autowired
  private Weaver weaver;
  @Autowired
  private MetadataIntrospector extractor;
  @Autowired
  private Redactor redactor;

  @Autowired(required = false)
  private KARSHrefBuilder hrefBuilder;

  private final _applyTransrepresent htmlTranslator = new SurrogateV2toHTMLTranslator();

  @Autowired(required = false)
  private TTAssetRepositoryConfig configuration;
  @Autowired
  private NamespaceManager names;

  private boolean publishedOnly;

  public TrisotechAssetRepository() {
    //
  }

  @PostConstruct
  void init() {
    if (configuration == null) {
      configuration = new TTAssetRepositoryConfig();
    }
    if (hrefBuilder == null) {
      hrefBuilder = new KARSHrefBuilder(configuration);
    }
    publishedOnly = configuration.getTyped(TTWParams.PUBLISHED_ONLY);
  }

  @Override
  public Answer<KnowledgeAssetCatalog> getKnowledgeAssetCatalog() {
    return Answer.of(new KnowledgeAssetCatalog()
        .withName("KMDP Trisotech DES Wrapper")
        .withId(newId(BASE_UUID_URN_URI, "TTW"))
        .withOwner("KMDP / MEA")
        .withSurrogateModels(rep(Knowledge_Asset_Surrogate_2_0, XML_1_1))
        .withSupportedAssetTypes(
            Decision_Model,
            Semantic_Decision_Model,
            Naturalistic_Decision_Model,
            Computable_Decision_Model,
            Clinical_Decision_Model,

            Care_Process_Model,
            Cognitive_Care_Process_Model,
            Case_Management_Model,
            Clinical_Case_Management_Model,

            Clinical_Eligibility_Rule,
            Clinical_Rule,
            Clinical_Calculation_Rule,
            Clinical_Contextualization_Rule,
            Clinical_Guidance_Rule,
            Clinical_Enrollment_Rule,
            Clinical_Inference_Rule,

            ReSTful_Service_Specification));
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

    Optional<String> modelId = mapper.getCurrentModelId(assetId, false);

    if (modelId.isEmpty()) {
      return Answer.ofTry(Optional.empty(), newId(assetId),
          () -> "No Asset found for the given ID");
    }
    // get the modelInfo for the latest artifactVersion
    return Answer.ofTry(
        client.getLatestModelFileInfo(modelId.get(), publishedOnly)
            .flatMap(modelInfo ->
                getKnowledgeAssetForModel(assetId, modelId.get(), modelInfo)));
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
      String internalId = mapper.resolveInternalArtifactID(assetId, versionTag, publishedOnly);

      return Answer.ofTry(
          client.getLatestModelFileInfo(internalId, publishedOnly)
              .flatMap(tfi -> getKnowledgeAssetForModel(assetId, internalId, tfi)));
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
    List<TrisotechFileInfo> modelVersions =
        client.getModelVersions(internalId);

    // reverse the list so the most recent version that matches is selected
    // there can be multiple versions of the artifact that map to one version of asset
    Collections.reverse(modelVersions);
    for (TrisotechFileInfo modelVersionInfo : modelVersions) {
      // skip any that are not published
      if (null == modelVersionInfo.getVersion() && null == modelVersionInfo.getState()) {
        continue;
      }

      Optional<KnowledgeAsset> surr =
          client.getModel(modelVersionInfo)
              .flatMap(dox ->
                  mapper.extractAssetIdFromDocument(dox)
                      .filter(axId -> versionTag.equals(axId.getVersionTag())
                          && assetId.equals(axId.getUuid()))
                      .flatMap(axId -> getKnowledgeAssetForModel(assetId, dox, modelVersionInfo))
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
    List<TrisotechFileInfo> trisotechFileInfoList
        = client.getModelsFileInfo(getXmlMimeTypeByAssetType(assetTypeTag), publishedOnly);

    List<Pointer> assetList = trisotechFileInfoList.stream()
        .skip((null == offset) ? 0 : offset)
        .limit((null == limit || limit < 0) ? Integer.MAX_VALUE : limit)
        .flatMap(this::getAssetPointersForModel)
        .collect(Collectors.toList());

    if (assetTypeTag != null) {
      Optional<KnowledgeAssetType> type = KnowledgeAssetTypeSeries.resolveTag(assetTypeTag)
          .or(() -> ClinicalKnowledgeAssetTypeSeries.resolveTag(assetTypeTag));
      return Answer.of(type.map(ConceptTerm::getReferentId)
          .map(typeUri -> assetList.stream()
              .filter(ptr -> ptr.getType().equals(typeUri))
              .collect(Collectors.toList()))
          .orElse(Collections.emptyList()));
    }
    return Answer.of(aggregateVersions(assetList));
  }

  /**
   * Retrieves Pointers to all Assets declared by a given Model
   * <p>
   * In general, a Model carries one Knowledge Asset, and referencs 0 to many Service Assets
   *
   * @param trisotechFileInfo the Model manifest
   * @return the Asset Pointers, in a Stream
   */
  private Stream<Pointer> getAssetPointersForModel(TrisotechFileInfo trisotechFileInfo) {
    // getId from fileInfo is the fileID
    try {
      var assetId = mapper
          .resolveEnterpriseAssetID(trisotechFileInfo.getId());
      var ptr1 = assetId.map(id -> id.toPointer()
          .withType(
              mapper.getDeclaredAssetType(id)
                  .orElseGet(() -> getDefaultAssetType(trisotechFileInfo.getMimetype()))
                  .getReferentId())
          .withName(trisotechFileInfo.getName())
          .withHref(hrefBuilder.getHref(id, HrefType.ASSET))
      );

      var serviceIds = mapper
          .resolveEnterpriseServiceIDs(trisotechFileInfo.getId());
      var ptr2 = serviceIds.map(id -> id.toPointer()
          .withType(
              mapper.getDeclaredAssetType(id)
                  .orElse(ReSTful_Service_Specification)
                  .getReferentId())
          .withName(mintServiceName(trisotechFileInfo, id.getName()))
          .withHref(hrefBuilder.getHref(id, HrefType.ASSET))
      );

      return Stream.concat(ptr1.stream(), ptr2);
    } catch (IllegalStateException ise) {
      logger.error(ise.getMessage(), ise);
      return Stream.empty();
    }
  }


  @Override
  public Answer<Void> clearKnowledgeAssetCatalog() {
    return client.clearCache()
        ? succeed()
        : failed();
  }

  private <T extends SemanticIdentifier> List<T> aggregateVersions(List<T> versionIdentifiers) {
    return versionIdentifiers.stream()
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
      return Answer.ofTry(getCarrier(assetId, versionTag));
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

    Optional<String> fileId = mapper.getCurrentModelId(assetId, false);
    // fileId is not found in the extractor for the assetId provided; fail
    if (fileId.isEmpty()) {
      return notFound();
    }
    //  1. Check  if assetId is latest (no server call needed)
    //  2. If assetId is not latest (exception), query versions from server
    //  3. process versions; if found, return succeed, else 404
    //  4. else if assetId is latest (no exception), get the artifact & confirm the one requested
    //  5. if artifact matches request, check version
    //  6. if version of artifact matches request, process
    //  7. else, version does not match, query other versions from server
    //  8. see step #3
    // first check asset id -- save server calls until needed.
    try {
      // getEnterpriseAssetVersionIdForAsset will throw exception if doesn't exist on latest
      Optional<ResourceIdentifier> enterpriseVersionAssetId = mapper
          .resolveAssetToCurrentAssetId(assetId, versionTag, false);

      if (enterpriseVersionAssetId.isEmpty()) {
        // should never happen...
        return notFound();
      }

      // asset matches for latest, now check the artifact for asset
      String modelUri = mapper.resolveInternalArtifactID(assetId, versionTag, false);
      // verify artifact for asset matches the artifactId requested
      if (modelUri.contains(artifactId.toString())) {
        // confirm version too
        Optional<String> artifactVersion = mapper.getLatestCarrierVersionTag(assetId, versionTag);
        Optional<String> artifactVersionTimestamp =
            mapper.getLatestCarrierTimestampedVersionTag(assetId, versionTag);

        if (artifactVersion.orElse("").equals(artifactVersionTag)
            || artifactVersionTimestamp.orElse("").equals(artifactVersionTag)) {
          return Answer.ofTry(getCarrier(assetId, versionTag));
        } else {
          // artifactId matched, but not version; get other versions to see if one of them matches
          return getKnowledgeCarrierFromOtherVersion(
              assetId, versionTag, modelUri, artifactVersionTag);
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
        ? surr.flatMap(ka -> htmlTranslator.applyTransrepresent(ka, codedRep(HTML), null))
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
        ? surr.flatMap(ka -> htmlTranslator.applyTransrepresent(ka, codedRep(HTML), null))
        : surr;
  }

  /**
   * Get the Representation for the language of the model given the model document. creates a
   * Representation object that can be used in .withRepresentation() method of creating a Carrier,
   * for example.
   *
   * @param dox the document of the model
   * @return SyntacticRepresentation which has the values set for the language of this model
   */
  private SyntacticRepresentation getLanguageRepresentationForModel(Document dox) {
    return TrisotechMetadataHelper.getRepLanguage(dox)
        .orElse(null);
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
        client.getModelVersions(modelUri);
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
      Optional<ResourceIdentifier> assetOpt = mapper.extractAssetIdFromDocument(dox);
      if (assetOpt.isEmpty()) {
        continue;
      }

      ResourceIdentifier resolvedAssetId = assetOpt.get();
      if (assetId.equals(resolvedAssetId.getUuid())
          && assetVersionTag.equals(resolvedAssetId.getVersionTag())
          && (modelVersionTag == null || modelVersionTag.equals(model.getVersion()))) {

        return Answer.ofTry(
            buildCarrierFromNativeModel(
                assetId,
                assetVersionTag,
                names.rewriteInternalId(model),
                dox));
      }
    }
    return notFound();
  }

  /**
   * Support for updating a model 'in place' The assetId, versionTag, artifactId and
   * artifactVersionTag should match the current model, any mismatch will fail as NOT_FOUND.
   *
   * @param assetId            enterprise asset ID
   * @param versionTag         version for the asset
   * @param artifactId         artifact ID
   * @param artifactVersionTag version for the artifact
   * @param exemplar           updated artifact to be posted to the server
   * @return return status
   */
  @Override
  public Answer<Void> setKnowledgeAssetCarrierVersion(UUID assetId, String versionTag,
      UUID artifactId, String artifactVersionTag, byte[] exemplar) {
    String internalId;
    Optional<String> fileId = mapper.getCurrentModelId(assetId, false);

    // fileId is not found in the extractor for the assetId provided; fail
    // At this time, not allowing for create, so if no fileId, fail
    if (fileId.isEmpty()) {
      return notFound();
    }
    String mimeType = mapper.getMimetype(assetId, versionTag);

    // 1. Check if assetId is latest (no server call needed)
    // 2. If asset is not latest (exception), return NOT_FOUND
    // 3. If asset is latest (no exception), get the artifact & confirm it matches the one requested
    // 4. If artifact matches request, check version
    // 5. If version of artifact matches request, process
    // 6. If version of artifact does not match, return NOT_FOUND
    // first check asset id -- save server calls until needed
    try {
      // getEnterpriseAssetVersionIdForAsset will throw exception if doesn't exist on latest
      Optional<ResourceIdentifier> enterpriseVersionAssetId = mapper
          .resolveAssetToCurrentAssetId(assetId, versionTag, false);
      if (enterpriseVersionAssetId
          .isEmpty()) { // should never happen, as exception should be thrown instead
        return notFound();
      }

      // asset matches for latest, now check the artifact for asset
      internalId = mapper.resolveInternalArtifactID(assetId, versionTag, false);
      Optional<TrisotechFileInfo> tfi =
          fileId
              .flatMap(x -> client.getLatestModelFileInfo(x, false));

      // verify artifact for asset matches the artifactId requested
      if (tfi.isPresent() && internalId.contains(artifactId.toString())) {
        // confirm version too
        Optional<String> artifactVersion = mapper.getLatestCarrierVersionTag(assetId, versionTag);

        if (artifactVersion.isPresent()) {
          if (artifactVersion.get().equals(artifactVersionTag)) {
            uploadFile(artifactVersionTag, exemplar, tfi.get(), mimeType);
            return succeed();
          } else {
            return conflict();
          }
        } else {
          // ok for version to not be present, in fact, preferred
          uploadFile(null, exemplar, tfi.get(), mimeType);
          return succeed();
        }
      }
    } catch (NotLatestAssetVersionException | NotFoundException e) {
      return notFound();
    } catch (IOException | HttpException e) {
      logger.error(e.getMessage(), e);
      return Answer.failed(e);
    }
    return notFound();
  }

  private void uploadFile(String artifactVersionTag, byte[] exemplar,
      TrisotechFileInfo trisotechFileInfo, String mimeType) throws IOException, HttpException {
    client
        .uploadXmlModel(trisotechFileInfo.getPath(), trisotechFileInfo.getName(), mimeType,
            artifactVersionTag,
            trisotechFileInfo.getState(), exemplar);
  }

  private Optional<KnowledgeCarrier> getCarrier(
      UUID assetId, String versionTag) throws NotFoundException, NotLatestAssetVersionException {

    Optional<Document> modelDox = mapper
        .getCurrentModelId(assetId, false)
        .flatMap(x -> dowloadLatestModelVersion(x, publishedOnly));

    ResourceIdentifier artifactId = mapper.getCarrierArtifactId(assetId, versionTag);
    return modelDox.flatMap(
        xml -> buildCarrierFromNativeModel(assetId, versionTag, artifactId, xml));
  }

  private Optional<KnowledgeCarrier> buildCarrierFromNativeModel(
      UUID assetId, String versionTag,
      ResourceIdentifier artifactId, Document dox) {

    Optional<Document> modelDox = Optional.of(dox)
        .map(weaver::weave)
        .map(redactor::redact);

    return modelDox.map(xml ->
        AbstractCarrier.of(XMLUtil.toByteArray(xml))
            .withRepresentation(getLanguageRepresentationForModel(xml))
            .withArtifactId(artifactId)
            .withAssetId(newId(names.getAssetNamespace(), assetId, versionTag))
    );
  }


  private Optional<KnowledgeAsset> getKnowledgeAssetForModel(
      UUID assetId,
      String modelId,
      TrisotechFileInfo modelInfo) {
    return dowloadLatestModelVersion(modelId, publishedOnly)
        .flatMap(dox -> getKnowledgeAssetForModel(assetId, dox, modelInfo));
  }

  private Optional<KnowledgeAsset> getKnowledgeAssetForModel(
      UUID assetId, Document dox, TrisotechFileInfo modelInfo) {
    Optional<Document> modelDocument = Optional.of(dox);

    // weave in KMD information
    Optional<Document> wovenDocument = modelDocument
        .map(weaver::weave)
        .map(redactor::redact);

    // extract data from Trisotech format to OMG format
    return wovenDocument
        .flatMap(wd ->
            extractor.extract(assetId, wd, modelInfo));
  }


  private Optional<Document> dowloadLatestModelVersion(String modelId, boolean publishedOnly) {
    return client.getModelById(modelId, publishedOnly);
  }

  private boolean negotiateHTML(String xAccept) {
    return decodeAll(xAccept).stream().findFirst()
        .filter(wr -> HTML.sameAs(wr.getRep().getLanguage()))
        .isPresent();
  }

}
