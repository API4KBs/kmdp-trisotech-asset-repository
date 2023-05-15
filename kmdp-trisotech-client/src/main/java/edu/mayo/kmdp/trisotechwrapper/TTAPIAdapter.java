package edu.mayo.kmdp.trisotechwrapper;

import com.github.benmanes.caffeine.cache.LoadingCache;
import edu.mayo.kmdp.trisotechwrapper.components.SemanticModelInfo;
import edu.mayo.kmdp.trisotechwrapper.components.graph.PlacePathIndex;
import edu.mayo.kmdp.trisotechwrapper.config.TTLanguages;
import edu.mayo.kmdp.trisotechwrapper.config.TTNotations;
import edu.mayo.kmdp.trisotechwrapper.config.TTWConfigParamsDef;
import edu.mayo.kmdp.trisotechwrapper.config.TTWEnvironmentConfiguration;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechExecutionArtifact;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechPlace;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.w3c.dom.Document;


/**
 * Core internal interface of the TTW
 * <p>
 * Adapts the DES (Web) API, providing an abstraction layers that supports the API4KP
 * implementation.
 *
 * Provides a few major capabilities
 * <ul>
 *   <li>Place Indexes and Model Caches</li>
 *   <li>Configuration access / discovery</li>
 *   <li>Place discovery</li>
 *   <li>Model discovery</li>
 *   <li>Excution discovery</li>
 *   <li>Metadata and Model access</li>
 * </ul>
 */
public interface TTAPIAdapter {

  /* ---------------------------------------------------------------------------------------- */

  /**
   * @return the Place/Path cache used to index the TT DES graph
   */
  @NonNull LoadingCache<TrisotechPlace, PlacePathIndex> getPlaceCache();

  /**
   * @return the Model cache
   */
  @NonNull LoadingCache<SemanticModelInfo, Document> getModelCache();

  /**
   * Invalidates all Caches: Place/Path Indexes, and Models
   */
  void invalidateAll();

  /**
   * Re-indexes all Places/Repositories.
   * <p>
   * Invalidates all Caches, and refreshes the Place/Path Caches. Implementations are NOT expected
   * to reload the Model Caches.
   */
  void rescan();

  /**
   * Invalidates a specific Place/Repository Cache.
   * <p>
   * Also invalidates any Cached Models that originated in that Place
   *
   * @param placeId the ID of the Place
   */
  void invalidatePlace(
      @Nonnull final String placeId);

  /**
   * Re-indexes a specific Place/Repository.
   * <p>
   * Invalidates its Cache and Models, then reloads that Place/Path Index
   *
   * @param placeId the ID of the Place to reindex
   */
  void rescanPlace(
      @Nonnull final String placeId);

  /**
   * Invalidates a specific Model, if cached
   *
   * @param modelUri the ID of the Model
   */
  void invalidateModel(
      @Nonnull final String modelUri);

  /**
   * Loads/Refreshes a specific Model in the Cache
   *
   * @param modelUri the ID of the Model
   */
  void rescanModel(
      @Nonnull final String modelUri);

  /* ---------------------------------------------------------------------------------------- */

  /**
   * Returns the state of the current {@link TTWEnvironmentConfiguration} values, as a copy
   *
   * @return a snapshot of the current configuration values
   */
  @Nonnull
  Map<String, Object> getConfigParameters();

  /**
   * Accessor
   *
   * @param param the configuration parameter to lookup
   * @return the value of the given param, or null
   */
  @Nullable
  Object getConfigParameter(
      @Nonnull final TTWConfigParamsDef param);

  /* ---------------------------------------------------------------------------------------- */

  /**
   * Provides a Stream of (metadata for) all available Models, across all Scoped Places/Paths.
   * <p>
   * Returns Model manifests, instead of the actual Modes, for performance reasons
   *
   * @return a list of the available Model manifests
   */
  @Nonnull
  default Stream<SemanticModelInfo> listModels() {
    return listModels(null);
  }

