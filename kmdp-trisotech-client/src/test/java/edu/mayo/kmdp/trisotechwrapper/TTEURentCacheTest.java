package edu.mayo.kmdp.trisotechwrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import edu.mayo.kmdp.trisotechwrapper.TTEURentCacheTest.ModelCacheTestConfig;
import edu.mayo.kmdp.trisotechwrapper.components.cache.CachingTTWKnowledgeStore;
import edu.mayo.kmdp.trisotechwrapper.config.TTWConfigParamsDef;
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

@SpringBootTest
@ActiveProfiles("dev")
@ContextConfiguration(classes = {ModelCacheTestConfig.class})
@TestPropertySource(properties = {
    "edu.mayo.kmdp.trisotechwrapper.repositoryId=9b6b13d5-00e5-42fe-a844-51a1a4c78106"})
class TTEURentCacheTest {

  @Autowired
  TTAPIAdapter client;

  @BeforeEach
  void setUp() {
    var apiEndpoint = client.getConfig().get(TTWConfigParamsDef.API_ENDPOINT);
    assumeTrue(apiEndpoint.isPresent());
    assumeFalse(client.listAccessiblePlaces().isEmpty());
  }

  @Test
  void testModelCache() {
    var cacheMgr = (CachingTTWKnowledgeStore) client.getCacheManager().orElseGet(Assertions::fail);
    var modelCache = cacheMgr.getModelCache();

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
    var cacheMgr = (CachingTTWKnowledgeStore) client.getCacheManager().orElseGet(Assertions::fail);
    var cache = cacheMgr.getPlaceCache();

    assertEquals(1, cache.stats().loadCount());
    cacheMgr.getCachablePlaces();
    assertEquals(1, cache.stats().loadCount());

    cacheMgr.invalidateCaches();
    assertTrue(cache.asMap().isEmpty());
    assertEquals(1, cache.stats().loadCount());
    cacheMgr.getAllCachedPlaces();
    assertEquals(2, cache.stats().loadCount());
  }

  @Configuration
  @ComponentScan(
      basePackageClasses = {TTWrapper.class})
  public static class ModelCacheTestConfig {

  }
}
