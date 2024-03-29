package edu.mayo.kmdp.trisotechwrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import edu.mayo.kmdp.trisotechwrapper.TTEURentCacheTest.ModelCacheTestConfig;
import edu.mayo.kmdp.trisotechwrapper.components.redactors.TTRedactor;
import edu.mayo.kmdp.trisotechwrapper.components.weavers.DomainSemanticsWeaver;
import edu.mayo.kmdp.trisotechwrapper.config.TTWConfigParamsDef;
import edu.mayo.kmdp.trisotechwrapper.config.TTWEnvironmentConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("dev")
@ContextConfiguration(classes = {ModelCacheTestConfig.class})
@TestPropertySource(properties = {
    "edu.mayo.kmdp.trisotechwrapper.repositoryId=9b6b13d5-00e5-42fe-a844-51a1a4c78106"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TTEURentCacheTest {

  @Autowired
  TTWEnvironmentConfiguration cfg;

  TTAPIAdapter client;

  @BeforeAll
  void setUp() {
    client = new TTWrapper(cfg, new DomainSemanticsWeaver(cfg), new TTRedactor());
    var apiEndpoint = client.getConfigParameter(TTWConfigParamsDef.API_ENDPOINT);
    assumeTrue(apiEndpoint != null);
    assumeFalse(client.listAccessiblePlaces().isEmpty());
  }

  @Test
  void testModelCache() {
    var modelCache = client.getModelCache();

    // model cache is fully lazy
    var stats = modelCache.stats();
    assertEquals(0, stats.loadCount());
    assertEquals(0, stats.hitCount());
    assertEquals(0, stats.totalLoadTime());

    client.getModelById(
            "http://www.trisotech.com/cmmn/definitions/_c09c87e0-a727-4dcd-8c8b-db70934d6688")
        .orElseGet(Assertions::fail);

    stats = modelCache.stats();
    assertEquals(0, stats.hitCount());
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadSuccessCount());
    var time = stats.totalLoadTime();

    client.getModelById(
            "http://www.trisotech.com/cmmn/definitions/_c09c87e0-a727-4dcd-8c8b-db70934d6688")
        .orElseGet(Assertions::fail);
    stats = modelCache.stats();
    assertEquals(1, stats.hitCount());
    assertEquals(1, stats.missCount());
    assertEquals(1, stats.loadSuccessCount());
    assertEquals(time, stats.totalLoadTime());

    client.rescan();
    client.getModelById(
            "http://www.trisotech.com/cmmn/definitions/_c09c87e0-a727-4dcd-8c8b-db70934d6688")
        .orElseGet(Assertions::fail);

    stats = modelCache.stats();
    assertEquals(1, stats.hitCount());
    assertEquals(2, stats.missCount());
    assertEquals(2, stats.loadSuccessCount());
    assertTrue(stats.totalLoadTime() > time);
  }

  @Test
  void testPlaceCache() {
    var cache = client.getPlaceCache();

    assertEquals(1, cache.stats().loadCount());
    client.getCacheablePlaces();
    assertEquals(1, cache.stats().loadCount());

    client.invalidateAll();
    assertTrue(cache.asMap().isEmpty());
    assertEquals(1, cache.stats().loadCount());
    client.rescan();
    assertEquals(2, cache.stats().loadCount());
  }

  @Configuration
  @ComponentScan(
      basePackageClasses = {TTWEnvironmentConfiguration.class})
  public static class ModelCacheTestConfig {

  }
}
