/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package edu.mayo.kmdp;

import static edu.mayo.kmdp.registry.Registry.MAYO_ARTIFACTS_BASE_URI;
import static edu.mayo.kmdp.registry.Registry.MAYO_ASSETS_BASE_URI;
import static edu.mayo.kmdp.trisotechwrapper.TrisotechApiUrls.CMMN_LOWER;
import static edu.mayo.kmdp.trisotechwrapper.TrisotechApiUrls.CMMN_UPPER;
import static edu.mayo.ontology.taxonomies.ws.responsecodes.ResponseCodeSeries.NotImplemented;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Care_Process_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Decision_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatusSeries.Final_Draft;

import edu.mayo.kmdp.kdcaci.knew.trisotech.TrisotechAssetRepository;
import edu.mayo.kmdp.util.XMLUtil;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.repository.KnowledgeAssetCatalog;
import org.omg.spec.api4kp._20200801.surrogate.Dependency;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.Link;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

/**
 * Integration test for TrisotechAssetRepository, using data from test repository.
 */
@SpringBootTest
@ContextConfiguration(classes = {TrisotechAssetRepositoryConfig.class})
@TestPropertySource(properties = {
    "edu.mayo.kmdp.trisotechwrapper.repositoryName=MEA-Test",
    "edu.mayo.kmdp.trisotechwrapper.repositoryId=d4aca01b-d446-4bc8-a6f0-85d84f4c1aaf"})
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
    Answer<KnowledgeAssetCatalog> answer = tar.getKnowledgeAssetCatalog();
    assertEquals(NotImplemented, answer.getOutcomeType());
  }

  @Test
  void getKnowledgeAsset() {
    // Basic Decision Model.dmn
    String expectedAssetId = MAYO_ASSETS_BASE_URI
        + "261543d9-90b6-4fe0-a26d-f329111d77ca";
    String expectedAssetVersionId = MAYO_ASSETS_BASE_URI
        + "261543d9-90b6-4fe0-a26d-f329111d77ca/versions/1.0.0";
    String expectedArtifactId = MAYO_ARTIFACTS_BASE_URI
        + "ee0c768a-a0d4-4052-a6ea-fc0a3889b356/versions/1.4.0";
    Answer<KnowledgeAsset> answer = tar
        .getKnowledgeAsset(UUID.fromString("261543d9-90b6-4fe0-a26d-f329111d77ca"), null);
    assertTrue(answer.isSuccess());
    assertNotNull(answer.get());
    KnowledgeAsset ka = answer.get();
    assertEquals(expectedAssetId, ka.getAssetId().getResourceId().toString());
    assertEquals(expectedAssetVersionId, ka.getAssetId().getVersionId().toString());
    assertEquals(1, ka.getCarriers().size());
    assertTrue(ka.getCarriers().get(0).getArtifactId().getVersionId()
        .toString().startsWith(expectedArtifactId));
  }

  @Test
  void getKnowledgeAsset_notFound() {
    Answer<KnowledgeAsset> answer = tar
        .getKnowledgeAsset(UUID.fromString("735a5764-fe3f-4ab8-b103-650b6e805db3"), null);
    assertTrue(answer.isClientFailure());
  }

  @Test
  void getKnowledgeAssetVersions() {
    Answer<List<Pointer>> answer = tar
        .listKnowledgeAssetVersions(UUID.randomUUID(), 0, 10, "beforeTag", "afterTag", "ascend");
    assertEquals(NotImplemented, answer.getOutcomeType());
  }

  @Test
  void getVersionedKnowledgeAsset() {
    // Basic Decision Model.dmn
    String expectedAssetId = MAYO_ASSETS_BASE_URI
        + "261543d9-90b6-4fe0-a26d-f329111d77ca";
    String expectedAssetVersionId = MAYO_ASSETS_BASE_URI
        + "261543d9-90b6-4fe0-a26d-f329111d77ca/versions/1.0.0";
    String expectedArtifactId = MAYO_ARTIFACTS_BASE_URI
        + "ee0c768a-a0d4-4052-a6ea-fc0a3889b356/versions/1.4.0";

    Answer<KnowledgeAsset> answer = tar
        .getKnowledgeAssetVersion(UUID.fromString("261543d9-90b6-4fe0-a26d-f329111d77ca"),
            "1.0.0");

    assertTrue(answer.isSuccess());
    assertNotNull(answer.get());
    KnowledgeAsset ka = answer.get();
    assertEquals(expectedAssetId, ka.getAssetId().getResourceId().toString());
    assertEquals(expectedAssetVersionId, ka.getAssetId().getVersionId().toString());
    assertEquals(1, ka.getCarriers().size());
    assertTrue(ka.getCarriers().get(0).getArtifactId().getVersionId()
        .toString().startsWith(expectedArtifactId));
  }

  /*
  Test for a version that is not the latest and must be 'found' by searching through older versions.
  Verify the dependencies are of the correct version also.
   */
  @Test
  void getVersionedKnowledgeAsset_found() {
    String expectedAssetId = MAYO_ASSETS_BASE_URI
        + "14321e7c-cb9a-427f-abf5-1420bf26e03c";
    String expectedAssetVersionId = expectedAssetId
        + "/versions/1.0.0";
    String expectedArtifactId = MAYO_ARTIFACTS_BASE_URI
        + "16086bb8-c1fc-49b0-800b-c9b995dc5ed5/versions/1.6.0+1565742456000";
    String expectedDependencyId = MAYO_ARTIFACTS_BASE_URI
        + "ee0c768a-a0d4-4052-a6ea-fc0a3889b356/versions/1.2+1565368008000";
    Answer<KnowledgeAsset> answer = tar
        .getKnowledgeAssetVersion(UUID.fromString("14321e7c-cb9a-427f-abf5-1420bf26e03c"),
            "1.0.0");

    assertTrue(answer.isSuccess());
    assertNotNull(answer.get());
    KnowledgeAsset ka = answer.get();
    assertEquals(expectedAssetId, ka.getAssetId().getResourceId().toString());
    assertEquals(expectedAssetVersionId, ka.getAssetId().getVersionId().toString());
    assertEquals(1, ka.getCarriers().size());
    assertEquals(expectedArtifactId,
        ka.getCarriers().get(0).getArtifactId().getVersionId().toString());
    Dependency dependency = (Dependency) ka.getCarriers().get(0).getLinks().get(0);
    assertEquals(expectedDependencyId, dependency.getHref().getVersionId().toString());

  }

  /*
  More complex 'found' version test
   */
  @Test
  void getVersionKnowledgeAsset_found2() {
    String expectedAssetId = MAYO_ASSETS_BASE_URI
        + "3c99cf3a-93c4-4e09-b1aa-14088c76aded";
    String expectedAssetVersionId = expectedAssetId
        + "/versions/1.0.0";
    // Weave Test 1 (CMMN)
    String expectedArtifactId = MAYO_ARTIFACTS_BASE_URI
        + "f59708b6-96c0-4aa3-be4a-31e075d76ec9/versions/3.0.0+1587068731000";
    // Weaver Test 1 (DMN)
    String expectedDependencyId1 = MAYO_ARTIFACTS_BASE_URI
        + "5682fa26-b064-43c8-9475-1e4281e74068/versions/2.0.0+1587068239000";
    // Weaver Test 2 (DMN)
    String expectedDependencyId2 = MAYO_ARTIFACTS_BASE_URI
        + "ede3b331-7b10-4580-98be-66ebff344c21/versions/0.5.0+1587068342000";

    Answer<KnowledgeAsset> answer = tar
        .getKnowledgeAssetVersion(UUID.fromString("3c99cf3a-93c4-4e09-b1aa-14088c76aded"),
            "1.0.0");

    assertTrue(answer.isSuccess());
    assertNotNull(answer.get());
    KnowledgeAsset ka = answer.get();
    assertEquals(expectedAssetId, ka.getAssetId().getResourceId().toString());
    assertEquals(expectedAssetVersionId, ka.getAssetId().getVersionId().toString());
    assertEquals(1, ka.getCarriers().size());
    assertEquals(expectedArtifactId,
        ka.getCarriers().get(0).getArtifactId().getVersionId().toString());
    assertEquals(2, ka.getCarriers().get(0).getLinks().size());
    List<Link> links = ka.getCarriers().get(0).getLinks();
    for (Link link : links) {
      Dependency dependency = (Dependency) link;
      if (dependency.getHref().getTag().equals("5682fa26-b064-43c8-9475-1e4281e74068")) {
        assertEquals(expectedDependencyId1, dependency.getHref().getVersionId().toString());
      } else if (dependency.getHref().getTag().equals("ede3b331-7b10-4580-98be-66ebff344c21")) {
        assertEquals(expectedDependencyId2, dependency.getHref().getVersionId().toString());
      } else {
        fail("Unexpected dependency value");
      }
    }
  }

  /*
  expect to get back the latest versions of dependencies
   */
  @Test
  void getVersionKnowledgeAsset_latest() {
    String expectedAssetId = MAYO_ASSETS_BASE_URI
        + "3c99cf3a-93c4-4e09-b1aa-14088c76aded";
    String expectedAssetVersionId = expectedAssetId
        + "/versions/2.0.0";
    // Weave Test 1 (CMMN)
    String expectedArtifactId = MAYO_ARTIFACTS_BASE_URI
        + "f59708b6-96c0-4aa3-be4a-31e075d76ec9/versions/3.0.1";
    // Weaver Test 1 (DMN)
    String expectedDependencyId1 = MAYO_ARTIFACTS_BASE_URI
        + "5682fa26-b064-43c8-9475-1e4281e74068/versions/2.0.1";
    // Weaver Test 2 (DMN)
    String expectedDependencyId2 = MAYO_ARTIFACTS_BASE_URI
        + "ede3b331-7b10-4580-98be-66ebff344c21/versions/0.6.0";

    Answer<KnowledgeAsset> answer = tar
        .getKnowledgeAssetVersion(UUID.fromString("3c99cf3a-93c4-4e09-b1aa-14088c76aded"),
            "2.0.0");

    assertTrue(answer.isSuccess());
    assertNotNull(answer.get());
    KnowledgeAsset ka = answer.get();
    assertEquals(expectedAssetId, ka.getAssetId().getResourceId().toString());
    assertEquals(expectedAssetVersionId, ka.getAssetId().getVersionId().toString());
    assertEquals(1, ka.getCarriers().size());
    assertTrue(ka.getCarriers().get(0).getArtifactId().getVersionId()
        .toString().startsWith(expectedArtifactId));
    assertEquals(2, ka.getCarriers().get(0).getLinks().size());
    List<Link> links = ka.getCarriers().get(0).getLinks();
    for (Link link : links) {
      Dependency dependency = (Dependency) link;
      if (dependency.getHref().getTag().equals("5682fa26-b064-43c8-9475-1e4281e74068")) {
        assertTrue(dependency.getHref().getVersionId().toString().startsWith(expectedDependencyId1));
      } else if (dependency.getHref().getTag().equals("ede3b331-7b10-4580-98be-66ebff344c21")) {
        assertTrue(dependency.getHref().getVersionId().toString().startsWith(expectedDependencyId2));
      } else {
        fail("Unexpected dependency value: " + dependency.getHref().getVersionId().toString());
      }
    }
  }

  @Test
  void getVersionedKnowledgeAsset_PendingApproval() {
    String expectedAssetId = MAYO_ASSETS_BASE_URI
        + "e35a686e-5b72-4feb-b923-b79ac1417613";
    String expectedAssetVersionId = MAYO_ASSETS_BASE_URI
        + "e35a686e-5b72-4feb-b923-b79ac1417613/versions/1.0.0";
    String expectedArtifactId = MAYO_ARTIFACTS_BASE_URI
        + "ad174bca-8dd1-4e35-8933-e7456e1f3e5c/versions/0.0.1";

    Answer<KnowledgeAsset> answer = tar
        .getKnowledgeAssetVersion(UUID.fromString("e35a686e-5b72-4feb-b923-b79ac1417613"),
            "1.0.0");

    assertTrue(answer.isSuccess());
    assertNotNull(answer.get());
    KnowledgeAsset ka = answer.get();
    assertEquals(expectedAssetId, ka.getAssetId().getResourceId().toString());
    assertEquals(expectedAssetVersionId, ka.getAssetId().getVersionId().toString());
    assertEquals(1, ka.getCarriers().size());
    assertTrue(ka.getCarriers().get(0).getArtifactId().getVersionId()
        .toString().startsWith(expectedArtifactId));
    assertEquals(Final_Draft, ka.getLifecycle().getPublicationStatus());
  }

  @Test
  void getVersionedKnowledgeAsset_notFound_badVersion() {
    Answer<KnowledgeAsset> answer = tar
        .getKnowledgeAssetVersion(UUID.fromString("14321e7c-cb9a-427f-abf5-1420bf26e03c"),
            "1.2.0");
    assertTrue(answer.isClientFailure());
  }

  @Test
  void getVersionedKnowledgeAsset_notFound_badId() {
    Answer<KnowledgeAsset> answer = tar
        .getKnowledgeAssetVersion(UUID.fromString("14321e7c-cb9a-427f-abf5-1420bf26e03d"),
            "1.0.0");
    assertTrue(answer.isClientFailure());
  }


  @Test
  void initKnowledgeAsset() {
    Answer<UUID> answer = tar
        .initKnowledgeAsset();
    assertTrue(answer.isFailure());
    assertEquals(NotImplemented, answer.getOutcomeType());
  }

  @Test
  void listKnowledgeAssets() {
    Answer<List<Pointer>> listAnswer = tar.listKnowledgeAssets(null, null, null, null, null);
    assertTrue(listAnswer.isSuccess());
    assertFalse(listAnswer.get().isEmpty());
    String expectedDecisionId =
        MAYO_ASSETS_BASE_URI + "261543d9-90b6-4fe0-a26d-f329111d77ca/versions/1.0.0";
    String expectedDecisionName = "Basic Decision Model";
    String expectedCaseId =
        MAYO_ASSETS_BASE_URI + "14321e7c-cb9a-427f-abf5-1420bf26e03c/versions/1.0.1";
    String expectedCaseName = "Basic Case Model";
    AtomicBoolean foundDecision = new AtomicBoolean(false);
    AtomicBoolean foundCase = new AtomicBoolean(false);

    List<Pointer> pointers = listAnswer.get();

    assertTrue(pointers.size() >= 6);
    // Confirm some of the values
    pointers.forEach((ptr) -> {
      if (expectedDecisionId.equals(ptr.getVersionId().toString())) {
        assertEquals(expectedDecisionName, ptr.getName());
        assertEquals(Decision_Model.getReferentId(), ptr.getType());
        foundDecision.set(true);
      }
      if (expectedCaseId.equals(ptr.getVersionId().toString())) {
        assertEquals(expectedCaseName, ptr.getName());
        assertEquals(Care_Process_Model.getReferentId(), ptr.getType());
        foundCase.set(true);
      }
    });
    assertTrue(foundDecision.get());
    assertTrue(foundCase.get());
  }

  @Test
  void listKnowledgeAssets_DMN() {
    Answer<List<Pointer>> listAnswer = tar.listKnowledgeAssets("dmn", null, null, null, null);
    assertTrue(listAnswer.isSuccess());

    assertFalse(listAnswer.get().isEmpty());

    List<Pointer> pointers = listAnswer.get();

    assertTrue(pointers.size() >= 4);
    pointers.forEach((ptr) -> {
      // only Decision Models should be returned
      assertEquals(Decision_Model.getReferentId(), ptr.getType());
    });
  }

  @Test
  void listKnowledgeAssets_DMN_limit() {
    Answer<List<Pointer>> listAnswer = tar.listKnowledgeAssets("dmn", null, null, null, 2);
    assertTrue(listAnswer.isSuccess());

    assertFalse(listAnswer.get().isEmpty());

    List<Pointer> pointers = listAnswer.get();

    assertEquals(2, pointers.size());
    pointers.forEach((ptr) -> {
      // only Decision Models should be returned
      assertEquals(Decision_Model.getReferentId(), ptr.getType());
    });
  }


  @Test
  void listKnowledgeAssets_DMN_offset() {
    Answer<List<Pointer>> listAnswer = tar.listKnowledgeAssets("dmn", null, null, 3, 2);
    assertTrue(listAnswer.isSuccess());

    assertFalse(listAnswer.get().isEmpty());

    List<Pointer> pointers = listAnswer.get();

    assertTrue(pointers.size() >= 1 && pointers.size() < 3);

    pointers.forEach((ptr) -> {
      // only Decision Models should be returned
      assertEquals(Decision_Model.getReferentId(), ptr.getType());
    });
  }


  @Test
  void listKnowledgeAssets_CMMN() {
    Answer<List<Pointer>> listAnswer = tar.listKnowledgeAssets(CMMN_UPPER, null, null, null, null);
    assertTrue(listAnswer.isSuccess());

    assertFalse(listAnswer.get().isEmpty());

    List<Pointer> pointers = listAnswer.get();

    assertEquals(4, pointers.size());
    pointers.forEach((ptr) -> {
      // Only Care Process Models should be returned
      assertEquals(Care_Process_Model.getReferentId(), ptr.getType());
    });
  }

  @Test
  void listKnowledgeAssets_CMMN_limit() {
    Answer<List<Pointer>> listAnswer = tar.listKnowledgeAssets(CMMN_LOWER, null, null, null, 1);
    assertTrue(listAnswer.isSuccess());

    assertFalse(listAnswer.get().isEmpty());

    List<Pointer> pointers = listAnswer.get();

    assertEquals(1, pointers.size());
    pointers.forEach((ptr) -> {
      // Only Care Process Models should be returned
      assertEquals(Care_Process_Model.getReferentId(), ptr.getType());
    });
  }

  @Test
  void setVersionedKnowledgeAsset() {
    KnowledgeAsset ka = new KnowledgeAsset();
    Answer<Void> answer = tar
        .setKnowledgeAssetVersion(UUID.randomUUID(), "s", ka);
    assertTrue(answer.isFailure());
    assertEquals(NotImplemented, answer.getOutcomeType());
  }

  @Test
  void addKnowledgeAssetCarrier() {
    Answer<Void> answer = tar
        .addKnowledgeAssetCarrier(UUID.randomUUID(), "s", null);
    assertTrue(answer.isFailure());
    assertEquals(NotImplemented, answer.getOutcomeType());
  }

  @Test
  void getCanonicalKnowledgeAssetCarrier() {
    // Basic Decision Model.dmn
    String expectedAssetId = MAYO_ASSETS_BASE_URI
        + "261543d9-90b6-4fe0-a26d-f329111d77ca";
    String expectedAssetVersionId = MAYO_ASSETS_BASE_URI
        + "261543d9-90b6-4fe0-a26d-f329111d77ca/versions/1.0.0";
    String expectedArtifactId = MAYO_ARTIFACTS_BASE_URI
        + "ee0c768a-a0d4-4052-a6ea-fc0a3889b356/versions/1.4.0";

    Answer<KnowledgeCarrier> answer = tar
        .getCanonicalKnowledgeAssetCarrier(UUID.fromString("261543d9-90b6-4fe0-a26d-f329111d77ca"),
            "1.0.0",
            null);
    assertTrue(answer.isSuccess());
    assertNotNull(answer.get());
    KnowledgeCarrier kc = answer.get();
    assertEquals(expectedAssetId, kc.getAssetId().getResourceId().toString());
    assertEquals(expectedAssetVersionId, kc.getAssetId().getVersionId().toString());
    assertTrue(kc.getArtifactId().getVersionId().toString().startsWith(expectedArtifactId));

  }

  @Test
  void getCanonicalKnowledgeAssetCarrier_found() {
    String expectedAssetId = MAYO_ASSETS_BASE_URI
        + "14321e7c-cb9a-427f-abf5-1420bf26e03c";
    String expectedAssetVersionId = expectedAssetId
        + "/versions/1.0.0";
    String expectedArtifactId = MAYO_ARTIFACTS_BASE_URI
        + "16086bb8-c1fc-49b0-800b-c9b995dc5ed5/versions/1.6.0+1565742456000";

    Answer<KnowledgeCarrier> answer = tar
        .getCanonicalKnowledgeAssetCarrier(UUID.fromString("14321e7c-cb9a-427f-abf5-1420bf26e03c"),
            "1.0.0", null);
    assertTrue(answer.isSuccess());
    assertNotNull(answer.get());
    KnowledgeCarrier kc = answer.get();
    assertEquals(expectedAssetId, kc.getAssetId().getResourceId().toString());
    assertEquals(expectedAssetVersionId, kc.getAssetId().getVersionId().toString());
    assertEquals(expectedArtifactId,
        kc.getArtifactId().getVersionId().toString());


  }

  @Test
  void getCanonicalKnowledgeAssetCarrier_notFound_badVersion() {
    Answer<KnowledgeCarrier> answer = tar
        .getCanonicalKnowledgeAssetCarrier(UUID.fromString("14321e7c-cb9a-427f-abf5-1420bf26e03c"),
            "1.2.0", null);
    assertTrue(answer.isClientFailure());
  }

  @Test
  void getCanonicalKnowledgeAssetCarrier_notFound_badId() {
    Answer<KnowledgeCarrier> answer = tar
        .getCanonicalKnowledgeAssetCarrier(UUID.fromString("14321e7c-cb9a-427f-abf5-1420bf26e03d"),
            "1.0.0", null);
    assertTrue(answer.isClientFailure());
  }

  @Test
  void getKnowledgeAssetCarrierVersion() {
    // Basic Decision Model.dmn
    String expectedAssetId = MAYO_ASSETS_BASE_URI
        + "261543d9-90b6-4fe0-a26d-f329111d77ca";
    String expectedAssetVersionId = expectedAssetId
        + "/versions/1.0.0";
    String expectedArtifactId = MAYO_ARTIFACTS_BASE_URI
        + "ee0c768a-a0d4-4052-a6ea-fc0a3889b356";
    String expectedArtifactVersionId = expectedArtifactId + "/versions/1.4.0";

    Answer<KnowledgeCarrier> answer = tar
        .getKnowledgeAssetCarrierVersion(UUID.fromString("261543d9-90b6-4fe0-a26d-f329111d77ca"),
            "1.0.0",
            UUID.fromString("ee0c768a-a0d4-4052-a6ea-fc0a3889b356"), "1.4.0");
    assertTrue(answer.isSuccess());
    assertNotNull(answer.get());
    KnowledgeCarrier kc = answer.get();
    assertEquals(expectedAssetId, kc.getAssetId().getResourceId().toString());
    assertEquals(expectedAssetVersionId, kc.getAssetId().getVersionId().toString());
    assertEquals(expectedArtifactId, kc.getArtifactId().getResourceId().toString());
    assertEquals(expectedArtifactVersionId, kc.getArtifactId().getVersionId().toString());

  }


  @Test
  void getKnowledgeAssetCarrierVersion_found() {
    String expectedAssetId = MAYO_ASSETS_BASE_URI
        + "14321e7c-cb9a-427f-abf5-1420bf26e03c";
    String expectedAssetVersionId = expectedAssetId
        + "/versions/1.0.0";
    String expectedArtifactId = MAYO_ARTIFACTS_BASE_URI
        + "16086bb8-c1fc-49b0-800b-c9b995dc5ed5/versions/1.6.0+1565742456000";

    Answer<KnowledgeCarrier> answer = tar
        .getKnowledgeAssetCarrierVersion(UUID.fromString("14321e7c-cb9a-427f-abf5-1420bf26e03c"),
            "1.0.0", UUID.fromString("16086bb8-c1fc-49b0-800b-c9b995dc5ed5"), "1.6.0");

    assertTrue(answer.isSuccess());
    assertNotNull(answer.get());
    KnowledgeCarrier kc = answer.get();
    assertEquals(expectedAssetId, kc.getAssetId().getResourceId().toString());
    assertEquals(expectedAssetVersionId, kc.getAssetId().getVersionId().toString());
    assertEquals(expectedArtifactId,
        kc.getArtifactId().getVersionId().toString());

  }

  @Test
  void getKnowledgeAssetCarrierVersion_notFound_badAssetVersion() {
    Answer<KnowledgeCarrier> answer = tar
        .getKnowledgeAssetCarrierVersion(UUID.fromString("261543d9-90b6-4fe0-a26d-f329111d77ca"),
            "1.2.0", UUID.fromString("ee0c768a-a0d4-4052-a6ea-fc0a3889b356"), "1.3.0");
    assertTrue(answer.isClientFailure());
  }


  @Test
  void getKnowledgeAssetCarrierVersion_notFound_badArtifactVersion() {
    Answer<KnowledgeCarrier> answer = tar
        .getKnowledgeAssetCarrierVersion(UUID.fromString("261543d9-90b6-4fe0-a26d-f329111d77ca"),
            "1.0.0", UUID.fromString("ee0c768a-a0d4-4052-a6ea-fc0a3889b356"), "1.2.0");
    assertTrue(answer.isClientFailure());
  }

  @Test
  void getKnowledgeAssetCarrierVersion_notFound_badAssetId() {
    Answer<KnowledgeCarrier> answer = tar
        .getKnowledgeAssetCarrierVersion(UUID.fromString("14321e7c-cb9a-427f-abf5-1420bf26e03d"),
            "1.0.0", UUID.fromString("16086bb8-c1fc-49b0-800b-c9b995dc5ed5"), "1.8.0");
    assertTrue(answer.isClientFailure());
  }

  @Test
  void getKnowledgeAssetCarrierVersion_notFound_badArtifactId() {
    Answer<KnowledgeCarrier> answer = tar
        .getKnowledgeAssetCarrierVersion(UUID.fromString("14321e7c-cb9a-427f-abf5-1420bf26e03c"),
            "1.0.1", UUID.fromString("16086bb8-c1fc-49b0-800b-c9b995dc5ed6"), "1.8.0");
    assertTrue(answer.isClientFailure());
  }

  @Test
  void getKnowledgeAssetCarriers() {
    Answer<List<Pointer>> answer = tar
        .listKnowledgeAssetCarriers(UUID.randomUUID(), "s");
    assertTrue(answer.isFailure());
    assertEquals(NotImplemented, answer.getOutcomeType());

  }

  @Test
  void setKnowledgeAssetCarrierVersion_NotFound_InvalidAssetId() {
    Answer<Void> answer = tar
        .setKnowledgeAssetCarrierVersion(UUID.randomUUID(), "versionTag", UUID.randomUUID(),
            "artifactVersionTag", null);
    assertTrue(answer.isClientFailure());
  }

  @Test
  void setKnowledgeAssetCarrierVersion_NotFound_InvalidVersion() {
    InputStream testFile = TrisotechAssetRepositoryIntTest.class.getResourceAsStream(
        "/Test Save As.raw.dmn.xml");
    Answer<Void> answer = tar
        .setKnowledgeAssetCarrierVersion(UUID.fromString("3c66cf3a-93c4-4e09-b1aa-14088c76dead"),
            "1.0.0",
            UUID.fromString("e36338e7-500c-43a0-881d-22aa5dc538df"), "",
            XMLUtil.loadXMLDocument(testFile).map(XMLUtil::toByteArray).orElse(new byte[0]));
    assertTrue(answer.isClientFailure());
  }

  @Test
  void setKnowledgeAssetCarrierVersion_NotFound_InvalidArtifactid() {
    InputStream testFile = TrisotechAssetRepositoryIntTest.class
        .getResourceAsStream("/Test Save As.raw.dmn.xml");
    Answer<Void> answer = tar
        .setKnowledgeAssetCarrierVersion(UUID.fromString("3c66cf3a-93c4-4e09-b1aa-14088c76dead"),
            "1.0.0-SNAPSHOT", UUID.fromString("e36338e7-500c-43a0-881d-22aa5dc53abc"), "",
            XMLUtil.loadXMLDocument(testFile).map(XMLUtil::toByteArray).orElse(new byte[0]));
    assertTrue(answer.isClientFailure());
  }

  @Test
  void setKnowledgeAssetCarrierVersion_not_found() {
    InputStream testFile = TrisotechAssetRepositoryIntTest.class
        .getResourceAsStream("/Test Save As.raw.dmn.xml");
    Answer<Void> answer = tar
        .setKnowledgeAssetCarrierVersion(
            UUID.fromString("3c66cf3a-93c4-4e09-b1aa-14088c76dead"),
            "1.0.0-SNAPSHOT",
            UUID.fromString("e36338e7-500c-43a0-881d-22aa5dc538df"),
            // no version specified -> not found
            "",
            XMLUtil.loadXMLDocument(testFile).map(XMLUtil::toByteArray).orElse(new byte[0]));
    assertFalse(answer.isSuccess());
  }

  @Test
  void setKnowledgeAssetCarrierVersion_published_found() {
    // not planning on uploading published versions, but test that it works until further notice
    InputStream publishedFile = TrisotechAssetRepositoryIntTest.class
        .getResourceAsStream("/Test Save As.raw.dmn.xml");
    Answer<Void> answer = tar.setKnowledgeAssetCarrierVersion(
        UUID.fromString("3c66cf3a-93c4-4e09-b1aa-14088c76dead"),
        "1.0.0-SNAPSHOT",
        UUID.fromString("e36338e7-500c-43a0-881d-22aa5dc538df"),
        "1.0.3",
        XMLUtil.loadXMLDocument(publishedFile).map(XMLUtil::toByteArray).orElse(new byte[0]));
    assertTrue(answer.isSuccess());
  }
