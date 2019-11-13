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
import static edu.mayo.kmdp.trisotechwrapper.TrisotechApiUrls.CMMN_LOWER;
import static edu.mayo.kmdp.trisotechwrapper.TrisotechApiUrls.CMMN_UPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.registry.Registry;
import edu.mayo.kmdp.util.XMLUtil;
import edu.mayo.ontology.taxonomies.kao.knowledgeassettype._20190801.KnowledgeAssetType;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._1_0.identifiers.Pointer;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;
import org.omg.spec.api4kp._1_0.services.repository.KnowledgeAssetCatalog;
import org.omg.spec.api4kp._1_0.services.resources.BinaryCarrier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;

/**
 * Integration test for TrisotechAssetRepository, using data from test repository.
 */
@SpringBootTest
@ContextConfiguration(classes = {TrisotechAssetRepositoryConfig.class})
class TrisotechAssetRepositoryIntTest {

  @Autowired
  TrisotechAssetRepository tar;

  @BeforeEach
  void setUp() {
  }

  @AfterEach
  void tearDown() {
  }

  @Test
  void getAssetCatalog() {
    ResponseEntity<KnowledgeAssetCatalog> responseEntity = tar.getAssetCatalog();
    assertEquals(HttpStatus.NOT_IMPLEMENTED, responseEntity.getStatusCode());
  }

  @Test
  void getKnowledgeAsset() {
    String expectedAssetId = Registry.MAYO_ASSETS_BASE_URI
        + "735a5764-fe3f-4ab8-b103-650b6e805db2";
    String expectedAssetVersionId = Registry.MAYO_ASSETS_BASE_URI
        + "735a5764-fe3f-4ab8-b103-650b6e805db2/versions/1.0.0";
    String expectedArtifactId = CLINICALKNOWLEDGEMANAGEMENT_MAYO_ARTIFACTS_BASE_URI
        + "ee0c768a-a0d4-4052-a6ea-fc0a3889b356/versions/1.3.0";
    ResponseEntity<KnowledgeAsset> responseEntity = tar
        .getKnowledgeAsset(UUID.fromString("735a5764-fe3f-4ab8-b103-650b6e805db2"));
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
    KnowledgeAsset ka = responseEntity.getBody();
    assertEquals(expectedAssetId, ka.getAssetId().getUri().toString());
    assertEquals(expectedAssetVersionId, ka.getAssetId().getVersionId().toString());
    assertEquals(1, ka.getCarriers().size());
    assertEquals(expectedArtifactId,
        ka.getCarriers().get(0).getArtifactId().getVersionId().toString());
  }

  @Test
  void getKnowledgeAsset_notFound() {
    ResponseEntity<KnowledgeAsset> responseEntity = tar
        .getKnowledgeAsset(UUID.fromString("735a5764-fe3f-4ab8-b103-650b6e805db3"));
    assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
  }

  @Test
  void getKnowledgeAssetVersions() {
    ResponseEntity<List<Pointer>> responseEntity = tar
        .getKnowledgeAssetVersions(UUID.randomUUID(), 0, 10, "beforeTag", "afterTag", "ascend");
    assertEquals(HttpStatus.NOT_IMPLEMENTED, responseEntity.getStatusCode());
  }

  @Test
  void getVersionedKnowledgeAsset() {
    String expectedAssetId = Registry.MAYO_ASSETS_BASE_URI
        + "735a5764-fe3f-4ab8-b103-650b6e805db2";
    String expectedAssetVersionId = Registry.MAYO_ASSETS_BASE_URI
        + "735a5764-fe3f-4ab8-b103-650b6e805db2/versions/1.0.0";
    String expectedArtifactId = CLINICALKNOWLEDGEMANAGEMENT_MAYO_ARTIFACTS_BASE_URI
        + "ee0c768a-a0d4-4052-a6ea-fc0a3889b356/versions/1.3.0";

    ResponseEntity<KnowledgeAsset> responseEntity = tar
        .getVersionedKnowledgeAsset(UUID.fromString("735a5764-fe3f-4ab8-b103-650b6e805db2"),
            "1.0.0");

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
    KnowledgeAsset ka = responseEntity.getBody();
    assertEquals(expectedAssetId, ka.getAssetId().getUri().toString());
    assertEquals(expectedAssetVersionId, ka.getAssetId().getVersionId().toString());
    assertEquals(1, ka.getCarriers().size());
    assertEquals(expectedArtifactId,
        ka.getCarriers().get(0).getArtifactId().getVersionId().toString());

  }

