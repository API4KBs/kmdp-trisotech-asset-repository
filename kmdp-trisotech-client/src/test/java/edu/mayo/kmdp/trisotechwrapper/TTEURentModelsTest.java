package edu.mayo.kmdp.trisotechwrapper;

import static edu.mayo.kmdp.trisotechwrapper.config.TrisotechApiUrls.CMMN_LOWER;
import static edu.mayo.kmdp.trisotechwrapper.config.TrisotechApiUrls.DMN_LOWER;
import static edu.mayo.kmdp.util.DateTimeUtil.parseDateTime;
import static java.util.UUID.fromString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.mayo.kmdp.trisotechwrapper.TTEURentModelsTest.EURentTestConfig;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
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
  TrisotechWrapper client;

  private String ttRepositoryUrl;

  private String testRepoId;


  @BeforeEach
  void setUp() {
    ttRepositoryUrl =
        client.getConfig().getApiEndpoint() + "/repositoryfilecontent?repository=";
    testRepoId = client.getConfig().getRepositoryId();

    Assumptions.assumeFalse(client.listPlaces().isEmpty());
  }


  @Test
  final void testGetDmnModels() {
    List<TrisotechFileInfo> dmnModels
        = client.getModelsFileInfo("dmn", false);
    assertNotNull(dmnModels);
  }


  @Test
  final void testGetModelByIdAndVersionCMMN() {
    Optional<Document> dox = client
        .getModelByIdAndVersion(CMMN_PUB_TEST_1_ID, "1.2");
    assertTrue(dox.isPresent());
  }


  @Test
  final void testGetLatestVersionInfoDMN() {
    TrisotechFileInfo latestFileInfo = client.getLatestModelFileInfo(DMN_PUB_TEST_1_ID, false)
        .orElseGet(Assertions::fail);
    List<TrisotechFileInfo> historyInfos = client.getModelPreviousVersions(DMN_PUB_TEST_1_ID);
    assertTrue(historyInfos.isEmpty());

    String fileId = latestFileInfo.getId();
    String version = latestFileInfo.getVersion();
    Date updateDate = parseDateTime(latestFileInfo.getUpdated());

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
    List<TrisotechFileInfo> fileVersions = client.getModelVersions(DMN_PUB_TEST_1_ID);
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
    fileVersions = client.getModelVersions(testRepoId,
        DMN_PUB_TEST_1_ID);
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
    ResourceIdentifier versionIdentifier = client.getLatestVersionId(CMMN_PUB_TEST_1_ID)
        .orElse(null);
    assertNotNull(versionIdentifier);
    assertEquals(CMMN_PUB_TEST_1_TAG, versionIdentifier.getTag());
    assertEquals(expectedVersion, versionIdentifier.getVersionTag());
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
    String expectedVersionId = "https://clinicalknowledgemanagement.mayo.edu/artifacts/"
        + "a199c656-4291-4f10-9941-e2b53cd52efc/versions/"
        + expectedVersion;

    ResourceIdentifier versionIdentifier =
        client.getLatestVersionId(DMN_PUB_TEST_1_ID)
            .orElse(null);
    assertNotNull(versionIdentifier);
    assertEquals(DMN_PUB_TEST_1_TAG, versionIdentifier.getTag());
    assertEquals(expectedVersion, versionIdentifier.getVersionTag());
    assertEquals(expectedVersionId, versionIdentifier.getVersionId().toString());
    assertNotNull(versionIdentifier.getEstablishedOn());
  }

  @Test
  final void testGetLatestVersionTrisotechFileInfoDMN() {
    String expectedVersion = "1.2";
    String expectedVersionId = "https://clinicalknowledgemanagement.mayo.edu/artifacts/"
        + "a199c656-4291-4f10-9941-e2b53cd52efc/versions/"
        + expectedVersion;

    TrisotechFileInfo trisotechFileInfo = client.getLatestModelFileInfo(DMN_PUB_TEST_1_ID, false)
        .orElse(null);
    assertNotNull(trisotechFileInfo);
    ResourceIdentifier versionIdentifier = client.getLatestVersionId(trisotechFileInfo)
        .orElse(null);
    assertNotNull(versionIdentifier);
    assertEquals(DMN_PUB_TEST_1_TAG, versionIdentifier.getTag());
    assertEquals(expectedVersion, versionIdentifier.getVersionTag());
    assertNotNull(versionIdentifier.getEstablishedOn());
    assertEquals(expectedVersionId, versionIdentifier.getVersionId().toString());
  }


  @Test
  final void testGetLatestVersionTrisotechFileInfoCMMN() {
    String expectedVersion = "1.2";
    String expectedVersionId = "https://clinicalknowledgemanagement.mayo.edu/artifacts/"
        + "c09c87e0-a727-4dcd-8c8b-db70934d6688/versions/"
        + expectedVersion;

    TrisotechFileInfo trisotechFileInfo = client.getLatestModelFileInfo(CMMN_PUB_TEST_1_ID, false)
        .orElse(null);
    assertNotNull(trisotechFileInfo);
    ResourceIdentifier versionIdentifier = client.getLatestVersionId(trisotechFileInfo)
        .orElse(null);
    assertNotNull(versionIdentifier);
    assertEquals(CMMN_PUB_TEST_1_TAG, versionIdentifier.getTag());
    assertEquals(expectedVersion, versionIdentifier.getVersionTag());
    assertNotNull(versionIdentifier.getEstablishedOn());
    assertEquals(expectedVersionId, versionIdentifier.getVersionId().toString());
  }


  @Test
  final void testGetModelVersionsWithRepositoryDMN() {
    String version = "1.2";
    List<TrisotechFileInfo> fileVersions = client
        .getModelVersions(DMN_PUB_TEST_1_ID);
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
    fileVersions = client.getModelVersions(testRepoId, DMN_PUB_TEST_1_ID);
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
        .getModelVersions(CMMN_PUB_TEST_1_ID);
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
        .getFileInfoByIdAndVersion(fromString(CMMN_PUB_TEST_1_TAG), expectedVersion)
        .orElse(null);
    assertNotNull(fileInfo);
    assertEquals(expectedVersion, fileInfo.getVersion());
    assertEquals(CMMN_PUB_TEST_1_ID, fileInfo.getId());
  }


  @Test
  final void testGetModelInfoByIdAndVersionDMN() {
    String expectedVersion = "1.2";
    var fileInfo =
        client.getFileInfoByIdAndVersion(fromString(DMN_PUB_TEST_1_TAG), expectedVersion)
            .orElse(null);
    assertNotNull(fileInfo);
    assertEquals(expectedVersion, fileInfo.getVersion());
    assertEquals(DMN_PUB_TEST_1_ID, fileInfo.getId());

    String expectedVersion2 = "1.2";
    var fileInfo2 =
        client.getFileInfoByIdAndVersion(fromString(DMN_PUB_TEST_2_TAG), expectedVersion2)
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
    List<TrisotechFileInfo> trisotechFileInfos =
        client.getModelsFileInfo("cmmn", false);
    TrisotechFileInfo trisotechFileInfo = trisotechFileInfos.stream()
        .filter((f) -> f.getId().equals(CMMN_PUB_TEST_1_ID))
        .findAny()
        .orElse(null);
    assertNotNull(trisotechFileInfo);

    Optional<Document> dox = client
        .getPublishedModel(trisotechFileInfo);
    assertTrue(dox.isPresent());
  }


  @Test
  final void testGetPublishedCmmnModels() {
    List<TrisotechFileInfo> publishedModels =
        client.getModelsFileInfo("cmmn", true);
    assertNotNull(publishedModels);
    assertEquals(1, publishedModels.size());
  }


  @Test
  final void testGetPublishedDmnModels() {
    List<TrisotechFileInfo> publishedModels =
        client.getModelsFileInfo("dmn", true);
    assertNotNull(publishedModels);
    assertEquals(4, publishedModels.size());
  }


  @Test
  final void testGetModelInfoDMN() {
    TrisotechFileInfo fileInfo = client.getLatestModelFileInfo(DMN_PUB_TEST_1_ID, false)
        .orElse(null);
    assertNotNull(fileInfo);
    assertEquals("Determine the Repair Location", fileInfo.getName());
    assertEquals(DMN_PUB_TEST_1_ID, fileInfo.getId());
    assertTrue(fileInfo.getUrl().contains("&mimetype="));
    assertTrue(fileInfo.getMimetype().contains(DMN_LOWER));
  }


  @Test
  final void testGetModelInfoCMMN() {
    TrisotechFileInfo fileInfo = client.getLatestModelFileInfo(CMMN_PUB_TEST_1_ID, false)
        .orElse(null);
    assertNotNull(fileInfo);
    assertEquals("Roadside Assistance", fileInfo.getName());
    assertEquals(CMMN_PUB_TEST_1_ID, fileInfo.getId());
    assertTrue(fileInfo.getUrl().contains("&mimetype="));
    assertTrue(fileInfo.getMimetype().contains(CMMN_LOWER));
  }


  @Test
  final void testDownloadXmlModelDMN() {
    String repositoryFileUrl = ttRepositoryUrl + EU_RENT_REPO
        + "&mimetype=application%2Fdmn-1-2%2Bxml&path=/&sku=" + DMN_PUB_TEST_1_ID;
    assertDoesNotThrow(() -> {
      Optional<Document> dox = client.downloadXmlModel(repositoryFileUrl);
      assertTrue(dox.isPresent());
    });
  }


  @Test
  final void testDownloadXmlModelCMMN() {
    String repositoryFileUrl = ttRepositoryUrl + EU_RENT_REPO
        + "&mimetype=application%2Fcmmn-1-1%2Bxml&path=/&sku=" + CMMN_PUB_TEST_1_ID;
    assertDoesNotThrow(() -> {
      Optional<Document> dox = client.downloadXmlModel(repositoryFileUrl);
      assertTrue(dox.isPresent());
    });
  }


  @Test
  final void testGetPlaces() {
    String key = EU_RENT_REPO;

    Map<String, String> placeMap = client.listPlaces();
    assertNotNull(placeMap);
    assertFalse(placeMap.isEmpty());

    System.out.println(placeMap);
    assertTrue(placeMap.containsKey(key));
    assertEquals("EU-Rent", placeMap.get(key));
  }


  private Optional<Document> getPublishedModelById(
      TrisotechWrapper client, String fileId) {
    return client.getLatestModelFileInfo(fileId, false)
        .flatMap(client::getPublishedModel);
  }

  @Configuration
  @ComponentScan(
      basePackageClasses = {TrisotechWrapper.class})
  public static class EURentTestConfig {

  }
}
