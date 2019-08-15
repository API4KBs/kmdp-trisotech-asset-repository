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

import static edu.mayo.kmdp.util.ws.ResponseHelper.notSupported;
import static edu.mayo.kmdp.util.ws.ResponseHelper.succeed;

import edu.mayo.kmdp.id.helper.DatatypeHelper;
import edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.preprocess.NoArtifactVersionException;
import edu.mayo.kmdp.preprocess.meta.IdentityMapper;
import edu.mayo.kmdp.preprocess.meta.MetadataExtractor;
import edu.mayo.kmdp.preprocess.meta.Weaver;
import edu.mayo.kmdp.repository.asset.server.KnowledgeAssetCatalogApiDelegate;
import edu.mayo.kmdp.repository.asset.server.KnowledgeAssetRepositoryApiDelegate;
import edu.mayo.kmdp.repository.asset.server.KnowledgeAssetRetrievalApiDelegate;
import edu.mayo.kmdp.trisotechwrapper.TrisotechWrapper;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.ontology.taxonomies.kao.knowledgeassettype._20190801.KnowledgeAssetType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
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
    // want the latest version of the model
    VersionIdentifier version = TrisotechWrapper.getLatestVersion(internalFileId.get());
    // get the modelInfo for the latest version
    TrisotechFileInfo modelInfo = TrisotechWrapper.getModelInfoByIdAndVersion(internalFileId.get(), version.getVersion(), mimeType.get());
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
    // all versions of given knowledge asset May make sense to implement
    // may be empty if no versions have been established
    return null;
  }


  // TODO: versionTag is version for ASSET (yes? yes). Q: for Davide: triples don't return artifacts by version,
  //  just the latest version; is this code intended to get the artifactID that corresponds to the specific asset version? yes
  //  so for assetId ab356 version 1.0.1 need to get the version of the artifact CD3 that matches that version.
  //  might want to keep a map of those that have been scanned so don't have to scan again?
  //  scan all versions of one so have saved? or only scan until find what we need? CAO
  @Override
  public ResponseEntity<KnowledgeAsset> getVersionedKnowledgeAsset(UUID assetId,
      String versionTag) {
    String internalId;
    VersionIdentifier version = null;
    Optional<String> fileId = null;
    TrisotechFileInfo trisotechFileInfo = null;
    // TODO: confirm extractor behavior and data here w/Davide CAO
    //  what is the UUID value passed in? assetId -- the ASSETID is a Mayo-specific ID; it is in the trisotech model
    //  is internalId internal to enterprise or internal to Trisotech? Maybe better naming? Yes, better naming; internal is enterprise
    //  from Signavio code, appears to be from the editor, why not just get it from the model using the version? Signavio code had to do things no longer needed w/Trisotech models
    //  does the UUID not tell me which model? Do I need the following line of code? the following is resolving the asset Id <-> artifact Id relationship; can maybe do w/triples now?
    // For assetId, find artifactId; For assetId/versionTag, is latest artifactId/version a match? if not, get versions of artifactId and weave each one to get assetId/version
    try {
      internalId = extractor.resolveInternalArtifactID(assetId.toString(), versionTag);
      fileId = extractor.getFileId(internalId);
      version = TrisotechWrapper.getLatestVersion(fileId.orElse(null));
      trisotechFileInfo = TrisotechWrapper.getLatestModelInfo(fileId.orElse(null));
    } catch (NoArtifactVersionException e) {
      System.out.println("error from NoArtifactVersionException: " + e.getMessage());
      // check other versions of the model
      trisotechFileInfo = findArtifactVersionForAsset(e.getMessage(), assetId, versionTag);
      e.printStackTrace();
    }
//    new KnowledgeAsset().withAssetId(DatatypeHelper.uri(assetId.toString(), versionTag))
//        .withCarriers(Collections.singleton(TrisotechFileInfo)) // TODO: what goes here? should be KnowledgeArtifact collection Don't know why it says TristechFileInfo... CAO
//        .with
    Document dox = weaver.weave(TrisotechWrapper.downloadXmlModel(trisotechFileInfo.getUrl()));

    return succeed(extractor.extract(dox, trisotechFileInfo), HttpStatus.OK);
  }

  private TrisotechFileInfo findArtifactVersionForAsset(String internalId, UUID assetId, String versionTag) {
    Optional<String> fileId = extractor.getFileId(internalId);
    Optional<String> mimeType = extractor.getMimetype(assetId);
    // need to get all versions for the file
    List<TrisotechFileInfo> modelVersions = TrisotechWrapper.getModelVersions(fileId.get(), mimeType.get());
    // weave each version
    for (TrisotechFileInfo model : modelVersions) {
      // check assetId for each version
      Document dox = weaver.weave(TrisotechWrapper.downloadXmlModel(model.getUrl()));
      URIIdentifier asset = extractor.getAssetId(dox, model);
      // if there is a match to asset looking for, return version
      if (asset.getTag().equals(assetId) && asset.getVersion().equals(versionTag)) { // found it
        return TrisotechWrapper.getModelInfoByIdAndVersion(fileId.orElse(null), versionTag, mimeType.get());
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
   *
   * @param assetType: ignore;
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

    trisotechFileInfoList = TrisotechWrapper.getPublishedModels();
    // Don't want to filter. Want ALL assets
    if ("CMMN".equalsIgnoreCase(assetType)) {
      // get CMMN assets
      trisotechFileInfoList = TrisotechWrapper.getCmmnModels();
    } else {
      // DMN
      trisotechFileInfoList = TrisotechWrapper.getDmnModels();
    } // TODO: ? ability to get both? NOTE: way to retrieve in XML format is specific to each type;
    // TODO cont: Url for the fileInfo is returned for XML retrieval CAO

    // TODO: this is all wrong; need ASSET information here too
    trisotechFileInfoList.stream().skip(offset).limit(limit).forEach((trisotechFileInfo -> {
      URIIdentifier assetId = extractor.resolveEnterpriseAssetID(trisotechFileInfo.getId());
      System.out.println("assetId from exractor.resolveEnterpriseAsset: " + assetId);
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
    // get the model file -- always do get Canonical
//    return new org.omg.spec.api4kp._1_0.services.resources.KnowledgeCarrier()
//        .withAssetId(new URIIdentifier()
//            .withUri(extractor.getAssetId(assetId))
//        .withVersion(versionTag))
    // TODO: depending on what ArtifactId is, may need to change this either by making
    //  additional calls to get what I need OR changing what resolveInternalArtifactID returns/processes CAO
//        .withArtifactId(extractor.resolveInternalArtifactID(assetId.toString(), versionTag));
    return null;
  }

  // corresponds to this uri: /cat/assets/{assetId}/versions/{versionTag}/carriers/{artifactId}/versions/{artifactVersionTag}
  // Retrieves (a copy of) a specific version of an Artifact.
  // That Artifact must be known to the client to carry at least one expression, in some language, of the given Knowledge Asset.
  // TODO: only return if the assetId/version is associated with the artifactid/version provided? CAO
  @Override
  public ResponseEntity<KnowledgeCarrier> getKnowledgeAssetCarrierVersion(UUID assetId,
      String versionTag, UUID artifactId, String artifactVersionTag) {
    // a specific version of knowledge asset carrier (model)
    return null;
  }

  @Override
  public ResponseEntity<List<Pointer>> getKnowledgeAssetCarriers(UUID assetId, String versionTag) {
    // TODO: all the carriers (only one); Canonical will give XML, this part of the spec may be broken CAO
    return notSupported(); // for now
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
    Document model = resolveModel(internalFileId, modelInfo);

    // extract data from Trisotech format to OMG format
    KnowledgeAsset ka = extractor.extract(model, modelInfo);

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
