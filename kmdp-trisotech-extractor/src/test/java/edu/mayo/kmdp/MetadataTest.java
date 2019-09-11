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

import static edu.mayo.kmdp.preprocess.meta.MetadataExtractor.Format.JSON;
import static edu.mayo.kmdp.preprocess.meta.MetadataExtractor.Format.XML;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.preprocess.NotLatestVersionException;
import edu.mayo.kmdp.preprocess.meta.MetadataExtractor;
import edu.mayo.kmdp.preprocess.meta.Weaver;
import edu.mayo.kmdp.util.JaxbUtil;
import edu.mayo.kmdp.util.Util;
import edu.mayo.kmdp.util.XMLUtil;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.PostConstruct;
import javax.xml.transform.stream.StreamSource;
import org.apache.jena.shared.NotFoundException;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._1_0.identifiers.URIIdentifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringBootTest
@SpringJUnitConfig(classes = {ExtractorConfig.class, IdentityMapperConfig.class})
class MetadataTest {

//  private static MetadataExtractor extractor = new MetadataExtractor();

  @Autowired
  private MetadataExtractor extractor;

  private static String dmnPath = "/Weaver Test 1.dmn";
  private static String metaPath = "/WeaverTest1Meta.json";
  private static String cmmnPath = "/Weave Test 1.cmmn";
  private static String cmmnMetaPath = "/WeaveTest1Meta.json";
  private static boolean constructed = false;

  @Autowired
  private Weaver weaver;
//  private static Weaver cmmnWeaver;

  private static byte[] annotatedDMN;
  private static byte[] annotatedCMMN;

//  @BeforeAll

  /**
   * Need to use @PostConstruct instead of @BeforeAll because @BeforeAll requires the method be
   * static, and cannot @Autowired on static variables which would be needed for the static method.
   * Have the check for constructed as otherwise @PostConstruct will be run before EVERY @Test.
   * It will will, but now the processing won't happen after the first time.
   */
  @PostConstruct
  public void init() {

//    dmnWeaver = new Weaver();
//
//    cmmnWeaver = new Weaver();
    if(!constructed) {

      Optional<byte[]> dmn = XMLUtil
          .loadXMLDocument(MetadataTest.class.getResourceAsStream(dmnPath))
          .map(weaver::weave)
          .map(XMLUtil::toByteArray);
      assertTrue(dmn.isPresent());
      annotatedDMN = dmn.get();

      System.out.println(new String(annotatedDMN));

      Optional<byte[]> cmmn = XMLUtil
          .loadXMLDocument(MetadataTest.class.getResourceAsStream(cmmnPath))
          .map(weaver::weave)
          .map(XMLUtil::toByteArray);
      assertTrue(cmmn.isPresent());
      annotatedCMMN = cmmn.get();

      System.out.println(new String(annotatedCMMN));
      constructed = true;
    }

  }

