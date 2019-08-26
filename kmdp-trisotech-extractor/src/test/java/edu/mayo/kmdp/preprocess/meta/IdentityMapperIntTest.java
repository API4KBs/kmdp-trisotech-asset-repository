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
package edu.mayo.kmdp.preprocess.meta;

import static org.junit.jupiter.api.Assertions.*;

import edu.mayo.kmdp.id.helper.DatatypeHelper;
import edu.mayo.kmdp.preprocess.NotLatestVersionException;
import edu.mayo.kmdp.registry.Registry;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._1_0.identifiers.URIIdentifier;

/**
 * Tests for IdentityMapper.
 *
 * Named as an integration test even though not starting SpringBoot, but
 * because communicating with the Trisotech server through SPARQL queries.
 */
class IdentityMapperIntTest {

  static IdentityMapper identityMapper;

  @BeforeAll
  public static void setUp() {
    identityMapper = new IdentityMapper();
  }

  @AfterAll
  public static void tearDown() {
    identityMapper = null;
  }

  @Test
  void getArtifactId() {
    String expectedArtifactId = "http://www.trisotech.com/definitions/_16086bb8-c1fc-49b0-800b-c9b995dc5ed5";
    URIIdentifier assetId = DatatypeHelper
        .uri(Registry.MAYO_ASSETS_BASE_URI + "14321e7c-cb9a-427f-abf5-1420bf26e03c", "1.0.1");
    //DatatypeHelper.uri(/versions/1.0.1");
    String artifactId = null;
    try {
      artifactId = identityMapper.getArtifactId(assetId);
    } catch (NotLatestVersionException e) {
      e.printStackTrace();
    }
    assertEquals(expectedArtifactId, artifactId);
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
  void getEnterpriseAssetVersionIdForAsset() {
    UUID assetId = UUID.fromString("14321e7c-cb9a-427f-abf5-1420bf26e03c");
    String versionTag = "1.0.1";
    String expectedEnterpriseVersionId =
        Registry.MAYO_ASSETS_BASE_URI + "14321e7c-cb9a-427f-abf5-1420bf26e03c/versions/1.0.1";

    Optional<URI> enterpriseAssetVersionId = null;
    try {
      enterpriseAssetVersionId = identityMapper
          .getEnterpriseAssetVersionIdForAsset(assetId, versionTag);
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

    Optional<URIIdentifier> assetId = identityMapper.getAssetId(fileId);
    assertEquals(expectedAssetId, assetId.get().getUri().toString());
  }


  @Test
  void getArtifactVersion() {
    UUID assetId = UUID.fromString("14321e7c-cb9a-427f-abf5-1420bf26e03c");
    String expectedArtifactVersion = "1.8.0";

    Optional<String> artifactVersion = identityMapper.getArtifactIdVersion(assetId);
    assertEquals(expectedArtifactVersion, artifactVersion.get());
  }

  @Test
  void getAssetId_ArtifactIdVersion_Latest() {
    URIIdentifier artifactId = DatatypeHelper
        .uri("http://www.trisotech.com/definitions/_16086bb8-c1fc-49b0-800b-c9b995dc5ed5");
    String versionTag = "1.8.0";
    String expectedAssetId =
        Registry.MAYO_ASSETS_BASE_URI + "14321e7c-cb9a-427f-abf5-1420bf26e03c/versions/1.0.1";

    Optional<URIIdentifier> assetId = null;
    try {
      assetId = identityMapper.getAssetId(artifactId, versionTag);
    } catch (NotLatestVersionException e) {
      e.printStackTrace();
    }
    assertEquals(expectedAssetId, assetId.get().getUri().toString());
  }

  @Test
  void getAssetId_ArtifactIdVersion_matchButNotLatest() {
    URIIdentifier artifactId = DatatypeHelper
        .uri("http://www.trisotech.com/definitions/_16086bb8-c1fc-49b0-800b-c9b995dc5ed5");
    String versionTag = "1.6.0";

    NotLatestVersionException ave = assertThrows(
        NotLatestVersionException.class, () -> identityMapper.getAssetId(artifactId, versionTag));
    assertEquals(artifactId.getUri().toString(), ave.getMessage());
  }


  @Test
  void getAssetId_ArtifactIdVersion_noArtifactWithVersion() {
    // TODO: this may not be a valid test for identityMapper -- maybe TrisotechWrapper -- CAO
    URIIdentifier artifactId = DatatypeHelper
        .uri("http://www.trisotech.com/definitions/_16086bb8-c1fc-49b0-800b-c9b995dc5ed5");
    String versionTag = "1.2.0";

    NotLatestVersionException ave = assertThrows(
        NotLatestVersionException.class, () -> identityMapper.getAssetId(artifactId, versionTag));
    assertEquals(artifactId.getUri().toString(), ave.getMessage());
  }

  @Test
  void getFileId_AssetUUID() {
    UUID assetId = UUID.fromString("14321e7c-cb9a-427f-abf5-1420bf26e03c");
    String expectedFileId = "a0240977-f789-4922-90c8-e6468e59f5c2";
    Optional<String> fileId = identityMapper.getFileId(assetId);
    assertNotNull(fileId);
    assertEquals(expectedFileId, fileId.get());
  }

  @Test
  void getMimetype() {
    UUID assetId = UUID.fromString("14321e7c-cb9a-427f-abf5-1420bf26e03c");
    String expectedMimetype = "application/vnd.triso-cmmn+json";
    Optional<String> mimetype = identityMapper.getMimetype(assetId);
    assertNotNull(mimetype);
    assertEquals(expectedMimetype, mimetype.get());
  }


}