  /**
   * Provides a Stream of (metadata for) all available Models, across all Scoped Places/Paths,
   * filtered by an optional mimeType
   * <p>
   * Returns Model manifests, instead of the actual Modes, for performance reasons
   * <p>
   * MimeType comparison should be performed at the mime class level, only using the
   * {@link TTLanguages} implied by the mime
   *
   * @param xmlMimeType the mimeType filter
   * @return a list of the available Model manifests, such that the Models match the given mimeType.
   * @see TTNotations#mimeMatches(String, String)
   */
  @Nonnull
  Stream<SemanticModelInfo> listModels(
      @Nullable String xmlMimeType);


  /**
   * Provides a Stream of (metadata for) all available Models, across all Scoped Paths for the given
   * Place
   * <p>
   * Returns Model manifests, instead of the actual Modes, for performance reasons
   *
   * @param placeId the ID of the place
   * @return a list of the available Model manifests in the given Place
   */
  @Nonnull
  default Stream<SemanticModelInfo> listModelsByPlace(
      @Nonnull final String placeId) {
    return listModelsByPlace(placeId, null);
  }

  /**
   * Provides a Stream of (metadata for) all available Models, across all Scoped Paths for the given
   * Place, filtered by an optional mimeType
   * <p>
   * Returns Model manifests, instead of the actual Modes, for performance reasons
   * <p>
   * MimeType comparison should be performed at the mime class level, only using the
   * {@link TTLanguages} implied by the mime
   *
   * @param placeId     the ID of the place
   * @param xmlMimeType the mimeType filter
   * @return a list of the available Model manifests in the given Place, such that the Models match
   * the given mimeType.
   * @see TTNotations#mimeMatches(String, String)
   */
  @Nonnull
  Stream<SemanticModelInfo> listModelsByPlace(
      @Nonnull final String placeId,
      @Nullable final String xmlMimeType);

  /* ---------------------------------------------------------------------------------------- */

  /**
   * Provides a list of the Places that the client has access to, based on their token/credentials
   *
   * @return a Place ID to Place descriptor Map
   */
  @Nonnull
  Map<String, TrisotechPlace> listAccessiblePlaces();

  /**
   * Provides a list of the Places/Paths that have been configured for (scoped) access
   *
   * @return a Place descriptor to Scoped paths map
   */
  @Nonnull
  Map<TrisotechPlace, Set<String>> getConfiguredPlacePathScopes();

  /**
   * Provides a list of the Places that the client has access to, AND have been configured for
   * (scoped) access
   *
   * @return a Place ID to Place descriptor Map
   */
  @Nonnull
  Map<String, TrisotechPlace> getCacheablePlaces();

  /**
   * Provides a list of the Places that the client has access to, AND have been configured for
   * (scoped) access, AND are currently cached
   *
   * @return a Place ID to Place descriptor Map
   */
  @Nonnull
  Map<String, TrisotechPlace> getCachedPlaces();

  /* ---------------------------------------------------------------------------------------- */

  /**
   * Returns a collection of the Execution artifacts (Decision / Process Services) deployed in the
   * give execution environment
   *
   * @param slBaseUrl the base URL of the Service Library hosting the exec environment
   * @param env the ID of the execution environment
   * @return a collection of {@link TrisotechExecutionArtifact}, indexed by ID
   */
  @Nonnull
  Map<String, List<TrisotechExecutionArtifact>> listExecutionArtifacts(
      @Nonnull final String slBaseUrl,
      @Nonnull final Set<String> env);

  /**
   * Retrieves the deployments of a given Service, across the configured Service Libraries
   *
   * @param serviceName the internal name of the service
   * @param manifest    the artifact metadata of the model exposed as a service
   * @return descriptors of the artifact deployments, if the service is deployed
   * @see TTWConfigParamsDef#SERVICE_LIBRARY_ENVIRONMENT
   */
  @Nonnull
  Stream<TrisotechExecutionArtifact> getExecutionArtifacts(
      @Nonnull final String serviceName,
      @Nonnull final SemanticModelInfo manifest);

