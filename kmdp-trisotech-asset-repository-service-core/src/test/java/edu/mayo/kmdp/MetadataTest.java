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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.JSON;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;

import edu.mayo.kmdp.kdcaci.knew.trisotech.NamespaceManager;
import edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.MetadataIntrospector;
import edu.mayo.kmdp.kdcaci.knew.trisotech.components.redactors.Redactor;
import edu.mayo.kmdp.kdcaci.knew.trisotech.components.weavers.Weaver;
import edu.mayo.kmdp.util.XMLUtil;
import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
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

@SpringBootTest
@ContextConfiguration(classes = {TrisotechAssetRepositoryTestConfig.class})
class MetadataTest {

  // FYI: IDE may complain about
  // the following two not being able to be autowired, but the code works.
  @Autowired
  private MetadataIntrospector extractor;

  @Autowired
  NamespaceManager names;

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
    ResourceIdentifier fileId = names.rewriteInternalId(internalId, null, "Test model");
    assertNotNull(fileId);
    assertEquals(id, fileId.getTag());
    assertEquals(expectedFileId, fileId.getResourceId().toString());

    // test w/version
    fileId = names.rewriteInternalId(internalId, versionTag, null, updated);
    assertNotNull(fileId);
    assertEquals(id, fileId.getTag());
    assertEquals(expectedVersionTag, fileId.getVersionTag());
    assertEquals(expectedFileId, fileId.getResourceId().toString());
    assertEquals(expectedFileIdAndVersion, fileId.getVersionId().toString());
  }


}
