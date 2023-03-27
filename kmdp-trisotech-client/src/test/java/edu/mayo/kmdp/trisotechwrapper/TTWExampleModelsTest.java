package edu.mayo.kmdp.trisotechwrapper;

import static edu.mayo.kmdp.trisotechwrapper.config.TTWConfigParamsDef.API_ENDPOINT;
import static edu.mayo.kmdp.trisotechwrapper.config.TTWConfigParamsDef.PUBLISHED_ONLY_FLAG;
import static edu.mayo.kmdp.trisotechwrapper.config.TTWConfigParamsDef.REPOSITORY_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import edu.mayo.kmdp.trisotechwrapper.TTWExampleModelsTest.PublishedOnlyTestConfig;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechPlace;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.w3c.dom.Document;

/**
 * Tests for TrisotechWrapper.
 * <p>
 * Named as an integration test even though not starting SpringBoot, but because communicating with
 * the Trisotech server.
 */
@SpringBootTest
@ActiveProfiles("dev")
@ContextConfiguration(classes = {PublishedOnlyTestConfig.class})
@TestPropertySource(properties = {
    "edu.mayo.kmdp.trisotechwrapper.repositoryName=Trisotech Examples Working Space",
    "edu.mayo.kmdp.trisotechwrapper.repositoryId=4f5f5508-2137-4004-aef9-3ebef74f177d"})
class TTWExampleModelsTest {

  public static final String EXAMPLE_REPO = "4f5f5508-2137-4004-aef9-3ebef74f177d";


  // CMMN unpublished - Treat fracture from Examples
  private static final String CMMN_UNPUB_ID_1 = "http://www.trisotech.com/cmmn/definitions/_407918cc-68ab-492f-acee-b61afd188e77";

  private static final String DMN_PUB_ID_1 = "http://www.trisotech.com/definitions/_27188a19-403f-471c-8ac3-843c270f8065";


  @Autowired
  TTWrapper client;


  @BeforeEach
  void setUp() {
    var apiEndpoint = client.getConfig().tryGetTyped(API_ENDPOINT, URI.class);
    assumeTrue(apiEndpoint.isPresent());

    client.getConfig().get(REPOSITORY_ID)
        .orElseGet(Assertions::fail);
    assertTrue(client.getConfig().getTyped(PUBLISHED_ONLY_FLAG, Boolean.class));

    assumeFalse(client.listAccessiblePlaces().isEmpty());
  }


  @Test
  final void testGetDmnModels() {
    var dmnModels
        = client.listModels("dmn");
    assertNotNull(dmnModels);
  }


  @Test
  final void testGetPublishedModelByIdDMN_Null() {
    Optional<Document> dox2 = client.getModelById(DMN_PUB_ID_1);
    assertTrue(dox2.isPresent());
  }


  @Test
  final void testGetModelByIdCMMN() {
    Optional<Document> dox = client.getModelById(CMMN_UNPUB_ID_1);
    assertFalse(dox.isPresent());
  }

  @Test
  final void testGetCmmnModels() {
    var cmmnModels =
        client.listModels("cmmn");
    assertNotNull(cmmnModels);
    assertEquals(0, cmmnModels.count());
  }

  @Test
  final void testGetPublishedCmmnModels() {
    var publishedModels =
        client.listModels("cmmn");
    assertNotNull(publishedModels);
    assertEquals(0, publishedModels.count());
  }


  @Test
  final void testGetPublishedModelByIdWithFileInfoDMN() {
    var trisotechFileInfos
        = client.listModels("dmn");
    TrisotechFileInfo trisotechFileInfo = trisotechFileInfos
        .filter((f) -> f.getId().equals(DMN_PUB_ID_1)).findAny()
        .orElse(null);
    assertNotNull(trisotechFileInfo);
    Optional<Document> dox = client
        .getModel(trisotechFileInfo);
    assertTrue(dox.isPresent());
  }


  @Test
  final void testGetLatestVersionCMMN_Null() {
    // while a file may have multiple versions, no version tag is given to a file until it is published
    var latestInfo = client.getVersionsMetadataByModelId(CMMN_UNPUB_ID_1).stream()
        .findFirst();

    assertNotNull(latestInfo);
    assertTrue(latestInfo.isEmpty());
  }


  @Test
  final void testGetModelByIdDMN() {
    // getModelById returns empty if model is not published
    Optional<Document> dox = client.getModelById(DMN_PUB_ID_1);
    assertTrue(dox.isPresent());
    assertNotNull(dox.get());
  }


  @Test
  final void testGetPublishedDmnModels() {
    var publishedModels =
        client.listModels("dmn");
    assertNotNull(publishedModels);
    assertEquals(13, publishedModels.count());
  }

  @Test
  final void testGetPlaces() {
    String key = EXAMPLE_REPO;

    Map<String, TrisotechPlace> placeMap = client.listAccessiblePlaces();
    assertNotNull(placeMap);
    assertFalse(placeMap.isEmpty());

    assertTrue(placeMap.containsKey(key));
    assertEquals("Trisotech Examples Working Space", placeMap.get(key).getName());
  }


  @Configuration
  @ComponentScan(
      basePackageClasses = {TTWrapper.class})
  public static class PublishedOnlyTestConfig {

  }
}
