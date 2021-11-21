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
import static edu.mayo.kmdp.trisotechwrapper.TrisotechWrapper.applyTimestampToVersion;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newVersionId;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.JSON;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;

import edu.mayo.kmdp.kdcaci.knew.trisotech.IdentityMapper;
import edu.mayo.kmdp.kdcaci.knew.trisotech.NamespaceManager;
import edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.MetadataIntrospector;
import edu.mayo.kmdp.kdcaci.knew.trisotech.components.redactors.Redactor;
import edu.mayo.kmdp.kdcaci.knew.trisotech.components.weavers.Weaver;
import edu.mayo.kmdp.kdcaci.knew.trisotech.exception.NotFoundException;
import edu.mayo.kmdp.kdcaci.knew.trisotech.exception.NotLatestAssetVersionException;
import edu.mayo.kmdp.util.XMLUtil;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.PostConstruct;
import javax.xml.transform.stream.StreamSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.SurrogateHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ContextConfiguration(classes = {TrisotechAssetRepositoryTestConfig.class})
@TestPropertySource(properties = {
    "edu.mayo.kmdp.trisotechwrapper.repositoryName=MEA-Test",
    "edu.mayo.kmdp.trisotechwrapper.repositoryId=d4aca01b-d446-4bc8-a6f0-85d84f4c1aaf",
    "edu.mayo.kmdp.trisotechwrapper.baseUrl=https://mc.trisotech.com/",
    "edu.mayo.kmdp.application.flag.assetsOnly=true"})
class MetadataTest {

  // FYI: IDE may complain about
  // the following two not being able to be autowired, but the code works.
  @Autowired
  private MetadataIntrospector extractor;

  @Autowired
  NamespaceManager names;

  @Autowired
  private IdentityMapper mapper;

  @Autowired
  private Weaver weaver;

  @Autowired
  private Redactor redactor;

  private static final String dmnPath = "/Weaver Test 1.dmn.xml";
  private static final String metaPath = "/Weaver Test 1.meta.json";
  private static final String cmmnPath = "/Weave Test 1.cmmn.xml";
  private static final String cmmnMetaPath = "/Weave Test 1.meta.json";
  private static boolean constructed = false;

  private static byte[] annotatedDMN;
  private static byte[] annotatedCMMN;

  private final String xmlCodedRep = codedRep(
      Knowledge_Asset_Surrogate_2_0, XML_1_1, Charset.defaultCharset(), Encodings.DEFAULT);
  private final String jsonCodedRep = codedRep(
      Knowledge_Asset_Surrogate_2_0, JSON, Charset.defaultCharset(), Encodings.DEFAULT);


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
          .map(redactor::redact)
          .map(XMLUtil::toByteArray);
      assertTrue(dmn.isPresent());
      annotatedDMN = dmn.get();

      Optional<byte[]> cmmn = XMLUtil
          .loadXMLDocument(MetadataTest.class.getResourceAsStream(cmmnPath))
          .map(weaver::weave)
          .map(redactor::redact)
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
    Optional<byte[]> baos = extractor
        .extractBinary(new ByteArrayInputStream(annotatedDMN),
            MetadataTest.class.getResourceAsStream(metaPath),
            xmlCodedRep);

    if (baos.isEmpty()) {
      fail("Unable to create metadata");
    } else {
      boolean ans = baos
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
    assertTrue(extractor.extractBinary(new ByteArrayInputStream(annotatedDMN),
        MetadataTest.class.getResourceAsStream(metaPath),
        xmlCodedRep)
//        .map(String::new).map(Util::printOut)
        .isPresent());
    assertTrue(extractor.extractBinary(new ByteArrayInputStream(annotatedCMMN),
        MetadataTest.class.getResourceAsStream(cmmnMetaPath),
        xmlCodedRep)
//        .map(String::new).map(Util::printOut)
        .isPresent());
  }

