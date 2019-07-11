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
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.w3c.dom.Document;


/**
 * Tests for TrisotechWrapper.
 *
 * Named as an integration test even though not starting SpringBoot, but
 * because communicating with the Trisotech server.
 */
class TrisotechWrapperTestInt {

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
  final void testGetModelVersionsDMN() {
    List<TrisotechFileInfo> fileVersions = TrisotechWrapper.getModelVersions(WEAVER_TEST_1_ID); // 7/9/2019 -- should be at least 15
    assertNotNull(fileVersions);
    assertTrue(fileVersions.size() > 10);

    TrisotechFileInfo file = fileVersions.stream()
        .filter(f -> f.getVersion() != null)
        .filter(f -> f.getVersion().equals("1.6"))
        .findAny()
        .orElse(null);

    assertTrue(file.getVersion().equals("1.6"));
  }

  @Test
  final void testGetModelVersionsWithRepositoryDMN() {
    List<TrisotechFileInfo> fileVersions = TrisotechWrapper.getModelVersions(MEA_TEST, WEAVER_TEST_1_ID);
    assertNotNull(fileVersions);
    assertTrue(fileVersions.size() > 10); // 7/9/2019 -- should be at least 15

    TrisotechFileInfo file = fileVersions.stream()
        .filter(f -> f.getVersion() != null)
        .filter(f -> f.getVersion().equals("1.6"))
        .findAny()
        .orElse(null);

    assertTrue(file.getVersion().equals("1.6"));
  }

  @Test
  final void testGetModelVersionsCMMN() {
    List<TrisotechFileInfo> fileVersions = TrisotechWrapper.getModelVersions(WEAVE_TEST_1_ID);
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
    List<TrisotechFileInfo> fileVersions = TrisotechWrapper.getModelVersions(MEA_TEST, WEAVE_TEST_1_ID);
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
    String url = TrisotechWrapper.getLatestVersion(WEAVER_TEST_1_ID);
    assertNotNull(url);
    assertTrue(url.contains(WEAVER_TEST_1_ID));
    assertFalse(url.contains("version"));
  }
  // latest version 7/9/2019:
  //             "file": {
  //                "id": "123720a6-9758-45a3-8c5c-5fffab12c494",
  //                "sku": "123720a6-9758-45a3-8c5c-5fffab12c494",
  //                "name": "Weaver Test 1",
  //                "path": "/",
  //                "mimetype": "application/vnd.triso-dmn+json",
  //                "updated": "2019-07-09T19:16:42Z",
  //                "updater": "Cheryl Orcutt",
  //                "url": "https://mc.trisotech.com/publicapi/repositoryfilecontent?repository=d4aca01b-d446-4bc8-a6f0-85d84f4c1aaf&sku=123720a6-9758-45a3-8c5c-5fffab12c494",
  //                "version": "1.7",
  //                "state": "Published"
  //            }

  @Test
  final void testGetLatestVersionTrisotechFileInfoDMN() {
    TrisotechFileInfo trisotechFileInfo = TrisotechWrapper.getModelInfo(WEAVER_TEST_1_ID);
    String url = TrisotechWrapper.getLatestVersion(trisotechFileInfo);
    assertNotNull(url);
    assertTrue(url.contains(WEAVER_TEST_1_ID));
    assertFalse(url.contains("version"));
  }

  @Test
  final void testGetLatestVersionTagDMN() {
    String version = TrisotechWrapper.getLatestVersionTag(WEAVER_TEST_1_ID);
    assertNotNull(version);
    assertEquals("1.7", version);
  }

  @Test
  final void testGetLatestVersionArtifactIdCMMN() {
    String url = TrisotechWrapper.getLatestVersion(WEAVE_TEST_1_ID);
    assertNotNull(url);
    assertTrue(url.contains(WEAVE_TEST_1_ID));
    assertFalse(url.contains("version"));
  }

  @Test
  final void testGetLatestVersionTrisotechFileInfoCMMN() {
    TrisotechFileInfo trisotechFileInfo = TrisotechWrapper.getModelInfo(WEAVE_TEST_1_ID);
    String url = TrisotechWrapper.getLatestVersion(trisotechFileInfo);
    assertNotNull(url);
    assertTrue(url.contains(WEAVE_TEST_1_ID));
    assertFalse(url.contains("version"));
  }

