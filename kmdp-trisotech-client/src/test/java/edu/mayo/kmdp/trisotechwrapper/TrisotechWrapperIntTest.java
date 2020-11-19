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

import static edu.mayo.kmdp.registry.Registry.MAYO_ARTIFACTS_BASE_URI;
import static edu.mayo.kmdp.trisotechwrapper.TrisotechApiUrls.CMMN_LOWER;
import static edu.mayo.kmdp.trisotechwrapper.TrisotechApiUrls.CMMN_XML_MIMETYPE;
import static edu.mayo.kmdp.trisotechwrapper.TrisotechApiUrls.DMN_LOWER;
import static edu.mayo.kmdp.trisotechwrapper.TrisotechApiUrls.DMN_XML_MIMETYPE;
import static edu.mayo.kmdp.util.DateTimeUtil.isSameDay;
import static edu.mayo.kmdp.util.DateTimeUtil.parseDate;
import static edu.mayo.kmdp.util.DateTimeUtil.parseDateTime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.util.DateTimeUtil;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.w3c.dom.Document;

/**
 * Tests for TrisotechWrapper.
 *
 * Named as an integration test even though not starting SpringBoot, but
 * because communicating with the Trisotech server.
 */
@SpringBootTest
@ActiveProfiles("dev")
@ContextConfiguration(classes = {TrisotechWrapperConfig.class})
@TestPropertySource(properties = {
    "edu.mayo.kmdp.trisotechwrapper.repositoryName=MEA-Test",
    "edu.mayo.kmdp.trisotechwrapper.repositoryId=d4aca01b-d446-4bc8-a6f0-85d84f4c1aaf"})
class TrisotechWrapperIntTest {

  @Autowired
  TrisotechWrapper client;

  private String repositoryBaseUrl;

  // id to MEA-Test repository
  private String testRepoName;

  private String testRepoId;

  // Test files
  // DMN Published
  private static final String WEAVER_TEST_1_ID = "123720a6-9758-45a3-8c5c-5fffab12c494";
  // DMN published 'Draft'
  private static final String WEAVER_TEST_2_ID = "ffa53262-4d36-4656-890b-4e48ed1cb9c3";
  // DMN unpublished
  private static final String DMN_UNPUBLISHED = "1dd05040-0bc8-4f3f-85b2-a06026c2cdc6";
  // CMMN Published
  private static final String WEAVE_TEST_1_ID = "93e58aa9-c258-46fd-909d-1cb096e19e64";
  // CMMN unpublished
  private static final String WEAVE_TEST_2_ID = "84da9f52-44f5-46d1-ae3f-c5599f78ad1f";

  @BeforeAll
  static void setUpBeforeClass() {
  }

  @AfterAll
  static void tearDownAfterClass() {
  }

  @BeforeEach
  void setUp() {
    repositoryBaseUrl = client.getConfig().getBaseURL() +  "/repositoryfilecontent?repository=";
    testRepoName = client.getConfig().getRepositoryName();
    testRepoId = client.getConfig().getRepositoryId();
  }

  @AfterEach
  void tearDown() {
  }

  @Test
  final void testGetModelByIdAndVersionDMN() {
    String expectedVersion = "1.4";
    String expectedVersion_2 = "0.2";
    Optional<Document> dox = client
        .getModelByIdAndVersion(WEAVER_TEST_1_ID, expectedVersion);
    assertNotEquals(Optional.empty(), dox);
    assertTrue(dox.isPresent());
    assertNotNull(dox.get());

    dox = client.getModelByIdAndVersion(WEAVER_TEST_2_ID, expectedVersion_2);
    assertNotEquals(Optional.empty(), dox);
    assertTrue(dox.isPresent());
    assertNotNull(dox.get());
  }

  @Test
  final void testGetModelByIdAndVersionDMNInvalidVersion() {
    String expectedVersion = "2.6";
    String expectedVersion_2 = "0.0";
    Optional<Document> dox = client
        .getModelByIdAndVersion(WEAVER_TEST_1_ID, expectedVersion);
    assertEquals(Optional.empty(), dox);

    dox = client.getModelByIdAndVersion(WEAVER_TEST_2_ID, expectedVersion_2);
    assertEquals(Optional.empty(), dox);

  }