  @Test
  void testToJson() {
    assertTrue(extractor.extractBinary(new ByteArrayInputStream(annotatedDMN),
        MetadataTest.class.getResourceAsStream(metaPath),
        jsonCodedRep)
//        .map(String::new).map(Util::printOut)
        .isPresent());
    assertTrue(extractor.extractBinary(new ByteArrayInputStream(annotatedCMMN),
        MetadataTest.class.getResourceAsStream(cmmnMetaPath),
        jsonCodedRep)
//        .map(String::new).map(Util::printOut)
        .isPresent());
  }

  @Test
  void testGetEnterpriseAssetIdForAsset() {
    Optional<URI> enterpriseAsset = mapper
        .getCurrentAssetSeriesUri(UUID.fromString("14321e7c-cb9a-427f-abf5-1420bf26e03c"));
    assertNotNull(enterpriseAsset);
    assertEquals(
        "https://clinicalknowledgemanagement.mayo.edu/assets/14321e7c-cb9a-427f-abf5-1420bf26e03c",
        enterpriseAsset.map(Objects::toString).orElse(""));
  }

  @Test
  void testGetEnterpriseAssetIdForAsset_empty() {
    Optional<URI> enterpriseAsset = mapper
        .getCurrentAssetSeriesUri(UUID.fromString("14421eac-cb9a-427f-abf5-1420bf26e03c"));
    assertNotNull(enterpriseAsset);
    assertFalse(enterpriseAsset.isPresent());
    assertEquals(Optional.empty(), enterpriseAsset);
  }

  @Test
  void testGetEnterpriseAssetVersionIdForAsset() {
    Optional<ResourceIdentifier> enterpriseAssetVersion = Optional.empty();
    try {
      enterpriseAssetVersion = mapper.resolveAssetToCurrentAssetId(
          UUID.fromString("14321e7c-cb9a-427f-abf5-1420bf26e03c"),
          "1.0.1", false);
    } catch (NotLatestAssetVersionException e) {
      fail();
    }
    assertEquals(
        "https://clinicalknowledgemanagement.mayo.edu/assets/14321e7c-cb9a-427f-abf5-1420bf26e03c/versions/1.0.1",
        enterpriseAssetVersion.map(ResourceIdentifier::getVersionId).map(Objects::toString)
            .orElse(""));
  }

  @Test
  void testGetEnterpriseAssetVersionIdForAsset_badId() {
    try {
      Optional<ResourceIdentifier> uri = mapper.resolveAssetToCurrentAssetId(
          UUID.fromString("14ba1e7c-cb9a-427f-abf5-1420bf26e03c"),
          "1.0.1", false);
      assertTrue(uri.isEmpty());
    } catch (NotLatestAssetVersionException e) {
      fail(e.getMessage());
    }
  }

  @Test
  void testGetArtifactVersion() {
    Optional<String> artifactVersion = mapper
        .getLatestCarrierVersionTag(UUID.fromString("14321e7c-cb9a-427f-abf5-1420bf26e03c"));
    assertNotNull(artifactVersion);
    assertEquals("1.8.5", artifactVersion.orElse(""));
  }


  @Test
  void testGetArtifactVersionWithTimestamp() {
    Optional<String> artifactVersion = mapper
        .getLatestCarrierTimestampedVersionTag(
            UUID.fromString("14321e7c-cb9a-427f-abf5-1420bf26e03c"));
    assertNotNull(artifactVersion);
    assertTrue(artifactVersion.orElse("").matches("\\d+\\.\\d+\\.\\d-\\d+"));
  }

  @Test
  void testGetMimeType() {
    String mimetype = mapper
        .getMimetype(UUID.fromString("bd0014e6-afbe-4006-b182-baa973f2929a"));
    assertNotNull(mimetype);
    assertEquals("application/vnd.triso-dmn+json", mimetype);

    mimetype = mapper.getMimetype(UUID.fromString("14321e7c-cb9a-427f-abf5-1420bf26e03c"));
    assertNotNull(mimetype);
    assertFalse(mimetype.isEmpty());
    assertEquals("application/vnd.triso-cmmn+json", mimetype);
  }

