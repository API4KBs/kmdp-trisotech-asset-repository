package edu.mayo.kmdp.trisotechwrapper.components.cache;

import static edu.mayo.kmdp.trisotechwrapper.config.TTWConfigParamsDef.REPOSITORY_ID;
import static edu.mayo.kmdp.trisotechwrapper.config.TTWConfigParamsDef.REPOSITORY_NAME;
import static edu.mayo.kmdp.trisotechwrapper.config.TTWConfigParamsDef.REPOSITORY_PATH;
import static edu.mayo.kmdp.trisotechwrapper.config.TTWConfigParamsDef.REPOSITORY_PATHS;
import static edu.mayo.kmdp.util.Util.isEmpty;

import edu.mayo.kmdp.trisotechwrapper.components.TTDigitalEnterpriseServerClient;
import edu.mayo.kmdp.trisotechwrapper.config.TTWEnvironmentConfiguration;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechPlace;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class used to build Place/Path Scopes, which filter the Assets/Artifacts exposed via the
 * TTW KARS API. Scopes are applied to the results of the TT DES APIs, and are subject to the access
 * rights given by the TT API client.
 *
 * <p>
 * A Place/Path Scope consists in a Place, and the Path to one directory within that place. The root
 * folder "/" is used to scope the entire Place. Models that are not stored under one of the scoped
 * directory should be excluded.
 * <p>
 * Example: "place1/path1, place1/path2, place2/path3"
 */
public final class PlaceScopeHelper {

  /**
   * Logger
   */
  private static final Logger logger = LoggerFactory.getLogger(PlaceScopeHelper.class);

  /**
   * No-op Constructor. This class only contains static functions and should not be instantiated
   */
  private PlaceScopeHelper() {
    // static methods only
  }

  /**
   * Uses the environment configuration to determine the Place/Path scopes. Validates the Places,
   * but not the Paths, grouping the Paths by (valid) Place
   *
   * @param cfg       the environment configuration
   * @param webClient the DES API client
   * @return a Map that associates valid Places to the scoping Paths within that Place
   */
  @Nonnull
  public static Map<TrisotechPlace, Set<String>> getScope(
      @Nonnull final TTWEnvironmentConfiguration cfg,
      @Nonnull final TTDigitalEnterpriseServerClient webClient) {
    var targetPaths = cfg.tryGetTyped(REPOSITORY_PATHS, String.class);
    return targetPaths.map(paths -> reduceMultipleScopes(paths, webClient))
        .orElseGet(() -> getSingleScope(cfg, webClient));
  }


  /**
   * Parses a comma-separated list of Place/Path scopes.
   * <p>
   * Example: the scopes "place1/path1, place1/path2, place2/path3, invalid/path4" will yield the
   * map "[Place(place1) -> {path1, path2}; Place(place2) -> {path3}]
   *
   * @param targetPaths the list of Place/Path scopes
   * @param webClient   the DES API client
   * @return a Map that associates valid Places to the scoping Paths within that Place
   */
  @Nonnull
  public static Map<TrisotechPlace, Set<String>> reduceMultipleScopes(
      @Nonnull final String targetPaths,
      @Nonnull final TTDigitalEnterpriseServerClient webClient) {
    Map<TrisotechPlace, Set<String>> acc = new HashMap<>();
    Arrays.stream(targetPaths.split(","))
        .map(PlaceScopeHelper::split)
        .filter(a -> a.length > 0)
        .forEach(pp -> {
          var ttp = getRepositoryById(pp[0], webClient);
          ttp.ifPresent(trisotechPlace ->
              acc.computeIfAbsent(trisotechPlace, k -> new HashSet<>())
                  .add(pp[1]));
        });
    return acc;
  }


  /**
   * Builds a Place scope from the combination of configuration parameters:
   * {REPOSITORY_ID|REPOSITORY_NAME}/REPOSITORY_PATH
   *
   * @param cfg       the environment configuration
   * @param webClient the DES API client
   * @return a single place/path scope
   * @deprecated use {@link #reduceMultipleScopes(String, TTDigitalEnterpriseServerClient)}, which
   * relies on a single configuration parameter, REPOSITORY_PATHS
   */
  @Nonnull
  @Deprecated(since = "6.0.0", forRemoval = true)
  public static Map<TrisotechPlace, Set<String>> getSingleScope(
      @Nonnull final TTWEnvironmentConfiguration cfg,
      @Nonnull final TTDigitalEnterpriseServerClient webClient) {
    var focusPath = cfg.getTyped(REPOSITORY_PATH, String.class);
    var focusPlace = cfg.tryGetTyped(REPOSITORY_ID, String.class)
        .flatMap(id -> getRepositoryById(id, webClient))
        .or(() -> cfg.tryGetTyped(REPOSITORY_NAME, String.class)
            .flatMap(name -> getRepositoryByName(name, webClient)));
    return focusPlace.map(place -> Map.of(place, Set.of(focusPath)))
        .orElseGet(Collections::emptyMap);
  }


  /**
   * Resolves the name of a TT "Place" (repository) to a descriptor. The descriptor contains the
   * placeId, which is used in the API
   *
   * @param repositoryName the name of the Place
   * @return the descriptor of the Place with that name, if any
   * @see TTDigitalEnterpriseServerClient#getPlaces()
   */
  @Nonnull
  public static Optional<TrisotechPlace> getRepositoryByName(
      @Nonnull final String repositoryName,
      @Nonnull final TTDigitalEnterpriseServerClient webClient) {
    try {
      return webClient.getPlaces()
          .stream()
          .filter(tp -> tp.getName().equals(repositoryName))
          .findFirst();
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      return Optional.empty();
    }
  }

  /**
   * Resolves the internal ID of a TT "Place" (repository) to its descriptor.
   * <p>
   * IDs are usually UUIDs, though the specific format is not part of the public APIs contract
   *
   * @param repositoryId the id of the Place
   * @return the descriptor of the Place with that ID, if any
   * @see TTDigitalEnterpriseServerClient#getPlaces()
   */
  @Nonnull
  public static Optional<TrisotechPlace> getRepositoryById(
      @Nonnull final String repositoryId,
      @Nonnull final TTDigitalEnterpriseServerClient webClient) {
    try {
      return webClient.getPlaces()
          .stream()
          .filter(tp -> tp.getId().equals(repositoryId))
          .findFirst();
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      return Optional.empty();
    }
  }


  /**
   * Splits a "place/path" String into its placeId and path components.
   * <p>
   * Uses the first occurrence of '/' as a separator. Expects a non-empty placeId. If unable to
   * detect a place path, uses the default root "/" to scope the entire place
   *
   * @param scope the place/path String to be parsed
   * @return an Array where the first element is the placeId, and the scoped path is the second
   */
  @Nonnull
  private static String[] split(
      @Nullable final String scope) {
    if (scope == null) {
      return new String[0];
    }
    int idx = scope.indexOf('/');
    var placeId = idx < 0 ? scope : scope.substring(0, idx);
    if (isEmpty(placeId)) {
      return new String[0];
    }
    var path = scope.substring(placeId.length());
    if (isEmpty(path)) {
      path = "/";
    }
    return new String[]{placeId, path};
  }
}
