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

import edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.util.FileUtil;
import edu.mayo.kmdp.util.Util;
import edu.mayo.ontology.taxonomies.krlanguage._20190801.KnowledgeRepresentationLanguage;
import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._1_0.identifiers.Pointer;
import org.omg.spec.api4kp._1_0.identifiers.VersionIdentifier;
import org.omg.spec.api4kp._1_0.services.BinaryCarrier;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;
import org.omg.spec.api4kp._1_0.services.repository.KnowledgeAssetCatalog;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static edu.mayo.kmdp.id.helper.DatatypeHelper.toVersionIdentifier;
import static org.junit.jupiter.api.Assertions.*;

class TrisotechAssetRepositoryTestInt {

  private static TrisotechAssetRepository tar = new TrisotechAssetRepository();

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
    String expectedAssetId = "https://clinicalknowledgemanagement.mayo.edu/assets/735a5764-fe3f-4ab8-b103-650b6e805db2";
    String expectedAssetVersionId = "https://clinicalknowledgemanagement.mayo.edu/assets/735a5764-fe3f-4ab8-b103-650b6e805db2/versions/1.0.0";
    String expectedArtifactId = "https://clinicalknowledgemanagement.mayo.edu/artifacts/ee0c768a-a0d4-4052-a6ea-fc0a3889b356/versions/1.3.0";
    ResponseEntity<KnowledgeAsset> responseEntity = tar
        .getKnowledgeAsset(UUID.fromString("735a5764-fe3f-4ab8-b103-650b6e805db2"));
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
    KnowledgeAsset ka = responseEntity.getBody();
    assertEquals(expectedAssetId, ka.getAssetId().getUri().toString());
    assertEquals(expectedAssetVersionId, ka.getAssetId().getVersionId().toString());
    assertEquals(1, ka.getCarriers().size());
    assertEquals(expectedArtifactId, ka.getCarriers().get(0).getArtifactId().getUri().toString());
  }

  @Test
  void getKnowledgeAssetVersions() {
    ResponseEntity<List<Pointer>> responseEntity = tar
        .getKnowledgeAssetVersions(UUID.randomUUID(), 0, 10, "beforeTag", "afterTag", "ascend");
    assertEquals(HttpStatus.NOT_IMPLEMENTED, responseEntity.getStatusCode() );
  }

  // TODO: add 2 more tests: one with a version that needs to be 'found' and one with a version that doesn't exist CAO
  @Test
  void getVersionedKnowledgeAsset() {
    String expectedAssetId = "https://clinicalknowledgemanagement.mayo.edu/assets/735a5764-fe3f-4ab8-b103-650b6e805db2";
    String expectedAssetVersionId = "https://clinicalknowledgemanagement.mayo.edu/assets/735a5764-fe3f-4ab8-b103-650b6e805db2/versions/1.0.0";
    String expectedArtifactId = "https://clinicalknowledgemanagement.mayo.edu/artifacts/ee0c768a-a0d4-4052-a6ea-fc0a3889b356/versions/1.3.0";

    ResponseEntity<KnowledgeAsset> responseEntity = tar
        .getVersionedKnowledgeAsset(UUID.fromString("735a5764-fe3f-4ab8-b103-650b6e805db2"), "1.0.0");

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
    KnowledgeAsset ka = responseEntity.getBody();
    assertEquals(expectedAssetId, ka.getAssetId().getUri().toString());
    assertEquals(expectedAssetVersionId, ka.getAssetId().getVersionId().toString());
    assertEquals(1, ka.getCarriers().size());
    assertEquals(expectedArtifactId, ka.getCarriers().get(0).getArtifactId().getUri().toString());

  }

  @Test
  void initKnowledgeAsset() {
    ResponseEntity<UUID> responseEntity = tar
        .initKnowledgeAsset();
    assertEquals(HttpStatus.NOT_IMPLEMENTED, responseEntity.getStatusCode() );
  }

  @Test
  void listKnowledgeAssets() {
    ResponseEntity<List<Pointer>> models = tar.listKnowledgeAssets(null, null, null, null);
    assertSame( models.getStatusCode(), HttpStatus.OK );
    assertFalse( models.getBody().isEmpty() );

    List<Pointer> pointers = models.getBody();

    assertEquals(6, pointers.size());
    pointers.forEach( (ptr) -> {

      VersionIdentifier assetId = toVersionIdentifier( ptr.getHref() );

      System.out.println("assetId: " + assetId.toString());
      System.out.println("name: " + ptr.getName());
      System.out.println("type: " + ptr.getType().toString());

    });
  }



  @Test
  void listKnowledgeAssets_DMN() {
    ResponseEntity<List<Pointer>> models = tar.listKnowledgeAssets("dmn", null, null, null);
    assertSame( models.getStatusCode(), HttpStatus.OK );
    assertFalse( models.getBody().isEmpty() );

    List<Pointer> pointers = models.getBody();

    assertEquals(4, pointers.size());
    pointers.forEach( (ptr) -> {
      // TODO: other data to assert here? CAO

      System.out.println("assetId: " + ptr.getHref().toString());
      System.out.println("name: " + ptr.getName());
      System.out.println("type: " + ptr.getType().toString());

    });
  }


  @Test
  void listKnowledgeAssets_CMMN() {
    ResponseEntity<List<Pointer>> models = tar.listKnowledgeAssets("cmmn", null, null, null);
    assertSame( models.getStatusCode(), HttpStatus.OK );
    assertFalse( models.getBody().isEmpty() );

    List<Pointer> pointers = models.getBody();

    assertEquals(2, pointers.size());
    pointers.forEach( (ptr) -> {

      VersionIdentifier assetId = toVersionIdentifier( ptr.getHref() );

      System.out.println("assetId: " + assetId);
      System.out.println("name: " + ptr.getName());
      System.out.println("type: " + ptr.getType().toString());

    });
  }

  @Test
  void setVersionedKnowledgeAsset() {
    KnowledgeAsset ka = new KnowledgeAsset();
    ResponseEntity<Void> responseEntity = tar
        .setVersionedKnowledgeAsset(UUID.randomUUID(), "s", ka);
    assertEquals(HttpStatus.NOT_IMPLEMENTED, responseEntity.getStatusCode() );
  }

  @Test
  void addKnowledgeAssetCarrier() {
    KnowledgeAsset ka = new KnowledgeAsset();
    ResponseEntity<Void> responseEntity = tar
        .addKnowledgeAssetCarrier(UUID.randomUUID(), "s", null);
    assertEquals(HttpStatus.NOT_IMPLEMENTED, responseEntity.getStatusCode() );
  }

  // TODO: add at least 2 more tests: one with a version that needs to be 'found' and one with a version that doesn't exist CAO
  @Test
  void getCanonicalKnowledgeAssetCarrier() {
    String expectedAssetId = "https://clinicalknowledgemanagement.mayo.edu/assets/735a5764-fe3f-4ab8-b103-650b6e805db2";
    String expectedAssetVersionId = "https://clinicalknowledgemanagement.mayo.edu/assets/735a5764-fe3f-4ab8-b103-650b6e805db2/versions/1.0.0";
    String expectedArtifactId = "https://clinicalknowledgemanagement.mayo.edu/artifacts/ee0c768a-a0d4-4052-a6ea-fc0a3889b356/versions/1.3.0";

    ResponseEntity<KnowledgeCarrier> responseEntity = tar
        .getCanonicalKnowledgeAssetCarrier(UUID.fromString("735a5764-fe3f-4ab8-b103-650b6e805db2"), "1.0.0",
            null);
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
    KnowledgeCarrier kc = responseEntity.getBody();
    assertEquals(expectedAssetId, kc.getAssetId().getUri().toString());
    assertEquals(expectedAssetVersionId, kc.getAssetId().getVersionId().toString());
    assertEquals(expectedArtifactId, kc.getArtifactId().getUri().toString());

  }

  // TODO: more tests for versions that need to be 'found' and for versions that don't exist, for both asset and artifact CAO
  @Test
  void getKnowledgeAssetCarrierVersion() {
    String expectedAssetId = "https://clinicalknowledgemanagement.mayo.edu/assets/735a5764-fe3f-4ab8-b103-650b6e805db2";
    String expectedAssetVersionId = "https://clinicalknowledgemanagement.mayo.edu/assets/735a5764-fe3f-4ab8-b103-650b6e805db2/versions/1.0.0";
    String expectedArtifactId = "https://clinicalknowledgemanagement.mayo.edu/artifacts/ee0c768a-a0d4-4052-a6ea-fc0a3889b356/versions/1.3.0";

    ResponseEntity<KnowledgeCarrier> responseEntity = tar
        .getKnowledgeAssetCarrierVersion(UUID.fromString("735a5764-fe3f-4ab8-b103-650b6e805db2"), "1.0.0",
            UUID.fromString("ee0c768a-a0d4-4052-a6ea-fc0a3889b356"),"1.3.0");

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
    KnowledgeCarrier kc = responseEntity.getBody();
    assertEquals(expectedAssetId, kc.getAssetId().getUri().toString());
    assertEquals(expectedAssetVersionId, kc.getAssetId().getVersionId().toString());
    assertEquals(expectedArtifactId, kc.getArtifactId().getUri().toString());

  }

  @Test
  void getKnowledgeAssetCarriers() {
    ResponseEntity<List<Pointer>> responseEntity = tar
        .getKnowledgeAssetCarriers(UUID.randomUUID(), "s");
    assertEquals(HttpStatus.NOT_IMPLEMENTED, responseEntity.getStatusCode() );
  }

  @Test
  void setKnowledgeAssetCarrierVersion() {
    ResponseEntity<Void> responseEntity = tar
        .setKnowledgeAssetCarrierVersion(UUID.randomUUID(), "versionTag", UUID.randomUUID(), "artifactVersionTag", null);
    assertEquals(HttpStatus.NOT_IMPLEMENTED, responseEntity.getStatusCode() );
  }

  @Test
  void getCompositeKnowledgeAsset() {
    ResponseEntity<List<KnowledgeCarrier>> responseEntity = tar
        .getCompositeKnowledgeAsset(UUID.randomUUID(), "s", false, "s1");
    assertEquals(HttpStatus.NOT_IMPLEMENTED, responseEntity.getStatusCode() );
  }

  @Test
  void getCompositeKnowledgeAssetStructure() {
    ResponseEntity<KnowledgeCarrier> responseEntity = tar
        .getCompositeKnowledgeAssetStructure(UUID.randomUUID(), "s");
    assertEquals(HttpStatus.NOT_IMPLEMENTED, responseEntity.getStatusCode() );
  }

  @Test
  void getKnowledgeArtifactBundle() {
    ResponseEntity<List<KnowledgeCarrier>> responseEntity = tar
        .getKnowledgeArtifactBundle(UUID.randomUUID(), "s", "s1", 6, "s2");
    assertEquals(HttpStatus.NOT_IMPLEMENTED, responseEntity.getStatusCode() );
  }

  @Test
  void getKnowledgeAssetBundle() {
    ResponseEntity<List<KnowledgeAsset>> responseEntity = tar
        .getKnowledgeAssetBundle(UUID.randomUUID(), "s", "s2", 7);
    assertEquals(HttpStatus.NOT_IMPLEMENTED, responseEntity.getStatusCode() );
  }

  @Test
  void queryKnowledgeAssets() {
    ResponseEntity<Void> responseEntity = tar
        .queryKnowledgeAssets("s" );
    assertEquals(HttpStatus.NOT_IMPLEMENTED, responseEntity.getStatusCode() );
  }
}