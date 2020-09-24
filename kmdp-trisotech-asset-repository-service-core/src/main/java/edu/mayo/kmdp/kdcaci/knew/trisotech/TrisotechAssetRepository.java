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
package edu.mayo.kmdp.kdcaci.knew.trisotech;

import static edu.mayo.kmdp.registry.Registry.MAYO_ARTIFACTS_BASE_URI_URI;
import static edu.mayo.kmdp.registry.Registry.MAYO_ASSETS_BASE_URI_URI;
import static edu.mayo.kmdp.trisotechwrapper.TrisotechApiUrls.CMMN_UPPER;
import static edu.mayo.kmdp.trisotechwrapper.TrisotechApiUrls.DMN_LOWER;
import static edu.mayo.kmdp.trisotechwrapper.TrisotechApiUrls.DMN_UPPER;
import static java.nio.charset.Charset.defaultCharset;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.Answer.unsupported;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Care_Process_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Decision_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.CMMN_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.DMN_1_2;
import static org.omg.spec.api4kp._20200801.taxonomy.krserialization.KnowledgeRepresentationLanguageSerializationSeries.CMMN_1_1_XML_Syntax;
import static org.omg.spec.api4kp._20200801.taxonomy.krserialization.KnowledgeRepresentationLanguageSerializationSeries.DMN_1_2_XML_Syntax;

import edu.mayo.kmdp.kdcaci.knew.trisotech.preprocess.NotLatestVersionException;
import edu.mayo.kmdp.kdcaci.knew.trisotech.preprocess.MetadataExtractor;
import edu.mayo.kmdp.kdcaci.knew.trisotech.preprocess.Weaver;
import edu.mayo.kmdp.trisotechwrapper.TrisotechWrapper;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.util.XMLUtil;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.http.HttpException;
import org.apache.jena.shared.NotFoundException;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.server.KnowledgeAssetCatalogApiInternal;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.server.KnowledgeAssetRepositoryApiInternal;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KPServer;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.services.repository.KnowledgeAssetCatalog;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

/**
 * A 'wrapper' class. This class implements the Knowledge Asset API and wraps that API around
 * the Trisotech API to return Trisotech data in a Knowledge Asset compatible way.
 */
