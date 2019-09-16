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
package edu.mayo.kmdp;

import static edu.mayo.kmdp.preprocess.meta.Weaver.CLINICALKNOWLEDGEMANAGEMENT_MAYO_ARTIFACTS_BASE_URI;
import static edu.mayo.kmdp.util.ws.ResponseHelper.notSupported;
import static edu.mayo.kmdp.util.ws.ResponseHelper.succeed;

import edu.mayo.kmdp.metadata.surrogate.KnowledgeArtifact;
import edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.preprocess.NotLatestVersionException;
import edu.mayo.kmdp.preprocess.meta.MetadataExtractor;
import edu.mayo.kmdp.preprocess.meta.Weaver;
import edu.mayo.kmdp.repository.asset.server.KnowledgeAssetCatalogApiDelegate;
import edu.mayo.kmdp.repository.asset.server.KnowledgeAssetRepositoryApiDelegate;
import edu.mayo.kmdp.repository.asset.server.KnowledgeAssetRetrievalApiDelegate;
import edu.mayo.kmdp.trisotechwrapper.TrisotechWrapper;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.ontology.taxonomies.kao.knowledgeassettype._20190801.KnowledgeAssetType;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.jena.shared.NotFoundException;
import org.omg.spec.api4kp._1_0.identifiers.Pointer;
import org.omg.spec.api4kp._1_0.identifiers.URIIdentifier;
import org.omg.spec.api4kp._1_0.identifiers.VersionIdentifier;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;
import org.omg.spec.api4kp._1_0.services.repository.KnowledgeAssetCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;


