package edu.mayo.kmdp.trisotechwrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.mayo.kmdp.trisotechwrapper.TTWExampleModelsTest.ExampleTestConfig;
import edu.mayo.kmdp.trisotechwrapper.config.TTWParams;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
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
@ContextConfiguration(classes = {ExampleTestConfig.class})
@TestPropertySource(properties = {
    "edu.mayo.kmdp.trisotechwrapper.repositoryName=Trisotech Examples Working Space",
    "edu.mayo.kmdp.trisotechwrapper.repositoryId=4f5f5508-2137-4004-aef9-3ebef74f177d"})
class TTWExampleModelsTest {

  public static final String EXAMPLE_REPO = "4f5f5508-2137-4004-aef9-3ebef74f177d";


  // CMMN unpublished - Treat fracture from Examples
  private static final String CMMN_UNPUB_ID_1 = "http://www.trisotech.com/cmmn/definitions/_407918cc-68ab-492f-acee-b61afd188e77";

  private static final String DMN_PUB_ID_1 = "http://www.trisotech.com/definitions/_27188a19-403f-471c-8ac3-843c270f8065";


  @Autowired
  TrisotechWrapper client;

  private String ttRepositoryUrl;

  private String testRepoId;


  @BeforeEach
  void setUp() {
    var apiEndpoint = client.getConfig().tryGetTyped(TTWParams.API_ENDPOINT, URI.class);
    Assumptions.assumeTrue(apiEndpoint.isPresent());

    ttRepositoryUrl =
        apiEndpoint + "/repositoryfilecontent?repository=";
    testRepoId = client.getConfig().get(TTWParams.REPOSITORY_ID)
        .orElseGet(Assertions::fail);

    Assumptions.assumeFalse(client.listPlaces().isEmpty());
  }


  @Test
  final void testGetDmnModels() {
    List<TrisotechFileInfo> dmnModels
        = client.getModelsFileInfo("dmn", false);
    assertNotNull(dmnModels);
  }


  @Test
  final void testGetPublishedModelByIdDMN_Null() {
    Optional<Document> dox2 = client.getModelById(DMN_PUB_ID_1, false);
    assertTrue(dox2.isPresent());
  }


  @Test
  final void testGetModelByIdCMMN() {
    Optional<Document> dox = client.getModelById(CMMN_UNPUB_ID_1, false);
    assertTrue(dox.isPresent());
  }

  @Test
  final void testGetCmmnModels() {
    List<TrisotechFileInfo> cmmnModels =
        client.getModelsFileInfo("cmmn", false);
    assertNotNull(cmmnModels);
    assertEquals(2, cmmnModels.size());
  }

  @Test
  final void testGetPublishedCmmnModels() {
    List<TrisotechFileInfo> publishedModels =
        client.getModelsFileInfo("cmmn", true);
    assertNotNull(publishedModels);
    assertEquals(0, publishedModels.size());
  }


  @Test
  final void testGetPublishedModelByIdWithFileInfoDMN() {
    List<TrisotechFileInfo> trisotechFileInfos
        = client.getModelsFileInfo("dmn", false);
    TrisotechFileInfo trisotechFileInfo = trisotechFileInfos.stream()
        .filter((f) -> f.getId().equals(DMN_PUB_ID_1)).findAny()
        .orElse(null);
    assertNotNull(trisotechFileInfo);
    Optional<Document> dox = client
        .getPublishedModel(trisotechFileInfo);
    assertTrue(dox.isPresent());
  }


  @Test
  final void testGetLatestVersionCMMN_Null() {
    // while a file may have multiple versions, no version tag is given to a file until it is published
    var latestInfo = client.getModelVersions(CMMN_UNPUB_ID_1).stream()
        .findFirst();

    assertNotNull(latestInfo);
    assertTrue(latestInfo.isPresent());
    assertNull(latestInfo.get().getVersion());
  }


  @Test
  final void testGetModelByIdDMN() {
    // getModelById returns empty if model is not published
    Optional<Document> dox = client.getModelById(DMN_PUB_ID_1, false);
    assertTrue(dox.isPresent());
    assertNotNull(dox.get());
  }


  @Test
  final void testGetPublishedDmnModels() {
    List<TrisotechFileInfo> publishedModels =
        client.getModelsFileInfo("dmn", true);
    assertNotNull(publishedModels);
    assertEquals(13, publishedModels.size());
  }

  @Test
  final void testGetPlaces() {
    String key = EXAMPLE_REPO;

    Map<String, String> placeMap = client.listPlaces();
    assertNotNull(placeMap);
    assertFalse(placeMap.isEmpty());

    System.out.println(placeMap);
    assertTrue(placeMap.containsKey(key));
    assertEquals("Trisotech Examples Working Space", placeMap.get(key));
  }


  @Configuration
  @ComponentScan(
      basePackageClasses = {TrisotechWrapper.class})
  public static class ExampleTestConfig {

  }
}
