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
package edu.mayo.kmdp.trisotechwrapper;

import static edu.mayo.kmdp.trisotechwrapper.TrisotechApiUrls.*;
import static org.junit.jupiter.api.Assertions.*;

import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Optional;

import org.omg.spec.api4kp._1_0.identifiers.VersionIdentifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.w3c.dom.Document;

/**
 * Tests for TrisotechWrapper.
 *
 * Named as an integration test even though not starting SpringBoot, but
 * because communicating with the Trisotech server.
 */
@SpringBootTest
@ContextConfiguration(classes = {TrisotechWrapperConfig.class})
class TrisotechWrapperIntTest {

  static final String TRISOTECH_PUBLICAPI_REPOSITORYFILECONTENT_REPOSITORY = "https://mc.trisotech.com/publicapi/repositoryfilecontent?repository=";

  // Test files
  // DMN Published
  static final String WEAVER_TEST_1_ID = "123720a6-9758-45a3-8c5c-5fffab12c494";
  // DMN published 'Draft'
  static final String WEAVER_TEST_2_ID = "ffa53262-4d36-4656-890b-4e48ed1cb9c3";
  // DMN unpublished
  static final String DMN_UNPUBLISHED = "1dd05040-0bc8-4f3f-85b2-a06026c2cdc6";
  // CMMN Published
  static final String WEAVE_TEST_1_ID = "93e58aa9-c258-46fd-909d-1cb096e19e64";
  // CMMN unpublished
  static final String WEAVE_TEST_2_ID = "84da9f52-44f5-46d1-ae3f-c5599f78ad1f";

  @BeforeAll
  static void setUpBeforeClass() throws Exception {
  }

  @AfterAll
  static void tearDownAfterClass() throws Exception {
  }

  @BeforeEach
  void setUp() throws Exception {
  }

  @AfterEach
  void tearDown() throws Exception {
  }

  @Test
  final void testGetModelByIdAndVersionDMN() {
    String expectedVersion = "1.4";
    String expectedVersion_2 = "0.2";
    Optional<Document> dox = TrisotechWrapper
        .getModelByIdAndVersion(WEAVER_TEST_1_ID, expectedVersion);
    assertNotEquals(Optional.empty(), dox);
    assertTrue(dox.isPresent());
    assertNotNull(dox.get());

    dox = TrisotechWrapper.getModelByIdAndVersion(WEAVER_TEST_2_ID, expectedVersion_2);
    assertNotEquals(Optional.empty(), dox);
    assertTrue(dox.isPresent());
    assertNotNull(dox.get());
  }

  @Test
  final void testGetModelByIdAndVersionDMNInvalidVersion() {
    String expectedVersion = "2.6";
    String expectedVersion_2 = "0.0";
    Optional<Document> dox = TrisotechWrapper
        .getModelByIdAndVersion(WEAVER_TEST_1_ID, expectedVersion);
    assertEquals(Optional.empty(), dox);

    dox = TrisotechWrapper.getModelByIdAndVersion(WEAVER_TEST_2_ID, expectedVersion_2);
    assertEquals(Optional.empty(), dox);

  }

  @Test
  final void testGetModelByIdAndVersionCMMN() {
    String expectedVersion = "2.0";
    String expectedVersion_2 = "1.0";
    Optional<Document> dox = TrisotechWrapper
        .getModelByIdAndVersion(WEAVE_TEST_1_ID, expectedVersion);
    assertNotEquals(Optional.empty(), dox);
    assertTrue(dox.isPresent());
    assertNotNull(dox.get());
    // what else to test here??
  }

  @Test
  final void testGetModelByIdAndVersionCMMNInvalidVersion() {
    String expectedVersion = "6.5";
    String expectedVersion_2 = "1.0";
    Optional<Document> dox = TrisotechWrapper
        .getModelByIdAndVersion(WEAVE_TEST_1_ID, expectedVersion);
    assertEquals(Optional.empty(), dox);
    assertFalse(dox.isPresent());

    dox = TrisotechWrapper.getModelByIdAndVersion(WEAVE_TEST_2_ID, expectedVersion_2);
    assertEquals(Optional.empty(), dox);
    assertFalse(dox.isPresent());

  }

