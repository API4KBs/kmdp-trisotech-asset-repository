package edu.mayo.kmdp.trisotechwrapper.components.cache;

import static edu.mayo.kmdp.trisotechwrapper.components.graph.TTGraphQueryHelper.reindexPlace;
import static edu.mayo.kmdp.trisotechwrapper.config.TTWConfigParamsDef.CACHE_EXPIRATION;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalListener;
import edu.mayo.kmdp.trisotechwrapper.components.SemanticModelInfo;
import edu.mayo.kmdp.trisotechwrapper.components.TTDigitalEnterpriseServerClient;
import edu.mayo.kmdp.trisotechwrapper.components.graph.PlacePathIndex;
import edu.mayo.kmdp.trisotechwrapper.config.TTWEnvironmentConfiguration;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechPlace;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.annotation.Nonnull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 * Helper class used to build TTW Caches
 * <p>
 * The TTW supports a Place/Path cache, which caches and indexes the scoped portion of the DES
 * Knowledge Graph, and a Model cache, which caches copies of the actual Models stores in the DES.
 */
public final class AssetCacheHelper {

  /**
   * Initial size of the Place/Path Cache
   * <p>
   * TODO: Consider moving to a configuration parameter
   */
  public static final int PLACE_CACHE_INIT_SIZE = 3;
  /**
   * Maximum size of the Place/Path Cache. Estimated at 3-5 times the initial size of the
   * PLACE_CACHE_INIT_SIZE
   */
  public static final int PLACE_CACHE_MAX_SIZE = 4 * PLACE_CACHE_INIT_SIZE;
  /**
   * Maximum size of the Models Cache. Estimated at 10 times the initial size of the
   * PLACE_CACHE_INIT_SIZE
   */
  public static final int MODEL_CACHE_INIT_SIZE = 10 * PLACE_CACHE_INIT_SIZE;
  /**
   * Maximum size of the Models Cache Estimated at 5 times the initial size of the
   * MODEL_CACHE_INIT_SIZE
   */
  public static final int MODEL_CACHE_MAX_SIZE = 5 * MODEL_CACHE_INIT_SIZE;

  /**
   * Logger
   */
  private static final Logger logger = LoggerFactory.getLogger(AssetCacheHelper.class);


  /**
   * No-op Constructor. This class only contains static functions and should not be instantiated
   */
  private AssetCacheHelper() {
    // helper
  }

  /**
   * Builds the Place/Path cache, querying the DES graph based on the configured Place scopes
   *
   * @param scopedPaths the Place/Path scopes
   * @param webClient   the DES API client
   * @param cfg         the environment configuration
   * @return a Place/Path {@link LoadingCache}
   * @see PlaceScopeHelper
   * @see PlacePathIndex
   */
  public static LoadingCache<TrisotechPlace, PlacePathIndex> newPlaceCache(
      @NonNull final Map<TrisotechPlace, Set<String>> scopedPaths,
      @NonNull final TTDigitalEnterpriseServerClient webClient,
      @NonNull final TTWEnvironmentConfiguration cfg) {
    return Caffeine.newBuilder()
        .expireAfterWrite(cfg.getTyped(CACHE_EXPIRATION, Long.class), TimeUnit.MINUTES)
        .initialCapacity(PLACE_CACHE_INIT_SIZE)
        .maximumSize(PLACE_CACHE_MAX_SIZE)
        .recordStats()
        .evictionListener((RemovalListener<TrisotechPlace, PlacePathIndex>) (key, value, cause) -> {
          if (value != null) {
            value.destroy();
          }
        })
        .build(new CacheLoader<>() {
          @Override
          public @NonNull PlacePathIndex load(@NonNull TrisotechPlace key) {
            return reindexPlace(webClient, key, scopedPaths.get(key), cfg);
          }

          @Override
          public @NonNull Map<@NonNull TrisotechPlace, @NonNull PlacePathIndex> loadAll(
              @NonNull Iterable<? extends @NonNull TrisotechPlace> placeIds) {
            return reindexPlaces(placeIds, scopedPaths, webClient, cfg);
          }
        });
  }

  /**
   * Builds the Model cache, configuring the Model acquisition function, which normalizes the
   * model's XML document in the process
   *
   * @param preProcessor an Operator used to manipulate the models as they are loaded
   * @param webClient    the DES API client
   * @param cfg          the environment configuration
   * @return a Manifest/Model {@link LoadingCache}
   * @see SemanticModelInfo
   * @see Document
   */
  public static LoadingCache<SemanticModelInfo, Document> newModelCache(
      @NonNull final TTDigitalEnterpriseServerClient webClient,
      @NonNull final UnaryOperator<Document> preProcessor,
      @NonNull final TTWEnvironmentConfiguration cfg) {
    return Caffeine.newBuilder()
        .expireAfterWrite(cfg.getTyped(CACHE_EXPIRATION, Long.class), TimeUnit.MINUTES)
        .initialCapacity(MODEL_CACHE_INIT_SIZE)
        .maximumSize(MODEL_CACHE_MAX_SIZE)
        .recordStats()
        .build(new CacheLoader<>() {
          @Override
          public @Nullable Document load(@NonNull SemanticModelInfo key) {
            return webClient.downloadXmlModel(key)
                .map(preProcessor)
                .orElse(null);
          }
        });
  }

  /**
   * Iterates over a given set of Places, (re)indexing each Place
   *
   * @param places      the Places to be (re)indexes
   * @param scopedPaths the Place/Path filters
   * @param client      the DES web client used to interact with the DES SPAQRL API
   * @param cfg         the Environment Configuration
   * @return A Place to Index Map, for the given Places to be (re)indexed
   */
  public static Map<TrisotechPlace, PlacePathIndex> reindexPlaces(
      @Nonnull final Iterable<? extends TrisotechPlace> places,
      @Nonnull final Map<TrisotechPlace, Set<String>> scopedPaths,
      @Nonnull final TTDigitalEnterpriseServerClient client,
      @Nonnull final TTWEnvironmentConfiguration cfg) {
    if (logger.isDebugEnabled()) {
      logger.debug("Start Indexing of all Places");
    }
    return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(places.iterator(), Spliterator.ORDERED), false)
        .map(scope -> reindexPlace(client, scope, scopedPaths.get(scope), cfg))
        .collect(Collectors.toMap(
            PlacePathIndex::getPlace,
            ppi -> ppi
        ));
  }
}