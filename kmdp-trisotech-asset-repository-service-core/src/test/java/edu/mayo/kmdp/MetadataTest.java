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

import static edu.mayo.kmdp.kdcaci.knew.trisotech.preprocess.MetadataExtractor.Format.JSON;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.preprocess.MetadataExtractor.Format.XML;
import static edu.mayo.kmdp.registry.Registry.MAYO_ARTIFACTS_BASE_URI;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import edu.mayo.kmdp.kdcaci.knew.trisotech.preprocess.MetadataExtractor;
import edu.mayo.kmdp.kdcaci.knew.trisotech.preprocess.NotLatestVersionException;
import edu.mayo.kmdp.kdcaci.knew.trisotech.preprocess.Weaver;
import edu.mayo.kmdp.util.DateTimeUtil;
import edu.mayo.kmdp.util.JaxbUtil;
import edu.mayo.kmdp.util.Util;
import edu.mayo.kmdp.util.XMLUtil;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.PostConstruct;
import javax.xml.transform.stream.StreamSource;
import org.apache.jena.shared.NotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.SurrogateHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringBootTest
@ContextConfiguration(classes = {TrisotechAssetRepositoryConfig.class})
@TestPropertySource(properties = {
    "edu.mayo.kmdp.trisotechwrapper.repositoryName=MEA-Test",
    "edu.mayo.kmdp.trisotechwrapper.repositoryId=d4aca01b-d446-4bc8-a6f0-85d84f4c1aaf"})
class MetadataTest {

  // FYI: IDE may complain about
  // the following two not being able to be autowired, but the code works.
  @Autowired
  private MetadataExtractor extractor;

  @Autowired
  private Weaver weaver;

  private static String dmnPath = "/Weaver Test 1.dmn.xml";
  private static String metaPath = "/Weaver Test 1.meta.json";
  private static String cmmnPath = "/Weave Test 1.cmmn.xml";
  private static String cmmnMetaPath = "/Weave Test 1.meta.json";
  private static boolean constructed = false;

  private static byte[] annotatedDMN;
  private static byte[] annotatedCMMN;


  /**
   * Need to use @PostConstruct instead of @BeforeAll because @BeforeAll requires the method be
   * static, and cannot @Autowired on static variables which would be needed for the static method.
   * Have the check for constructed as otherwise @PostConstruct will be run before EVERY @Test. It
   * still will but now the processing won't happen after the first time.
   */
  @PostConstruct
  public void init() {

    if (!constructed) {

      Optional<byte[]> dmn = XMLUtil
          .loadXMLDocument(MetadataTest.class.getResourceAsStream(dmnPath))
          .map(weaver::weave)
          .map(XMLUtil::toByteArray);
      assertTrue(dmn.isPresent());
      annotatedDMN = dmn.get();

      Optional<byte[]> cmmn = XMLUtil
          .loadXMLDocument(MetadataTest.class.getResourceAsStream(cmmnPath))
          .map(weaver::weave)
          .map(XMLUtil::toByteArray);
      assertTrue(cmmn.isPresent());
      annotatedCMMN = cmmn.get();

      constructed = true;
    }

  }