  @Test
  final void testGetModelInfoByIdAndVersionDMN() {
    String expectedVersion = "1.2";
    String expectedVersion_2 = "0.1";
    TrisotechFileInfo fileInfo = TrisotechWrapper
        .getFileInfoByIdAndVersion(WEAVER_TEST_1_ID, expectedVersion);
    assertNotNull(fileInfo);
    assertTrue(expectedVersion.equals(fileInfo.getVersion()));
    assertTrue(WEAVER_TEST_1_ID.equals(fileInfo.getId()));

    fileInfo = TrisotechWrapper.getFileInfoByIdAndVersion(WEAVER_TEST_2_ID, expectedVersion_2);
    assertTrue(expectedVersion_2.equals(fileInfo.getVersion()));
    assertTrue(WEAVER_TEST_2_ID.equals(fileInfo.getId()));
  }

  @Test
  final void testGetModelInfoByIdAndVersionCMMN() {
    String expectedVersion = "2.0";
    String expectedVersion_2 = "1.0";
    TrisotechFileInfo fileInfo = TrisotechWrapper
        .getFileInfoByIdAndVersion(WEAVE_TEST_1_ID, expectedVersion);
    assertNotNull(fileInfo);
    assertTrue(expectedVersion.equals(fileInfo.getVersion()));
    assertTrue(WEAVE_TEST_1_ID.equals(fileInfo.getId()));

    fileInfo = TrisotechWrapper.getFileInfoByIdAndVersion(WEAVE_TEST_2_ID, expectedVersion_2);
    assertNull(fileInfo);

  }

  // TODO: add test w/bad repository name

  @Test
  final void testGetModelVersionsDMN() {
    List<TrisotechFileInfo> fileVersions = TrisotechWrapper
        .getModelVersions(WEAVER_TEST_1_ID, DMN_XML_MIMETYPE); // 7/9/2019 -- should be at least 15
    assertNotNull(fileVersions);
    assertTrue(fileVersions.size() > 10);

    TrisotechFileInfo file = fileVersions.stream()
        .filter(f -> f.getVersion() != null)
        .filter(f -> f.getVersion().equals("1.6"))
        .findAny()
        .orElse(null);

    assertTrue(file.getVersion().equals("1.6"));

    // expect same results with repository provided
    fileVersions = TrisotechWrapper.getModelVersions(MEA_TEST, WEAVER_TEST_1_ID,
        DMN_XML_MIMETYPE); // 7/9/2019 -- should be at least 15
    assertNotNull(fileVersions);
    assertTrue(fileVersions.size() > 10);

    file = fileVersions.stream()
        .filter(f -> f.getVersion() != null)
        .filter(f -> f.getVersion().equals("1.6"))
        .findAny()
        .orElse(null);

    assertTrue(file.getVersion().equals("1.6"));
  }

  // TODO: add test w/bad repository name

  @Test
  final void testGetModelVersionsWithRepositoryDMN() {
    List<TrisotechFileInfo> fileVersions = TrisotechWrapper
        .getModelVersions(WEAVER_TEST_1_ID, DMN_XML_MIMETYPE);
    assertNotNull(fileVersions);
    assertTrue(fileVersions.size() > 10); // 7/9/2019 -- should be at least 15

    TrisotechFileInfo file = fileVersions.stream()
        .filter(f -> f.getVersion() != null)
        .filter(f -> f.getVersion().equals("1.6"))
        .findAny()
        .orElse(null);

    assertTrue(file.getVersion().equals("1.6"));

    // expect same results with repository provided
    fileVersions = TrisotechWrapper.getModelVersions(MEA_TEST, WEAVER_TEST_1_ID, DMN_XML_MIMETYPE);
    assertNotNull(fileVersions);
    assertTrue(fileVersions.size() > 10); // 7/9/2019 -- should be at least 15

    file = fileVersions.stream()
        .filter(f -> f.getVersion() != null)
        .filter(f -> f.getVersion().equals("1.6"))
        .findAny()
        .orElse(null);

    assertTrue(file.getVersion().equals("1.6"));
  }