@Component
@KPServer
public class TrisotechAssetRepository implements KnowledgeAssetCatalogApiInternal,
    KnowledgeAssetRepositoryApiInternal {

  private static final Logger logger = LoggerFactory.getLogger(TrisotechAssetRepository.class);

  @Autowired
  private Weaver weaver;

  @Autowired
  private MetadataExtractor extractor;

  @Autowired
  private TrisotechWrapper client;

  public TrisotechAssetRepository() {
  }

  @Override
  public Answer<KnowledgeAssetCatalog> getKnowledgeAssetCatalog() {
    return unsupported();
  }


  /**
   * Of all the versions in the series, several criteria concur to determine the LATEST, including
   * the time at which a version was created, the (partial) ordering of the version tags, and the
   * association of that version of the Asset with an Artifact in a "published" state
   *
   * @return The asset surrogate for a given Asset ID
   */
  @Override
  public Answer<KnowledgeAsset> getKnowledgeAsset(UUID assetId, String s) {
    // need the fileId of the model in order to query for modelInfo
    Optional<String> internalFileId = extractor.getFileId(assetId, false);
    // need the mimetype to get the correct file format
    Optional<String> mimeType = extractor.getMimetype(assetId);
    // want the latest artifactVersion of the model
    Optional<String> artifactVersion = extractor.getArtifactVersion(assetId);
    if (!internalFileId.isPresent()
        || !mimeType.isPresent()
        || !artifactVersion.isPresent()) {
      return Answer.of(Optional.empty());
    }
    // get the modelInfo for the latest artifactVersion

    return Answer.of(
        client.getLatestModelFileInfo(internalFileId.get())
            .flatMap(modelInfo ->
                getKnowledgeAssetForModel(internalFileId.get(), modelInfo)));
  }

  /**
   * @return Pointers to the versions of a given asset
   */
  @Override
  public Answer<List<Pointer>> listKnowledgeAssetVersions(UUID assetId, Integer offset,
      Integer limit, String beforeTag, String afterTag, String sort) {
    // all versions of given knowledge asset. May make sense to implement
    // may be empty if no versions have been established
    return unsupported(); // for now
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
    Optional<String> fileId;

    // For assetId, find artifactId; For assetId/versionTag, is latest artifactId/version a match? if not, get versions of artifactId and weave each one to get assetId/version
    try {
      internalId = extractor.resolveInternalArtifactID(assetId, versionTag, false);

      fileId = extractor.getFileId(internalId);
      // the url is available from the extractor, HOWEVER, it is not the correct form to retrieve
      // the file in the correct format (xml) so need to make a server call now to get the proper URL
      return Answer.of(
          fileId
              .flatMap(client::getLatestModelFileInfo)
              .flatMap(tfi -> client.downloadXmlModel(tfi.getUrl())
                  .map(weaver::weave)
                  .map(dox -> extractor.extract(dox, tfi)))
      );
    } catch (NotLatestVersionException e) {
      logger.debug("error message from NotLatestVersionException: {}", e.getMessage());
      // check other versions of the model
      try {
        return Answer.of(findArtifactVersionForAsset(e.getMessage(), assetId, versionTag));
      } catch (NotFoundException nfe) {
        return Answer.of(Optional.empty());
      }
    } catch (NotFoundException nfe) {
      return Answer.of(Optional.empty());
    }
  }

  /**
   * When asset version cannot be found on current artifact, need to search all versions of the
   * artifacts.
   *
   * @param internalId the internal trisotech URL for the model
   * @param assetId the assetId looking for
   * @param versionTag the version of the asset looking for
   * @return The KnowledgeAsset for the version found
   */
  private KnowledgeAsset findArtifactVersionForAsset(String internalId, UUID assetId,
      String versionTag) {
    List<TrisotechFileInfo> modelVersions = extractor.getTrisotechModelVersions(internalId);
    // reverse the list so the most recent version that matches is selected
    // there can be multiple versions of the artifact that map to one version of asset
    Collections.reverse(modelVersions);
    for (TrisotechFileInfo model : modelVersions) {
      // skip any that are not published
      if (null == model.getVersion() && null == model.getState()) {
        continue;
      }
      Optional<Document> downloadedXml =
          client.downloadXmlModel(model.getUrl());
      // check assetId for each version
      if (downloadedXml.isPresent()) {
        Document dox = downloadedXml.get();
        ResourceIdentifier asset = weaver.getAssetID(dox);
        if ((null != asset.getTag()
            && null != asset.getVersionTag())
            && asset.getTag().equals(assetId.toString())
            && asset.getVersionTag().equals(versionTag)) {
          // weave and extract the KA here otherwise have to re-query and re-weave
          Document woven = weaver.weave(dox);
          return extractor.extract(woven, model, asset);
        }
      }
    }
    // have gone through all versions of the artifact and not found...
    throw new NotFoundException("No artifact for asset " + assetId + " version: " + versionTag);
  }

  @Override
  public Answer<UUID> initKnowledgeAsset() {
    return unsupported();
  }

  /**
   * list of the all published assets. If assetTypeTag is available will return all published assets
   * of that type.
   *
   * @param assetTypeTag : the type of asset to retrieve; if null, will get ALL types;
   * @param assetAnnotationTag ignore
   * @param assetAnnotationConcept ignore
   * @param offset ignore -- needed if we have pagination
   * @param limit ignore -- needed if we have pagination
   * @return Pointers to available Assets
   */
  @Override
  public Answer<List<Pointer>> listKnowledgeAssets(String assetTypeTag, String assetAnnotationTag,
      String assetAnnotationConcept, Integer offset, Integer limit) {
    List<TrisotechFileInfo> trisotechFileInfoList;

    if (CMMN_UPPER.equalsIgnoreCase(assetTypeTag)) {
      // get CMMN assets
      trisotechFileInfoList = client.getPublishedCMMNModelsFileInfo();
    } else if (DMN_UPPER.equalsIgnoreCase(assetTypeTag)) {
      // DMN
      trisotechFileInfoList = client.getPublishedDMNModelsFileInfo();
    } else {
      // get all published models
      trisotechFileInfoList = client.getPublishedModelsFileInfo();
    }

    List<Pointer> assetList = trisotechFileInfoList.stream()
        .skip((null == offset) ? 0 : offset)
        .limit((null == limit) ? Integer.MAX_VALUE : limit)
        .map(trisotechFileInfo -> {
          // getId from fileInfo is the fileID
          ResourceIdentifier assetId = extractor
              .resolveEnterpriseAssetID(trisotechFileInfo.getId());
          return assetId.toPointer()
              .withType(isDMNModel(trisotechFileInfo)
                  ? Decision_Model.getReferentId()
                  : Care_Process_Model.getReferentId())
              .withName(trisotechFileInfo.getName());

        })
        .collect(Collectors.toList());

    return Answer.of(assetList);

  }


  @Override
  public Answer<Void> setKnowledgeAssetVersion(UUID uuid, String s,
      KnowledgeAsset knowledgeAsset) {
    return unsupported();
  }

  @Override
  public Answer<Void> addKnowledgeAssetCarrier(UUID assetId, String versionTag,
      KnowledgeCarrier assetCarrier) {
    return unsupported();
  }

  /**
   * corresponds to this uri:  /cat/assets/{assetId}/versions/{versionTag}/
   * KnowledgeCarrier: A Resource that wraps a Serialized, Encoded Knowledge Artifact
   *
   * @param assetId assetId of the asset
   * @param versionTag version of the asset
   * @param extAccept 'accept' MIME type
   */
  @Override
  public Answer<KnowledgeCarrier> getCanonicalKnowledgeAssetCarrier(UUID assetId,
      String versionTag, String extAccept) {
    Optional<URI> enterpriseVersionId;
    KnowledgeCarrier carrier;

    try {
      // TODO This should be consistent with getAsset

      enterpriseVersionId = extractor
          .getEnterpriseAssetVersionIdForAsset(assetId, versionTag, false);
      if (enterpriseVersionId.isPresent()) {
        // get the model file -- always do get Canonical
        String internalFileId = extractor.getFileId(assetId, false)
            .orElse("");

        Optional<Document> modelDox = resolveModel(internalFileId, null);
        carrier = AbstractCarrier.of(modelDox
            .map(weaver::weave)
            .map(XMLUtil::toByteArray).orElse(new byte[0]))
            // MUST have the representation info set, which can be obtained from the document
            .withRepresentation(getLanguageRepresentationForModel(modelDox))
            .withArtifactId(getInternalIdAndVersion(assetId, versionTag))
            .withAssetId(SemanticIdentifier.newVersionId(enterpriseVersionId.get()));
      } else {
        return Answer.of(Optional.empty());
      }
    } catch (NotLatestVersionException e) {
      try {
        return tryAnotherVersion(e.getMessage(), assetId, versionTag);
      } catch (NotFoundException nfe) {
        return Answer.of(Optional.empty());
      }
    } catch (NotFoundException e) {
      return Answer.of(Optional.empty());
    }

    return Answer.of(carrier);

  }

  @Override
  public Answer<KnowledgeCarrier> getCanonicalKnowledgeAssetSurrogate(UUID assetId,
      String versionTag,
      String extAccept) {
    return unsupported();
  }

  private Answer<KnowledgeCarrier> tryAnotherVersion(
      String internalId, UUID assetId, String versionTag) {
    KnowledgeCarrier carrier = null;
    List<TrisotechFileInfo> trisotechVersions = extractor.getTrisotechModelVersions(internalId);
    Collections.reverse(trisotechVersions);
    for (TrisotechFileInfo model : trisotechVersions) {
      if (null == model.getVersion() && null == model.getState()) {
        continue;
      }
      Optional<Document> downloadXml = client.downloadXmlModel(model.getUrl());
      if (downloadXml.isPresent()) {
        Document dox = downloadXml.get();
        ResourceIdentifier asset = weaver.getAssetID(dox);
        if ((null != asset.getTag()
            && null != asset.getVersionTag())
            && asset.getTag().equals(assetId.toString())
            && asset.getVersionTag().equals(versionTag)) {
          ResourceIdentifier artifactId = extractor
              .convertInternalId(internalId, model.getVersion(),
                  model.getUpdated())
              .withEstablishedOn(Date.from(Instant.parse(model.getUpdated())));
          return Answer.of(AbstractCarrier.of(downloadXml
              .map(weaver::weave)
              .map(XMLUtil::toByteArray).orElse(new byte[0]))
              .withRepresentation(getLanguageRepresentationForModel(downloadXml))
              .withArtifactId(artifactId)
              .withAssetId(SemanticIdentifier.newId(MAYO_ASSETS_BASE_URI_URI, assetId, versionTag))
          );
        }
      }
    }
    throw new NotFoundException("No artifact for asset " + assetId + " version: " + versionTag);
  }

  private Optional<Document> findArtifactVersionDocumentForAsset(String internalId, UUID assetId,
      String versionTag) {

    return Optional.empty();
  }

  private ResourceIdentifier getInternalIdAndVersion(UUID assetId, String versionTag)
      throws NotLatestVersionException {
    String internalId = extractor.resolveInternalArtifactID(assetId, versionTag, false);
    Optional<String> version = extractor.getArtifactVersion(assetId);
    Optional<String> updated = extractor.getArtifactIdUpdateTime(assetId);
    return extractor.convertInternalId(internalId,
        version.orElse(null), updated.orElse(null)); // TODO: better alternative? error? CAO
  }

  /**
   * Retrieves (a copy of) a specific version of an Artifact. That Artifact must be known to the
   * client to carry at least one expression, in some language, of the given Knowledge Asset.
   * corresponds to this uri: /cat/assets/{assetId}/versions/{versionTag}/carriers/{artifactId}/versions/{artifactVersionTag}
   * only return if the assetId/version is associated with the artifactid/version provided
   *
   * @param assetId enterprise asset ID
   * @param versionTag version for the asset
   * @param artifactId artifact ID
   * @param artifactVersionTag version for the artifact
   */
  @Override
  public Answer<KnowledgeCarrier> getKnowledgeAssetCarrierVersion(UUID assetId,
      String versionTag, UUID artifactId, String artifactVersionTag) {
    KnowledgeCarrier carrier;
    String internalId;

    Optional<String> fileId = extractor.getFileId(assetId, false);
    // fileId is not found in the extractor for the assetId provided; fail
    if (!fileId.isPresent()) {
      return Answer.of(Optional.empty());
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
      Optional<URI> enterpriseVersionAssetId = extractor
          .getEnterpriseAssetVersionIdForAsset(assetId, versionTag, false);
      if (!enterpriseVersionAssetId
          .isPresent()) { // should never happen, as exception should be thrown instead
        return Answer.of(Optional.empty());
      }

      // asset matches for latest, now check the artifact for asset
      internalId = extractor.resolveInternalArtifactID(assetId, versionTag, false);
      // verify artifact for asset matches the artifactId requested
      if (internalId.contains(artifactId.toString())) {
        // confirm version too
        Optional<String> artifactVersion = extractor.getArtifactVersion(assetId);
        if (artifactVersion.isPresent()
            && artifactVersion.get().equals(artifactVersionTag)) {
          // artifact matches, get the file and process
          // a specific version of knowledge asset carrier (fileId)
          Optional<ResourceIdentifier> lav = client
              .getLatestVersion(fileId.get());
          if (lav.isPresent()) {
//            VersionedIdentifier latestArtifactVersion = lav.get();
            logger.debug("latestArtifactVersion version {}", lav.get().getVersionTag());
            logger.debug("latestArtifactVersion Tag {}", lav.get().getTag());

            // 02/03 -- need to return the actual FILE (woven)
            Optional<Document> dox = resolveModel(fileId.get(), null);
            carrier = AbstractCarrier.of(dox
                .map(weaver::weave)
                .map(XMLUtil::toByteArray).orElse(new byte[0]))
                .withAssetId(
                    SemanticIdentifier
                        .newId(MAYO_ASSETS_BASE_URI_URI, assetId.toString(), versionTag))
                .withArtifactId(
                    SemanticIdentifier.newId(MAYO_ARTIFACTS_BASE_URI_URI, artifactId.toString(),
                        lav.get().getVersionTag()))
                .withRepresentation(getLanguageRepresentationForModel(dox));
          } else {
            return Answer.of(Optional.empty());
          }
        } else {
          // artifactId matched, but not version; get other versions to see if one of them matches
          return getKnowledgeCarrierFromOtherVersion(assetId, versionTag, internalId,
              artifactVersionTag);
        }
      } else {
        // artifactId does not match what was requested
        return Answer.of(Optional.empty());
      }
    } catch (NotLatestVersionException e) {
      // something failed to be in the latest version of the artifact, so check all other artifact versions
      // need to confirm the internalId returned in the exception message matches the artifactId requested
      if (e.getMessage().contains(artifactId.toString())) {
        return getKnowledgeCarrierFromOtherVersion(assetId, versionTag, e.getMessage(),
            artifactVersionTag);
      } else {
        return Answer.of(Optional.empty());
      }

    } catch (NotFoundException nfe) {
      return Answer.of(Optional.empty());
    }
    return Answer.of(carrier);
  }

  /**
   * Get the Representation for the language of the model given the model document. creates a
   * Representation object that can be used in .withRepresentation() method of creating a Carrier,
   * for example.
   *
   * @param dox the document of the model
   * @return SyntacticRepresentation which has the values set for the language of this model
   */
  private SyntacticRepresentation getLanguageRepresentationForModel(Optional<Document> dox) {
    if (dox.isPresent()) {
      Optional<org.omg.spec.api4kp._20200801.services.SyntacticRepresentation> rep
          = extractor.getRepLanguage(dox.get());

      if (rep.isPresent()) {
        switch (rep.get().getLanguage().asEnum()) {
          case DMN_1_2:
            return rep(DMN_1_2,DMN_1_2_XML_Syntax,XML_1_1,defaultCharset(), Encodings.DEFAULT);
          case CMMN_1_1:
            return rep(CMMN_1_1,CMMN_1_1_XML_Syntax,XML_1_1,defaultCharset(), Encodings.DEFAULT);
          default:
            throw new IllegalStateException(
                "Invalid document representation language: " + rep.get().getLanguage().toString());
        }
      }
    }
    return null;
  }

  /**
   * when asset version does not match for model (artifact) version given, need to search all
   * versions of the artifacts for the assetID. NOTE: This should NOT happen in reality. New
   * artifacts should be created if the assetID changes. This will only match if all ids and tags
   * match.
   * TODO: much of this code is the same as findArtifactVersionForAsset. Refactor.
   *
   * @param assetId the assetId looking for
   * @param versionTag the version of the asset looking for
   * @param internalId the artifactID
   * @param artifactVersionTag the version of the artifactID
   * @return KnowledgeCarrier for the version of the artifact with the requested version of the
   * asset
   */
  private Answer<KnowledgeCarrier> getKnowledgeCarrierFromOtherVersion(UUID assetId,
      String versionTag,
      String internalId, String artifactVersionTag) {
    List<TrisotechFileInfo> modelVersions = extractor.getTrisotechModelVersions(internalId);
    // reverse the list so the most recent version that matches is selected
    // there can be multiple versions of the artifact that map to one version of asset
    Collections.reverse(modelVersions);
    for (TrisotechFileInfo model : modelVersions) {
      // skip any that are not published
      if (null == model.getVersion() && null == model.getState()) {
        continue;
      }
      Optional<Document> downloadXml =
          client.downloadXmlModel(model.getUrl());
      if (downloadXml.isPresent()) {
        Document dox = downloadXml.get();
        // check assetId for each version
        ResourceIdentifier asset = weaver.getAssetID(dox);
        if (null != asset.getTag() && null != asset.getVersionTag()
            // no need to check artifactId here as all the versions are for the same artifact
            && asset.getTag().equals(assetId.toString())
            && asset.getVersionTag().equals(versionTag)
            && model.getVersion().equals(artifactVersionTag)) {

          Document woven = weaver.weave(dox);
          return Answer.of(
              AbstractCarrier.of(downloadXml
                  .map(weaver::weave)
                  .map(XMLUtil::toByteArray).orElse(new byte[0]))
                  .withAssetId(asset)
                  .withArtifactId(extractor.convertInternalId(internalId, model.getVersion(),
                      model.getUpdated()))
                  .withRepresentation(getLanguageRepresentationForModel(downloadXml)));
        }
      }
    }
    return Answer.of(Optional.empty());
  }

  @Override
  public Answer<List<Pointer>> listKnowledgeAssetCarriers(UUID assetId, String versionTag) {
    // Expect only one carrier ; Canonical will give XML
    return unsupported();
  }

  @Override
  public Answer<KnowledgeCarrier> getKnowledgeAssetSurrogateVersion(UUID uuid, String s, UUID uuid1,
      String s1) {
    return unsupported();
  }


  // to upload the "dictionary" DMN model

  /**
   * Support for updating a model 'in place' The assetId, versionTag, artifactId and
   * artifactVersionTag should match the current model, any mismatch will fail as NOT_FOUND. TODO:
   * Confirm w/Davide which cases should fail and why
   *
   * @param assetId enterprise asset ID
   * @param versionTag version for the asset
   * @param artifactId artifact ID
   * @param artifactVersionTag version for the artifact
   * @param exemplar updated artifact to be posted to the server
   * @return return status
   */
  @Override
  public Answer<Void> setKnowledgeAssetCarrierVersion(UUID assetId, String versionTag,
      UUID artifactId, String artifactVersionTag, byte[] exemplar) {
    String internalId;
    Optional<String> fileId = extractor.getFileId(assetId, true);
    String mimeType = extractor.getMimetype(assetId)
        .orElse(MediaType.APPLICATION_XML.toString());

    // fileId is not found in the extractor for the assetId provided; fail
    // At this time, not allowing for create, so if no fileId, fail
    if (!fileId.isPresent()) {
      return Answer.of(Optional.empty()); //new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    // 1. Check if assetId is latest (no server call needed)
    // 2. If asset is not latest (exception), return NOT_FOUND
    // 3. If asset is latest (no exception), get the artifact & confirm it matches the one requested
    // 4. If artifact matches request, check version
    // 5. If version of artifact matches request, process
    // 6. If version of artifact does not match, return NOT_FOUND
    // first check asset id -- save server calls until needed
    try {
      // getEnterpriseAssetVersionIdForAsset will throw exception if doesn't exist on latest
      Optional<URI> enterpriseVersionAssetId = extractor
          .getEnterpriseAssetVersionIdForAsset(assetId, versionTag, true);
      if (enterpriseVersionAssetId.isEmpty()) { // should never happen, as exception should be thrown instead
        return Answer.of(Optional.empty());
      }

      // asset matches for latest, now check the artifact for asset
      internalId = extractor.resolveInternalArtifactID(assetId, versionTag, true);
      Optional<TrisotechFileInfo> tfi =
          fileId
              .flatMap(client::getLatestModelFileInfo);

      // verify artifact for asset matches the artifactId requested
      if (tfi.isPresent() && internalId.contains(artifactId.toString())) {
        // confirm version too
        Optional<String> artifactVersion = extractor.getArtifactVersion(assetId);
        // TODO: If there is a version, do we fail? expect to only have this support on
        //  non-published models, and only published models have a version CAO
        // pursuant to the answer to the above TODO, go ahead and PUSH to the current version if it matches CAO
        if (artifactVersion.isPresent()) {
          if (artifactVersion.get().equals(artifactVersionTag)) {
            // if have a version, provide it and state to the PUSH
            // TODO: OR ERROR? if there is a version and state, then the model is published;
            //  should not be using this support for published models CAO
            uploadFile(artifactVersionTag, exemplar, tfi.get(), mimeType);
          } else {
            // TODO: do we care? CAO
            // if we care, can try to find the matching version
            return Answer.of(Optional.empty());
          }
        } else {
          // ok for version to not be present, in fact, preferred
          uploadFile(null, exemplar, tfi.get(), mimeType);
        }
      } else {
        return Answer.of(Optional.empty());
      }
      return Answer.of();
    } catch (NotLatestVersionException e) {
      return Answer.of(Optional.empty());
    } catch (IOException | HttpException e) {
      logger.error(e.getMessage(), e);
      return Answer.failed();
    }
  }

  private void uploadFile(String artifactVersionTag, byte[] exemplar,
      TrisotechFileInfo trisotechFileInfo, String mimeType) throws IOException, HttpException {
    client
        .uploadXmlModel(trisotechFileInfo.getPath(), trisotechFileInfo.getName(), mimeType,
            artifactVersionTag,
            trisotechFileInfo.getState(), exemplar);
  }

  private Optional<KnowledgeAsset> getKnowledgeAssetForModel(String internalFileId,
      TrisotechFileInfo modelInfo) {
    Optional<Document> modelDocument = resolveModel(internalFileId, modelInfo);

    // weave in KMD information
    Optional<Document> wovenDocument = modelDocument.map(weaver::weave);

    // extract data from Trisotech format to OMG format
    return wovenDocument
        .map(wd -> extractor.extract(wd, modelInfo));
  }

  private Optional<Document> resolveModel(String internalFileId, TrisotechFileInfo modelInfo) {
    Optional<Document> model = Optional.ofNullable(modelInfo)
        .flatMap(client::getModel);
    if (!model.isPresent()) {
      model = client.getModelById(internalFileId);
    }
    return model;
  }


  private boolean isDMNModel(TrisotechFileInfo fileInfo) {
    return fileInfo.getMimetype().contains(DMN_LOWER);
  }
//
//  @Override
//  public Answer<List<KnowledgeCarrier>> getCompositeKnowledgeAsset(UUID uuid, String s,
//      Boolean aBoolean, String s1) {
//    return unsupported();
//  }
//
//  @Override
//  public Answer<KnowledgeCarrier> getCompositeKnowledgeAssetStructure(UUID uuid, String s) {
//    return unsupported();
//  }
//
//  @Override
//  public Answer<List<KnowledgeCarrier>> getKnowledgeArtifactBundle(UUID uuid, String s,
//      String s1, Integer integer, String s2) {
//    return unsupported();
//  }
//
//  @Override
//  public Answer<List<KnowledgeAsset>> getKnowledgeAssetBundle(UUID uuid, String s,
//      String s1, Integer integer) {
//    return unsupported();
//  }
//
//  @Override
//  public Answer<List<Bindings>> queryKnowledgeAssets(KnowledgeCarrier knowledgeCarrier) {
//    return unsupported();
//  }

}