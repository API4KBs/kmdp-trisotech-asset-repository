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

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.preprocess.meta.Weaver;
import edu.mayo.kmdp.repository.asset.server.KnowledgeAssetCatalogApiDelegate;
import edu.mayo.kmdp.repository.asset.server.KnowledgeAssetRepositoryApiDelegate;
import edu.mayo.kmdp.repository.asset.server.KnowledgeAssetRetrievalApiDelegate;
import edu.mayo.kmdp.trisotechwrapper.TrisotechWrapper;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.util.ws.ResponseHelper;
import edu.mayo.ontology.taxonomies.kao.knowledgeassetcategory._1_0.KnowledgeAssetCategory;
import edu.mayo.ontology.taxonomies.kao.knowledgeassettype._1_0.KnowledgeAssetType;
import org.omg.spec.api4kp._1_0.identifiers.Pointer;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;
import org.omg.spec.api4kp._1_0.services.repository.KnowledgeAssetCatalog;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.omg.spec.api4kp._1_0.identifiers.URIIdentifier;


import edu.mayo.kmdp.preprocess.meta.MetadataExtractor;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.w3c.dom.Document;

import static edu.mayo.kmdp.util.ws.ResponseHelper.notSupported;

// TODO: questions for Davide:
//  Do we need an internal 'cache'? (previously done as 'resolvedModels')
@Component
public class TrisotechAssetRepository implements KnowledgeAssetCatalogApiDelegate, KnowledgeAssetRepositoryApiDelegate, KnowledgeAssetRetrievalApiDelegate {
  Logger log = LoggerFactory.getLogger(TrisotechAssetRepository.class);
  private Weaver weaver;
  private MetadataExtractor extractor;


  @Override
  public ResponseEntity<KnowledgeAssetCatalog> getAssetCatalog() {
    return notSupported();
  }

  @Override
  public ResponseEntity<KnowledgeAsset> getKnowledgeAsset(UUID assetId) {
    // TODO: need to get latest version, or just use TrisotechWrapper.getModelById which will return the Document? CAO
    // TODO: then modelInfo can be
    String versionTag = TrisotechWrapper.getLatestVersionTag(assetId.toString());
    return getVersionedKnowledgeAsset(assetId, versionTag);
//    return null; // TODO: fix this CAO per Davide: ResponseHelper.succeed();
  }

  /**
   *
   * @param uuid
   * @param offset
   * @param limit
   * @param beforeTag
   * @param afterTag
   * @param sort
   * @return
   */
  @Override
  public ResponseEntity<List<Pointer>> getKnowledgeAssetVersions(UUID uuid, Integer offset, Integer limit, String beforeTag, String afterTag, String sort) {
    // all versions of given knowledge asset May make sense to implement
    return null;
  }


  @Override
  public ResponseEntity<KnowledgeAsset> getVersionedKnowledgeAsset(UUID uuid, String versionTag) {
    // TODO: confirm extractor behavior and data here w/Davide CAO
    //  what is the UUID value passed in? assetId -- the ASSETID is a Mayo-specific ID; it is in the trisotech model
    //  is internalId internal to enterprise or internal to Trisotech? Maybe better naming? Yes, better naming; internal is enterprise
    //  from Signavio code, appears to be from the editor, why not just get it from the model using the version? Signavio code had to do things no longer needed w/Trisotech models
    //  does the UUID not tell me which model? Do I need the following line of code? the following is resolving the asset Id <-> artifact Id relationship; can maybe do w/triples now?
//    String internalId = extractor.resolveInternalArtifactID(uuid.toString(), versionTag);

    TrisotechFileInfo trisotechFileInfo = TrisotechWrapper.getModelInfoByIdAndVersion(uuid.toString(), versionTag);
    return null;
  }

  @Override
  public ResponseEntity<UUID> initKnowledgeAsset() {
    return notSupported();
  }

  /**
   * list of the all published assets
   *
   * @param assetType:      ignore;
   * @param assetAnnotation ignore
   * @param offset  ignore -- needed if we have pagination
   * @param limit   ignore -- needed if we have pagination
   * @return
   */
  @Override
  public ResponseEntity<List<Pointer>> listKnowledgeAssets(String assetType, String assetAnnotation, Integer offset, Integer limit) {
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
//    trisotechFileInfoList.stream().skip(offset).limit(limit).forEach((trisotechFileInfo -> {
//      modelPointer.withEntityRef(/*enterprise assetId*/) // TODO: fileID (123720a6-9758-45a3-8c5c-5fffab12c494) or URL? CAO
//          .withHref(/*URL used for getAsset w/UID & versionTag from assetId */)
//          .withType(isDMNModel(trisotechFileInfo)
//              ? KnowledgeAssetType.Decision_Model.getRef()
//              : KnowledgeAssetType.Care_Process_Model.getRef())
//          .withName(trisotechFileInfo.getName());
//      assetList.add(modelPointer);
//    }));

    return new ResponseEntity<>(assetList,
        new HttpHeaders(),
        HttpStatus.OK);

  }


  @Override
  public ResponseEntity<Void> setVersionedKnowledgeAsset(UUID uuid, String s, KnowledgeAsset knowledgeAsset) {
    return notSupported(); // TODO:  USE THIS CAO
  }

  @Override
  public ResponseEntity<Void> addKnowledgeAssetCarrier(UUID uuid, String s, byte[] bytes) {
    return notSupported();
  }

  @Override
  public ResponseEntity<KnowledgeCarrier> getCanonicalKnowledgeAssetCarrier(UUID assetId, String versionTag, String xAccept) {
    // get the model file -- always do get Canonical
    return null;
  }

  @Override
  public ResponseEntity<KnowledgeCarrier> getKnowledgeAssetCarrierVersion(UUID uuid, String s, UUID uuid1, String s1) {
    // a specific version of knowledge asset carrier
return null;
  }

  @Override
  public ResponseEntity<List<Pointer>> getKnowledgeAssetCarriers(UUID uuid, String s) {
    // all the carriers (only one); Canonical will give XML, this part of the spec may be broken CAO
    throw new UnsupportedOperationException();
  }

  // to upload the "dictionary" DMN model (Davide's note)
  // not needed/usable until can upload the accelerators/dictionary (no way to do via API as yet)
  @Override
  public ResponseEntity<Void> setKnowledgeAssetCarrierVersion(UUID assetId, String versionTag, UUID artifactId, String artifactVersionTag, byte[] exemplar) {
    return notSupported(); // for now
  }

  private boolean isDMNModel(TrisotechFileInfo fileInfo) {
    return fileInfo.getMimetype().contains("dmn");
  }

  private boolean isCMMNModel(TrisotechFileInfo fileInfo) {
    return fileInfo.getMimetype().contains("cmmn");
  }

  @Override
  public ResponseEntity<List<KnowledgeCarrier>> getCompositeKnowledgeAsset(UUID uuid, String s, Boolean aBoolean, String s1) {
    return notSupported();
  }

  @Override
  public ResponseEntity<KnowledgeCarrier> getCompositeKnowledgeAssetStructure(UUID uuid, String s) {
    return notSupported();
  }

  @Override
  public ResponseEntity<List<KnowledgeCarrier>> getKnowledgeArtifactBundle(UUID uuid, String s, String s1, Integer integer, String s2) {
    return notSupported();
  }

  @Override
  public ResponseEntity<List<KnowledgeAsset>> getKnowledgeAssetBundle(UUID uuid, String s, String s1, Integer integer) {
    return notSupported();
  }

  @Override
  public ResponseEntity<Void> queryKnowledgeAssets(String s) {
    return notSupported();
  }
}