  @Test
  final void testGetModelVersionsCMMN() {
    List<TrisotechFileInfo> fileVersions = TrisotechWrapper
        .getModelVersions(WEAVE_TEST_1_ID, CMMN_XML_MIMETYPE);
    assertNotNull(fileVersions);
    assertTrue(fileVersions.size() > 5);

    TrisotechFileInfo file = fileVersions.stream()
        .filter(f -> f.getVersion() != null)
        .filter(f -> f.getVersion().equals("2.0"))
        .findAny()
        .orElse(null);

    assertTrue(file.getVersion().equals("2.0"));
  }

  @Test
  final void testGetModelVersionsWithRepositoryCMMN() {
    List<TrisotechFileInfo> fileVersions = TrisotechWrapper
        .getModelVersions(WEAVE_TEST_1_ID, CMMN_XML_MIMETYPE);
    assertNotNull(fileVersions);
    assertTrue(fileVersions.size() > 5);

    TrisotechFileInfo file = fileVersions.stream()
        .filter(f -> f.getVersion() != null)
        .filter(f -> f.getVersion().equals("2.0"))
        .findAny()
        .orElse(null);

    assertTrue(file.getVersion().equals("2.0"));
  }

  @Test
  final void testGetLatestVersionArtifactIdDMN() {
    String expectedVersion = "1.8";
    XMLGregorianCalendar expectedUpdated = null;
    try {
      expectedUpdated = DatatypeFactory.newInstance()
          .newXMLGregorianCalendar("2019-08-06T22:16:54Z");
    } catch (DatatypeConfigurationException e) {
      e.printStackTrace();
    }

    VersionIdentifier versionIdentifier = TrisotechWrapper.getLatestVersion(WEAVER_TEST_1_ID);
    assertNotNull(versionIdentifier);
    assertEquals(WEAVER_TEST_1_ID, versionIdentifier.getTag());
    assertEquals(expectedVersion, versionIdentifier.getVersion());
    assertEquals(expectedUpdated.toString(), versionIdentifier.getEstablishedOn().toString());
  }
  // latest version 7/9/2019:
  //             "file": {
  //                "id": "123720a6-9758-45a3-8c5c-5fffab12c494",
  //                "sku": "123720a6-9758-45a3-8c5c-5fffab12c494",
  //                "name": "Weaver Test 1",
  //                "path": "/",
  //                "mimetype": "application/vnd.triso-dmn+json",
  //                "updated": "2019-08-06T20:21:53Z",
  //                "updater": "Cheryl Orcutt",
  //                "url": "https://mc.trisotech.com/publicapi/repositoryfilecontent?repository=d4aca01b-d446-4bc8-a6f0-85d84f4c1aaf&sku=123720a6-9758-45a3-8c5c-5fffab12c494",
  //                "version": "1.8",
  //                "state": "Published"
  //            }

  @Test
  final void testGetLatestVersionTrisotechFileInfoDMN() {
    String expectedVersion = "1.8";
    TrisotechFileInfo trisotechFileInfo = TrisotechWrapper.getFileInfo(WEAVER_TEST_1_ID);
    VersionIdentifier versionIdentifier = TrisotechWrapper.getLatestVersion(trisotechFileInfo);
    assertNotNull(versionIdentifier);
    assertEquals(WEAVER_TEST_1_ID, versionIdentifier.getTag());
    assertEquals(expectedVersion, versionIdentifier.getVersion());
  }

  @Test
  final void testGetLatestVersionArtifactIdCMMN() {
    String expectedVersion = "2.2.1";
    VersionIdentifier versionIdentifier = TrisotechWrapper.getLatestVersion(WEAVE_TEST_1_ID);
    assertNotNull(versionIdentifier);
    assertEquals(WEAVE_TEST_1_ID, versionIdentifier.getTag());
    assertEquals(expectedVersion, versionIdentifier.getVersion());
  }