  @Test
  void testExtraction() {
    try {
      Optional<KnowledgeAsset> res = extractor.extract(new ByteArrayInputStream(annotatedDMN),
          MetadataTest.class.getResourceAsStream(metaPath));
      if (!res.isPresent()) {
        fail("Unable to instantiate metadata object");
      }
      KnowledgeAsset surr = res.get();
      assertNotNull(surr);
      assertNotNull(surr.getCarriers());

      assertNotNull(surr.getAssetId());
      assertNotNull(surr.getAssetId().getUri());
      assertNotNull(surr.getAssetId().getVersionId());
      assertNotNull(surr.getName());
      // TODO: anything else to validate? CAO
    } catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  void testXMLValidate() {
    Optional<ByteArrayOutputStream> baos = extractor
        .doExtract(new ByteArrayInputStream(annotatedDMN),
            MetadataTest.class.getResourceAsStream(metaPath),
            XML,
            JaxbUtil.defaultProperties());

    if (!baos.isPresent()) {
      fail("Unable to create metadata");
    } else {
      boolean ans = baos.map(ByteArrayOutputStream::toByteArray)
          .map(ByteArrayInputStream::new)
          .map(StreamSource::new)
          .map((dox) -> XMLUtil.validate(dox, SurrogateHelper.getSchema().get()))
          .orElse(false);
      assertTrue(ans);
    }
  }

  @Test
  void testToXML() {
    assertTrue(extractor.doExtract(new ByteArrayInputStream(annotatedDMN),
        MetadataTest.class.getResourceAsStream(metaPath),
        XML,
        JaxbUtil.defaultProperties())
        .map(Util::printOut).isPresent());
    assertTrue(extractor.doExtract(new ByteArrayInputStream(annotatedCMMN),
        MetadataTest.class.getResourceAsStream(cmmnMetaPath),
        XML,
        JaxbUtil.defaultProperties())
        .map(Util::printOut).isPresent());

  }

  @Test
  void testToJson() {
    assertTrue(extractor.doExtract(new ByteArrayInputStream(annotatedDMN),
        MetadataTest.class.getResourceAsStream(metaPath),
        JSON,
        JaxbUtil.defaultProperties())
        .map(Util::printOut).isPresent());
    assertTrue(extractor.doExtract(new ByteArrayInputStream(annotatedCMMN),
        MetadataTest.class.getResourceAsStream(cmmnMetaPath),
        JSON,
        JaxbUtil.defaultProperties())
        .map(Util::printOut).isPresent());
  }

  @Test
  void testGetEnterpriseAssetIdForAsset() {
    Optional<URI> enterpriseAsset = extractor
        .getEnterpriseAssetIdForAsset(UUID.fromString("14321e7c-cb9a-427f-abf5-1420bf26e03c"));
    assertNotNull(enterpriseAsset);
    assertEquals(
        "https://clinicalknowledgemanagement.mayo.edu/assets/14321e7c-cb9a-427f-abf5-1420bf26e03c",
        enterpriseAsset.get().toString());
  }

  @Test
  void testGetEnterpriseAssetIdForAsset_empty() {
    Optional<URI> enterpriseAsset = extractor
        .getEnterpriseAssetIdForAsset(UUID.fromString("14421eac-cb9a-427f-abf5-1420bf26e03c"));
    assertNotNull(enterpriseAsset);
    assertFalse(enterpriseAsset.isPresent());
    assertEquals(Optional.empty(), enterpriseAsset);
  }

  @Test
  void testGetEnterpriseAssetVersionIdForAsset() {
    Optional<URI> enterpriseAssetVersion = null;
    try {
      enterpriseAssetVersion = extractor.getEnterpriseAssetVersionIdForAsset(
          UUID.fromString("14321e7c-cb9a-427f-abf5-1420bf26e03c"),
          "1.0.1");
    } catch (NotLatestVersionException e) {
      fail();
    }
    assertNotNull(enterpriseAssetVersion);
    assertEquals(
        "https://clinicalknowledgemanagement.mayo.edu/assets/14321e7c-cb9a-427f-abf5-1420bf26e03c/versions/1.0.1",
        enterpriseAssetVersion.get().toString());
  }

  @Test
  void testGetEnterpriseAssetVersionIdForAsset_badId() {
    Optional<URI> enterpriseAssetVersion = null;
    NotFoundException nfe = assertThrows(
        NotFoundException.class,
        () -> extractor.getEnterpriseAssetVersionIdForAsset(
            UUID.fromString("14ba1e7c-cb9a-427f-abf5-1420bf26e03c"),
            "1.0.1"));

  }

  @Test
  void testGetArtifactVersion() {
    Optional<String> artifactVersion = extractor
        .getArtifactVersion(UUID.fromString("14321e7c-cb9a-427f-abf5-1420bf26e03c"));
    assertNotNull(artifactVersion);
    assertEquals("1.8.0", artifactVersion.get());
  }

  @Test
  void testGetMimeType() {
    Optional<String> mimetype = extractor
        .getMimetype(UUID.fromString("bd0014e6-afbe-4006-b182-baa973f2929a"));
    assertNotNull(mimetype);
    assertEquals("application/vnd.triso-dmn+json", mimetype.get());

    mimetype = extractor.getMimetype(UUID.fromString("14321e7c-cb9a-427f-abf5-1420bf26e03c"));
    assertNotNull(mimetype);
    assertEquals("application/vnd.triso-cmmn+json", mimetype.get());
  }

  @Test
  void testGetEnterpriseAssetVersionIdForAsset_badVersion() {
    NotLatestVersionException nlve = assertThrows(
        NotLatestVersionException.class,
        () -> extractor.getEnterpriseAssetVersionIdForAsset(
            UUID.fromString("14321e7c-cb9a-427f-abf5-1420bf26e03c"),
            "1.1.0"));
    // internalId provided with exception
    assertEquals(
        "http://www.trisotech.com/definitions/_16086bb8-c1fc-49b0-800b-c9b995dc5ed5"
        , nlve.getMessage());
  }

  @Test
  void testResolveInternalArtifactID() {
    try {
      String artifactId = extractor
          .resolveInternalArtifactID("3c66cf3a-93c4-4e09-b1aa-14088c76aded", "1.0.0-SNAPSHOT");
      assertEquals("http://www.trisotech.com/definitions/_5682fa26-b064-43c8-9475-1e4281e74068",
          artifactId);
    } catch (NotLatestVersionException e) {
      fail("Should have artifact for specified asset: 3c66cf3a-93c4-4e09-b1aa-14088c76aded");
      e.printStackTrace();
    }
  }

  @Test
  void testResolveInternalArtifactID_NotLatestVersionException() {
    NotLatestVersionException ave = assertThrows(
        NotLatestVersionException.class,
        () -> extractor.resolveInternalArtifactID("3c66cf3a-93c4-4e09-b1aa-14088c76aded", "2.0.0"));
    assertEquals("http://www.trisotech.com/definitions/_5682fa26-b064-43c8-9475-1e4281e74068",
        ave.getMessage());
  }

  @Test
  void testResolveInternalArtifactID_Null() {
    try {
      String artifactId = extractor
          .resolveInternalArtifactID("abcdef3a-93c4-4e09-b1aa-14088c76adee", "1.0.0-SNAPSHOT");
      assertEquals(null, artifactId);
    } catch (NotLatestVersionException e) {
      fail("Unexpected error");
      e.printStackTrace();
    }
  }

  @Test
  void testResolveEnterpriseAssetID_IllegalStateException() {
    IllegalStateException ave = assertThrows(
        IllegalStateException.class,
        () -> extractor.resolveEnterpriseAssetID("3c66cf3a-93c4-4e09-b1aa-14088c76aded"));
    assertEquals(
        "Defensive: Unable to resolve internal ID 3c66cf3a-93c4-4e09-b1aa-14088c76aded to a known Enterprise ID",
        ave.getMessage());
  }

  @Test
  void testResolveEnterpriseAssetID() {
    URIIdentifier assetID = extractor
        .resolveEnterpriseAssetID("123720a6-9758-45a3-8c5c-5fffab12c494");
    assertEquals(
        "https://clinicalknowledgemanagement.mayo.edu/assets/3c66cf3a-93c4-4e09-b1aa-14088c76aded/versions/1.0.0-SNAPSHOT",
        assetID.getUri().toString());
  }

  @Test
  void testGetFileId_AssetUUID() {
    Optional<String> fileid = extractor
        .getFileId(UUID.fromString("3c66cf3a-93c4-4e09-b1aa-14088c76aded"));
    assertNotNull(fileid);
    assertTrue(fileid.isPresent());
    assertEquals("123720a6-9758-45a3-8c5c-5fffab12c494", fileid.get());
  }

  @Test
  void testGetFileId_AssetUUID_empty() {
    Optional<String> fileid = extractor
        .getFileId(UUID.fromString("abcdef3a-93c4-4e09-b1aa-14088c76adee"));
    assertNotNull(fileid);
    assertFalse(fileid.isPresent());
    assertEquals(Optional.empty(), fileid);
  }

  @Test
  void testGetFileId_internalId() {
    Optional<String> fileid = extractor
        .getFileId("http://www.trisotech.com/definitions/_5682fa26-b064-43c8-9475-1e4281e74068");
    assertNotNull(fileid);
    assertTrue(fileid.isPresent());
    assertEquals("123720a6-9758-45a3-8c5c-5fffab12c494", fileid.get());
  }


  @Test
  void testGetFileId_internalId_empty() {
    Optional<String> fileid = extractor.getFileId("abcdef3a-93c4-4e09-b1aa-14088c76adee");
    assertNotNull(fileid);
    assertFalse(fileid.isPresent());
    assertEquals(Optional.empty(), fileid);
  }


}
