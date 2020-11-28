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
package edu.mayo.kmdp;

import static edu.mayo.kmdp.registry.Registry.MAYO_ASSETS_BASE_URI_URI;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newId;

import edu.mayo.kmdp.kdcaci.knew.trisotech.preprocess.NotLatestVersionException;
import edu.mayo.kmdp.kdcaci.knew.trisotech.preprocess.IdentityMapper;
import edu.mayo.kmdp.registry.Registry;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.shared.NotFoundException;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Tests for IdentityMapper.
 *
 * Named as an integration test even though not starting SpringBoot, but because communicating with
 * the Trisotech server through SPARQL queries.
 */
@SpringBootTest
@ContextConfiguration(classes = {TrisotechAssetRepositoryConfig.class})
@TestPropertySource(properties = {
    "edu.mayo.kmdp.trisotechwrapper.repositoryName=MEA-Test",
    "edu.mayo.kmdp.trisotechwrapper.repositoryId=d4aca01b-d446-4bc8-a6f0-85d84f4c1aaf",
    "edu.mayo.kmdp.trisotechwrapper.baseUrl=https://mc.trisotech.com/"})
class IdentityMapperIntTest {

  public static final String DMN_MIMETYPE = "application/vnd.triso-dmn+json";
  public static final String CMMN_MIMETYPE = "application/vnd.triso-cmmn+json";

  @Autowired
  IdentityMapper identityMapper;

  @Test
  void getArtifactId_Published() {
    String expectedArtifactId = "http://www.trisotech.com/definitions/_16086bb8-c1fc-49b0-800b-c9b995dc5ed5";
    ResourceIdentifier assetId = newId(MAYO_ASSETS_BASE_URI_URI, "14321e7c-cb9a-427f-abf5-1420bf26e03c", "1.0.1");

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
    ResourceIdentifier assetId = newId(MAYO_ASSETS_BASE_URI_URI, "14321e7c-cb9a-427f-abf5-1420bf26e03c", "1.0.1");

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
    // Temporal Correlation Decision.dmn - exists, but not published
    ResourceIdentifier assetId = newId(MAYO_ASSETS_BASE_URI_URI, "d2eeef7a-9248-4eaa-ab25-450e8359f512",
            "0.0.1");
    NotFoundException artifactId = assertThrows(NotFoundException.class,
        () -> identityMapper.getArtifactId(assetId, false));
    assertEquals("d2eeef7a-9248-4eaa-ab25-450e8359f512", artifactId.getMessage());
  }