  @Test
  final void testGetModelByIdAndVersionCMMN() {
    String expectedVersion = "2.0";
    String expectedVersion_2 = "1.0";
    Optional<Document> dox = client
        .getModelByIdAndVersion(WEAVE_TEST_1_ID, expectedVersion);
    assertNotEquals(Optional.empty(), dox);
    assertTrue(dox.isPresent());
    assertNotNull(dox.get());
    // what else to test here??
  }

  @Test
  final void testGetModelByIdAndVersionCMMNInvalidVersion() {
    String expectedVersion = "6.5";
    Optional<Document> dox = client
        .getModelByIdAndVersion(WEAVE_TEST_1_ID, expectedVersion);
    assertEquals(Optional.empty(), dox);
    assertFalse(dox.isPresent());

    String expectedVersion_2 = "1.0"; // not the latest, but existing nevertheless
    dox = client.getModelByIdAndVersion(WEAVE_TEST_2_ID, expectedVersion_2);
    assertTrue(dox.isPresent());
  }

  @Test
  final void testGetModelInfoByIdAndVersionDMN() {
    String expectedVersion = "1.2";
    String expectedVersion_2 = "0.1";
    TrisotechFileInfo fileInfo = client
        .getFileInfoByIdAndVersion(WEAVER_TEST_1_ID, expectedVersion)
        .orElse(null);
    assertNotNull(fileInfo);
    assertEquals(expectedVersion, fileInfo.getVersion());
    assertEquals(WEAVER_TEST_1_ID, fileInfo.getId());

    fileInfo = client.getFileInfoByIdAndVersion(WEAVER_TEST_2_ID, expectedVersion_2)
        .orElse(null);
    assertNotNull(fileInfo);
    assertEquals(expectedVersion_2, fileInfo.getVersion());
    assertEquals(WEAVER_TEST_2_ID, fileInfo.getId());
  }

  @Test
  final void testGetModelInfoByIdAndVersionCMMN() {
    String expectedVersion = "2.0";
    TrisotechFileInfo fileInfo = client
        .getFileInfoByIdAndVersion(WEAVE_TEST_1_ID, expectedVersion)
        .orElse(null);
    assertNotNull(fileInfo);
    assertEquals(expectedVersion, fileInfo.getVersion());
    assertEquals(WEAVE_TEST_1_ID, fileInfo.getId());

    String expectedVersion_2 = "1.0";
    fileInfo = client.getFileInfoByIdAndVersion(WEAVE_TEST_2_ID, expectedVersion_2)
        .orElse(null);
    assertNotNull(fileInfo);
  }

  // TODO: add test w/bad repository name CAO

  @Test
  final void testGetModelVersionsDMN() {
    List<TrisotechFileInfo> fileVersions = client
        .getModelVersions(WEAVER_TEST_1_ID, DMN_XML_MIMETYPE); // 7/9/2019 -- should be at least 15
    assertNotNull(fileVersions);
    assertTrue(fileVersions.size() > 10);

    TrisotechFileInfo file = fileVersions.stream()
        .filter(f -> f.getVersion() != null)
        .filter(f -> f.getVersion().equals("1.6"))
        .findAny()
        .orElse(null);
    assertNotNull(file);

    assertEquals("1.6", file.getVersion());

    // expect same results with repository provided
    fileVersions = client.getModelVersions(testRepoName, WEAVER_TEST_1_ID,
        DMN_XML_MIMETYPE); // 7/9/2019 -- should be at least 15
    assertNotNull(fileVersions);
    assertTrue(fileVersions.size() > 10);

    file = fileVersions.stream()
        .filter(f -> f.getVersion() != null)
        .filter(f -> f.getVersion().equals("1.6"))
        .findAny()
        .orElse(null);
    assertNotNull(file);

    assertEquals("1.6", file.getVersion());
  }

  @Test
  final void testGetLatestVersionInfoDMN() {
    TrisotechFileInfo latestFileInfo = client.getLatestModelFileInfo(WEAVER_TEST_1_ID)
        .orElseGet(Assertions::fail);
    List<TrisotechFileInfo> historyInfos = client.getModelVersions(WEAVER_TEST_1_ID,DMN_XML_MIMETYPE);
    assertFalse(historyInfos.isEmpty());

    String fileId = latestFileInfo.getId();
    String version = latestFileInfo.getVersion();
    Date updateDate = parseDateTime(latestFileInfo.getUpdated());

    assertTrue(historyInfos.stream().allMatch(hx -> fileId.equals(hx.getId())));
    List<TrisotechFileInfo> versionHistoryInfos = historyInfos.stream()
        .filter(hx -> version.equals(hx.getVersion()))
        .collect(Collectors.toList());
    assertFalse(versionHistoryInfos.isEmpty());

    versionHistoryInfos.sort(Comparator.comparing(i -> parseDateTime(i.getUpdated())));
    Collections.reverse(versionHistoryInfos);
    TrisotechFileInfo latestInHistory = versionHistoryInfos.get(0);

    assertTrue(updateDate.after(parseDateTime(latestInHistory.getUpdated())));
  }