  @Test
  void getVersionedKnowledgeAsset_found() {
    String expectedAssetId = Registry.MAYO_ASSETS_BASE_URI
        + "14321e7c-cb9a-427f-abf5-1420bf26e03c";
    String expectedAssetVersionId = expectedAssetId
        + "/versions/1.0.0";
    String expectedArtifactId = CLINICALKNOWLEDGEMANAGEMENT_MAYO_ARTIFACTS_BASE_URI
        + "16086bb8-c1fc-49b0-800b-c9b995dc5ed5/versions/1.6.0";

    ResponseEntity<KnowledgeAsset> responseEntity = tar
        .getVersionedKnowledgeAsset(UUID.fromString("14321e7c-cb9a-427f-abf5-1420bf26e03c"),
            "1.0.0");

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
    KnowledgeAsset ka = responseEntity.getBody();
    assertEquals(expectedAssetId, ka.getAssetId().getUri().toString());
    assertEquals(expectedAssetVersionId, ka.getAssetId().getVersionId().toString());
    assertEquals(1, ka.getCarriers().size());
    assertEquals(expectedArtifactId,
        ka.getCarriers().get(0).getArtifactId().getVersionId().toString());

  }

  @Test
  void getVersionedKnowledgeAsset_notFound_badVersion() {
    ResponseEntity response = tar
        .getVersionedKnowledgeAsset(UUID.fromString("14321e7c-cb9a-427f-abf5-1420bf26e03c"),
            "1.2.0");
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void getVersionedKnowledgeAsset_notFound_badId() {
    ResponseEntity response = tar
        .getVersionedKnowledgeAsset(UUID.fromString("14321e7c-cb9a-427f-abf5-1420bf26e03d"),
            "1.0.0");
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }


  @Test
  void initKnowledgeAsset() {
    ResponseEntity<UUID> responseEntity = tar
        .initKnowledgeAsset();
    assertEquals(HttpStatus.NOT_IMPLEMENTED, responseEntity.getStatusCode());
  }

  @Test
  void listKnowledgeAssets() {
    ResponseEntity<List<Pointer>> models = tar.listKnowledgeAssets(null, null, null, null);
    assertSame(HttpStatus.OK, models.getStatusCode());
    assertFalse(models.getBody().isEmpty());
    String expectedDecisionId = Registry.MAYO_ASSETS_BASE_URI + "735a5764-fe3f-4ab8-b103-650b6e805db2/versions/1.0.0";
    String expectedDecisionName = "Basic Decision Model";
    String expectedCaseId = Registry.MAYO_ASSETS_BASE_URI + "14321e7c-cb9a-427f-abf5-1420bf26e03c/versions/1.0.1";
    String expectedCaseName = "Basic Case Model";
    AtomicBoolean foundDecision = new AtomicBoolean(false);
    AtomicBoolean foundCase = new AtomicBoolean(false);

    List<Pointer> pointers = models.getBody();

    assertTrue(pointers.size() >= 6);
    // Confirm some of the values
    pointers.forEach((ptr) -> {
      if(expectedDecisionId.equals(ptr.getHref().toString())) {
        assertEquals(expectedDecisionName, ptr.getName());
        assertEquals(KnowledgeAssetType.Decision_Model.getRef(), ptr.getType());
        foundDecision.set(true);
      }
      if(expectedCaseId.equals(ptr.getHref().toString())) {
        assertEquals(expectedCaseName, ptr.getName());
        assertEquals(KnowledgeAssetType.Care_Process_Model.getRef(), ptr.getType());
        foundCase.set(true);
      }
    });
    assertTrue(foundDecision.get());
    assertTrue(foundCase.get());
  }

  @Test
  void listKnowledgeAssets_DMN() {
    ResponseEntity<List<Pointer>> models = tar.listKnowledgeAssets("dmn", null, null, null);
    assertSame(HttpStatus.OK, models.getStatusCode());
    assertFalse(models.getBody().isEmpty());

    List<Pointer> pointers = models.getBody();

    assertTrue(pointers.size() >= 4);
    pointers.forEach((ptr) -> {
      // only Decision Models should be returned
      assertEquals(KnowledgeAssetType.Decision_Model.getRef(), ptr.getType());
    });
  }

  @Test
  void listKnowledgeAssets_DMN_limit() {
    ResponseEntity<List<Pointer>> models = tar.listKnowledgeAssets("dmn", null, null, 2);
    assertSame(HttpStatus.OK, models.getStatusCode());
    assertFalse(models.getBody().isEmpty());

    List<Pointer> pointers = models.getBody();

    assertEquals(2, pointers.size());
    pointers.forEach((ptr) -> {
      // only Decision Models should be returned
      assertEquals(KnowledgeAssetType.Decision_Model.getRef(), ptr.getType());
    });
  }


  @Test
  void listKnowledgeAssets_DMN_offset() {
    ResponseEntity<List<Pointer>> models = tar.listKnowledgeAssets("dmn", null, 3, 2);
    assertSame(HttpStatus.OK, models.getStatusCode());
    assertFalse(models.getBody().isEmpty());

    List<Pointer> pointers = models.getBody();

    assertTrue(pointers.size() >= 1 && pointers.size() < 3);

    pointers.forEach((ptr) -> {
      // only Decision Models should be returned
      assertEquals(KnowledgeAssetType.Decision_Model.getRef(), ptr.getType());
    });
  }


  @Test
  void listKnowledgeAssets_CMMN() {
    ResponseEntity<List<Pointer>> models = tar.listKnowledgeAssets(CMMN_UPPER, null, null, null);
    assertSame(HttpStatus.OK, models.getStatusCode());
    assertFalse(models.getBody().isEmpty());

    List<Pointer> pointers = models.getBody();

    assertEquals(2, pointers.size());
    pointers.forEach((ptr) -> {
      // Only Care Process Models should be returned
      assertEquals(KnowledgeAssetType.Care_Process_Model.getRef(), ptr.getType());
    });
  }

  @Test
  void listKnowledgeAssets_CMMN_limit() {
    ResponseEntity<List<Pointer>> models = tar.listKnowledgeAssets(CMMN_LOWER, null, null, 1);
    assertSame(HttpStatus.OK, models.getStatusCode());
    assertFalse(models.getBody().isEmpty());

    List<Pointer> pointers = models.getBody();

    assertEquals(1, pointers.size());
    pointers.forEach((ptr) -> {
      // Only Care Process Models should be returned
      assertEquals(KnowledgeAssetType.Care_Process_Model.getRef(), ptr.getType());
    });
  }

  @Test
  void setVersionedKnowledgeAsset() {
    KnowledgeAsset ka = new KnowledgeAsset();
    ResponseEntity<Void> responseEntity = tar
        .setVersionedKnowledgeAsset(UUID.randomUUID(), "s", ka);
    assertEquals(HttpStatus.NOT_IMPLEMENTED, responseEntity.getStatusCode());
  }

  @Test
  void addKnowledgeAssetCarrier() {
    KnowledgeAsset ka = new KnowledgeAsset();
    ResponseEntity<Void> responseEntity = tar
        .addKnowledgeAssetCarrier(UUID.randomUUID(), "s", null);
    assertEquals(HttpStatus.NOT_IMPLEMENTED, responseEntity.getStatusCode());
  }

  @Test
  void getCanonicalKnowledgeAssetCarrier() {
    String expectedAssetId = Registry.MAYO_ASSETS_BASE_URI
        + "735a5764-fe3f-4ab8-b103-650b6e805db2";
    String expectedAssetVersionId = Registry.MAYO_ASSETS_BASE_URI
        + "735a5764-fe3f-4ab8-b103-650b6e805db2/versions/1.0.0";
    String expectedArtifactId = CLINICALKNOWLEDGEMANAGEMENT_MAYO_ARTIFACTS_BASE_URI
        + "ee0c768a-a0d4-4052-a6ea-fc0a3889b356/versions/1.3.0";

    ResponseEntity<KnowledgeCarrier> responseEntity = tar
        .getCanonicalKnowledgeAssetCarrier(UUID.fromString("735a5764-fe3f-4ab8-b103-650b6e805db2"),
            "1.0.0",
            null);
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
    KnowledgeCarrier kc = responseEntity.getBody();
    assertEquals(expectedAssetId, kc.getAssetId().getUri().toString());
    assertEquals(expectedAssetVersionId, kc.getAssetId().getVersionId().toString());
    assertEquals(expectedArtifactId, kc.getArtifactId().getUri().toString());

  }

  @Test
  void getCanonicalKnowledgeAssetCarrier_found() {
    String expectedAssetId = Registry.MAYO_ASSETS_BASE_URI
        + "14321e7c-cb9a-427f-abf5-1420bf26e03c";
    String expectedAssetVersionId = expectedAssetId
        + "/versions/1.0.0";
    String expectedArtifactId = CLINICALKNOWLEDGEMANAGEMENT_MAYO_ARTIFACTS_BASE_URI
        + "16086bb8-c1fc-49b0-800b-c9b995dc5ed5/versions/1.6.0";

    ResponseEntity<KnowledgeCarrier> responseEntity = tar
        .getCanonicalKnowledgeAssetCarrier(UUID.fromString("14321e7c-cb9a-427f-abf5-1420bf26e03c"),
            "1.0.0", null);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
    KnowledgeCarrier kc = responseEntity.getBody();
    assertEquals(expectedAssetId, kc.getAssetId().getUri().toString());
    assertEquals(expectedAssetVersionId, kc.getAssetId().getVersionId().toString());
    assertEquals(expectedArtifactId,
        kc.getArtifactId().getVersionId().toString());


  }

  @Test
  void getCanonicalKnowledgeAssetCarrier_notFound_badVersion() {
    ResponseEntity response = tar
        .getCanonicalKnowledgeAssetCarrier(UUID.fromString("14321e7c-cb9a-427f-abf5-1420bf26e03c"),
            "1.2.0", null);
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void getCanonicalKnowledgeAssetCarrier_notFound_badId() {
    ResponseEntity response = tar
        .getCanonicalKnowledgeAssetCarrier(UUID.fromString("14321e7c-cb9a-427f-abf5-1420bf26e03d"),
            "1.0.0", null);
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  // TODO: more tests for versions that need to be 'found' and for versions that don't exist, for both asset and artifact CAO
  @Test
  void getKnowledgeAssetCarrierVersion() {
    String expectedAssetId = Registry.MAYO_ASSETS_BASE_URI
        + "735a5764-fe3f-4ab8-b103-650b6e805db2";
    String expectedAssetVersionId = expectedAssetId
        + "/versions/1.0.0";
    String expectedArtifactId = CLINICALKNOWLEDGEMANAGEMENT_MAYO_ARTIFACTS_BASE_URI
        + "ee0c768a-a0d4-4052-a6ea-fc0a3889b356/versions/1.3.0";

    ResponseEntity<KnowledgeCarrier> responseEntity = tar
        .getKnowledgeAssetCarrierVersion(UUID.fromString("735a5764-fe3f-4ab8-b103-650b6e805db2"),
            "1.0.0",
            UUID.fromString("ee0c768a-a0d4-4052-a6ea-fc0a3889b356"), "1.3.0");

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
    KnowledgeCarrier kc = responseEntity.getBody();
    assertEquals(expectedAssetId, kc.getAssetId().getUri().toString());
    assertEquals(expectedAssetVersionId, kc.getAssetId().getVersionId().toString());
    assertEquals(expectedArtifactId, kc.getArtifactId().getUri().toString());

  }


  @Test
  void getKnowledgeAssetCarrierVersion_found() {
    String expectedAssetId = Registry.MAYO_ASSETS_BASE_URI
        + "14321e7c-cb9a-427f-abf5-1420bf26e03c";
    String expectedAssetVersionId = expectedAssetId
        + "/versions/1.0.0";
    String expectedArtifactId = CLINICALKNOWLEDGEMANAGEMENT_MAYO_ARTIFACTS_BASE_URI
        + "16086bb8-c1fc-49b0-800b-c9b995dc5ed5/versions/1.6.0";

    ResponseEntity<KnowledgeCarrier> responseEntity = tar
        .getKnowledgeAssetCarrierVersion(UUID.fromString("14321e7c-cb9a-427f-abf5-1420bf26e03c"),
            "1.0.0", UUID.fromString("16086bb8-c1fc-49b0-800b-c9b995dc5ed5"), "1.6.0");

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
    KnowledgeCarrier kc = responseEntity.getBody();
    assertEquals(expectedAssetId, kc.getAssetId().getUri().toString());
    assertEquals(expectedAssetVersionId, kc.getAssetId().getVersionId().toString());
    assertEquals(expectedArtifactId,
        kc.getArtifactId().getVersionId().toString());

  }

  @Test
  void getKnowledgeAssetCarrierVersion_notFound_badAssetVersion() {
    ResponseEntity response = tar
        .getKnowledgeAssetCarrierVersion(UUID.fromString("735a5764-fe3f-4ab8-b103-650b6e805db2"),
            "1.2.0", UUID.fromString("ee0c768a-a0d4-4052-a6ea-fc0a3889b356"), "1.3.0");
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }


  @Test
  void getKnowledgeAssetCarrierVersion_notFound_badArtifactVersion() {
    ResponseEntity response = tar
        .getKnowledgeAssetCarrierVersion(UUID.fromString("735a5764-fe3f-4ab8-b103-650b6e805db2"),
            "1.0.0", UUID.fromString("ee0c768a-a0d4-4052-a6ea-fc0a3889b356"), "1.2.0");
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void getKnowledgeAssetCarrierVersion_notFound_badAssetId() {
    ResponseEntity response = tar
        .getKnowledgeAssetCarrierVersion(UUID.fromString("14321e7c-cb9a-427f-abf5-1420bf26e03d"),
            "1.0.0", UUID.fromString("16086bb8-c1fc-49b0-800b-c9b995dc5ed5"), "1.8.0");
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void getKnowledgeAssetCarrierVersion_notFound_badArtifactId() {
    ResponseEntity response = tar
        .getKnowledgeAssetCarrierVersion(UUID.fromString("14321e7c-cb9a-427f-abf5-1420bf26e03c"),
            "1.0.1", UUID.fromString("16086bb8-c1fc-49b0-800b-c9b995dc5ed6"), "1.8.0");
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void getKnowledgeAssetCarriers() {
    ResponseEntity<List<Pointer>> responseEntity = tar
        .getKnowledgeAssetCarriers(UUID.randomUUID(), "s");
    assertEquals(HttpStatus.NOT_IMPLEMENTED, responseEntity.getStatusCode());
  }

  @Test
  void setKnowledgeAssetCarrierVersion_NotFound_InvalidAssetId() {
    ResponseEntity<Void> responseEntity = tar
        .setKnowledgeAssetCarrierVersion(UUID.randomUUID(), "versionTag", UUID.randomUUID(),
            "artifactVersionTag", null);
    assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
  }

  @Test
  void setKnowledgeAssetCarrierVersion_NotFound_InvalidVersion() {
    InputStream testFile = TrisotechAssetRepositoryIntTest.class.getResourceAsStream(
        "/Test Save As.dmn");
    ResponseEntity<Void> responseEntity = tar
        .setKnowledgeAssetCarrierVersion(UUID.fromString("3c66cf3a-93c4-4e09-b1aa-14088c76dead"),
            "1.0.0",
            UUID.fromString("e36338e7-500c-43a0-881d-22aa5dc538df"), "",
            XMLUtil.toByteArray(XMLUtil.loadXMLDocument(testFile).get()));
    assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
  }

  @Test
  void setKnowledgeAssetCarrierVersion_NotFound_InvalidArtifactid() {
    InputStream testFile = TrisotechAssetRepositoryIntTest.class.getResourceAsStream("/Test Save As.dmn");
    ResponseEntity<Void> responseEntity = tar
        .setKnowledgeAssetCarrierVersion(UUID.fromString("3c66cf3a-93c4-4e09-b1aa-14088c76dead"),
            "1.0.0-SNAPSHOT", UUID.fromString("e36338e7-500c-43a0-881d-22aa5dc53abc"), "",
            XMLUtil.toByteArray(XMLUtil.loadXMLDocument(testFile).get()));
    assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
  }

  @Test
  void setKnowledgeAssetCarrierVersion_found() {
    InputStream testFile = TrisotechAssetRepositoryIntTest.class.getResourceAsStream("/Test Save As.dmn");
    ResponseEntity<Void> responseEntity = tar
        .setKnowledgeAssetCarrierVersion(UUID.fromString("3c66cf3a-93c4-4e09-b1aa-14088c76dead"),
            "1.0.0-SNAPSHOT", UUID.fromString("e36338e7-500c-43a0-881d-22aa5dc538df"), "",
            XMLUtil.toByteArray(XMLUtil.loadXMLDocument(testFile).get()));
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
  }

  @Test
  void setKnowledgeAssetCarrierVersion_published_found() {
    // not planning on uploading published versions, but test that it works until further notice
    InputStream publishedFile = TrisotechAssetRepositoryIntTest.class.getResourceAsStream("/TestPushingAndPublished.dmn");
    ResponseEntity<Void> responseEntity = tar.setKnowledgeAssetCarrierVersion(
        UUID.fromString("abcdcf3a-93c4-4e09-b1aa-14088c76dead"),
        "1.0.1", UUID.fromString("ff05700d-8433-4bdc-baa7-ef62c4a165c5"), "1.2.0",
        XMLUtil.toByteArray(XMLUtil.loadXMLDocument(publishedFile).get()));
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
  }

  @Test
  void getCompositeKnowledgeAsset() {
    ResponseEntity<List<KnowledgeCarrier>> responseEntity = tar
        .getCompositeKnowledgeAsset(UUID.randomUUID(), "s", false, "s1");
    assertEquals(HttpStatus.NOT_IMPLEMENTED, responseEntity.getStatusCode());
  }

  @Test
  void getCompositeKnowledgeAssetStructure() {
    ResponseEntity<KnowledgeCarrier> responseEntity = tar
        .getCompositeKnowledgeAssetStructure(UUID.randomUUID(), "s");
    assertEquals(HttpStatus.NOT_IMPLEMENTED, responseEntity.getStatusCode());
  }

  @Test
  void getKnowledgeArtifactBundle() {
    ResponseEntity<List<KnowledgeCarrier>> responseEntity = tar
        .getKnowledgeArtifactBundle(UUID.randomUUID(), "s", "s1", 6, "s2");
    assertEquals(HttpStatus.NOT_IMPLEMENTED, responseEntity.getStatusCode());
  }

  @Test
  void getKnowledgeAssetBundle() {
    ResponseEntity<List<KnowledgeAsset>> responseEntity = tar
        .getKnowledgeAssetBundle(UUID.randomUUID(), "s", "s2", 7);
    assertEquals(HttpStatus.NOT_IMPLEMENTED, responseEntity.getStatusCode());
  }

  @Test
  void queryKnowledgeAssets() {
    ResponseEntity<Void> responseEntity = tar
        .queryKnowledgeAssets("s");
    assertEquals(HttpStatus.NOT_IMPLEMENTED, responseEntity.getStatusCode());
  }
}