  @Test
  void testGetEnterpriseAssetVersionIdForAsset_badVersion() {
    NotLatestAssetVersionException nlve = assertThrows(
        NotLatestAssetVersionException.class,
        () -> mapper.resolveAssetToCurrentAssetId(
            UUID.fromString("14321e7c-cb9a-427f-abf5-1420bf26e03c"),
            "1.1.0", false));
    // internalId provided with exception
    assertEquals(
        "http://www.trisotech.com/definitions/_16086bb8-c1fc-49b0-800b-c9b995dc5ed5"
        , nlve.getModelUri());
  }

  @Test
  void testResolveInternalArtifactID() {
    try {
      String artifactId = mapper
          .resolveInternalArtifactID(UUID.fromString("3c66cf3a-93c4-4e09-b1aa-14088c76aded"),
              "1.1.1", false);
      assertEquals("http://www.trisotech.com/definitions/_5682fa26-b064-43c8-9475-1e4281e74068",
          artifactId);
    } catch (NotLatestAssetVersionException | edu.mayo.kmdp.kdcaci.knew.trisotech.exception.NotFoundException e) {
      fail(
          "Should have artifact for specified asset: 3c66cf3a-93c4-4e09-b1aa-14088c76aded with version 1.1.1");
      e.printStackTrace();
    }
  }

  @Test
  void testResolveInternalArtifactID_Published_NotLatestVersionException() {
    NotLatestAssetVersionException ave = assertThrows(
        NotLatestAssetVersionException.class,
        () -> mapper
            .resolveInternalArtifactID(UUID.fromString("3c66cf3a-93c4-4e09-b1aa-14088c76aded"),
                "2.0.0",
                false));
    assertEquals("http://www.trisotech.com/definitions/_5682fa26-b064-43c8-9475-1e4281e74068",
        ave.getModelUri());
  }

  @Test
  void testResolveInternalArtifactID_Published_NotFound() {
    UUID assetId = UUID.fromString("abcdef3a-93c4-4e09-b1aa-14088c76adee");
    NotFoundException nfe = assertThrows(NotFoundException.class,
        () -> mapper
            .resolveInternalArtifactID(assetId, "1.0.0-SNAPSHOT", false));
    assertEquals(assetId, newVersionId(nfe.getInstance()).getUuid());
  }

  @Test
  void testResolveInternalArtifactID_Any() {
    try {
      String artifactId = mapper
          .resolveInternalArtifactID(UUID.fromString("3c66cf3a-93c4-4e09-b1aa-14088c76aded"),
              "1.1.1", true);
      assertEquals("http://www.trisotech.com/definitions/_5682fa26-b064-43c8-9475-1e4281e74068",
          artifactId);
    } catch (NotLatestAssetVersionException | NotFoundException e) {
      fail(
          "Should have artifact for specified asset: 3c66cf3a-93c4-4e09-b1aa-14088c76aded and version 1.1.1");
      e.printStackTrace();
    }
  }

  @Test
  void testResolveInternalArtifactID_Any_NotLatestVersionException() {
    NotLatestAssetVersionException ave = assertThrows(
        NotLatestAssetVersionException.class,
        () -> mapper
            .resolveInternalArtifactID(UUID.fromString("3c66cf3a-93c4-4e09-b1aa-14088c76aded"),
                "2.0.0",
                true));
    assertEquals("http://www.trisotech.com/definitions/_5682fa26-b064-43c8-9475-1e4281e74068",
        ave.getModelUri());
  }

  @Test
  void testResolveInternalArtifactID_Any_NotFound() {
    UUID assetId = UUID.fromString("abcdef3a-93c4-4e09-b1aa-14088c76adee");
    NotFoundException nfe = assertThrows(NotFoundException.class,
        () -> mapper
            .resolveInternalArtifactID(assetId, "1.0.0-SNAPSHOT",
                true));
    assertEquals(assetId, newVersionId(nfe.getInstance()).getUuid());
  }