  @Test
  void testExtraction() {
    try {
      Optional<KnowledgeAsset> res = extractor.extract(new ByteArrayInputStream(annotatedDMN),
          MetadataTest.class.getResourceAsStream(metaPath));
      if (res.isEmpty()) {
        fail("Unable to instantiate metadata object");
      }
      KnowledgeAsset surr = res.get();
      assertNotNull(surr);
      assertNotNull(surr.getCarriers());

      assertNotNull(surr.getAssetId());
      assertNotNull(surr.getAssetId().getResourceId());
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

    if (baos.isEmpty()) {
      fail("Unable to create metadata");
    } else {
      boolean ans = baos.map(ByteArrayOutputStream::toByteArray)
          .map(ByteArrayInputStream::new)
          .map(StreamSource::new)
          .map((dox) -> XMLUtil
              .validate(dox, SurrogateHelper.getSchema().orElseGet(Assertions::fail)))
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
          "1.0.1", false);
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
            "1.0.1", false));

  }

  @Test
  void testGetArtifactVersion() {
    Optional<String> artifactVersion = extractor
        .getArtifactVersion(UUID.fromString("14321e7c-cb9a-427f-abf5-1420bf26e03c"));
    assertNotNull(artifactVersion);
    assertEquals("1.8.3", artifactVersion.get());
  }

  @Test
  void testGetMimeType() {
    Optional<String> mimetype = extractor
        .getMimetype(UUID.fromString("bd0014e6-afbe-4006-b182-baa973f2929a"));
    assertNotNull(mimetype);
    assertEquals("application/vnd.triso-dmn+json", mimetype.get());

    mimetype = extractor.getMimetype(UUID.fromString("14321e7c-cb9a-427f-abf5-1420bf26e03c"));
    assertNotNull(mimetype);
    assertFalse(mimetype.isEmpty());
    assertEquals("application/vnd.triso-cmmn+json", mimetype.get());
  }

  @Test
  void testGetEnterpriseAssetVersionIdForAsset_badVersion() {
    NotLatestVersionException nlve = assertThrows(
        NotLatestVersionException.class,
        () -> extractor.getEnterpriseAssetVersionIdForAsset(
            UUID.fromString("14321e7c-cb9a-427f-abf5-1420bf26e03c"),
            "1.1.0", false));
    // internalId provided with exception
    assertEquals(
        "http://www.trisotech.com/definitions/_16086bb8-c1fc-49b0-800b-c9b995dc5ed5"
        , nlve.getMessage());
  }

  @Test
  void testResolveInternalArtifactID() {
    try {
      String artifactId = extractor
          .resolveInternalArtifactID(UUID.fromString("3c66cf3a-93c4-4e09-b1aa-14088c76aded"),
              "1.1.1", false);
      assertEquals("http://www.trisotech.com/definitions/_5682fa26-b064-43c8-9475-1e4281e74068",
          artifactId);
    } catch (NotLatestVersionException e) {
      fail(
          "Should have artifact for specified asset: 3c66cf3a-93c4-4e09-b1aa-14088c76aded with version 1.1.1");
      e.printStackTrace();
    }
  }

  @Test
  void testResolveInternalArtifactID_Published_NotLatestVersionException() {
    NotLatestVersionException ave = assertThrows(
        NotLatestVersionException.class,
        () -> extractor
            .resolveInternalArtifactID(UUID.fromString("3c66cf3a-93c4-4e09-b1aa-14088c76aded"),
                "2.0.0",
                false));
    assertEquals("http://www.trisotech.com/definitions/_5682fa26-b064-43c8-9475-1e4281e74068",
        ave.getMessage());
  }

  @Test
  void testResolveInternalArtifactID_Published_NotFound() {
    UUID assetId = UUID.fromString("abcdef3a-93c4-4e09-b1aa-14088c76adee");
    NotFoundException nfe = assertThrows(NotFoundException.class,
        () -> extractor
            .resolveInternalArtifactID(assetId, "1.0.0-SNAPSHOT", false));
    assertEquals(assetId.toString(), nfe.getMessage());
  }

  @Test
  void testResolveInternalArtifactID_Any() {
    try {
      String artifactId = extractor
          .resolveInternalArtifactID(UUID.fromString("3c66cf3a-93c4-4e09-b1aa-14088c76aded"),
              "1.1.1", true);
      assertEquals("http://www.trisotech.com/definitions/_5682fa26-b064-43c8-9475-1e4281e74068",
          artifactId);
    } catch (NotLatestVersionException e) {
      fail(
          "Should have artifact for specified asset: 3c66cf3a-93c4-4e09-b1aa-14088c76aded and version 1.1.1");
      e.printStackTrace();
    }
  }

  @Test
  void testResolveInternalArtifactID_Any_NotLatestVersionException() {
    NotLatestVersionException ave = assertThrows(
        NotLatestVersionException.class,
        () -> extractor
            .resolveInternalArtifactID(UUID.fromString("3c66cf3a-93c4-4e09-b1aa-14088c76aded"),
                "2.0.0",
                true));
    assertEquals("http://www.trisotech.com/definitions/_5682fa26-b064-43c8-9475-1e4281e74068",
        ave.getMessage());
  }

  @Test
  void testResolveInternalArtifactID_Any_NotFound() {
    UUID assetId = UUID.fromString("abcdef3a-93c4-4e09-b1aa-14088c76adee");
    NotFoundException nfe = assertThrows(NotFoundException.class,
        () -> extractor
            .resolveInternalArtifactID(assetId, "1.0.0-SNAPSHOT",
                true));
    assertEquals(assetId.toString(), nfe.getMessage());

  }

  @Test
  void testResolveEnterpriseAssetID_IllegalStateException() {
    IllegalStateException ave = assertThrows(
        IllegalStateException.class,
        () -> extractor.resolveEnterpriseAssetID("3c66cf3a-93c4-4e09-b1aa-14088c76aded"));
    assertTrue(ave.getMessage().contains("3c66cf3a-93c4-4e09-b1aa-14088c76aded"));
  }

  @Test
  void testResolveEnterpriseAssetID() {
    ResourceIdentifier assetID = extractor
        .resolveEnterpriseAssetID("http://www.trisotech.com/definitions/_5682fa26-b064-43c8-9475-1e4281e74068");
    assertEquals(
        "https://clinicalknowledgemanagement.mayo.edu/assets/3c66cf3a-93c4-4e09-b1aa-14088c76aded/versions/1.1.1",
        assetID.getVersionId().toString());
  }

  @Test
  void testGetModelId_AssetUUID() {
    Optional<String> modelId = extractor
        .getModelId(UUID.fromString("3c66cf3a-93c4-4e09-b1aa-14088c76aded"), false);
    assertNotNull(modelId);
    assertTrue(modelId.isPresent());
    assertEquals("http://www.trisotech.com/definitions/_5682fa26-b064-43c8-9475-1e4281e74068", modelId.get());
  }


  @Test
  void testGetModelId_internalId() {
    Optional<String> fileid = extractor
        .getModelId("http://www.trisotech.com/definitions/_5682fa26-b064-43c8-9475-1e4281e74068");
    assertNotNull(fileid);
    assertTrue(fileid.isPresent());
    assertEquals("http://www.trisotech.com/definitions/_5682fa26-b064-43c8-9475-1e4281e74068", fileid.get());
  }

  @Test
  void testGetModelId_internalId_tagOnly() {
    Optional<String> fileid = extractor
        .getModelId("5682fa26-b064-43c8-9475-1e4281e74068");
    assertNotNull(fileid);
    assertTrue(fileid.isPresent());
    assertEquals("http://www.trisotech.com/definitions/_5682fa26-b064-43c8-9475-1e4281e74068", fileid.get());
  }

  @Test
  void testGetModel_internalId_empty() {
    Optional<String> fileid = extractor.getModelId("abcdef3a-93c4-4e09-b1aa-14088c76adee");
    assertNotNull(fileid);
    assertFalse(fileid.isPresent());
    assertEquals(Optional.empty(), fileid);
  }

  @Test
  void testGetModelId_internalId_URIString_empty() {
    Optional<String> fileid = extractor
        .getModelId("http://www.trisotech.com/definitions/_5682fa26-b064-43c8-9475-1e4281e7abcd");
    assertNotNull(fileid);
    assertFalse(fileid.isPresent());
    assertEquals(Optional.empty(), fileid);
  }

  @Test
  void testConvertInternalId() {
    String id = "5682fa26-b064-43c8-9475-1e4281e7abcd";
    String internalId = "http://www.trisotech.com/definitions/_" + id;
    String versionTag = "3.2.4";
    String expectedFileId = MAYO_ARTIFACTS_BASE_URI + id;
    String updated = "2019-08-01T13:17:30Z";
    Date modelDate = Date.from(Instant.parse(updated));
    String expectedFileIdAndVersion =
        expectedFileId + "/versions/" + versionTag + "+" + modelDate.getTime();
    String expectedVersionTag = versionTag + "+" + modelDate.getTime();

    // test w/o a version
    ResourceIdentifier fileId = extractor.convertInternalId(internalId, null, null);
    assertNotNull(fileId);
    assertEquals(id, fileId.getTag());
    assertNull(fileId.getVersionTag());
    assertEquals(expectedFileId, fileId.getResourceId().toString());

    // test w/version
    fileId = extractor.convertInternalId(internalId, versionTag, DateTimeUtil.dateTimeStrToMillis(updated));
    assertNotNull(fileId);
    assertEquals(id, fileId.getTag());
    assertEquals(expectedVersionTag, fileId.getVersionTag());
    assertEquals(expectedFileId, fileId.getResourceId().toString());
    assertEquals(expectedFileIdAndVersion, fileId.getVersionId().toString());
  }


}
