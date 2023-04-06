package edu.mayo.kmdp.trisotechwrapper;

import static edu.mayo.kmdp.trisotechwrapper.config.TTLanguages.CMMN;
import static edu.mayo.kmdp.trisotechwrapper.config.TTLanguages.DMN;
import static edu.mayo.kmdp.trisotechwrapper.config.TTWConfigParamsDef.API_ENDPOINT;
import static edu.mayo.kmdp.trisotechwrapper.config.TTWConfigParamsDef.PUBLISHED_ONLY_FLAG;
import static edu.mayo.kmdp.trisotechwrapper.config.TTWConfigParamsDef.REPOSITORY_ID;
import static edu.mayo.kmdp.util.DateTimeUtil.parseDateTime;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import edu.mayo.kmdp.trisotechwrapper.TTEURentModelsTest.EURentTestConfig;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
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

@SpringBootTest
@ActiveProfiles("dev")
@ContextConfiguration(classes = {EURentTestConfig.class})
@TestPropertySource(properties = {
    "edu.mayo.kmdp.trisotechwrapper.repositoryName=EU-Rent",
    "edu.mayo.kmdp.trisotechwrapper.repositoryId=9b6b13d5-00e5-42fe-a844-51a1a4c78106"})
class TTEURentModelsTest {

  public static final String EU_RENT_REPO = "9b6b13d5-00e5-42fe-a844-51a1a4c78106";

  // DMN Published - Determine Repair Location from EU Rent
  private static final String DMN_PUB_TEST_1_ID = "http://www.trisotech.com/definitions/_a199c656-4291-4f10-9941-e2b53cd52efc";
  private static final String DMN_PUB_TEST_1_TAG = "a199c656-4291-4f10-9941-e2b53cd52efc";
  // DMN published - EU-Rent Pricing from EU-Rent
  private static final String DMN_PUB_TEST_2_ID = "http://www.trisotech.com/definitions/_ceaf902e-c7fe-4c1f-9740-f5f10f278086";
  private static final String DMN_PUB_TEST_2_TAG = "ceaf902e-c7fe-4c1f-9740-f5f10f278086";

  // CMMN Published - Roadside Assistance from EU RENT
  private static final String CMMN_PUB_TEST_1_ID = "http://www.trisotech.com/cmmn/definitions/_c09c87e0-a727-4dcd-8c8b-db70934d6688";
  private static final String CMMN_PUB_TEST_1_TAG = "c09c87e0-a727-4dcd-8c8b-db70934d6688";


  @Autowired
  TTAPIAdapter client;


  @BeforeEach
  void setUp() {
    var apiEndpoint = (URI) client.getConfigParameter(API_ENDPOINT);
    assumeTrue(apiEndpoint != null);

    assertNotNull(client.getConfigParameter(REPOSITORY_ID));
    assertTrue((Boolean) client.getConfigParameter(PUBLISHED_ONLY_FLAG));

    assumeFalse(client.listAccessiblePlaces().isEmpty());
  }


  @Test
  final void testGetDmnModels() {
    var dmnModels
        = client.listModels("dmn");
    assertFalse(dmnModels.findAny().isEmpty());
  }


  @Test
  final void testGetModelByIdAndVersionCMMN() {
    Optional<Document> dox = client
        .getModelByIdAndVersion(CMMN_PUB_TEST_1_ID, "1.2");
    assertTrue(dox.isPresent());
  }


  @Test
  final void testGetLatestVersionInfoDMN() {
    TrisotechFileInfo latestFileInfo = client.getMetadataByModelId(DMN_PUB_TEST_1_ID)
        .orElseGet(Assertions::fail);
    var historyInfos = client.getVersionsMetadataByModelId(DMN_PUB_TEST_1_ID);
    historyInfos = historyInfos.subList(1, historyInfos.size());
    assertTrue(historyInfos.isEmpty());

    String fileId = latestFileInfo.getId();
    String version = latestFileInfo.getVersion();
    Date updateDate = parseDateTime(latestFileInfo.getUpdated());
    assertNotNull(updateDate);

    assertTrue(historyInfos.stream().allMatch(hx -> fileId.equals(hx.getId())));
    List<TrisotechFileInfo> versionHistoryInfos = historyInfos.stream()
        .filter(hx -> version.equals(hx.getVersion()))
        .collect(Collectors.toList());
    // latest version is not returned in the history, so should not find
    // the only way this would not be empty would be if the model was published with the same
    // version more than once
    assertTrue(versionHistoryInfos.isEmpty());
  }


  @Test
  final void testGetModelVersionsDMN() {
    String version = "1.2";
    List<TrisotechFileInfo> fileVersions = client.getVersionsMetadataByModelId(DMN_PUB_TEST_1_ID);
    assertNotNull(fileVersions);
    assertEquals(1, fileVersions.size());

    TrisotechFileInfo file = fileVersions.stream()
        .filter(f -> f.getVersion() != null)
        .filter(f -> f.getVersion().equals(version))
        .findAny()
        .orElse(null);
    assertNotNull(file);

    assertEquals(version, file.getVersion());

    // expect same results with repository provided
    fileVersions = client.getVersionsMetadataByModelId(DMN_PUB_TEST_1_ID);
    assertNotNull(fileVersions);
    assertEquals(1, fileVersions.size());

    file = fileVersions.stream()
        .filter(f -> f.getVersion() != null)
        .filter(f -> f.getVersion().equals(version))
        .findAny()
        .orElse(null);
    assertNotNull(file);

    assertEquals(version, file.getVersion());
  }


