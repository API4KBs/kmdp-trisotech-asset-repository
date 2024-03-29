package edu.mayo.kmdp.trisotechwrapper.components;

import static edu.mayo.kmdp.trisotechwrapper.components.cache.PlaceScopeHelper.reduceMultipleScopes;
import static edu.mayo.kmdp.trisotechwrapper.config.TTWConfigParamsDef.REPOSITORY_PATHS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.mayo.kmdp.trisotechwrapper.config.TTWEnvironmentConfiguration;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechExecutionArtifact;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechPlace;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;
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
    cfg.setTyped(REPOSITORY_PATHS, invalidPlace + "/");

    var placePaths = reduceMultipleScopes(cfg.getTyped(REPOSITORY_PATHS), new MockClient());
    assertTrue(placePaths.keySet().isEmpty());
  }

  @Test
  void testSplitPathsWithInvalid() {
    var cfg = new TTWEnvironmentConfiguration();
    cfg.setTyped(REPOSITORY_PATHS, place1 + "/");

    var placePaths = reduceMultipleScopes(cfg.getTyped(REPOSITORY_PATHS), new MockClient());
    assertEquals(1, placePaths.keySet().size());

    var paths = placePaths.get(TrisotechPlace.key(place1));
    assertEquals(1, paths.size());
    assertTrue(paths.contains("/"));
  }

  private static class MockClient implements TTDigitalEnterpriseServerClient {

    @Nonnull
    @Override
    public Optional<TrisotechFileInfo> getModelLatestVersion(@Nonnull String repositoryId,
        @Nonnull String modelUri) {
      return Optional.empty();
    }

    @Override
    @Nonnull
    public List<TrisotechFileInfo> getModelPreviousVersions(
        @Nonnull String repositoryId,
        @Nonnull String modelUri) {
      throw new UnsupportedOperationException();
    }

    @Override
    @Nonnull
    public List<TrisotechPlace> getPlaces() {
      return List.of(
          new TrisotechPlace(place1, "Place 1"),
          new TrisotechPlace(place2, "Place 2"));
    }

    @Nonnull
    @Override
    public Optional<Document> downloadXmlModel(@Nonnull TrisotechFileInfo tt) {
      throw new UnsupportedOperationException();
    }

    @Nonnull
    @Override
    public List<TrisotechExecutionArtifact> getExecutionArtifacts(
        @Nonnull String baseUrl,
        @Nonnull Set<String> execEnv) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean uploadXmlModel(@Nonnull SemanticModelInfo manifest,
        @Nonnull byte[] fileContents) {
      return false;
    }

    @Nonnull
    @Override
    public ResultSet askQuery(@Nonnull Query query) {
      throw new UnsupportedOperationException();
    }
  }
}
