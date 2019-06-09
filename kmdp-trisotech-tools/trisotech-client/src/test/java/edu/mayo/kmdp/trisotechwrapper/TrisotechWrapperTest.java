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
 * These tests are really integration tests as they will communicate with the server.
 * Better place for these?
 * Unit tests also desired?
 */
class TrisotechWrapperTest {

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

  @Disabled("testGetModelVersions not yet implemented")
  @Test
  final void testGetModelVersions() {
    fail("Not yet implemented"); // TODO
  }

  @Test
  final void testGetDmnModelById() {
    Optional<Document> dox = TrisotechWrapper.getDmnModelById("123720a6-9758-45a3-8c5c-5fffab12c494");
    assertNotEquals(Optional.empty(), dox);
    assertNotNull(dox.get());
  }

  @Test
  final void testGetDmnModels() {
    List<TrisotechFileInfo> dmnModels = TrisotechWrapper.getDmnModels();
    assertNotNull(dmnModels);
  }

  @Disabled("testGetCMMNModel not yet implemented")
  @Test
  final void testGetCMMNModel() {
    fail("Not yet implemented"); // TODO
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
  final void testGetModelInfo() {
    TrisotechFileInfo fileInfo = TrisotechWrapper.getModelInfo("123720a6-9758-45a3-8c5c-5fffab12c494");
    assertNotNull(fileInfo);
  }

  @Disabled("testGetLatestVersionString not yet implemented")
  @Test
  final void testGetLatestVersionString() {
    fail("Not yet implemented"); // TODO
  }

  @Disabled("testGetLatestVersionTag not yet implemented")
  @Test
  final void testGetLatestVersionTag() {
    fail("Not yet implemented"); // TODO
  }

  @Test
  final void testDownloadXmlModel() {

    String repositoryFileUrl = " https://mc.trisotech.com/publicapi/repositoryfilecontent?repository=d4aca01b-d446-4bc8-a6f0-85d84f4c1aaf&mimetype=application%2Fdmn-1-2%2Bxml&path=/&sku=123720a6-9758-45a3-8c5c-5fffab12c494";
    Document dox = TrisotechWrapper.downloadXmlModel(repositoryFileUrl);
    assertNotNull(dox);
  }

}
