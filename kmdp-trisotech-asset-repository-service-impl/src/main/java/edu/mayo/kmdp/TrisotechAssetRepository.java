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

import edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.preprocess.NotLatestVersionException;
import edu.mayo.kmdp.preprocess.meta.IdentityMapper;
import edu.mayo.kmdp.preprocess.meta.MetadataExtractor;
import edu.mayo.kmdp.preprocess.meta.Weaver;
import edu.mayo.kmdp.repository.asset.server.KnowledgeAssetCatalogApiDelegate;
import edu.mayo.kmdp.repository.asset.server.KnowledgeAssetRepositoryApiDelegate;
import edu.mayo.kmdp.repository.asset.server.KnowledgeAssetRetrievalApiDelegate;
import edu.mayo.kmdp.trisotechwrapper.TrisotechWrapper;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.ontology.taxonomies.kao.knowledgeassettype._20190801.KnowledgeAssetType;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.omg.spec.api4kp._1_0.identifiers.Pointer;
import org.omg.spec.api4kp._1_0.identifiers.URIIdentifier;
import org.omg.spec.api4kp._1_0.identifiers.VersionIdentifier;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;
import org.omg.spec.api4kp._1_0.services.repository.KnowledgeAssetCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;


@Component
public class TrisotechAssetRepository implements KnowledgeAssetCatalogApiDelegate,
    KnowledgeAssetRepositoryApiDelegate, KnowledgeAssetRetrievalApiDelegate {

  Logger log = LoggerFactory.getLogger(TrisotechAssetRepository.class);

  private Weaver weaver;
  private MetadataExtractor extractor;
  private IdentityMapper mapper;
  private boolean initialized = false;

  private void init() {
    this.mapper = new IdentityMapper();
    this.weaver = new Weaver();
    this.extractor = new MetadataExtractor(mapper);
    initialized = true;
  }

  TrisotechAssetRepository() {
    init();
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
//    VersionIdentifier artifactVersion = TrisotechWrapper.getLatestVersion(internalFileId.get());
    // get the modelInfo for the latest artifactVersion
    TrisotechFileInfo modelInfo = TrisotechWrapper.getLatestModelFileInfo(internalFileId.get());
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

    // For assetId, find artifactId; For assetId/versionTag, is latest artifactId/version a match? if not, get versions of artifactId and weave each one to get assetId/version
    try {
      internalId = extractor.resolveInternalArtifactID(assetId.toString(), versionTag);
      fileId = extractor.getFileId(internalId);
      trisotechFileInfo = TrisotechWrapper.getLatestModelFileInfo(fileId.orElse(null));
    } catch (NotLatestVersionException e) {
      System.out.println("error from NotLatestVersionException: " + e.getMessage());
      // check other versions of the model
      trisotechFileInfo = findArtifactVersionForAsset(e.getMessage(), assetId, versionTag);
      e.printStackTrace();
    }

    Document dox = weaver.weave(TrisotechWrapper.downloadXmlModel(trisotechFileInfo.getUrl()));
    KnowledgeAsset ka = extractor.extract(dox, trisotechFileInfo);

    return succeed(ka, HttpStatus.OK);
  }

  private TrisotechFileInfo findArtifactVersionForAsset(String internalId, UUID assetId, String versionTag) {
    Optional<String> fileId = extractor.getFileId(internalId);
    // TODO: ERRORS if fileId and mimeType are not found?
    Optional<String> mimeType = extractor.getMimetype(assetId);
    // need to get all versions for the file
    List<TrisotechFileInfo> modelVersions = TrisotechWrapper.getModelVersions(fileId.get(), mimeType.get());
    // weave each version
    for (TrisotechFileInfo model : modelVersions) {
      // check assetId for each version
      Document dox = weaver.weave(TrisotechWrapper.downloadXmlModel(model.getUrl()));
      URIIdentifier asset = extractor.getAssetID(dox);
      // if there is a match to asset looking for, return version
      if (asset.getTag().equals(assetId) && asset.getVersion().equals(versionTag)) { // found it
        return TrisotechWrapper.getFileInfoByIdAndVersion(fileId.orElse(null), versionTag, mimeType.get());
      }
    }
    return null; // TODO: error? CAO
  }

  @Override
  public ResponseEntity<UUID> initKnowledgeAsset() {
    return notSupported();
  }

  /**
   * list of the all published assets
   * TODO: is it expected to only return for published artifacts or should ALL assets be returned? CAO
   *
   * @param assetType: the type of asset to retrieve; if null, will get ALL types;
   * @param assetAnnotation ignore
   * @param offset ignore -- needed if we have pagination
   * @param limit ignore -- needed if we have pagination
   */
  @Override
  public ResponseEntity<List<Pointer>> listKnowledgeAssets(String assetType, String assetAnnotation,
      Integer offset, Integer limit) {
    List<Pointer> assetList = new ArrayList<>();
    Pointer modelPointer = new Pointer();
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
    // TODO: NOTE: way to retrieve in XML format is specific to each type;
    // TODO cont: Url for the fileInfo is returned for XML retrieval.
    //  Is XML retrieval in info needed? CAO

    trisotechFileInfoList.stream()
        .skip((null == offset) ? 0 : offset)
        .limit((null == limit) ? Integer.MAX_VALUE : limit)
        .forEach((trisotechFileInfo -> {
      URIIdentifier assetId = extractor.resolveEnterpriseAssetID(trisotechFileInfo.getId());
      System.out.println("assetId from extractor.resolveEnterpriseAsset: " + assetId);
      System.out.println("assetId getURI: " + assetId.getUri() + " ???difference????");
      // getId from fileInfo is the fileID
      modelPointer.withEntityRef(assetId)
          .withHref(assetId.getUri()/*URL used for getAsset w/UID & versionTag from assetId */)
          .withType(isDMNModel(trisotechFileInfo)
              ? KnowledgeAssetType.Decision_Model.getRef()
              : KnowledgeAssetType.Care_Process_Model.getRef())
          .withName(trisotechFileInfo.getName());
      assetList.add(modelPointer);
    }));

    // TODO: return this or succeed? CAO
    return new ResponseEntity<>(assetList,
        new HttpHeaders(),
        HttpStatus.OK);

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
    // TODO: what to do with extAccept? what to expect as a value? CAO
    // get the model file -- always do get Canonical
    KnowledgeCarrier carrier = new org.omg.spec.api4kp._1_0.services.resources.KnowledgeCarrier()
        .withAssetId(new URIIdentifier()
            // TODO: Need to also send in versionTag and confirm the assetId and version match? CAO
            .withUri(extractor.getEnterpriseAssetIdForAsset(assetId).get())
        .withVersionId(extractor.getEnterpriseAssetVersionIdForAsset(assetId, versionTag).get()))
    // TODO: depending on what ArtifactId is, may need to change this either by making
    //  additional calls to get what I need OR changing what resolveInternalArtifactID returns/processes CAO
        .withArtifactId(new URIIdentifier().withUri(URI.create(
            getInternalIdAndVersion(assetId, versionTag))));
    return succeed(carrier, HttpStatus.OK);
  }

  private String getInternalIdAndVersion(UUID assetId, String versionTag)  {
    try {
      String internalId = extractor.resolveInternalArtifactID(assetId.toString(), versionTag);
      Optional<String> version = extractor.getArtifactVersion(assetId);
      return convertInternalId(internalId, version.orElse(null)); // TODO: better alternative? error? CAO
    } catch (NotLatestVersionException e) {
      // TODO: handle exception here? CAO
      e.printStackTrace();
    }
    return null; // TODO: error? Exception? CAO
  }

  /**
   * Need the Trisotech path converted to KMDP path and underscores removed
   *
   * @param internalId the Trisotech internal id for the model
   * @param versionTag
   * @return the KMDP-ified internal id
   */
  private String convertInternalId(String internalId, String versionTag) {
    String id = internalId.substring(internalId.lastIndexOf('/') + 1).replaceAll("_", "");
    return CLINICALKNOWLEDGEMANAGEMENT_MAYO_ARTIFACTS_BASE_URI + id + "/versions/" + versionTag;
  }

  // corresponds to this uri: /cat/assets/{assetId}/versions/{versionTag}/carriers/{artifactId}/versions/{artifactVersionTag}
  // Retrieves (a copy of) a specific version of an Artifact.
  // That Artifact must be known to the client to carry at least one expression, in some language, of the given Knowledge Asset.
  // TODO: only return if the assetId/version is associated with the artifactid/version provided? CAO
  @Override
  public ResponseEntity<KnowledgeCarrier> getKnowledgeAssetCarrierVersion(UUID assetId,
      String versionTag, UUID artifactId, String artifactVersionTag) {
    Optional<String> fileId = extractor.getFileId(assetId);
    // a specific version of knowledge asset carrier (fileId)
    VersionIdentifier latestArtifactVersion = TrisotechWrapper.getLatestVersion(fileId.get()); // TODO: orElseThrow? CAO
    System.out.println("latestArtifactVersion versionTag" + latestArtifactVersion.getVersion());
    System.out.println("latestArtifactVersion Tag" + latestArtifactVersion.getTag());
    if(latestArtifactVersion.getVersion().equals(artifactVersionTag)) {
      // artifact matches, now check asset
      URIIdentifier assetForArtifact = extractor.getAssetID(latestArtifactVersion.getFormat());
    }

    // TODO: discuss w/Davide -- what needs to be set on the KnowledgeCarrier? CAO
    KnowledgeCarrier carrier = new org.omg.spec.api4kp._1_0.services.resources.KnowledgeCarrier()
        .withAssetId(new URIIdentifier()
            // TODO: Need to also send in versionTag and confirm the assetId and version match?
            .withUri(extractor.getEnterpriseAssetIdForAsset(assetId).get())
            .withVersionId(extractor.getEnterpriseAssetVersionIdForAsset(assetId, versionTag).get()))
        // TODO: depending on what ArtifactId is, may need to change this either by making
        //  additional calls to get what I need OR changing what resolveInternalArtifactID returns/processes CAO
        .withArtifactId(new URIIdentifier()
            .withUri(URI.create(
                convertInternalId(artifactId.toString(), latestArtifactVersion.getVersion()))));

    // TODO: return 404 for             Asset or Artifact Version not found
    // does that include that this artifact/version and asset/version do not match?
    return succeed(carrier, HttpStatus.OK);

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
    KnowledgeAsset ka = extractor.extract(wovenDocument, modelInfo);

    return ka;
  }

  private Document resolveModel(String internalFileId, TrisotechFileInfo modelInfo) {

    Document model = null;

      model = TrisotechWrapper.getModelById(internalFileId, modelInfo).get();

      if (model == null) { // TODO: try again??? assuming modelInfo may be invalid? CAO
        model = TrisotechWrapper.getModelById(internalFileId).get();
      }

    return model;
  }


  private boolean isDMNModel(TrisotechFileInfo fileInfo) {
    return fileInfo.getMimetype().contains("dmn");
  }

  private boolean isCMMNModel(TrisotechFileInfo fileInfo) {
    return fileInfo.getMimetype().contains("cmmn");
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
