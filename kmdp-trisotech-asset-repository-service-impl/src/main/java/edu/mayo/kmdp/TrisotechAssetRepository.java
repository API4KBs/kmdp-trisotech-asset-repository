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
import edu.mayo.kmdp.repository.asset.server.KnowledgeAssetCatalogApiDelegate;
import edu.mayo.kmdp.repository.asset.server.KnowledgeAssetRepositoryApiDelegate;
import edu.mayo.kmdp.trisotechwrapper.TrisotechWrapper;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import org.omg.spec.api4kp._1_0.identifiers.Pointer;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;
import org.omg.spec.api4kp._1_0.services.repository.KnowledgeAssetCatalog;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import edu.mayo.kmdp.trisotechwrapper.TrisotechWrapper;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.w3c.dom.Document;

@Component
public class TrisotechAssetRepository implements KnowledgeAssetCatalogApiDelegate, KnowledgeAssetRepositoryApiDelegate {
  Logger log = LoggerFactory.getLogger(TrisotechAssetRepository.class);

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
    return null;
  }

  @Override
  public ResponseEntity<KnowledgeAsset> getKnowledgeAsset(UUID assetId) {
    return null; // TODO: fix this CAO ResponseHelper.succeed();
  }

  @Override
  public ResponseEntity<List<Pointer>> getKnowledgeAssetVersions(UUID uuid, Integer integer, Integer integer1, String s, String s1, String s2) {
    return null;
  }

  @Override
  public ResponseEntity<KnowledgeAsset> getVersionedKnowledgeAsset(UUID uuid, String s) {
    return null;
  }

  @Override
  public ResponseEntity<UUID> initKnowledgeAsset() {
    return null;
  }

  /**
   * list of the assets;
   * @param assetType: "CMMN" or "DMN" // TODO: allow for both?
   * @param assetAnnotation // TODO: what do to with this?
   * @param offset
   * @param limit
   * @return
   */
  @Override
  public ResponseEntity<List<Pointer>> listKnowledgeAssets(String assetType, String assetAnnotation, Integer offset, Integer limit) {
    List<Pointer> assetList = new ArrayList<>();
    Pointer modelPointer = new Pointer();

    if ("CMMN".equals(assetType)) {
      // get CMMN assets
    } else {
      // DMN
      TrisotechWrapper.getPublishedDmnModels().forEach((modelId, model) ->  {
        Optional<Document> dmnDox = TrisotechWrapper.getDmnModelById(modelId, model);
      });
    }
//    try {
//      return (ResponseEntity<List<Pointer>>) new ResponseEntity(((ObjectMapper) this.getObjectMapper()
//          .get()).readValue("application/json", List.class), HttpStatus.ACCEPTED);
//    } catch (IOException var4) {
//      log.error("Couldn't serialize response for content type application/xml", var4);
//      return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
//    }

    return null; // TBD
  }

  @Override
  public ResponseEntity<Void> setVersionedKnowledgeAsset(UUID uuid, String s, KnowledgeAsset knowledgeAsset) {
    return null;
  }

  @Override
  public ResponseEntity<Void> addKnowledgeAssetCarrier(UUID uuid, String s, byte[] bytes) {
    return null;
  }

  @Override
  public ResponseEntity<KnowledgeCarrier> getCanonicalKnowledgeAssetCarrier(UUID assetId, String versionTag, String xAccept) {
    return null;
  }

  @Override
  public ResponseEntity<KnowledgeCarrier> getKnowledgeAssetCarrierVersion(UUID uuid, String s, UUID uuid1, String s1) {
    return null;
  }

  @Override
  public ResponseEntity<List<Pointer>> getKnowledgeAssetCarriers(UUID uuid, String s) {
    return null;
  }


  // to upload the "dictionary" DMN model
  // TODO: Discuss w/Davide: how is "dictionary" handled differently? Not published? CAO
  @Override
  public ResponseEntity<Void> setKnowledgeAssetCarrierVersion(UUID assetId, String versionTag, UUID artifactId, String artifactVersionTag, byte[] exemplar) {
    return null;
  }

}
