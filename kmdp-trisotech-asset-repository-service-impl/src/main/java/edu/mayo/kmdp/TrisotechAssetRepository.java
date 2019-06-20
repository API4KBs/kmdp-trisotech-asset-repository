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
import edu.mayo.kmdp.util.ws.ResponseHelper;
import org.omg.spec.api4kp._1_0.identifiers.Pointer;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class TrisotechAssetRepository implements KnowledgeAssetCatalogApiDelegate, KnowledgeAssetRepositoryApiDelegate {
  @Override
  public Optional<ObjectMapper> getObjectMapper() {
    return KnowledgeAssetRepositoryApiDelegate.super.getObjectMapper();
  }

  @Override
  public Optional<HttpServletRequest> getRequest() {
    return KnowledgeAssetRepositoryApiDelegate.super.getRequest();
  }

  @Override
  public Optional<String> getAcceptHeader() {
    return KnowledgeAssetRepositoryApiDelegate.super.getAcceptHeader();
  }



  @Override
  public ResponseEntity<KnowledgeAsset> getKnowledgeAsset(UUID assetId) {
    return null; // TODO: fix this CAO ResponseHelper.succeed();
  }

  @Override
  public ResponseEntity<List<Pointer>> listKnowledgeAssets(String assetType, String assetAnnotation, Integer offset, Integer limit) {
    return null;
  }

  @Override
  public ResponseEntity<KnowledgeCarrier> getCanonicalKnowledgeAssetCarrier(UUID assetId, String versionTag, String xAccept) {
    return null;
  }


  // to upload the "dictionary" DMN model
  @Override
  public ResponseEntity<Void> setKnowledgeAssetCarrierVersion(UUID assetId, String versionTag, UUID artifactId, String artifactVersionTag, byte[] exemplar) {
    return null;
  }

}
