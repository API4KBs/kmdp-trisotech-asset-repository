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
package edu.mayo.kmdp.preprocess.meta;

import static edu.mayo.kmdp.registry.Registry.MAYO_ASSETS_BASE_URI_URI;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import edu.mayo.kmdp.IdentityMapperConfig;
import edu.mayo.kmdp.preprocess.NotLatestVersionException;
import edu.mayo.kmdp.registry.Registry;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._1_0.id.ResourceIdentifier;
import org.omg.spec.api4kp._1_0.id.SemanticIdentifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * Tests for IdentityMapper.
 *
 * Named as an integration test even though not starting SpringBoot, but because communicating with
 * the Trisotech server through SPARQL queries.
 */
@SpringBootTest
@ContextConfiguration(classes = {IdentityMapperConfig.class})
class IdentityMapperIntTest {

  public static final String DMN_MIMETYPE = "application/vnd.triso-dmn+json";
  public static final String CMMN_MIMETYPE = "application/vnd.triso-cmmn+json";
  @Autowired
  IdentityMapper identityMapper;

  @Test
  void getArtifactId_Published() {
    String expectedArtifactId = "http://www.trisotech.com/definitions/_16086bb8-c1fc-49b0-800b-c9b995dc5ed5";
    ResourceIdentifier assetId = SemanticIdentifier
        .newId(MAYO_ASSETS_BASE_URI_URI, "14321e7c-cb9a-427f-abf5-1420bf26e03c", "1.0.1");

    String artifactId = null;
    try {
      artifactId = identityMapper.getArtifactId(assetId, false);
    } catch (NotLatestVersionException e) {
      e.printStackTrace();
    }
    assertEquals(expectedArtifactId, artifactId);
  }

  @Test
  void getArtifactId_Any() {
    String expectedArtifactId = "http://www.trisotech.com/definitions/_16086bb8-c1fc-49b0-800b-c9b995dc5ed5";
    ResourceIdentifier assetId = SemanticIdentifier
        .newId(MAYO_ASSETS_BASE_URI_URI, "14321e7c-cb9a-427f-abf5-1420bf26e03c", "1.0.1");
    
    String artifactId = null;
    try {
      artifactId = identityMapper.getArtifactId(assetId, true);
    } catch (NotLatestVersionException e) {
      e.printStackTrace();
    }
    assertEquals(expectedArtifactId, artifactId);
  }

  @Test
  void getArtifactId_nonPublishedModel() {
    // valid assetId; non-published model, so not in our query set
    ResourceIdentifier assetId = SemanticIdentifier
        .newId(MAYO_ASSETS_BASE_URI_URI, "3c66cf3a-93c4-4e09-b1aa-14088c76dead",
            "1.0.0-SNAPSHOT");
    try {
      String artifactId = identityMapper.getArtifactId(assetId, false);
      assertNull(artifactId);
    } catch (NotLatestVersionException e) {
      fail(e.getMessage());
    }

  }

  @Test
  void getArtifactId_nonPublishedModel_anyQuery() {
    // valid assetId; non-published model, only available if query any
    String expectedArtifactId = "http://www.trisotech.com/definitions/_e36338e7-500c-43a0-881d-22aa5dc538df";
    ResourceIdentifier assetId = SemanticIdentifier
        .newId(MAYO_ASSETS_BASE_URI_URI, "3c66cf3a-93c4-4e09-b1aa-14088c76dead",
            "1.0.0-SNAPSHOT");
    try {
      String artifactId = identityMapper.getArtifactId(assetId, true);
      assertNotNull(artifactId);
      assertEquals(expectedArtifactId, artifactId);
    } catch (NotLatestVersionException e) {
      fail(e.getMessage());
    }

  }

  @Test
  void getEnterpriseAssetIdForAsset() {
    UUID assetId = UUID.fromString("14321e7c-cb9a-427f-abf5-1420bf26e03c");
    String expectedEnterpriseId =
        Registry.MAYO_ASSETS_BASE_URI + "14321e7c-cb9a-427f-abf5-1420bf26e03c";

    Optional<URI> enterpriseAssetId = identityMapper.getEnterpriseAssetIdForAsset(assetId);
    assertEquals(expectedEnterpriseId, enterpriseAssetId.get().toString());
  }


