package edu.mayo.kmdp.trisotechwrapper.components;

import static edu.mayo.kmdp.trisotechwrapper.components.cache.PlaceScopeHelper.reduceMultipleScopes;
import static edu.mayo.kmdp.trisotechwrapper.config.TTWConfigParamsDef.REPOSITORY_PATHS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.mayo.kmdp.trisotechwrapper.config.TTWEnvironmentConfiguration;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechExecutionArtifactData;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechPlace;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechPlaceData;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.jena.query.Query;
import org.apache.jena.query.ResultSet;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

class PlaceScopeHelperTest {

  private static final String place1 = UUID.randomUUID().toString();
  private static final String place2 = UUID.randomUUID().toString();
  private static final String invalidPlace = UUID.randomUUID().toString();

  String path1 = "/P1";
  String path2 = "/P2";
  String path3 = "/P3";

  @Test
  void testSplitPaths() {
    var scopes = String.join(",", List.of(place1 + path1, place1 + path2, place2 + path3));
    var cfg = new TTWEnvironmentConfiguration();
    cfg.setTyped(REPOSITORY_PATHS, scopes);

    var placePaths = reduceMultipleScopes(cfg.getTyped(REPOSITORY_PATHS), new MockClient());
    assertEquals(2, placePaths.keySet().size());

    assertEquals(2, placePaths.get(TrisotechPlace.key(place1)).size());
    assertEquals(1, placePaths.get(TrisotechPlace.key(place2)).size());
  }

  @Test
  void testSplitPathsWithPlaceOnly() {
    var cfg = new TTWEnvironmentConfiguration();
    cfg.setTyped(REPOSITORY_PATHS, place1);

    var placePaths = reduceMultipleScopes(cfg.getTyped(REPOSITORY_PATHS), new MockClient());
    assertEquals(1, placePaths.keySet().size());

    var paths = placePaths.get(TrisotechPlace.key(place1));
    assertEquals(1, paths.size());
    assertTrue(paths.contains("/"));
  }

  @Test
  void testSplitPathsWithPlaceRoot() {
    var cfg = new TTWEnvironmentConfiguration();
    cfg.setTyped(REPOSITORY_PATHS, invalidPlace +"/");

    var placePaths = reduceMultipleScopes(cfg.getTyped(REPOSITORY_PATHS), new MockClient());
    assertTrue(placePaths.keySet().isEmpty());
  }

  @Test
  void testSplitPathsWithInvalid() {
    var cfg = new TTWEnvironmentConfiguration();
    cfg.setTyped(REPOSITORY_PATHS, place1 +"/");

    var placePaths = reduceMultipleScopes(cfg.getTyped(REPOSITORY_PATHS), new MockClient());
    assertEquals(1, placePaths.keySet().size());

    var paths = placePaths.get(TrisotechPlace.key(place1));
    assertEquals(1, paths.size());
    assertTrue(paths.contains("/"));
  }

  private static class MockClient implements TTDigitalEnterpriseServerClient {

    @Override
    public List<TrisotechFileInfo> getModelPreviousVersions(String repositoryId, String fileId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<TrisotechPlaceData> getPlaces() throws IOException {
      var data = new TrisotechPlaceData();
      data.setData(List.of(
          new TrisotechPlace(place1, "Place 1"),
          new TrisotechPlace(place2, "Place 2")));
      return Optional.of(data);
    }

    @Override
    public Optional<Document> downloadXmlModel(TrisotechFileInfo tt) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<TrisotechExecutionArtifactData> getExecutionArtifacts(String execEnv) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void uploadXmlModel(String repositoryId, String path, String name, String mimeType,
        String version, String state, byte[] fileContents) {
      throw new UnsupportedOperationException();
    }

    @Override
    public ResultSet askQuery(Query query) {
      throw new UnsupportedOperationException();
    }
  }
}