  // TODO: add test w/bad repository name

  @Test
  final void testGetModelVersionsWithRepositoryDMN() {
    List<TrisotechFileInfo> fileVersions = client
        .getModelVersions(WEAVER_TEST_1_ID, DMN_XML_MIMETYPE);
    assertNotNull(fileVersions);
    assertTrue(fileVersions.size() > 10); // 7/9/2019 -- should be at least 15

    TrisotechFileInfo file = fileVersions.stream()
        .filter(f -> f.getVersion() != null)
        .filter(f -> f.getVersion().equals("1.6"))
        .findAny()
        .orElse(null);
    assertNotNull(file);

    assertEquals("1.6", file.getVersion());

    // expect same results with repository provided
    fileVersions = client.getModelVersions(testRepoName, WEAVER_TEST_1_ID, DMN_XML_MIMETYPE);
    assertNotNull(fileVersions);
    assertTrue(fileVersions.size() > 10); // 7/9/2019 -- should be at least 15

    file = fileVersions.stream()
        .filter(f -> f.getVersion() != null)
        .filter(f -> f.getVersion().equals("1.6"))
        .findAny()
        .orElse(null);
    assertNotNull(file);

    assertEquals("1.6", file.getVersion());
  }


  @Test
  final void testGetModelVersionsCMMN() {
    List<TrisotechFileInfo> fileVersions = client
        .getModelVersions(WEAVE_TEST_1_ID, CMMN_XML_MIMETYPE);
    assertNotNull(fileVersions);
    assertTrue(fileVersions.size() > 5);

    TrisotechFileInfo file = fileVersions.stream()
        .filter(f -> f.getVersion() != null)
        .filter(f -> f.getVersion().equals("2.0"))
        .findAny()
        .orElse(null);
    assertNotNull(file);

    assertEquals("2.0", file.getVersion());
  }

  @Test
  final void testGetModelVersionsWithRepositoryCMMN() {
    List<TrisotechFileInfo> fileVersions = client
        .getModelVersions(WEAVE_TEST_1_ID, CMMN_XML_MIMETYPE);
    assertNotNull(fileVersions);
    assertTrue(fileVersions.size() > 5);

    TrisotechFileInfo file = fileVersions.stream()
        .filter(f -> f.getVersion() != null)
        .filter(f -> f.getVersion().equals("2.0"))
        .findAny()
        .orElse(null);
    assertNotNull(file);

    assertEquals("2.0", file.getVersion());
  }

  @Test
  final void testGetLatestVersionArtifactIdDMN() {
    String expectedVersion = "2.0.1";
    Date expectedUpdated = parseDateTime("2020-11-18T17:00:00Z");
    String expectedVersionId = MAYO_ARTIFACTS_BASE_URI
        + "123720a6-9758-45a3-8c5c-5fffab12c494/versions/"
        + expectedVersion;

    ResourceIdentifier versionIdentifier =
        client.getLatestVersion(WEAVER_TEST_1_ID)
            .orElse(null);
    assertNotNull(versionIdentifier);
    assertEquals(WEAVER_TEST_1_ID, versionIdentifier.getTag());
    assertEquals(expectedVersion, versionIdentifier.getVersionTag());
    assertEquals(expectedVersionId, versionIdentifier.getVersionId().toString());
    assertTrue(isSameDay(expectedUpdated, versionIdentifier.getEstablishedOn()));
  }

  @Test
  final void testGetLatestVersionTrisotechFileInfoDMN() {
    String expectedVersion = "2.0.1";
    String updated = "2020-11-18";
    Date expectedUpdated = parseDate(updated);
    String expectedVersionId = MAYO_ARTIFACTS_BASE_URI
        + "123720a6-9758-45a3-8c5c-5fffab12c494/versions/"
        + expectedVersion;

    TrisotechFileInfo trisotechFileInfo = client.getFileInfo(WEAVER_TEST_1_ID)
        .orElse(null);
    assertNotNull(trisotechFileInfo);
    ResourceIdentifier versionIdentifier = client.getLatestVersion(trisotechFileInfo)
        .orElse(null);
    assertNotNull(versionIdentifier);
    assertEquals(WEAVER_TEST_1_ID, versionIdentifier.getTag());
    assertEquals(expectedVersion, versionIdentifier.getVersionTag());
    assertTrue(isSameDay(expectedUpdated, versionIdentifier.getEstablishedOn()));
    assertEquals(expectedVersionId, versionIdentifier.getVersionId().toString());
  }