  @Test
  void testResolveEnterpriseAssetID_IllegalStateException() {
    IllegalStateException ave = assertThrows(
        IllegalStateException.class,
        () -> mapper.resolveEnterpriseAssetID("3c66cf3a-93c4-4e09-b1aa-14088c76aded"));
    assertTrue(ave.getMessage().contains("3c66cf3a-93c4-4e09-b1aa-14088c76aded"));
  }

  @Test
  void testResolveEnterpriseAssetID() {
    ResourceIdentifier assetID = mapper
        .resolveEnterpriseAssetID(
            "http://www.trisotech.com/definitions/_5682fa26-b064-43c8-9475-1e4281e74068")
        .orElseGet(Assertions::fail);
    assertEquals(
        "https://clinicalknowledgemanagement.mayo.edu/assets/3c66cf3a-93c4-4e09-b1aa-14088c76aded/versions/1.1.1",
        assetID.getVersionId().toString());
  }

  @Test
  void testGetModelId_AssetUUID() {
    Optional<String> modelId = mapper
        .getCurrentModelId(UUID.fromString("3c66cf3a-93c4-4e09-b1aa-14088c76aded"), false);
    assertNotNull(modelId);
    assertTrue(modelId.isPresent());
    assertEquals("http://www.trisotech.com/definitions/_5682fa26-b064-43c8-9475-1e4281e74068",
        modelId.get());
  }


  @Test
  void testGetModelId_internalId() {
    Optional<String> fileid = mapper
        .resolveModelId(
            "http://www.trisotech.com/definitions/_5682fa26-b064-43c8-9475-1e4281e74068");
    assertNotNull(fileid);
    assertTrue(fileid.isPresent());
    assertEquals("http://www.trisotech.com/definitions/_5682fa26-b064-43c8-9475-1e4281e74068",
        fileid.get());
  }

  @Test
  void testGetModelId_internalId_tagOnly() {
    Optional<String> fileid = mapper
        .resolveModelId("5682fa26-b064-43c8-9475-1e4281e74068");
    assertNotNull(fileid);
    assertTrue(fileid.isPresent());
    assertEquals("http://www.trisotech.com/definitions/_5682fa26-b064-43c8-9475-1e4281e74068",
        fileid.get());
  }

  @Test
  void testGetModel_internalId_empty() {
    Optional<String> fileid = mapper.resolveModelId("abcdef3a-93c4-4e09-b1aa-14088c76adee");
    assertNotNull(fileid);
    assertFalse(fileid.isPresent());
    assertEquals(Optional.empty(), fileid);
  }

  @Test
  void testGetModelId_internalId_URIString_empty() {
    Optional<String> fileid = mapper
        .resolveModelId(
            "http://www.trisotech.com/definitions/_5682fa26-b064-43c8-9475-1e4281e7abcd");
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
        expectedFileId + "/versions/" + applyTimestampToVersion(versionTag, modelDate.getTime());
    String expectedVersionTag = applyTimestampToVersion(versionTag, modelDate.getTime());

    // test w/o a version
    ResourceIdentifier fileId = names.rewriteInternalId(internalId, null);
    assertNotNull(fileId);
    assertEquals(id, fileId.getTag());
    assertEquals(expectedFileId, fileId.getResourceId().toString());

    // test w/version
    fileId = names.rewriteInternalId(internalId, versionTag, updated);
    assertNotNull(fileId);
    assertEquals(id, fileId.getTag());
    assertEquals(expectedVersionTag, fileId.getVersionTag());
    assertEquals(expectedFileId, fileId.getResourceId().toString());
    assertEquals(expectedFileIdAndVersion, fileId.getVersionId().toString());
  }


}