  @Test
  final void testGetLatestVersionArtifactIdCMMN() {
    String expectedVersion = "1.2";
    var sinfo =
        client.getMetadataByModelId(CMMN_PUB_TEST_1_ID)
            .orElseGet(Assertions::fail);
    assertTrue(sinfo.getId().contains(CMMN_PUB_TEST_1_TAG));
    assertEquals(expectedVersion, sinfo.getVersion());
  }


  @Test
  final void testGetPublishedModelByIdDMN() {
    Optional<Document> dox = getPublishedModelById(client, DMN_PUB_TEST_1_ID);
    assertTrue(dox.isPresent());
  }


  @Test
  final void testGetPublishedModelByIdCMMN_NonEmpty() {
    Optional<Document> dox = getPublishedModelById(client, DMN_PUB_TEST_2_ID);
    assertTrue(dox.isPresent());
  }


  @Test
  final void testGetLatestVersionArtifactIdDMN() {
    // Weaver Test 1
    String expectedVersion = "1.2";

    var info = client.getMetadataByModelId(DMN_PUB_TEST_1_ID)
        .orElseGet(Assertions::fail);
    assertTrue(info.getId().contains(DMN_PUB_TEST_1_TAG));
    assertEquals(expectedVersion, info.getVersion());
    assertNotNull(info.getUpdated());
  }


  @Test
  final void testGetLatestVersionTrisotechFileInfoCMMN() {
    String expectedVersion = "1.2";

    var info = client.getMetadataByModelId(CMMN_PUB_TEST_1_ID)
        .orElseGet(Assertions::fail);
    assertTrue(info.getId().contains(CMMN_PUB_TEST_1_TAG));
    assertEquals(expectedVersion, info.getVersion());
    assertNotNull(info.getUpdated());

  }


  @Test
  final void testGetModelVersionsWithRepositoryDMN() {
    String version = "1.2";
    List<TrisotechFileInfo> fileVersions = client
        .getVersionsMetadataByModelId(DMN_PUB_TEST_1_ID);
    assertNotNull(fileVersions);
    assertEquals(1, fileVersions.size()); // 7/9/2019 -- should be at least 15

    TrisotechFileInfo file = fileVersions.stream()
        .filter(f -> f.getVersion() != null)
        .filter(f -> f.getVersion().equals(version))
        .findAny()
        .orElse(null);
    assertNotNull(file);

    assertEquals(version, file.getVersion());

    // expect same results with repository provided
    fileVersions = client.getVersionsMetadataByModelId(DMN_PUB_TEST_1_ID);
    assertNotNull(fileVersions);
    assertEquals(1, fileVersions.size());

    file = fileVersions.stream()
        .filter(f -> f.getVersion() != null)
        .filter(f -> f.getVersion().equals(version))
        .findAny()
        .orElse(null);
    assertNotNull(file);

    assertEquals(version, file.getVersion());
  }


  @Test
  final void testGetModelVersionsCMMN() {
    List<TrisotechFileInfo> fileVersions = client
        .getVersionsMetadataByModelId(CMMN_PUB_TEST_1_ID);
    assertNotNull(fileVersions);
    assertEquals(1, fileVersions.size());

    TrisotechFileInfo file = fileVersions.stream()
        .filter(f -> f.getVersion() != null)
        .filter(f -> f.getVersion().equals("1.2"))
        .findAny()
        .orElse(null);
    assertNotNull(file);

    assertEquals("1.2", file.getVersion());
  }


  @Test
  final void testGetModelInfoByIdAndVersionCMMN() {
    String expectedVersion = "1.2";
    TrisotechFileInfo fileInfo = client
        .getMetadataByModelIdAndVersion(CMMN_PUB_TEST_1_ID, expectedVersion)
        .orElse(null);
    assertNotNull(fileInfo);
    assertEquals(expectedVersion, fileInfo.getVersion());
    assertEquals(CMMN_PUB_TEST_1_ID, fileInfo.getId());
  }


  @Test
  final void testGetModelInfoByIdAndVersionDMN() {
    String expectedVersion = "1.2";
    var fileInfo =
        client.getMetadataByModelIdAndVersion(DMN_PUB_TEST_1_ID, expectedVersion)
            .orElse(null);
    assertNotNull(fileInfo);
    assertEquals(expectedVersion, fileInfo.getVersion());
    assertEquals(DMN_PUB_TEST_1_ID, fileInfo.getId());

    String expectedVersion2 = "1.2";
    var fileInfo2 =
        client.getMetadataByModelIdAndVersion(DMN_PUB_TEST_2_ID, expectedVersion2)
            .orElse(null);
    assertNotNull(fileInfo2);
    assertEquals(expectedVersion2, fileInfo2.getVersion());
    assertEquals(DMN_PUB_TEST_2_ID, fileInfo2.getId());
  }