  @Test
  final void testGetLatestVersionArtifactIdCMMN() {
    String expectedVersion = "3.0.1";
    ResourceIdentifier versionIdentifier = client.getLatestVersion(WEAVE_TEST_1_ID)
        .orElse(null);
    assertNotNull(versionIdentifier);
    assertEquals(WEAVE_TEST_1_ID, versionIdentifier.getTag());
    assertEquals(expectedVersion, versionIdentifier.getVersionTag());
  }

  @Test
  final void testGetLatestVersionTrisotechFileInfoCMMN() {
    String expectedVersion = "3.0.1";
    String updated = "2020-09-03T13:21:00Z";
    Date expectedUpdated = parseDateTime(updated);
    String expectedVersionId = MAYO_ARTIFACTS_BASE_URI
        + "93e58aa9-c258-46fd-909d-1cb096e19e64/versions/"
        + expectedVersion;

    TrisotechFileInfo trisotechFileInfo = client.getFileInfo(WEAVE_TEST_1_ID)
        .orElse(null);
    assertNotNull(trisotechFileInfo);
    ResourceIdentifier versionIdentifier = client.getLatestVersion(trisotechFileInfo)
        .orElse(null);
    assertNotNull(versionIdentifier);
    assertEquals(WEAVE_TEST_1_ID, versionIdentifier.getTag());
    assertEquals(expectedVersion, versionIdentifier.getVersionTag());
    assertTrue(DateTimeUtil.isSameDay(expectedUpdated,versionIdentifier.getEstablishedOn()));
    assertEquals(expectedVersionId, versionIdentifier.getVersionId().toString());
  }


  @Test
  final void testGetLatestVersionCMMN_Null() {
    // while a file may have multiple versions, no version tag is given to a file until it is published
    ResourceIdentifier version = client.getLatestVersion(WEAVE_TEST_2_ID)
        .orElse(null);
    assertNotNull(version);
  }

  @Test
  final void testGetPublishedModelByIdDMN() {
    Optional<Document> dox = client.getPublishedModelById(WEAVER_TEST_1_ID);
    assertTrue(dox.isPresent());
    assertNotNull(dox.get());
  }

  @Test
  final void testGetPublishedModelByIdDMN_Draft() {
    Optional<Document> dox = client.getPublishedModelById(WEAVER_TEST_2_ID);
    assertTrue(dox.isPresent());
    assertNotNull(dox.get());
  }

  @Test
  final void testGetPublishedModelByIdDMN_Null() {
    // getPublishedModelById returns empty if model is not published
    Optional<Document> dox = client.getPublishedModelById(DMN_UNPUBLISHED);
    assertFalse(dox.isPresent());
  }

  @Test
  final void testGetModelByIdDMN() {
    // getModelById returns empty if model is not published
    Optional<Document> dox = client.getModelById(WEAVER_TEST_2_ID);
    assertTrue(dox.isPresent());
    assertNotNull(dox.get());
  }

  @Test
  final void testGetPublishedModelByIdWithFileInfoDMN() {
    List<TrisotechFileInfo> trisotechFileInfos = client.getDMNModelsFileInfo();
    TrisotechFileInfo trisotechFileInfo = trisotechFileInfos.stream()
        .filter((f) -> f.getId().equals(WEAVER_TEST_1_ID)).findAny()
        .orElse(null);
    assertNotNull(trisotechFileInfo);
    Optional<Document> dox = client
        .getPublishedModel(trisotechFileInfo);
    assertTrue(dox.isPresent());
    assertNotNull(dox.get());
  }

  @Test
  final void testGetDmnModels() {
    List<TrisotechFileInfo> dmnModels = client.getDMNModelsFileInfo();
    assertNotNull(dmnModels);
  }

  @Test
  final void testGetPublishedModelByIdCMMN() {
    Optional<Document> dox = client.getPublishedModelById(WEAVE_TEST_1_ID);
    assertTrue(dox.isPresent());
    assertNotNull(dox.get());
  }