  @Test
  void getEnterpriseAssetVersionIdForAsset_published() {
    UUID assetId = UUID.fromString("14321e7c-cb9a-427f-abf5-1420bf26e03c");
    String versionTag = "1.0.1";
    String expectedEnterpriseVersionId =
        Registry.MAYO_ASSETS_BASE_URI + "14321e7c-cb9a-427f-abf5-1420bf26e03c/versions/1.0.1";

    Optional<URI> enterpriseAssetVersionId = null;
    try {
      enterpriseAssetVersionId = identityMapper
          .getEnterpriseAssetVersionIdForAsset(assetId, versionTag, false);
    } catch (NotLatestVersionException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
    assertEquals(expectedEnterpriseVersionId, enterpriseAssetVersionId.get().toString());
  }

  @Test
  void getEnterpriseAssetVersionIdForAsset_any() {
    UUID assetId = UUID.fromString("3c66cf3a-93c4-4e09-b1aa-14088c76dead");
    String versionTag = "1.0.0-SNAPSHOT";
    String expectedEnterpriseVersionId =
        Registry.MAYO_ASSETS_BASE_URI + "3c66cf3a-93c4-4e09-b1aa-14088c76dead/versions/"
            + versionTag;

    Optional<URI> enterpriseAssetVersionId = null;
    try {
      enterpriseAssetVersionId = identityMapper
          .getEnterpriseAssetVersionIdForAsset(assetId, versionTag, true);
    } catch (NotLatestVersionException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
    assertEquals(expectedEnterpriseVersionId, enterpriseAssetVersionId.get().toString());
  }

  @Test
  void getAssetId_FileId() {
    String fileId = "a0240977-f789-4922-90c8-e6468e59f5c2";
    String expectedAssetId =
        Registry.MAYO_ASSETS_BASE_URI + "14321e7c-cb9a-427f-abf5-1420bf26e03c/versions/1.0.1";

    Optional<ResourceIdentifier> assetId = identityMapper.getAssetId(fileId);
    assertEquals(expectedAssetId, assetId.get().getVersionId().toString());
  }


  @Test
  void getArtifactIdVersion() {
    UUID assetId = UUID.fromString("14321e7c-cb9a-427f-abf5-1420bf26e03c");
    String expectedArtifactVersion = "1.8.0";

    Optional<String> artifactVersion = identityMapper.getArtifactIdVersion(assetId);
    assertEquals(expectedArtifactVersion, artifactVersion.get());
  }

  @Test
  void getAssetId_ArtifactIdVersion_Latest() {
    ResourceIdentifier artifactId = SemanticIdentifier
        .newId(URI.create("http://www.trisotech.com/definitions/_16086bb8-c1fc-49b0-800b-c9b995dc5ed5"));
    String versionTag = "1.8.0";
    String expectedAssetId =
        Registry.MAYO_ASSETS_BASE_URI + "14321e7c-cb9a-427f-abf5-1420bf26e03c/versions/1.0.1";

    Optional<ResourceIdentifier> assetId = null;
    try {
      assetId = identityMapper.getAssetId(artifactId, versionTag);
    } catch (NotLatestVersionException e) {
      e.printStackTrace();
    }
    assertEquals(expectedAssetId, assetId.get().getVersionId().toString());
  }

  @Test
  void getAssetId_ArtifactIdVersion_matchButNotLatest() {
    ResourceIdentifier artifactId = SemanticIdentifier
        .newId(URI.create("http://www.trisotech.com/definitions/_16086bb8-c1fc-49b0-800b-c9b995dc5ed5"));
    String versionTag = "1.6.0";

    NotLatestVersionException ave = assertThrows(
        NotLatestVersionException.class, () -> identityMapper.getAssetId(artifactId, versionTag));
    assertEquals(artifactId.getResourceId().toString(), ave.getMessage());
  }


  @Test
  void getAssetId_ArtifactIdVersion_noArtifactWithVersion() {
    ResourceIdentifier artifactId = SemanticIdentifier
        .newId(URI.create("http://www.trisotech.com/definitions/_16086bb8-c1fc-49b0-800b-c9b995dc5ed5"));
    String versionTag = "1.2.0";

    NotLatestVersionException ave = assertThrows(
        NotLatestVersionException.class, () -> identityMapper.getAssetId(artifactId, versionTag));
    assertEquals(artifactId.getResourceId().toString(), ave.getMessage());
  }


  @Test
  void getAssetId_ArtifactIdVersion_invalidArtifact() {
    ResourceIdentifier artifactId = SemanticIdentifier
        .newId(URI.create("http://www.trisotech.com/definitions/_16086bb8-c1fc-49b0-800b-c9b995dc5abc"));
    String versionTag = "1.2.0";
    try {
      Optional<ResourceIdentifier> assetId = identityMapper.getAssetId(artifactId, versionTag);
      assertNotNull(assetId);
      assertFalse(assetId.isPresent());
      assertEquals(Optional.empty(), assetId);
    } catch (NotLatestVersionException e) {
      fail(e.getMessage());
      e.printStackTrace();
    }

  }

  @Test
  void getFileId_AssetUUID_published() {
    UUID assetId = UUID.fromString("14321e7c-cb9a-427f-abf5-1420bf26e03c");
    String expectedFileId = "a0240977-f789-4922-90c8-e6468e59f5c2";
    Optional<String> fileId = identityMapper.getFileId(assetId, false);
    assertNotNull(fileId);
    assertEquals(expectedFileId, fileId.get());
  }

  @Test
  void getFileId_AssetUUID_Any() {
    UUID assetId = UUID.fromString("3c66cf3a-93c4-4e09-b1aa-14088c76dead");
    String expectedFileId = "fe2e7db2-0642-4d37-b406-d0641e986dcf";
    Optional<String> fileId = identityMapper.getFileId(assetId, true);
    assertNotNull(fileId);
    assertEquals(expectedFileId, fileId.get());
  }

  @Test
  void getMimetype_CMMN() {
    UUID assetId = UUID.fromString("14321e7c-cb9a-427f-abf5-1420bf26e03c");
    Optional<String> mimetype = identityMapper.getMimetype(assetId);
    assertNotNull(mimetype);
    assertEquals(CMMN_MIMETYPE, mimetype.get());
  }

  @Test
  void getMimetype_DMN() {
    UUID assetId = UUID.fromString("bd0014e6-afbe-4006-b182-baa973f2929a");
    Optional<String> mimetype = identityMapper.getMimetype(assetId);
    assertNotNull(mimetype);
    assertEquals(DMN_MIMETYPE, mimetype.get());
  }

  @Test
  void getMimetype_invalidId() {
    UUID assetId = UUID.fromString("14321e7c-cb9a-427f-abf5-1420bf26e123");
    Optional<String> mimetype = identityMapper.getMimetype(assetId);
    assertNotNull(mimetype);
    assertFalse(mimetype.isPresent());
    assertEquals(Optional.empty(), mimetype);
  }

  @Test
  void getMimetype_internalId_CMMN() {
    Optional<String> mimetype = identityMapper
        .getMimetype("http://www.trisotech.com/definitions/_16086bb8-c1fc-49b0-800b-c9b995dc5ed5");
    assertNotNull(mimetype);
    assertEquals(CMMN_MIMETYPE, mimetype.get());
  }

  @Test
  void getMimetype_internalId_DMN() {
    Optional<String> mimetype = identityMapper
        .getMimetype("http://www.trisotech.com/definitions/_c2e182f5-96a2-4ced-958a-8c43b7469b26");
    assertNotNull(mimetype);
    assertEquals(DMN_MIMETYPE, mimetype.get());
  }

  @Test
  void getMimetype_internalId_invalidId() {
    Optional<String> mimetype = identityMapper
        .getMimetype("http://www.trisotech.com/definitions/_e36338e7-500c-43a0-881d-22aa5dc5dead");
    assertNotNull(mimetype);
    assertFalse(mimetype.isPresent());
    assertEquals(Optional.empty(), mimetype);
  }

  @Test
  void getOrderedModels() {
    List<Resource> models = identityMapper.getOrderedModels();
    assertNotNull(models);
  }

  @Test
  void getEnterpriseAssetIdForAssetVersionId() {
    URI assetId = identityMapper.getEnterpriseAssetIdForAssetVersionId(URI.create(
        Registry.MAYO_ASSETS_BASE_URI + "14321e7c-cb9a-427f-abf5-1420bf26e03c/versions/1.0.1"));
    assertNotNull(assetId);
    assertEquals(URI.create(Registry.MAYO_ASSETS_BASE_URI + "14321e7c-cb9a-427f-abf5-1420bf26e03c"), assetId);

    assetId = identityMapper.getEnterpriseAssetIdForAssetVersionId(URI.create(
        Registry.MAYO_ASSETS_BASE_URI + "14321e7c-cb9a-427f-abf5-1420bf26e03c"));
    assertNotNull(assetId);
    assertEquals(URI.create(Registry.MAYO_ASSETS_BASE_URI + "14321e7c-cb9a-427f-abf5-1420bf26e03c"), assetId);
  }

  @Test
  void getState() {
    // valid
    String fileId = "123720a6-9758-45a3-8c5c-5fffab12c494";
    String expectedState = "Published";
    Optional<String> state = identityMapper.getState(fileId);
    assertNotNull(state);
    assertTrue(state.isPresent());
    assertEquals(expectedState, state.get());
    // invalid
    fileId = "fe2e7db2-0642-4d37-b406-d0641e986dcf";
    state = identityMapper.getState(fileId);
    assertNotNull(state);
    assertFalse(state.isPresent());
    assertEquals(Optional.empty(), state);
  }

  @Test
  void getArtifactImports() {
  }

  @Test
  void getAssetRelations() {
  }
}