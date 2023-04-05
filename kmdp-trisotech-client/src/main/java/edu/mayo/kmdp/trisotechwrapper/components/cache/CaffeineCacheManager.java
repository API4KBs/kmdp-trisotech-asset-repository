package edu.mayo.kmdp.trisotechwrapper.components.cache;

import static edu.mayo.kmdp.trisotechwrapper.config.TTNotations.mimeMatches;
import static java.util.Collections.emptySortedSet;

import com.github.benmanes.caffeine.cache.LoadingCache;
import edu.mayo.kmdp.trisotechwrapper.components.SemanticModelInfo;
import edu.mayo.kmdp.trisotechwrapper.components.TTDigitalEnterpriseServerClient;
import edu.mayo.kmdp.trisotechwrapper.components.graph.PlacePathIndex;
import edu.mayo.kmdp.trisotechwrapper.config.TTWEnvironmentConfiguration;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechPlace;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.omg.spec.api4kp._20200801.id.KeyIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 * Implementation of {@link CachingTTWKnowledgeStore} based on Caffeine {@link LoadingCache}s
 */
public class CaffeineCacheManager implements CachingTTWKnowledgeStore {

  /**
   * Logger
   */
  private static final Logger logger = LoggerFactory.getLogger(CaffeineCacheManager.class);

  /**
   * The DES Web client, used to query the TT DES Graph, API and retrieve Models
   */
  protected final TTDigitalEnterpriseServerClient webClient;

  /**
   * The Environment Configuration
   */
  protected final TTWEnvironmentConfiguration cfg;

  /**
   * The Scoped Place/Paths, established from the Environment Configuration
   */
  protected final Map<TrisotechPlace, Set<String>> scopedPlacePaths;

  /**
   * The Place/Path Cache
   */
  protected LoadingCache<TrisotechPlace, PlacePathIndex> placeCache;
  /**
   * The Model Cache
   */
  protected LoadingCache<SemanticModelInfo, Document> modelCache;

  /**
   * Constructor.
   * <p>
   * Uses the provided configuration and components to create and initialize the Caches.
   * <p>
   * Supports the use of a pre-processor, which will be applied to a Model as it is loaded into the
   * Cache. Pre-processors can be used to manipulate the Models to add or remove elements, such as
   * semantic annotations or proprietary elements
   *
   * @param webClient    the DES client
   * @param preProcessor an Operator used to manipulate the models as they are loaded
   * @param cfg          the Environment configuration
   */
  public CaffeineCacheManager(
      @Nonnull final TTDigitalEnterpriseServerClient webClient,
      @NonNull final UnaryOperator<Document> preProcessor,
      @Nonnull final TTWEnvironmentConfiguration cfg) {
    this.webClient = webClient;
    this.cfg = cfg;
    this.scopedPlacePaths = PlaceScopeHelper.getScope(cfg, webClient);

    placeCache = AssetCacheHelper.newPlaceCache(scopedPlacePaths, webClient, cfg);
    var loaded = getAllCachedPlaces();
    if (loaded.size() < scopedPlacePaths.size() && logger.isWarnEnabled()) {
      logger.warn("Unable to load all configured Places, check the /health state");
    }

    modelCache = AssetCacheHelper.newModelCache(webClient, preProcessor, cfg);
  }

  @Override
  public void invalidateCaches() {
    placeCache.invalidateAll();
    modelCache.invalidateAll();
  }

  @Override
  public void invalidatePlaceCache(
      @Nonnull final String placeId) {
    placeCache.invalidate(placeId);
  }

  @Override
  public void invalidateModelCache(
      @Nonnull final String modelUri) {
    getMetadataByArtifact(modelUri)
        .ifPresent(modelCache::invalidate);
  }

  @Override
  public Stream<SemanticModelInfo> listAllModelsInfoByMimeClass(
      @Nullable final String mimeType) {
    return forceAllPlaces().flatMap(ppi ->
            ppi.getModelToManifestMappings().values().stream())
        .filter(si -> mimeType == null || mimeMatches(mimeType, si.getMimetype()));
  }

  @Override
  public Stream<SemanticModelInfo> listAllModelsInfoByPlaceAndMimeClass(
      @Nonnull final String placeId,
      @Nullable final String mimeType) {
    return Stream.ofNullable(this.getPlaceCache().get(TrisotechPlace.key(placeId)))
        .flatMap(ppi -> ppi.getModelToManifestMappings().values().stream())
        .filter(si -> mimeType == null || mimeMatches(mimeType, si.getMimetype()));
  }

  @Override
  public Optional<SemanticModelInfo> getMetadataByArtifact(
      @Nonnull final String modelUri) {
    return forceAllPlaces().flatMap(ppi ->
        Optional.ofNullable(ppi.getModelToManifestMappings().get(modelUri)).stream()).findFirst();
  }

  @Override
  public Stream<SemanticModelInfo> getMetadataByAssetVersion(
      @Nonnull KeyIdentifier assetId) {
    return forceAllPlaces().flatMap(ppi ->
        ppi.getAssetToManifestMappings().getOrDefault(assetId, emptySortedSet()).stream());
  }

  @Override
  public Stream<SemanticModelInfo> getServiceMetadataByModel(
      @Nonnull final String modelUri) {
    var modelInfo = getMetadataByArtifact(modelUri);

    return modelInfo.stream().flatMap(s -> s.getExposedServices().stream())
        .flatMap(this::getMetadataByAssetVersion)
        .filter(Objects::nonNull);
  }

  @Override
  public Optional<Document> downloadXmlModel(
      @Nonnull final TrisotechFileInfo info, @Nullable final String mimeType) {
    return Optional.ofNullable(
        modelCache.get(asSemanticInfo(info).orElseGet(() -> new SemanticModelInfo(info))));
  }

  @Override
  public LoadingCache<TrisotechPlace, PlacePathIndex> getPlaceCache() {
    return placeCache;
  }

  @Override
  public LoadingCache<SemanticModelInfo, Document> getModelCache() {
    return modelCache;
  }

  @Override
  public Set<TrisotechPlace> getCachablePlaces() {
    return scopedPlacePaths.keySet();
  }

  @Override
  public Set<TrisotechPlace> getAllCachedPlaces() {
    return getPlaceCache().getAll(scopedPlacePaths.keySet()).keySet();
  }

  @Override
  public Set<TrisotechPlace> getCachedPlaces() {
    return getPlaceCache().getAllPresent(scopedPlacePaths.keySet()).keySet();
  }

  @Override
  public Map<TrisotechPlace, Set<String>> getConfiguredPlacePathScopes() {
    return Collections.unmodifiableMap(scopedPlacePaths);
  }


  /**
   * Forces the Place Cache content to be loaded, then returns the cached Values
   *
   * @return the values in the Place Cache, fully loaded, as a Stream
   */
  protected Stream<PlacePathIndex> forceAllPlaces() {
    return placeCache.getAll(scopedPlacePaths.keySet()).values().stream();
  }

  /**
   * Converts a base {@link TrisotechFileInfo}, as obtained from the TT DES API, to an enhanced
   * {@link SemanticModelInfo}.
   *
   * @param info the source {@link TrisotechFileInfo}
   * @return the enhanced {@link SemanticModelInfo}
   */
  protected Optional<SemanticModelInfo> asSemanticInfo(TrisotechFileInfo info) {
    return info instanceof SemanticModelInfo
        ? Optional.of((SemanticModelInfo) info)
        : getMetadataByArtifact(info.getId());
  }

}