  @Test
  final void testGetPublishedModelByIdCMMN_NonEmpty() {
    Optional<Document> dox = client.getPublishedModelById(WEAVE_TEST_2_ID);
    assertTrue(dox.isPresent());
  }


  @Test
  final void testGetModelByIdCMMN() {
    Optional<Document> dox = client.getModelById(WEAVE_TEST_2_ID);
    assertTrue(dox.isPresent());
    assertNotNull(dox.get());
  }

  @Test
  final void testGetPublishedModelByIdWithFileInfoCMMN() {
    List<TrisotechFileInfo> trisotechFileInfos = client.getCMMNModelsFileInfo();
    TrisotechFileInfo trisotechFileInfo = trisotechFileInfos.stream()
        .filter((f) -> f.getId().equals(WEAVE_TEST_1_ID))
        .findAny()
        .orElse(null);
    assertNotNull(trisotechFileInfo);
    Optional<Document> dox = client
        .getPublishedModel(trisotechFileInfo);
    assertTrue(dox.isPresent());
    assertNotNull(dox.get());
  }

  @Test
  final void testGetCmmnModels() {
    List<TrisotechFileInfo> cmmnModels = client.getCMMNModelsFileInfo();
    assertNotNull(cmmnModels);
    assertTrue(cmmnModels.size() >= 3);
  }

  @Test
  final void testGetPublishedCmmnModels() {
    List<TrisotechFileInfo> publishedModels = client.getPublishedCMMNModelsFileInfo();
    assertNotNull(publishedModels);
    assertEquals(4, publishedModels.size());
  }

  @Test
  final void testGetPublishedDmnModels() {
    List<TrisotechFileInfo> publishedModels = client.getPublishedDMNModelsFileInfo();
    assertNotNull(publishedModels);
    assertTrue(publishedModels.size() >= 4);
  }

  @Test
  final void testGetModelInfoDMN() {
    TrisotechFileInfo fileInfo = client.getFileInfo(WEAVER_TEST_1_ID)
        .orElse(null);
    assertNotNull(fileInfo);
    assertEquals("Weaver Test 1", fileInfo.getName());
    assertEquals(WEAVER_TEST_1_ID, fileInfo.getId());
    assertTrue(fileInfo.getUrl().contains("&mimetype="));
    assertTrue(fileInfo.getMimetype().contains(DMN_LOWER));
  }

  @Test
  final void testGetModelInfoCMMN() {
    TrisotechFileInfo fileInfo = client.getFileInfo(WEAVE_TEST_1_ID)
        .orElse(null);
    assertNotNull(fileInfo);
    assertEquals("Weave Test 1", fileInfo.getName());
    assertEquals(WEAVE_TEST_1_ID, fileInfo.getId());
    assertTrue(fileInfo.getUrl().contains("&mimetype="));
    assertTrue(fileInfo.getMimetype().contains(CMMN_LOWER));
  }

  @Test
  final void testDownloadXmlModelDMN() {
    String repositoryFileUrl = repositoryBaseUrl + testRepoId
        + "&mimetype=application%2Fdmn-1-2%2Bxml&path=/&sku=" + WEAVER_TEST_1_ID;
    Optional<Document> dox = client.downloadXmlModel(repositoryFileUrl);
    assertTrue(dox.isPresent());
  }

  @Test
  final void testDownloadXmlModelCMMN() {

    String repositoryFileUrl = repositoryBaseUrl + testRepoId
        + "&mimetype=application%2Fcmmn-1-1%2Bxml&path=/&sku=" + WEAVE_TEST_1_ID;
    Optional<Document> dox = client.downloadXmlModel(repositoryFileUrl);
    assertTrue(dox.isPresent());
  }

  @Test
  final void testDownloadXmlModelErrors() {
    // Any other exceptions to confirm?
    final String repositoryFileUrl =
        repositoryBaseUrl + testRepoId
            + "&mimetype=application%2Fdmn-1-2%2Bxml&path=/&sku=" + WEAVE_TEST_1_ID;

    Optional<Document> dox = client.downloadXmlModel(repositoryFileUrl);
    assertFalse(dox.isPresent());

    final String repositoryFileUrl2 =
        repositoryBaseUrl + testRepoName
            + "&mimetype=application%2Fdmn-1-2%2Bxml&path=/&sku=" + WEAVER_TEST_1_ID;

    dox = client.downloadXmlModel(repositoryFileUrl2);
    assertFalse(dox.isPresent());
  }


}