  @Test
  final void testGetModelByIdAndVersionCMMNInvalidVersion() {
    Optional<Document> dox = client
        .getModelByIdAndVersion(CMMN_PUB_TEST_1_ID, "6.5");
    assertEquals(Optional.empty(), dox);
    assertFalse(dox.isPresent());
  }

  @Test
  final void testGetModelByIdAndVersionDMNInvalidVersion() {
    Optional<Document> dox = client
        .getModelByIdAndVersion(DMN_PUB_TEST_1_ID, "2.6");
    assertTrue(dox.isEmpty());

    dox = client.getModelByIdAndVersion(DMN_PUB_TEST_2_ID, "nonsense");
    assertTrue(dox.isEmpty());
  }

  @Test
  final void testGetModelByIdAndVersionDMN() {
    var dox = client
        .getModelByIdAndVersion(DMN_PUB_TEST_1_ID, "1.2");
    assertTrue(dox.isPresent());

    var dox2 = client.getModelByIdAndVersion(DMN_PUB_TEST_2_ID, "1.2");
    assertTrue(dox2.isPresent());
  }


  @Test
  final void testGetPublishedModelByIdDMN_Draft() {
    Optional<Document> dox = getPublishedModelById(client, DMN_PUB_TEST_2_ID);
    assertTrue(dox.isPresent());
  }

  @Test
  final void testGetPublishedModelByIdCMMN() {
    Optional<Document> dox = getPublishedModelById(client, CMMN_PUB_TEST_1_ID);
    assertTrue(dox.isPresent());
  }

  @Test
  final void testGetPublishedModelByIdWithFileInfoCMMN() {
    var trisotechFileInfos =
        client.listModels("cmmn");
    TrisotechFileInfo trisotechFileInfo = trisotechFileInfos
        .filter((f) -> f.getId().equals(CMMN_PUB_TEST_1_ID))
        .findAny()
        .orElse(null);
    assertNotNull(trisotechFileInfo);

    Optional<Document> dox = client
        .getModel(trisotechFileInfo);
    assertTrue(dox.isPresent());
  }


  @Test
  final void testGetPublishedCmmnModels() {
    var publishedModels =
        client.listModels("cmmn")
            .collect(Collectors.toList());
    assertNotNull(publishedModels);
    assertEquals(1, publishedModels.size());
  }


  @Test
  final void testGetPublishedDmnModels() {
    var publishedModels =
        client.listModels("dmn")
            .collect(Collectors.toList());
    assertNotNull(publishedModels);
    assertEquals(4, publishedModels.size());
  }


  @Test
  final void testGetModelInfoDMN() {
    TrisotechFileInfo fileInfo = client.getMetadataByModelId(DMN_PUB_TEST_1_ID)
        .orElse(null);
    assertNotNull(fileInfo);
    assertEquals("Determine the Repair Location", fileInfo.getName());
    assertEquals(DMN_PUB_TEST_1_ID, fileInfo.getId());
    assertTrue(fileInfo.getUrl().contains("&mimetype="));
    assertTrue(fileInfo.getMimetype().contains(DMN.getTag()));
  }


  @Test
  final void testGetModelInfoCMMN() {
    TrisotechFileInfo fileInfo = client.getMetadataByModelId(CMMN_PUB_TEST_1_ID)
        .orElse(null);
    assertNotNull(fileInfo);
    assertEquals("Roadside Assistance", fileInfo.getName());
    assertEquals(CMMN_PUB_TEST_1_ID, fileInfo.getId());
    assertTrue(fileInfo.getUrl().contains("&mimetype="));
    assertTrue(fileInfo.getMimetype().contains(CMMN.getTag()));
  }


  @Test
  final void testDownloadXmlModelDMN() {
    assertDoesNotThrow(() -> {
      Optional<Document> dox = client.getMetadataByModelId(DMN_PUB_TEST_1_ID)
          .flatMap(info -> client.getModel(info));
      assertTrue(dox.isPresent());
    });
  }


  @Test
  final void testDownloadXmlModelCMMN() {
    assertDoesNotThrow(() -> {
      Optional<Document> dox = client.getMetadataByModelId(CMMN_PUB_TEST_1_ID)
          .flatMap(info -> client.getModel(info));
      assertTrue(dox.isPresent());
    });
  }


  @Test
  final void testGetPlaces() {
    var placeMap = client.getCacheablePlaces();
    assertNotNull(placeMap);
    assertFalse(placeMap.isEmpty());
    assertEquals(1, placeMap.size());
    assertTrue(placeMap.containsKey(EU_RENT_REPO));
  }


  private Optional<Document> getPublishedModelById(
      TTAPIAdapter client, String fileId) {
    return client.getMetadataByModelId(fileId)
        .flatMap(client::getModel);
  }

  @Configuration
  @ComponentScan(
      basePackageClasses = {TTWrapper.class})
  public static class EURentTestConfig {

  }
}
