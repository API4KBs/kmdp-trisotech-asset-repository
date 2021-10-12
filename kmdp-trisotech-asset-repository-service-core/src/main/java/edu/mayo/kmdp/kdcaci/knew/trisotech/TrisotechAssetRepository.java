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

import static edu.mayo.kmdp.registry.Registry.BASE_UUID_URN_URI;
import static edu.mayo.kmdp.trisotechwrapper.config.TrisotechApiUrls.CMMN_LOWER;
import static edu.mayo.kmdp.trisotechwrapper.config.TrisotechApiUrls.DMN_LOWER;
import static edu.mayo.kmdp.trisotechwrapper.config.TrisotechApiUrls.getXmlMimeTypeByAssetType;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.Answer.conflict;
import static org.omg.spec.api4kp._20200801.Answer.failed;
import static org.omg.spec.api4kp._20200801.Answer.notFound;
import static org.omg.spec.api4kp._20200801.Answer.succeed;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newId;
import static org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries.Care_Process_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Decision_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;

import edu.mayo.kmdp.kdcaci.knew.trisotech.TTAssetRepositoryConfig.TTWParams;
import edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.MetadataIntrospector;
import edu.mayo.kmdp.kdcaci.knew.trisotech.components.redactors.Redactor;
import edu.mayo.kmdp.kdcaci.knew.trisotech.components.weavers.Weaver;
import edu.mayo.kmdp.kdcaci.knew.trisotech.exception.NotFoundException;
import edu.mayo.kmdp.kdcaci.knew.trisotech.exception.NotLatestVersionException;
import edu.mayo.kmdp.trisotechwrapper.TrisotechWrapper;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.util.StreamUtil;
import edu.mayo.kmdp.util.XMLUtil;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.apache.http.HttpException;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.server.KnowledgeAssetCatalogApiInternal;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.server.KnowledgeAssetRepositoryApiInternal;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.KPServer;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.services.repository.KnowledgeAssetCatalog;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
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
    publishedOnly = configuration.getTyped(TTWParams.PUBLISHED_ONLY);
  }

  @Override
  public Answer<KnowledgeAssetCatalog> getKnowledgeAssetCatalog() {
    return Answer.of(new KnowledgeAssetCatalog()
        .withName("KMDP Trisotech DES Wrapper")
        .withId(newId(BASE_UUID_URN_URI, "TTW"))
        .withOwner("KMDP / MEA")
        .withSurrogateModels(rep(Knowledge_Asset_Surrogate_2_0, XML_1_1))
        .withSupportedAssetTypes(Decision_Model, Care_Process_Model));
  }


  /**
   * Of all the versions in the series, several criteria concur to determine the LATEST, including
   * the time at which a version was created, the (partial) ordering of the version tags, and the
   * association of that version of the Asset with an Artifact in a "published" state
   *
   * @param assetId The *Knowledge Asset* UUID There should be exactly one Artifact (Model)
   *                annotated with this UUID
   * @param xAccept content negotiation paramater that controls the manifestation of the returned
   *                surrogate (not supported)
   * @return The asset surrogate for a given Asset ID
   */
  @Override
  public Answer<KnowledgeAsset> getKnowledgeAsset(UUID assetId, String xAccept) {
    // need the modelId of the model in order to query for modelInfo
    Optional<String> modelId = mapper.getCurrentModelId(assetId, publishedOnly);
    // want the latest artifactVersion of the model
    Optional<String> artifactVersion = mapper.getLatestCarrierVersionTag(assetId);

    if (modelId.isEmpty()
        || (publishedOnly && artifactVersion.isEmpty())) {
      return Answer.of(Optional.empty());
    }
    // get the modelInfo for the latest artifactVersion
    return Answer.of(
        client.getLatestModelFileInfo(modelId.get())
            .flatMap(modelInfo ->
                getKnowledgeAssetForModel(modelId.get(), modelInfo)));
  }


  // TODO: versionTag is version for ASSET (yes? yes). Q: for Davide: triples don't return artifacts by version,
  //  just the latest version; is this code intended to get the artifactID that corresponds to the specific asset version? yes
  //  so for assetId ab356 version 1.0.1 need to get the version of the artifact CD3 that matches that version.
  //  might want to keep a map of those that have been scanned so don't have to scan again?
  //  scan all versions of so have saved? or only scan until find what we need? CAO
  @Override
  public Answer<KnowledgeAsset> getKnowledgeAssetVersion(UUID assetId,
      String versionTag, String xAccept) {
    String internalId;

    // For assetId, find artifactId; For assetId/versionTag, is latest artifactId/version a match?
    // if not, get versions of artifactId and weave each one to get assetId/version
    try {
      internalId = mapper.resolveInternalArtifactID(assetId, versionTag, true);

      // the url is available from the extractor, HOWEVER, it is not the correct form to retrieve
      // the file in the correct format (xml) so need to make a server call now to get the proper URL
      return Answer.of(
          client.getLatestModelFileInfo(internalId)
              .flatMap(tfi -> getKnowledgeAssetForModel(internalId, tfi)));

    } catch (NotLatestVersionException e) {
      logger.debug(e.getMessage());
      // check other versions of the model
      try {
        return Answer.of(findArtifactVersionForAsset(e.getModelUri(), assetId, versionTag));
      } catch (NotFoundException notFoundException) {
        return notFound();
      }
    } catch (NotFoundException nfe) {
      return notFound();
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
        client.getModelVersions(internalId, mapper.getMimetype(assetId));

    // reverse the list so the most recent version that matches is selected
    // there can be multiple versions of the artifact that map to one version of asset
    Collections.reverse(modelVersions);
    for (TrisotechFileInfo modelVersionInfo : modelVersions) {
      // skip any that are not published
      if (null == modelVersionInfo.getVersion() && null == modelVersionInfo.getState()) {
        continue;
      }

      Optional<KnowledgeAsset> surr =
          client.downloadXmlModel(modelVersionInfo.getUrl())
              .flatMap(dox ->
                  mapper.extractAssetIdFromDocument(dox)
                      .filter(axId -> versionTag.equals(axId.getVersionTag())
                          && assetId.equals(axId.getUuid()))
                      .flatMap(axId -> getKnowledgeAssetForModel(dox, modelVersionInfo))
              );

      if (surr.isPresent()) {
        return surr.get();
      }
    }
    // have gone through all versions of the artifact and not found...
    throw new NotFoundException("No artifact for asset " + assetId + " version: " + versionTag);
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
        .map(trisotechFileInfo -> {
          // getId from fileInfo is the fileID
          Pointer ptr = null;
          try {
            ResourceIdentifier assetId = mapper
                .resolveEnterpriseAssetID(trisotechFileInfo.getId());
            ptr = assetId.toPointer()
                .withType(
                    mapper.getDeclaredAssetTypeOrDefault(trisotechFileInfo.getId()).getReferentId())
                .withName(trisotechFileInfo.getName());
          } catch (IllegalStateException ise) {
            logger.error(ise.getMessage(), ise);
          }
          return Optional.ofNullable(ptr);
        })
        .flatMap(StreamUtil::trimStream)
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
    return Answer.of(assetList);
  }

  @Override
  public Answer<Void> clearKnowledgeAssetCatalog() {
    return client.clearCache()
        ? succeed()
        : failed();
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
    Optional<ResourceIdentifier> enterpriseVersionId;
    try {
      enterpriseVersionId = mapper
          .resolveAssetToCurrentAssetId(assetId, versionTag, publishedOnly);
      return enterpriseVersionId.isPresent()
          ? Answer.of(getCarrier(assetId, versionTag))
          : notFound();
    } catch (NotLatestVersionException e) {
      return getKnowledgeCarrierFromOtherVersion(assetId, versionTag, e.getModelUri(), null);
    }
  }


  /**
   * Retrieves (a copy of) a specific version of an Artifact. That Artifact must be known to the
   * client to carry at least one expression, in some language, of the given Knowledge Asset.
   * corresponds to this uri: /cat/assets/{assetId}/versions/{versionTag}/carriers/{artifactId}/versions/{artifactVersionTag}
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
        Optional<String> artifactVersion = mapper.getLatestCarrierVersionTag(assetId);
        Optional<String> artifactVersionTimestamp =
            mapper.getLatestCarrierTimestampedVersionTag(assetId);

        if (artifactVersion.orElse("").equals(artifactVersionTag)
            || artifactVersionTimestamp.orElse("").equals(artifactVersionTag)) {
          return Answer.of(getCarrier(assetId, versionTag));
        } else {
          // artifactId matched, but not version; get other versions to see if one of them matches
          return getKnowledgeCarrierFromOtherVersion(
              assetId, versionTag, modelUri, artifactVersionTag);
        }
      }
    } catch (NotLatestVersionException e) {
      // something failed to be in the latest version of the artifact, so check all other artifact versions
      // need to confirm the internalId returned in the exception message matches the artifactId requested
      if (e.getModelUri().contains(artifactId.toString())) {
        return getKnowledgeCarrierFromOtherVersion(
            assetId, versionTag, e.getModelUri(), artifactVersionTag);
      } else {
        return notFound();
      }
    } catch (NotFoundException nfe) {
      // default
    }
    return notFound();
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
    return extractor.getRepLanguage(dox, true)
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
        client.getModelVersions(modelUri, mapper.getMimetype(assetId));
    // reverse the list so the most recent version that matches is selected
    // there can be multiple versions of the artifact that map to one version of asset
    Collections.reverse(modelVersions);
    for (TrisotechFileInfo model : modelVersions) {
      // skip any that are not published
      if (null == model.getVersion() && null == model.getState()) {
        continue;
      }

      Optional<Document> downloadXml = client.downloadXmlModel(model.getUrl());
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

        return Answer.of(
            getCarrier(
                assetId,
                assetVersionTag,
                names.rewriteInternalId(model),
                dox));
      }
    }
    return notFound();
  }

  // to upload the "dictionary" DMN model

  /**
   * Support for updating a model 'in place' The assetId, versionTag, artifactId and
   * artifactVersionTag should match the current model, any mismatch will fail as NOT_FOUND. TODO:
   * Confirm w/Davide which cases should fail and why
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
    String mimeType = mapper.getMimetype(assetId);

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
              .flatMap(client::getLatestModelFileInfo);

      // verify artifact for asset matches the artifactId requested
      if (tfi.isPresent() && internalId.contains(artifactId.toString())) {
        // confirm version too
        Optional<String> artifactVersion = mapper.getLatestCarrierVersionTag(assetId);

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
    } catch (NotLatestVersionException | NotFoundException e) {
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
      UUID assetId, String versionTag) {

    try {
      Optional<Document> modelDox = mapper
          .getCurrentModelId(assetId, publishedOnly)
          .flatMap(this::dowloadLatestModelVersion);

      ResourceIdentifier artifactId = mapper.getCarrierArtifactId(assetId, versionTag);
      return modelDox.flatMap(xml -> getCarrier(assetId, versionTag, artifactId, xml));
    } catch (NotLatestVersionException | NotFoundException e) {
      e.printStackTrace();
    }

    return Optional.empty();
  }

  private Optional<KnowledgeCarrier> getCarrier(
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
      String modelId,
      TrisotechFileInfo modelInfo) {
    return dowloadLatestModelVersion(modelId)
        .flatMap(dox -> getKnowledgeAssetForModel(dox, modelInfo));
  }

  private Optional<KnowledgeAsset> getKnowledgeAssetForModel(
      Document dox, TrisotechFileInfo modelInfo) {
    Optional<Document> modelDocument = Optional.of(dox);

    // weave in KMD information
    Optional<Document> wovenDocument = modelDocument
        .map(weaver::weave)
        .map(redactor::redact);

    // extract data from Trisotech format to OMG format
    return wovenDocument
        .map(wd -> extractor.extract(wd, modelInfo));
  }


  private Optional<Document> dowloadLatestModelVersion(String modelId) {
    return client.getModelById(modelId);
  }

  private boolean isDMNModel(TrisotechFileInfo fileInfo) {
    return fileInfo.getMimetype().contains(DMN_LOWER);
  }

  private boolean isCMMNModel(TrisotechFileInfo fileInfo) {
    return fileInfo.getMimetype().contains(CMMN_LOWER);
  }


}