  @Test
  final void testGetLatestVersionTrisotechFileInfoCMMN() {
    String expectedVersion = "2.2.1";
    TrisotechFileInfo trisotechFileInfo = TrisotechWrapper.getFileInfo(WEAVE_TEST_1_ID);
    VersionIdentifier versionIdentifier = TrisotechWrapper.getLatestVersion(trisotechFileInfo);
    assertNotNull(versionIdentifier);
    assertEquals(WEAVE_TEST_1_ID, versionIdentifier.getTag());
    assertEquals(expectedVersion, versionIdentifier.getVersion());
  }


  @Test
  final void testGetLatestVersionCMMN_Null() {
    // while a file may have multiple versions, no version tag is given to a file until it is published
    VersionIdentifier version = TrisotechWrapper.getLatestVersion(WEAVE_TEST_2_ID);
    assertNull(version);
  }

  @Test
  final void testGetPublishedModelByIdDMN() {
    Optional<Document> dox = TrisotechWrapper.getPublishedModelById(WEAVER_TEST_1_ID);
    assertNotEquals(Optional.empty(), dox);
    assertNotNull(dox.get());
  }

  @Test
  final void testGetPublishedModelByIdDMN_Draft() {
    Optional<Document> dox = TrisotechWrapper.getPublishedModelById(WEAVER_TEST_2_ID);
    assertNotEquals(Optional.empty(), dox);
    assertNotNull(dox.get());
  }

  @Test
  final void testGetPublishedModelByIdDMN_Null() {
    // getPublishedModelById returns empty if model is not published
    Optional<Document> dox = TrisotechWrapper.getPublishedModelById(DMN_UNPUBLISHED);
    assertNotNull(dox);
    assertEquals(Optional.empty(), dox);
  }

  @Test
  final void testGetModelByIdDMN() {
    // getModelById returns empty if model is not published
    Optional<Document> dox = TrisotechWrapper.getModelById(WEAVER_TEST_2_ID);
    assertNotEquals(Optional.empty(), dox);
    assertNotNull(dox.get());
  }

  @Test
  final void testGetPublishedModelByIdWithFileInfoDMN() {
    List<TrisotechFileInfo> trisotechFileInfos = TrisotechWrapper.getDMNModelsFileInfo();
    TrisotechFileInfo trisotechFileInfo = trisotechFileInfos.stream()
        .filter((f) -> f.getId().equals(WEAVER_TEST_1_ID)).findAny().get();
    Optional<Document> dox = TrisotechWrapper
        .getPublishedModelById(WEAVER_TEST_1_ID, trisotechFileInfo);
    assertNotEquals(Optional.empty(), dox);
    assertNotNull(dox.get());
  }

  @Test
  final void testGetDmnModels() {
    List<TrisotechFileInfo> dmnModels = TrisotechWrapper.getDMNModelsFileInfo();
    assertNotNull(dmnModels);
  }

  @Test
  final void testGetPublishedModelByIdCMMN() {
    Optional<Document> dox = TrisotechWrapper.getPublishedModelById(WEAVE_TEST_1_ID);
    assertNotEquals(Optional.empty(), dox);
    assertNotNull(dox.get());
  }

  @Test
  final void testGetPublishedModelByIdCMMN_Empty() {
    Optional<Document> dox = TrisotechWrapper.getPublishedModelById(WEAVE_TEST_2_ID);
    assertEquals(Optional.empty(), dox);
  }


  @Test
  final void testGetModelByIdCMMN() {
    Optional<Document> dox = TrisotechWrapper.getModelById(WEAVE_TEST_2_ID);
    assertNotEquals(Optional.empty(), dox);
    assertNotNull(dox.get());
  }

  @Test
  final void testGetPublishedModelByIdWithFileInfoCMMN() {
    List<TrisotechFileInfo> trisotechFileInfos = TrisotechWrapper.getCMMNModelsFileInfo();
    TrisotechFileInfo trisotechFileInfo = trisotechFileInfos.stream()
        .filter((f) -> f.getId().equals(WEAVE_TEST_1_ID)).findAny().get();
    Optional<Document> dox = TrisotechWrapper
        .getPublishedModelById(WEAVE_TEST_1_ID, trisotechFileInfo);
    assertNotEquals(Optional.empty(), dox);
    assertNotNull(dox.get());
  }