//
//  @Test
//  void getCompositeKnowledgeAsset() {
//    Answer<List<KnowledgeCarrier>> answer= tar
//        .getCompositeKnowledgeAssetS(UUID.randomUUID(), "s", false, "s1");
//    assertTrue(answer.isFailure());
//    assertEquals(NotImplemented, answer.getOutcomeType());
//
//  }

  @Test
  void getCompositeKnowledgeAssetStructure() {
    Answer<KnowledgeCarrier> answer = tar
        .getCompositeKnowledgeAssetStructure(UUID.randomUUID(), "s");
    assertTrue(answer.isFailure());
    assertEquals(NotImplemented, answer.getOutcomeType());

  }

//  @Test
//  void getKnowledgeArtifactBundle() {
//    Answer<List<KnowledgeCarrier>> answer= tar
//        .getKnowledgeArtifactBundle(UUID.randomUUID(), "s", "s1", 6, "s2");
//    assertTrue(answer.isFailure());
//    assertEquals(NotImplemented, answer.getOutcomeType());
//
//  }
//
//  @Test
//  void getKnowledgeAssetBundle() {
//    Answer<List<KnowledgeAsset>> answer= tar
//        .getKnowledgeAssetBundle(UUID.randomUUID(), "s", "s2", 7);
//    assertTrue(answer.isFailure());
//    assertEquals(NotImplemented, answer.getOutcomeType());
//
//  }

//  @Test
//  void queryKnowledgeAssets() {
//    Answer<List<Bindings>> answer= tar
//        .queryKnowledgeAssets(null);
//    assertTrue(answer.isFailure());
//    assertEquals(NotImplemented, answer.getOutcomeType());
//
//  }
}