  @Test
  void getArtifactId_nonPublishedModel_anyQuery() {
    // valid assetId; non-published model, only available if query any
    String expectedArtifactId = "http://www.trisotech.com/definitions/_22419c97-fb08-4b6d-8a68-87b101a6e716";
    // Management of Cholesterol
    ResourceIdentifier assetId =
        newId(MAYO_ASSETS_BASE_URI_URI, "3a78cf3a-93c4-4e08-b1ab-14088c76adad",
            "1.0.0");

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
    UUID assetId = UUID.fromString("3c66cf3a-93c4-4e09-b1aa-14088c76aded");
    String versionTag = "1.1.1";
    String expectedEnterpriseVersionId =
        Registry.MAYO_ASSETS_BASE_URI + "3c66cf3a-93c4-4e09-b1aa-14088c76aded/versions/"
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
  void getAssetId_ModelId() {
    String modelId = "http://www.trisotech.com/definitions/_16086bb8-c1fc-49b0-800b-c9b995dc5ed5";
    String expectedAssetId =
        Registry.MAYO_ASSETS_BASE_URI + "14321e7c-cb9a-427f-abf5-1420bf26e03c/versions/1.0.1";

    Optional<ResourceIdentifier> assetId = identityMapper.getAssetId(modelId);
    assertEquals(expectedAssetId, assetId.get().getVersionId().toString());
  }


  @Test
  void getArtifactIdVersion() {
    UUID assetId = UUID.fromString("14321e7c-cb9a-427f-abf5-1420bf26e03c");
    String expectedArtifactVersion = "1.8.3";

    Optional<String> artifactVersion = identityMapper.getArtifactIdVersion(assetId);
    assertEquals(expectedArtifactVersion, artifactVersion.get());
  }

  @Test
  void getAssetId_ArtifactIdVersion_Latest() {
    ResourceIdentifier artifactId = newId(URI.create(
            "http://www.trisotech.com/definitions/_16086bb8-c1fc-49b0-800b-c9b995dc5ed5"));
    String versionTag = "1.8.3";
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
    ResourceIdentifier artifactId = newId(URI.create(
            "http://www.trisotech.com/definitions/_16086bb8-c1fc-49b0-800b-c9b995dc5ed5"));
    String versionTag = "1.6.0";

    NotLatestVersionException ave = assertThrows(
        NotLatestVersionException.class, () -> identityMapper.getAssetId(artifactId, versionTag));
    assertEquals(artifactId.getResourceId().toString(), ave.getMessage());
  }


  @Test
  void getAssetId_ArtifactIdVersion_noArtifactWithVersion() {
    ResourceIdentifier artifactId = newId(URI.create(
            "http://www.trisotech.com/definitions/_16086bb8-c1fc-49b0-800b-c9b995dc5ed5"));
    String versionTag = "1.2.0";

    NotLatestVersionException ave = assertThrows(
        NotLatestVersionException.class, () -> identityMapper.getAssetId(artifactId, versionTag));
    assertEquals(artifactId.getResourceId().toString(), ave.getMessage());
  }


  @Test
  void getAssetId_ArtifactIdVersion_invalidArtifact() {
    ResourceIdentifier artifactId = newId(URI.create(
            "http://www.trisotech.com/definitions/_16086bb8-c1fc-49b0-800b-c9b995dc5abc"));
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
  void getModelId_MultipleAssetUUID(){
    // this test is to test a model that will have a exported file that contains
    // multiple customAttribute entries due to reuse of another model
    // should still find the correct model based on the assetId
    // should only be one model that matches the assetId
    UUID assetId = UUID.fromString("3c99cf3a-93c4-4e09-b1aa-14088c76aded");
    String expectedModelId = "http://www.trisotech.com/definitions/_f59708b6-96c0-4aa3-be4a-31e075d76ec9";
    Optional<String> modelId = identityMapper.getModelId(assetId, false);
    assertNotNull(modelId);
    assertEquals(expectedModelId, modelId.get());
  }

  @Test
  void getFileId_AssetUUID_Any() {
    // this model is the one reused in the test above
    UUID assetId = UUID.fromString("3c66cf3a-93c4-4e09-b1aa-14088c76aded");
    String expectedModelId = "http://www.trisotech.com/definitions/_5682fa26-b064-43c8-9475-1e4281e74068";
    Optional<String> modelId = identityMapper.getModelId(assetId, true);
    assertNotNull(modelId);
    assertEquals(expectedModelId, modelId.get());
  }

  @Test
  void getModelId_AssetUUID_published() {
    UUID assetId = UUID.fromString("14321e7c-cb9a-427f-abf5-1420bf26e03c");
    String expectedModelId = "http://www.trisotech.com/definitions/_16086bb8-c1fc-49b0-800b-c9b995dc5ed5";
    Optional<String> modelId = identityMapper.getModelId(assetId, false);
    assertNotNull(modelId);
    assertEquals(expectedModelId, modelId.get());
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
    assertNull(models);
  }

  @Test
  void getEnterpriseAssetIdForAssetVersionId() {
    URI assetId = identityMapper.getEnterpriseAssetIdForAssetVersionId(URI.create(
        Registry.MAYO_ASSETS_BASE_URI + "14321e7c-cb9a-427f-abf5-1420bf26e03c/versions/1.0.1"));
    assertNotNull(assetId);
    assertEquals(URI.create(Registry.MAYO_ASSETS_BASE_URI + "14321e7c-cb9a-427f-abf5-1420bf26e03c"),
        assetId);

    assetId = identityMapper.getEnterpriseAssetIdForAssetVersionId(URI.create(
        Registry.MAYO_ASSETS_BASE_URI + "14321e7c-cb9a-427f-abf5-1420bf26e03c"));
    assertNotNull(assetId);
    assertEquals(URI.create(Registry.MAYO_ASSETS_BASE_URI + "14321e7c-cb9a-427f-abf5-1420bf26e03c"),
        assetId);
  }

  @Test
  void getState() {
    // valid
    String modelId = "http://www.trisotech.com/definitions/_5682fa26-b064-43c8-9475-1e4281e74068";
    String expectedState = "Draft";
    Optional<String> state = identityMapper.getState(modelId);
    assertNotNull(state);
    assertTrue(state.isPresent());
    assertEquals(expectedState, state.get());

    // not published
    modelId = "http://www.trisotech.com/definitions/_ec53ebfe-6a50-4a70-a386-e52f802b8b5c";
    state = identityMapper.getState(modelId);
    assertNotNull(state);
    assertFalse(state.isPresent());
  }

}