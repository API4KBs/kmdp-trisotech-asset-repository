package edu.mayo.kmdp.trisotechwrapper;

import static edu.mayo.kmdp.trisotechwrapper.config.TTWConfigParamsDef.API_ENDPOINT;
import static edu.mayo.kmdp.trisotechwrapper.config.TTWConfigParamsDef.PUBLISHED_ONLY_FLAG;
import static edu.mayo.kmdp.trisotechwrapper.config.TTWConfigParamsDef.REPOSITORY_ID;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import edu.mayo.kmdp.trisotechwrapper.TTWExampleModelsTest.PublishedOnlyTestConfig;
import java.net.URI;
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
    "edu.mayo.kmdp.application.flag.publishedOnly=false",
    "edu.mayo.kmdp.trisotechwrapper.repositoryName=Trisotech Examples Working Space",
    "edu.mayo.kmdp.trisotechwrapper.repositoryId=4f5f5508-2137-4004-aef9-3ebef74f177d"})
class TTWExampleModelsTestWithUnpublished {

  public static final String EXAMPLE_REPO = "4f5f5508-2137-4004-aef9-3ebef74f177d";


  // CMMN unpublished - Treat fracture from Examples
  private static final String CMMN_UNPUB_ID_1 = "http://www.trisotech.com/cmmn/definitions/_407918cc-68ab-492f-acee-b61afd188e77";


  @Autowired
  TTWrapper client;


  @BeforeEach
  void setUp() {
    var apiEndpoint = client.getConfig().tryGetTyped(API_ENDPOINT, URI.class);
    assumeTrue(apiEndpoint.isPresent());

    client.getConfig().get(REPOSITORY_ID)
        .orElseGet(Assertions::fail);
    assertFalse(client.getConfig().getTyped(PUBLISHED_ONLY_FLAG, Boolean.class));

    assumeFalse(client.listAccessiblePlaces().isEmpty());
  }


  @Test
  final void testGetModelByIdCMMN() {
    Optional<Document> dox = client.getModelById(CMMN_UNPUB_ID_1);
    assertTrue(dox.isPresent());
  }

  @Test
  final void testGetCmmnModels() {
    var publishedModels =
        client.listModels("cmmn");
    assertNotNull(publishedModels);
    assertTrue(publishedModels.findAny().isPresent());
  }

  @Configuration
  @ComponentScan(
      basePackageClasses = {TTWrapper.class})
  public static class PubUnpubTestConfig {

  }
}