  /* ---------------------------------------------------------------------------------------- */

  /**
   * Retrieves the {@link SemanticModelInfo} metadata for the latest version of a model
   *
   * @param modelUri the ID of the model
   * @return a {@link SemanticModelInfo} for that model, if any
   */
  @Nonnull
  Optional<SemanticModelInfo> getMetadataByModelId(
      @Nonnull final String modelUri);


  /**
   * Returns metadata for all the versions of the Model with a given ID
   *
   * @param modelUri the ID of the model
   * @return a List of {@link TrisotechFileInfo} for the versions of that model, or an empty list
   */
  @Nonnull
  List<TrisotechFileInfo> getVersionsMetadataByModelId(
      @Nonnull final String modelUri);

  /**
   * Gets the {@link SemanticModelInfo} manifest for the Model and Model Version provided. This
   * version does not need be the latest.
   * <p>
   * Note: Version that are not the latest are not indexed nor cached, and will be more
   * computationally expensive to retrieve
   *
   * @param modelUri     the ID of the model
   * @param modelVersion the modelVersion
   * @return a Manifest for that version of that file, if any
   */
  @Nonnull
  Optional<TrisotechFileInfo> getMetadataByModelIdAndVersion(
      @Nonnull final String modelUri,
      @Nonnull final String modelVersion);

  /**
   * Returns the Service Asset metadata, as per the TT KG, for a given model ID
   * <p>
   * (Assumption: one model may define 0..N inference services)
   *
   * @param modelUri The ID of the Model
   * @return the {@link SemanticModelInfo} descriptors of the Service Assets exposed by that Model,
   * if any, as a Stream
   */
  @Nonnull
  Stream<SemanticModelInfo> getServicesMetadataByModelId(
      @Nonnull final String modelUri);

  /**
   * Returns metadata for the Models that carry a specific version of a given Knowledge Asset
   * <p>
   * (Assumption: a given Asset version may be carried by 0..N Models)
   *
   * @param assetId         The ID of the Knowledge Asset
   * @param assetVersionTag the version of the Knowledge Asset
   * @return the {@link SemanticModelInfo} descriptors of the Models that carry that version of the
   * Asset, if any, as a Stream
   */
  @Nonnull
  Stream<SemanticModelInfo> getMetadataByAssetId(
      @Nonnull final UUID assetId,
      @Nonnull final String assetVersionTag);

  /**
   * Returns metadata for the Models that carry the GREATEST version of a given Knowledge Asset,
   * according to the natural ordering of version tags that follow a SemVer/CalVer pattern
   * <p>
   * (Assumption: a given Asset version may be carried by 0..N Models)
   *
   * @param assetId         The ID of the Knowledge Asset
   * @return the {@link SemanticModelInfo} descriptors of the Models that carry that version of the
   * Asset, if any, as a Stream
   */
  @Nonnull
  Stream<SemanticModelInfo> getMetadataByGreatestAssetId(
      @Nonnull final UUID assetId);


  /**
   * Retrieves the LATEST version of the Model with the given ID
   *
   * @param modelUri The model ID of the Model to retrieve
   * @return XML Document for the model, if any
   */
  @Nonnull
  Optional<Document> getModelById(
      @Nonnull final String modelUri);

  /**
   * Retrieves the given version of the Model with the given ID
   *
   * @param modelUri     the ID of the model
   * @param modelVersion the versionTag for the model
   * @return XML Document for the model version, if any
   */
  @Nonnull
  Optional<Document> getModelByIdAndVersion(
      @Nonnull final String modelUri,
      @Nonnull final String modelVersion);

  /**
   * Retrieves the Model for the given Manifest
   *
   * @param trisotechFileInfo the manifest for the Model.
   * @return XML Document for the associated Model, if any
   */
  @Nonnull
  Optional<Document> getModel(
      @Nonnull final TrisotechFileInfo trisotechFileInfo);


}