@Component
public class TrisotechAssetRepository implements KnowledgeAssetCatalogApiDelegate,
    KnowledgeAssetRepositoryApiDelegate, KnowledgeAssetRetrievalApiDelegate {

  private static final Logger logger = LoggerFactory.getLogger(TrisotechAssetRepository.class);

  @Autowired
  private Weaver weaver;

  @Autowired
  private MetadataExtractor extractor;

  TrisotechAssetRepository() {
  }

  @Override
  public ResponseEntity<KnowledgeAssetCatalog> getAssetCatalog() {
    return notSupported();
  }

  /**
   * Of all the versions in the series, several criteria concur to determine the LATEST, including
   * the time at which a version was created, the (partial) ordering of the version tags, and the
   * association of that version of the Asset with an Artifact in a "published" state
   */
  @Override
  public ResponseEntity<KnowledgeAsset> getKnowledgeAsset(UUID assetId) {
    // need the fileId of the model in order to query for modelInfo
    Optional<String> internalFileId = extractor.getFileId(assetId);
    // need the mimetype to get the correct file format
    Optional<String> mimeType = extractor.getMimetype(assetId);
    // want the latest artifactVersion of the model
    Optional<String> artifactVersion = extractor.getArtifactVersion(assetId);
    if (!internalFileId.isPresent()
        || !mimeType.isPresent()
        || !artifactVersion.isPresent()) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    // get the modelInfo for the latest artifactVersion
    TrisotechFileInfo modelInfo = TrisotechWrapper.getLatestModelFileInfo(internalFileId.get());
    if (null == modelInfo) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    // get the knowledgeAsset
    KnowledgeAsset ka = getKnowledgeAssetForModel(internalFileId.get(), modelInfo);
    return succeed(ka, HttpStatus.OK);
  }

  /**
   *
   */
  @Override
  public ResponseEntity<List<Pointer>> getKnowledgeAssetVersions(UUID assetId, Integer offset,
      Integer limit, String beforeTag, String afterTag, String sort) {
    // all versions of given knowledge asset. May make sense to implement
    // may be empty if no versions have been established
    return notSupported(); // for now
  }


  // TODO: versionTag is version for ASSET (yes? yes). Q: for Davide: triples don't return artifacts by version,
  //  just the latest version; is this code intended to get the artifactID that corresponds to the specific asset version? yes
  //  so for assetId ab356 version 1.0.1 need to get the version of the artifact CD3 that matches that version.
  //  might want to keep a map of those that have been scanned so don't have to scan again?
  //  scan all versions of so have saved? or only scan until find what we need? CAO
  @Override
  public ResponseEntity<KnowledgeAsset> getVersionedKnowledgeAsset(UUID assetId,
      String versionTag) {
    String internalId;
    Optional<String> fileId = null;
    TrisotechFileInfo trisotechFileInfo = null;
    Document dox;
    KnowledgeAsset ka = null;

    // For assetId, find artifactId; For assetId/versionTag, is latest artifactId/version a match? if not, get versions of artifactId and weave each one to get assetId/version
    try {
      internalId = extractor.resolveInternalArtifactID(assetId.toString(), versionTag);
      fileId = extractor.getFileId(internalId);
      if (!fileId.isPresent()) { // not found
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
      }
      trisotechFileInfo = TrisotechWrapper.getLatestModelFileInfo(fileId.orElse(null));
      dox = weaver.weave(TrisotechWrapper.downloadXmlModel(trisotechFileInfo.getUrl()));
      ka = extractor.extract(dox, trisotechFileInfo);
    } catch (NotLatestVersionException e) {
      logger.debug("error message from NotLatestVersionException: " + e.getMessage());
      // check other versions of the model
      try {
        ka = findArtifactVersionForAsset(e.getMessage(), assetId, versionTag);
      } catch (NotFoundException nfe) {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
      }
    }
    return succeed(ka, HttpStatus.OK);
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
    List<TrisotechFileInfo> modelVersions = getTrisotechModelVersions(internalId);
    // reverse the list so the most recent version that matches is selected
    // there can be multiple versions of the artifact that map to one version of asset
    Collections.reverse(modelVersions);
    for (TrisotechFileInfo model : modelVersions) {
      // skip any that are not published
      if (null == model.getVersion() && null == model.getState()) {
        continue;
      }
      // weave each version
      // need to weave to be able to getAssetID from extractor
      Document dox = weaver.weave(TrisotechWrapper.downloadXmlModel(model.getUrl()));
      // check assetId for each version
      URIIdentifier asset = extractor.getAssetID(dox);
      if ((null != asset.getTag()
          && null != asset.getVersion())
          && asset.getTag().equals(assetId.toString())
          && asset.getVersion().equals(versionTag)) {
        // go ahead and extract the KA here otherwise have to re-query and re-weave
        return extractor.extract(dox, model);
      }
    }
    // have gone through all versions of the artifact and not found...
    throw new NotFoundException("No artifact for asset " + assetId + " version: " + versionTag);
  }

  private List<TrisotechFileInfo> getTrisotechModelVersions(String internalId) {
    // need fileId as trisotech APIs work on fileId
    Optional<String> fileId = extractor.getFileId(internalId);
    // need mimetype to get the correct URL to download XML
    Optional<String> mimeType = extractor.getMimetype(internalId);
    if (!fileId.isPresent() || !mimeType.isPresent()) {
      throw new NotFoundException("Error finding fileId or mimetype for internalid " + internalId);
    }
    // need to get all versions for the file
    return TrisotechWrapper
        .getModelVersions(fileId.get(), mimeType.get());
  }

  @Override
  public ResponseEntity<UUID> initKnowledgeAsset() {
    return notSupported();
  }

  /**
   * list of the all published assets. If assetType is available will return all published assets of
   * that type.
   *
   * @param assetType: the type of asset to retrieve; if null, will get ALL types;
   * @param assetAnnotation ignore
   * @param offset ignore -- needed if we have pagination
   * @param limit ignore -- needed if we have pagination
   */
  @Override
  public ResponseEntity<List<Pointer>> listKnowledgeAssets(String assetType, String assetAnnotation,
      Integer offset, Integer limit) {
    List<TrisotechFileInfo> trisotechFileInfoList;

    if ("CMMN".equalsIgnoreCase(assetType)) {
      // get CMMN assets
      trisotechFileInfoList = TrisotechWrapper.getPublishedCMMNModelsFileInfo();
    } else if ("DMN".equalsIgnoreCase(assetType)) {
      // DMN
      trisotechFileInfoList = TrisotechWrapper.getPublishedDMNModelsFileInfo();
    } else {
      // get all published models
      trisotechFileInfoList = TrisotechWrapper.getPublishedModelsFileInfo();
    }

    List<Pointer> assetList = trisotechFileInfoList.stream()
        .skip((null == offset) ? 0 : offset)
        .limit((null == limit) ? Integer.MAX_VALUE : limit)
        .map(trisotechFileInfo -> {
          URIIdentifier assetId = extractor.resolveEnterpriseAssetID(trisotechFileInfo.getId());
          // getId from fileInfo is the fileID
          return new Pointer().withEntityRef(assetId)
              .withHref(assetId.getUri()/*URL used for getAsset w/UID & versionTag from assetId */)
              .withType(isDMNModel(trisotechFileInfo)
                  ? KnowledgeAssetType.Decision_Model.getRef()
                  : KnowledgeAssetType.Care_Process_Model.getRef())
              .withName(trisotechFileInfo.getName());
        })
        .collect(Collectors.toList());

    return succeed(assetList, HttpStatus.OK);

  }


  @Override
  public ResponseEntity<Void> setVersionedKnowledgeAsset(UUID uuid, String s,
      KnowledgeAsset knowledgeAsset) {
    return notSupported();
  }

  @Override
  public ResponseEntity<Void> addKnowledgeAssetCarrier(UUID uuid, String s, byte[] bytes) {
    return notSupported();
  }

  // corresponds to this uri:  /cat/assets/{assetId}/versions/{versionTag}/
  // KnowledgeCarrier:
  //A Resource that wraps a Serialized, Encoded Knowledge Artifact
  @Override
  public ResponseEntity<KnowledgeCarrier> getCanonicalKnowledgeAssetCarrier(UUID assetId,
      String versionTag, String extAccept) {
    Optional<URI> enterpriseVersionId;
    KnowledgeCarrier carrier;

    // TODO: what to do with extAccept? what to expect as a value? CAO
    try {
      enterpriseVersionId = extractor
          .getEnterpriseAssetVersionIdForAsset(assetId, versionTag);
      if (enterpriseVersionId.isPresent()) {
        URI enterpriseId = extractor
            .getEnterpriseAssetIdForAssetVersionId(enterpriseVersionId.get());
        // get the model file -- always do get Canonical
        carrier = new org.omg.spec.api4kp._1_0.services.resources.KnowledgeCarrier()
            .withAssetId(new URIIdentifier()
                .withUri(enterpriseId)
                .withVersionId(enterpriseVersionId.get()))
            .withArtifactId(new URIIdentifier().withUri(URI.create(
                getInternalIdAndVersion(assetId, versionTag))));

      } else {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
      }
    } catch (NotLatestVersionException e) {
      return tryAnotherVersion(e.getMessage(), assetId, versionTag);
    } catch (NotFoundException e) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    return succeed(carrier, HttpStatus.OK);
  }

  private ResponseEntity<KnowledgeCarrier> tryAnotherVersion(
      String internalId, UUID assetId, String versionTag) {
    KnowledgeCarrier carrier;
    try {
      KnowledgeAsset ka = findArtifactVersionForAsset(internalId, assetId, versionTag);
      KnowledgeArtifact knowledgeArtifact = ka.getCarriers().get(0);

      carrier = new org.omg.spec.api4kp._1_0.services.resources.KnowledgeCarrier()
          .withAssetId(ka.getAssetId())
          .withArtifactId(knowledgeArtifact.getArtifactId());
    } catch (NotFoundException nfe) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    return succeed(carrier, HttpStatus.OK);
  }

  private String getInternalIdAndVersion(UUID assetId, String versionTag)
      throws NotLatestVersionException {
    try {
      String internalId = extractor.resolveInternalArtifactID(assetId.toString(), versionTag);
      Optional<String> version = extractor.getArtifactVersion(assetId);
      return convertInternalId(internalId,
          version.orElse(null)); // TODO: better alternative? error? CAO
    } catch (NotLatestVersionException e) {
      // TODO: handle exception here? CAO
      logger.error(e.getStackTrace().toString());
      throw e;
    }
  }

  /**
   * Need the Trisotech path converted to KMDP path and underscores removed TODO: move to utility
   * class? put in extractor?  The other place this happens is Weaver CAO
   *
   * @param internalId the Trisotech internal id for the model
   * @return the KMDP-ified internal id
   */
  private String convertInternalId(String internalId, String versionTag) {
    String id = internalId.substring(internalId.lastIndexOf('/') + 1).replace("_", "");
    if (null == versionTag) {
      return CLINICALKNOWLEDGEMANAGEMENT_MAYO_ARTIFACTS_BASE_URI + id;
    } else {
      return CLINICALKNOWLEDGEMANAGEMENT_MAYO_ARTIFACTS_BASE_URI + id + "/versions/" + versionTag;
    }
  }

  // corresponds to this uri: /cat/assets/{assetId}/versions/{versionTag}/carriers/{artifactId}/versions/{artifactVersionTag}
  // Retrieves (a copy of) a specific version of an Artifact.
  // That Artifact must be known to the client to carry at least one expression, in some language, of the given Knowledge Asset.
  // TODO: only return if the assetId/version is associated with the artifactid/version provided? CAO
  @Override
  public ResponseEntity<KnowledgeCarrier> getKnowledgeAssetCarrierVersion(UUID assetId,
      String versionTag, UUID artifactId, String artifactVersionTag) {
    KnowledgeCarrier carrier;
    String internalId;

    Optional<String> fileId = extractor.getFileId(assetId);
    // fileId is not found in the extractor for the assetId provided; fail
    if (!fileId.isPresent()) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
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
          .getEnterpriseAssetVersionIdForAsset(assetId, versionTag);
      if (!enterpriseVersionAssetId
          .isPresent()) { // should never happen, as exception should be thrown instead
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
      }

      // asset matches for latest, now check the artifact for asset
      internalId = extractor.resolveInternalArtifactID(assetId.toString(), versionTag);
      // verify artifact for asset matches the artifactId requested
      if (internalId.contains(artifactId.toString())) {
        // confirm version too
        Optional<String> artifactVersion = extractor.getArtifactVersion(assetId);
        if (artifactVersion.isPresent()
            && artifactVersion.get().equals(artifactVersionTag)) {
          // artifact matches, get the file and process
          // a specific version of knowledge asset carrier (fileId)
          VersionIdentifier latestArtifactVersion = TrisotechWrapper
              .getLatestVersion(fileId.get());
          if (logger.isDebugEnabled()) {
            logger.debug(String
                .format("latestArtifactVersion versionTag %s", latestArtifactVersion.getVersion()));
            logger.debug(
                String.format("latestArtifactVersion Tag %s", latestArtifactVersion.getTag()));
          }

          // TODO: discuss w/Davide -- what needs to be set on the KnowledgeCarrier? CAO
          carrier = new org.omg.spec.api4kp._1_0.services.resources.KnowledgeCarrier()
              .withAssetId(new URIIdentifier()
                  .withUri(extractor
                      .getEnterpriseAssetIdForAssetVersionId(enterpriseVersionAssetId.get()))
                  .withVersionId(enterpriseVersionAssetId.get()))
              .withArtifactId(new URIIdentifier()
                  .withUri(URI.create(
                      convertInternalId(artifactId.toString(),
                          latestArtifactVersion.getVersion()))));

        } else {
          // artifactId matched, but not version; get other versions to see if one of them matches
          return getKnowledgeCarrierFromOtherVersion(assetId, versionTag, internalId,
              artifactVersionTag);
        }
      } else {
        // artifactId does not match what was requested
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
      }
    } catch (NotLatestVersionException e) {
      // something failed to be in the latest version of the artifact, so check all other artifact versions
      // need to confirm the internalId returned in the exception message matches the artifactId requested
      if (e.getMessage().contains(artifactId.toString())) {
        return getKnowledgeCarrierFromOtherVersion(assetId, versionTag, e.getMessage(),
            artifactVersionTag);
      } else {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
      }

    } catch (NotFoundException nfe) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    return succeed(carrier, HttpStatus.OK);
  }

  private ResponseEntity<KnowledgeCarrier> getKnowledgeCarrierFromOtherVersion(UUID assetId,
      String versionTag,
      String internalId, String artifactVersionTag) {
    List<TrisotechFileInfo> modelVersions = getTrisotechModelVersions(internalId);
    // reverse the list so the most recent version that matches is selected
    // there can be multiple versions of the artifact that map to one version of asset
    Collections.reverse(modelVersions);
    for (TrisotechFileInfo model : modelVersions) {
      // skip any that are not published
      if (null == model.getVersion() && null == model.getState()) {
        continue;
      }
      // weave each version
      // need to weave to be able to getAssetID from extractor
      Document dox = weaver.weave(TrisotechWrapper.downloadXmlModel(model.getUrl()));
      // check assetId for each version
      URIIdentifier asset = extractor.getAssetID(dox);
      if (null != asset.getTag() && null != asset.getVersion()
          // no need to check artifactId here as all the versions are for the same artifact
          && asset.getTag().equals(assetId.toString())
          && asset.getVersion().equals(versionTag)
          && model.getVersion().equals(artifactVersionTag)) {
        return succeed(
            new org.omg.spec.api4kp._1_0.services.resources.KnowledgeCarrier()
                .withAssetId(asset)
                .withArtifactId(new URIIdentifier()
                    .withUri(URI.create(convertInternalId(internalId, null)))
                    .withVersionId(URI.create(convertInternalId(internalId, model.getVersion())))),
            HttpStatus.OK);
      }
    }
    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
  }

  @Override
  public ResponseEntity<List<Pointer>> getKnowledgeAssetCarriers(UUID assetId, String versionTag) {
    // TODO: all the carriers (only one); Canonical will give XML, this part of the spec may be broken CAO
    return notSupported();
  }

  // to upload the "dictionary" DMN model (Davide's note)
  // not needed/usable until can upload the accelerators/dictionary (no way to do via API as yet)
  @Override
  public ResponseEntity<Void> setKnowledgeAssetCarrierVersion(UUID assetId, String versionTag,
      UUID artifactId, String artifactVersionTag, byte[] exemplar) {
    return notSupported(); // for now
  }

  private KnowledgeAsset getKnowledgeAssetForModel(String internalFileId,
      TrisotechFileInfo modelInfo) {
    Document modelDocument = resolveModel(internalFileId, modelInfo);

    // weave in KMD information
    Document wovenDocument = weaver.weave(modelDocument);

    // extract data from Trisotech format to OMG format
    return extractor.extract(wovenDocument, modelInfo);

  }

  private Document resolveModel(String internalFileId, TrisotechFileInfo modelInfo) {

    Optional<Document> model = TrisotechWrapper.getModelById(internalFileId, modelInfo);

    if (Optional.empty()
        .equals(model)) { // TODO: try again??? assuming modelInfo may be invalid? CAO
      model = TrisotechWrapper.getModelById(internalFileId);
    }

    return model.isPresent() ? model.get() : null;
  }


  private boolean isDMNModel(TrisotechFileInfo fileInfo) {
    return fileInfo.getMimetype().contains("dmn");
  }

  @Override
  public ResponseEntity<List<KnowledgeCarrier>> getCompositeKnowledgeAsset(UUID uuid, String s,
      Boolean aBoolean, String s1) {
    return notSupported();
  }

  @Override
  public ResponseEntity<KnowledgeCarrier> getCompositeKnowledgeAssetStructure(UUID uuid, String s) {
    return notSupported();
  }

  @Override
  public ResponseEntity<List<KnowledgeCarrier>> getKnowledgeArtifactBundle(UUID uuid, String s,
      String s1, Integer integer, String s2) {
    return notSupported();
  }

  @Override
  public ResponseEntity<List<KnowledgeAsset>> getKnowledgeAssetBundle(UUID uuid, String s,
      String s1, Integer integer) {
    return notSupported();
  }

  @Override
  public ResponseEntity<Void> queryKnowledgeAssets(String s) {
    return notSupported();
  }


}
