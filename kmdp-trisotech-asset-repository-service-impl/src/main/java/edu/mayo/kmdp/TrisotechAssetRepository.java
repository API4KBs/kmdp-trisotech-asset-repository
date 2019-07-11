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
import edu.mayo.ontology.taxonomies.kao.knowledgeassetcategory._1_0.KnowledgeAssetCategory;
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

@Component
public class TrisotechAssetRepository implements KnowledgeAssetCatalogApiDelegate, KnowledgeAssetRepositoryApiDelegate, KnowledgeAssetRetrievalApiDelegate {
  Logger log = LoggerFactory.getLogger(TrisotechAssetRepository.class);
  private Weaver weaver;
  private MetadataExtractor extractor;

//  @Override
//  public Optional<ObjectMapper> getObjectMapper() {
//    return KnowledgeAssetRepositoryApiDelegate.super.getObjectMapper();
//  }
//
//  @Override
//  public Optional<HttpServletRequest> getRequest() {
//    return KnowledgeAssetRepositoryApiDelegate.super.getRequest();
//  }
//
//  @Override
//  public Optional<String> getAcceptHeader() {
//    return KnowledgeAssetRepositoryApiDelegate.super.getAcceptHeader();
//  }


  @Override
  public ResponseEntity<KnowledgeAssetCatalog> getAssetCatalog() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ResponseEntity<KnowledgeAsset> getKnowledgeAsset(UUID assetId) {
    return null; // TODO: fix this CAO per Davide: ResponseHelper.succeed();
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
    return null;
  }

  @Override
  public ResponseEntity<KnowledgeAsset> getVersionedKnowledgeAsset(UUID uuid, String s) {
    return null;
  }

  @Override
  public ResponseEntity<UUID> initKnowledgeAsset() {
    throw new UnsupportedOperationException();
  }

  /**
   * list of the assets;
   *
   * @param assetType:      "CMMN" or "DMN" // TODO: allow for both?
   * @param assetAnnotation // TODO: what do to with this?
   * @param offset
   * @param limit
   * @return
   */
  @Override
  public ResponseEntity<List<Pointer>> listKnowledgeAssets(String assetType, String assetAnnotation, Integer offset, Integer limit) {
    List<Pointer> assetList = new ArrayList<>();
    Pointer modelPointer = new Pointer();
    List<TrisotechFileInfo> trisotechFileInfoList;

    if ("CMMN".equalsIgnoreCase(assetType)) {
      // get CMMN assets
      trisotechFileInfoList = TrisotechWrapper.getCmmnModels();
    } else {
      // DMN
      trisotechFileInfoList = TrisotechWrapper.getDmnModels();
    } // TODO: ? ability to get both? NOTE: way to retrieve in XML format is specific to each type;
    // TODO cont: Url for the fileInfo is returned for XML retrieval CAO

    trisotechFileInfoList.stream().skip(offset).limit(limit).forEach((trisotechFileInfo -> {
      modelPointer.withEntityRef(new URIIdentifier().withUri(URI.create(trisotechFileInfo.getId()))) // TODO: fileID (123720a6-9758-45a3-8c5c-5fffab12c494) or URL? CAO
          .withHref(URI.create(trisotechFileInfo.getUrl()))
          .withType(isDMNModel(trisotechFileInfo)
              ? KnowledgeAssetCategory.Assessment_Predictive_And_Inferential_Models.getRef()
              : KnowledgeAssetCategory.Rules_Policies_And_Guidelines.getRef())
          .withName(trisotechFileInfo.getName());
      assetList.add(modelPointer);
    }));

    return new ResponseEntity<>(assetList,
        new HttpHeaders(),
        HttpStatus.OK);

  }


  @Override
  public ResponseEntity<Void> setVersionedKnowledgeAsset(UUID uuid, String s, KnowledgeAsset knowledgeAsset) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ResponseEntity<Void> addKnowledgeAssetCarrier(UUID uuid, String s, byte[] bytes) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ResponseEntity<KnowledgeCarrier> getCanonicalKnowledgeAssetCarrier(UUID assetId, String versionTag, String xAccept) {
    return null;
  }

  @Override
  public ResponseEntity<KnowledgeCarrier> getKnowledgeAssetCarrierVersion(UUID uuid, String s, UUID uuid1, String s1) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ResponseEntity<List<Pointer>> getKnowledgeAssetCarriers(UUID uuid, String s) {
    throw new UnsupportedOperationException();
  }

  // to upload the "dictionary" DMN model (Davide's note)
  // TODO: Discuss w/Davide: how is "dictionary" handled differently? CAO
  @Override
  public ResponseEntity<Void> setKnowledgeAssetCarrierVersion(UUID assetId, String versionTag, UUID artifactId, String artifactVersionTag, byte[] exemplar) {
    return null;
  }

  private boolean isDMNModel(TrisotechFileInfo fileInfo) {
    return fileInfo.getMimetype().contains("dmn");
  }

  private boolean isCMMNModel(TrisotechFileInfo fileInfo) {
    return fileInfo.getMimetype().contains("cmmn");
  }

  @Override
  public ResponseEntity<List<KnowledgeCarrier>> getCompositeKnowledgeAsset(UUID uuid, String s, Boolean aBoolean, String s1) {
    return null;
  }

  @Override
  public ResponseEntity<KnowledgeCarrier> getCompositeKnowledgeAssetStructure(UUID uuid, String s) {
    return null;
  }

  @Override
  public ResponseEntity<List<KnowledgeCarrier>> getKnowledgeArtifactBundle(UUID uuid, String s, String s1, Integer integer, String s2) {
    return null;
  }

  @Override
  public ResponseEntity<List<KnowledgeAsset>> getKnowledgeAssetBundle(UUID uuid, String s, String s1, Integer integer) {
    return null;
  }

  @Override
  public ResponseEntity<Void> queryKnowledgeAssets(String s) {
    return null;
  }
}
