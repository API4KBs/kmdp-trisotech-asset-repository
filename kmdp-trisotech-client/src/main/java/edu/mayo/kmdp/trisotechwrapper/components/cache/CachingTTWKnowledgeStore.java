package edu.mayo.kmdp.trisotechwrapper.components.cache;

import com.github.benmanes.caffeine.cache.LoadingCache;
import edu.mayo.kmdp.trisotechwrapper.components.SemanticModelInfo;
import edu.mayo.kmdp.trisotechwrapper.components.TTWKnowledgeStore;
import edu.mayo.kmdp.trisotechwrapper.components.graph.PlacePathIndex;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechPlace;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.w3c.dom.Document;

/**
 * A {@link TTWKnowledgeStore} that is able to Cache and Index the Models and their Manifests
 * <p>
 * Implementations are expected to provide a Cache by Place, which will index a Place scoped by the
 * configured Paths within that Place, and Cache by Model, which will store individual Models
 */
public interface CachingTTWKnowledgeStore extends TTWKnowledgeStore {

  /**
   * @return the Place/Path cache used to index the TT DES graph
   */
  @NonNull LoadingCache<TrisotechPlace, PlacePathIndex> getPlaceCache();

  /**
   * @return the Model cache
   */
  @NonNull LoadingCache<SemanticModelInfo, Document> getModelCache();

  /**
   * Invalidates the Place and Model Caches, for all Places and Models
   */
  void invalidateCaches();

  /**
   * Invalidates the Place Cache for a given Place ID
   *
   * @param placeId the ID of the Place
   */
  void invalidatePlaceCache(
      @NonNull final String placeId);

  /**
   * Invalidates the Model Cache for a given Model ID
   *
   * @param modelUri the ID of the Model
   */
  void invalidateModelCache(
      @NonNull final String modelUri);


  /**
   * Returns all the Places configured for caching, which should coincide with all the Places
   * configured for access, as described n {@link PlaceScopeHelper}.
   *
   * @return the Places configured to be Cached
   */
  @NonNull Set<TrisotechPlace> getCacheablePlaces();

  /**
   * Returns all the Places configured for caching, and accessible according to the configured
   * access token. Ensures that the Places are actually cached in the process.
   *
   * @return all the accessible Places actually Cached
   */
  @NonNull Set<TrisotechPlace> getAllCachedPlaces();

  /**
   * Returns all the Places configured for caching, and accessible according to the configured
   * access token, and currently loaded in the Cache.
   *
   * @return the accessible Places currently in the Cache
   */
  @NonNull Set<TrisotechPlace> getCachedPlaces();

}