  @Test
  final void testGetCmmnModels() {
    List<TrisotechFileInfo> cmmnModels = TrisotechWrapper.getCMMNModelsFileInfo();
    assertNotNull(cmmnModels);
    assertEquals(3, cmmnModels.size());
  }

  @Test
  final void testGetPublishedCmmnModels() {
    List<TrisotechFileInfo> publishedModels = TrisotechWrapper.getPublishedCMMNModelsFileInfo();
    assertNotNull(publishedModels);
    assertEquals(2, publishedModels.size());
  }

  @Test
  final void testGetPublishedDmnModels() {
    List<TrisotechFileInfo> publishedModels = TrisotechWrapper.getPublishedDMNModelsFileInfo();
    assertNotNull(publishedModels);
    assertEquals(4, publishedModels.size());
  }

  @Test
  final void testGetModelInfoDMN() {
    TrisotechFileInfo fileInfo = TrisotechWrapper.getFileInfo(WEAVER_TEST_1_ID);
    assertNotNull(fileInfo);
    assertTrue("Weaver Test 1".equals(fileInfo.getName()));
    assertTrue(WEAVER_TEST_1_ID.equals(fileInfo.getId()));
    assertTrue(fileInfo.getUrl().contains("&mimetype="));
    assertTrue(fileInfo.getMimetype().contains("dmn"));
  }

  @Test
  final void testGetModelInfoCMMN() {
    TrisotechFileInfo fileInfo = TrisotechWrapper.getFileInfo(WEAVE_TEST_1_ID);
    assertNotNull(fileInfo);
    assertTrue("Weave Test 1".equals(fileInfo.getName()));
    assertTrue(WEAVE_TEST_1_ID.equals(fileInfo.getId()));
    assertTrue(fileInfo.getUrl().contains("&mimetype="));
    assertTrue(fileInfo.getMimetype().contains("cmmn"));
  }

  @Test
  final void testDownloadXmlModelDMN() {

    String repositoryFileUrl = TRISOTECH_PUBLICAPI_REPOSITORYFILECONTENT_REPOSITORY + MEA_TEST_ID
        + "&mimetype=application%2Fdmn-1-2%2Bxml&path=/&sku=" + WEAVER_TEST_1_ID;
    Document dox = TrisotechWrapper.downloadXmlModel(repositoryFileUrl);
    assertNotNull(dox);
  }

  @Test
  final void testDownloadXmlModelCMMN() {

    String repositoryFileUrl = TRISOTECH_PUBLICAPI_REPOSITORYFILECONTENT_REPOSITORY + MEA_TEST_ID
        + "&mimetype=application%2Fcmmn-1-1%2Bxml&path=/&sku=" + WEAVE_TEST_1_ID;
    Document dox = TrisotechWrapper.downloadXmlModel(repositoryFileUrl);
    assertNotNull(dox);
  }

  @Test
  final void testDownloadXmlModelErrors() {
    // Any other exceptions to confirm?
    final String repositoryFileUrl =
        TRISOTECH_PUBLICAPI_REPOSITORYFILECONTENT_REPOSITORY + MEA_TEST_ID
            + "&mimetype=application%2Fdmn-1-2%2Bxml&path=/&sku=" + WEAVE_TEST_1_ID;
    RuntimeException re = assertThrows(RuntimeException.class,
        () -> TrisotechWrapper.downloadXmlModel(repositoryFileUrl));
    assertEquals("Failed : HTTP error code : 400", re.getMessage());

    final String repositoryFileUrl2 =
        TRISOTECH_PUBLICAPI_REPOSITORYFILECONTENT_REPOSITORY + MEA_TEST
            + "&mimetype=application%2Fdmn-1-2%2Bxml&path=/&sku=" + WEAVER_TEST_1_ID;
    re = assertThrows(RuntimeException.class,
        () -> TrisotechWrapper.downloadXmlModel(repositoryFileUrl2));
    assertEquals("Failed : HTTP error code : 404", re.getMessage());
  }


}