  @Test
  final void testGetLatestVersionTagCMMN() {
    String version = TrisotechWrapper.getLatestVersionTag(WEAVE_TEST_1_ID);
    assertNotNull(version);
    assertEquals("2.1", version);
  }

  @Test
  final void testGetLatestVersionTagCMMN_Null() {
    // while a file may have multiple versions, no version tag is given to a file until it is published
    String version = TrisotechWrapper.getLatestVersionTag(WEAVE_TEST_2_ID);
    assertNull(version);
  }

  @Test
  final void testGetPublishedModelByIdDMN() {
    Optional<Document> dox = TrisotechWrapper.getPublishedModelById(WEAVER_TEST_1_ID);
    assertNotEquals(Optional.empty(), dox);
    assertNotNull(dox.get());
  }

  @Test
  final void testGetPublishedModelByIdDMN_Null() {
    // getModelById returns empty if model is not published
    Optional<Document> dox = TrisotechWrapper.getPublishedModelById(WEAVER_TEST_2_ID);
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
    List<TrisotechFileInfo> trisotechFileInfos = TrisotechWrapper.getDmnModels();
    TrisotechFileInfo trisotechFileInfo = trisotechFileInfos.stream().filter((f) -> f.getId().equals(WEAVER_TEST_1_ID)).findAny().get();
    Optional<Document> dox = TrisotechWrapper.getPublishedModelById(WEAVER_TEST_1_ID, trisotechFileInfo);
    assertNotEquals(Optional.empty(), dox);
    assertNotNull(dox.get());
  }

  @Test
  final void testGetDmnModels() {
    List<TrisotechFileInfo> dmnModels = TrisotechWrapper.getDmnModels();
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
    List<TrisotechFileInfo> trisotechFileInfos = TrisotechWrapper.getCmmnModels();
    TrisotechFileInfo trisotechFileInfo = trisotechFileInfos.stream().filter((f) -> f.getId().equals(WEAVE_TEST_1_ID)).findAny().get();
    Optional<Document> dox = TrisotechWrapper.getPublishedModelById(WEAVE_TEST_1_ID, trisotechFileInfo);
    assertNotEquals(Optional.empty(), dox);
    assertNotNull(dox.get());
  }

  @Test
  final void testGetCmmnModels() {
    List<TrisotechFileInfo> cmmnModels = TrisotechWrapper.getCmmnModels();
    assertNotNull(cmmnModels);
    assertEquals(2, cmmnModels.size());
  }

  @Test
  final void testGetPublishedCmmnModels() {
    Map<String, TrisotechFileInfo> publishedModels = TrisotechWrapper.getPublishedCMMNModels();
    assertNotNull(publishedModels);
    assertEquals(1, publishedModels.size());
  }

  @Test
  final void testGetPublishedDmnModels() {
    Map<String, TrisotechFileInfo> publishedModels = TrisotechWrapper.getPublishedDmnModels();
    assertNotNull(publishedModels);
    assertEquals(1, publishedModels.size());
  }

  @Test
  final void testGetModelInfoDMN() {
    TrisotechFileInfo fileInfo = TrisotechWrapper.getModelInfo(WEAVER_TEST_1_ID);
    assertNotNull(fileInfo);
    assertTrue("Weaver Test 1".equals(fileInfo.getName()));
    assertTrue(WEAVER_TEST_1_ID.equals(fileInfo.getId()));
    assertTrue(fileInfo.getUrl().contains("&mimetype="));
    assertTrue(fileInfo.getMimetype().contains("dmn"));
  }

  @Test
  final void testGetModelInfoCMMN() {
    TrisotechFileInfo fileInfo = TrisotechWrapper.getModelInfo(WEAVE_TEST_1_ID);
    assertNotNull(fileInfo);
    assertTrue("Weave Test 1".equals(fileInfo.getName()));
    assertTrue(WEAVE_TEST_1_ID.equals(fileInfo.getId()));
    assertTrue(fileInfo.getUrl().contains("&mimetype="));
    assertTrue(fileInfo.getMimetype().contains("cmmn"));
  }

  @Test
  final void testDownloadXmlModel() {

    String repositoryFileUrl = " https://mc.trisotech.com/publicapi/repositoryfilecontent?repository="+ MEA_TEST+"&mimetype=application%2Fdmn-1-2%2Bxml&path=/&sku="+WEAVER_TEST_1_ID;
    Document dox = TrisotechWrapper.downloadXmlModel(repositoryFileUrl);
    assertNotNull(dox);
  }

}
