package edu.mayo.kmdp.trisotechwrapper;

import static edu.mayo.kmdp.trisotechwrapper.config.TTWConfigParamsDef.DEFAULT_VERSION_TAG;
import static edu.mayo.kmdp.util.DateTimeUtil.parseDateTime;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newKey;

import com.github.benmanes.caffeine.cache.LoadingCache;
import edu.mayo.kmdp.trisotechwrapper.components.SemanticModelInfo;
import edu.mayo.kmdp.trisotechwrapper.components.TTDigitalEnterpriseServerClient;
import edu.mayo.kmdp.trisotechwrapper.components.TTWebClient;
import edu.mayo.kmdp.trisotechwrapper.components.cache.CachingTTWKnowledgeStore;
import edu.mayo.kmdp.trisotechwrapper.components.cache.CaffeineCacheManager;
import edu.mayo.kmdp.trisotechwrapper.components.graph.PlacePathIndex;
import edu.mayo.kmdp.trisotechwrapper.components.redactors.Redactor;
import edu.mayo.kmdp.trisotechwrapper.components.weavers.Weaver;
import edu.mayo.kmdp.trisotechwrapper.config.TTWConfigParamsDef;
import edu.mayo.kmdp.trisotechwrapper.config.TTWEnvironmentConfiguration;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechExecutionArtifact;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechPlace;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

/**
 * {@inheritDoc}
 * <p>
 * Primary implemntation of the {@link TTAPIAdapter}
 */
@Component
public class TTWrapper implements TTAPIAdapter {

  /**
   * Logger
   */
  private final Logger logger = LoggerFactory.getLogger(TTWrapper.class);

  /**
   * Environment Configuration
   */
  @Autowired
  private TTWEnvironmentConfiguration cfg;

  /**
   * {@link Weaver}, to annotate and rewrite proprietary elements in Models
   */
  @Autowired
  protected Weaver weaver;

  /**
   * {@link Redactor}, to remove unncessary fragments
   */
  @Autowired
  protected Redactor redactor;

  /**
   * Web Client, to interact with the DES web API
   */
  TTDigitalEnterpriseServerClient webClient;

  /**
   * Cache Maager, to Index Place/Paths and Cache Models
   */
  CachingTTWKnowledgeStore cacheManager;

  @PostConstruct
  void init() {
    this.webClient = initWebClient(cfg);
    this.cacheManager = initCacheManager(webClient, cfg);
  }

  /**
   * Initializes the {@link CachingTTWKnowledgeStore}
   *
   * @param webClient the DES client, used to (re)load the cache content
   * @param cfg       the environment configuration
   * @return the initialized {@link CachingTTWKnowledgeStore}
   */
  protected CachingTTWKnowledgeStore initCacheManager(
      @Nonnull final TTDigitalEnterpriseServerClient webClient,
      @Nonnull final TTWEnvironmentConfiguration cfg) {
    return new CaffeineCacheManager(
        webClient,
        dox -> redactor.redact(weaver.weave(dox)),
        cfg);
  }

  /**
   * Initializes the {@link TTDigitalEnterpriseServerClient}
   *
   * @param cfg the environment configuration
   * @return the initialized {@link TTDigitalEnterpriseServerClient}
   */
  protected TTDigitalEnterpriseServerClient initWebClient(
      @Nonnull final TTWEnvironmentConfiguration cfg) {
    return new TTWebClient(cfg);
  }


  @Override
  @Nonnull
  public Map<String, Object> getConfigParameters() {
    if (logger.isDebugEnabled()) {
      logger.debug("The current Trisotech configuration is {}", cfg);
    }
    return cfg.toMap();
  }

  @Override
  @Nullable
  public Object getConfigParameter(
      @Nonnull final TTWConfigParamsDef param) {
    return cfg.getTyped(param, param.getType());
  }

  @Nonnull
  @Override
  public Stream<SemanticModelInfo> getServicesMetadataByModelId(
      @Nonnull final String modelUri) {
    return cacheManager.getServiceMetadataByModel(modelUri);
  }


  @Nonnull
  @Override
  public Stream<SemanticModelInfo> getMetadataByAssetId(
      @Nonnull final UUID assetId,
      @Nonnull final String assetVersionTag) {
    return cacheManager.getMetadataByAssetVersion(newKey(assetId, assetVersionTag));
  }

  @Override
  public void invalidateAll() {
    cacheManager.invalidateCaches();
  }

  @Override
  public void rescan() {
    invalidateAll();
    cacheManager.getAllCachedPlaces();
  }

  @Override
  public void invalidatePlace(
      @Nonnull final String placeId) {
    listModelsByPlace(placeId)
        .map(TrisotechFileInfo::getId)
        .forEach(cacheManager::invalidateModelCache);
    cacheManager.invalidatePlaceCache(placeId);
  }

  @Override
  public void rescanPlace(
      @Nonnull final String placeId) {
    invalidatePlace(placeId);
    cacheManager.getPlaceCache().refresh(TrisotechPlace.key(placeId));
  }


  @Override
  public void invalidateModel(
      @Nonnull final String modelUri) {
    cacheManager.invalidateModelCache(modelUri);
  }

  @Override
  public void rescanModel(
      @Nonnull final String modelUri) {
    cacheManager.getModelCache().refresh(new SemanticModelInfo(modelUri));
  }

  @Override
  @Nonnull
  public Optional<Document> getModelById(
      @Nonnull final String fileId) {
    return getMetadataByModelId(fileId)
        .flatMap(this::getModel);
  }


  @Nonnull
  @Override
  public Optional<Document> getModel(
      @Nonnull final TrisotechFileInfo trisotechFileInfo) {
    try {
      return cacheManager.downloadXmlModel(trisotechFileInfo);
    } catch (Exception e) {
      logger.error(String.format("%s %s", e.getMessage(), Arrays.toString(e.getStackTrace())), e);
    }
    return Optional.empty();
  }

  @Override
  @Nonnull
  public Stream<SemanticModelInfo> listModels(
      @Nullable final String xmlMimeType) {
    return cacheManager.listAllModelsInfoByMimeClass(xmlMimeType);
  }

  @Override
  @Nonnull
  public Stream<SemanticModelInfo> listModelsByPlace(
      @Nonnull final String placeId,
      @Nullable final String xmlMimeType) {
    return cacheManager.listAllModelsInfoByPlaceAndMimeClass(placeId, xmlMimeType);
  }

  @Override
  @Nonnull
  public Optional<TrisotechFileInfo> getMetadataByModelIdAndVersion(
      @Nonnull final String modelUri,
      @Nonnull final String modelVersion) {
    return getMetadataByModelId(modelUri)
        .map(TrisotechFileInfo.class::cast)
        .filter(trisotechFileInfo -> matchesVersion(trisotechFileInfo, modelVersion,
            this::defaultVersion))
        .or(() ->
            getVersionsMetadataByModelId(modelUri).stream()
                .filter(trisotechFileInfo -> matchesVersion(trisotechFileInfo, modelVersion,
                    this::defaultVersion))
                .findFirst());
  }

  @Override
  @Nonnull
  public Optional<SemanticModelInfo> getMetadataByModelId(
      @Nonnull final String modelId) {
    var info = cacheManager.getMetadataByArtifact(modelId);
    if (info.isEmpty()) {
      logger.warn("Unable to get FileInfo for {}", modelId);
    }
    logger.trace("The info found from the cache manager is {}", info);
    return info;
  }


  @Nonnull
  @Override
  public Optional<Document> getModelByIdAndVersion(
      @Nonnull final String modelUri,
      @Nonnull final String modelVersion) {
    var latest = cacheManager.getMetadataByArtifact(modelUri);
    if (latest.isEmpty()) {
      // not found
      return Optional.empty();
    }
    return latest.filter(info -> matchesVersion(info, modelVersion, this::defaultVersion))
        // if versionTag matches latest, return from cache
        .flatMap(this::getModel)
        // else, will not be in cache - need to access the server
        .or(() -> getOldVersion(modelUri, modelVersion));
  }

  @Override
  @Nonnull
  public List<TrisotechFileInfo> getVersionsMetadataByModelId(
      @Nonnull final String modelUri) {
    var mostRecent = cacheManager.getMetadataByArtifact(modelUri);
    if (mostRecent.isEmpty() || mostRecent.map(SemanticModelInfo::getPlaceId).isEmpty()) {
      return Collections.emptyList();
    }
    var previous = webClient.getModelPreviousVersions(mostRecent.get().getPlaceId(), modelUri);
    List<TrisotechFileInfo> history = new ArrayList<>(1 + previous.size());
    mostRecent.ifPresent(history::add);
    history.addAll(previous);
    return history;
  }

  @Override
  @Nonnull
  public Map<String, TrisotechPlace> listAccessiblePlaces() {
    return webClient.getPlaces().stream()
        .collect(Collectors.toMap(
            TrisotechPlace::getId,
            tp -> tp
        ));
  }

  @Override
  @Nonnull
  public Map<TrisotechPlace, Set<String>> getConfiguredPlacePathScopes() {
    return cacheManager.getConfiguredPlacePathScopes();
  }

  @Override
  @Nonnull
  public Map<String, TrisotechPlace> getCacheablePlaces() {
    return cacheManager.getCacheablePlaces().stream()
        .collect(Collectors.toMap(
            TrisotechPlace::getId,
            tp -> tp
        ));
  }

  @Override
  @Nonnull
  public Map<String, TrisotechPlace> getCachedPlaces() {
    return cacheManager.getCachedPlaces().stream()
        .collect(Collectors.toMap(
            TrisotechPlace::getId,
            tp -> tp
        ));
  }

  @Override
  @Nonnull
  public Map<String, TrisotechExecutionArtifact> listExecutionArtifacts(
      @Nonnull final String env) {
    try {
      return webClient.getExecutionArtifacts(env).stream()
          .collect(Collectors.toMap(
              TrisotechExecutionArtifact::getName,
              x -> x
          ));
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      return Collections.emptyMap();
    }
  }


  @Override
  @Nonnull
  public Optional<TrisotechExecutionArtifact> getExecutionArtifact(
      @Nonnull final String serviceName,
      @Nonnull final TrisotechFileInfo manifest) {
    var execs = listExecutionArtifacts(
        cfg.getTyped(TTWConfigParamsDef.SERVICE_LIBRARY_ENVIRONMENT));
    // DMN executables - the whole model is mapped to a service
    return Optional.ofNullable(execs.get(manifest.getName()))
        // BPMN executables - each process is mapped to a service
        .or(() -> Optional.ofNullable(execs.get(serviceName)));
  }


  @Override
  @NonNull
  public LoadingCache<TrisotechPlace, PlacePathIndex> getPlaceCache() {
    return cacheManager.getPlaceCache();
  }


  @Override
  @NonNull
  public LoadingCache<SemanticModelInfo, Document> getModelCache() {
    return cacheManager.getModelCache();
  }


  /**
   * Retrieves the given version of the Model with the given ID, when the version is not the latest
   * version
   *
   * @param modelUri     the ID of the model
   * @param modelVersion the modelVersion for the model
   * @return XML Document for the model version, if any
   */
  private Optional<? extends Document> getOldVersion(
      String modelUri,
      String modelVersion) {

    return getVersionsMetadataByModelId(modelUri).stream()
        .filter(fi -> matchesVersion(fi, modelVersion, this::defaultVersion))
        .findFirst()
        .flatMap(tt -> webClient.downloadXmlModel(tt));
  }


  /**
   * Combines a Model version Tag with a timestamp derived from the Model's last update. Assuming
   * the model version follows the SemVer scheme, appends the timestamp as a pre-release tag
   * <p>
   * Note that TT allows to reuse version tags, effectively making versions not immutable, and
   * causing "latest" versions to not always be the "greatest" (and vice-versa).
   * <p>
   * Attaching the timestamp, which should be necessary for non-published Models only, effectively
   * treats Models as SNAPSHOTS.
   *
   * @param versionTag The version tag of Model
   * @param timeStamp  The timestamp of the retrieved model
   * @return a 'version-timestamp' tag
   */
  public static String applyTimestampToVersion(
      @Nonnull final String versionTag,
      long timeStamp) {
    return versionTag + "-" + timeStamp;
  }


  /**
   * Determines whether a given Manifest has a specific Model versionTag.
   * <p>
   * Compares the given version to the manifest's {@link TrisotechFileInfo#getVersion}, either with
   * or without the timestamp implied by the {@link TrisotechFileInfo#getUpdated()}
   * <p>
   * When a manifest does not include an actual version tag (i.e. info.version is null), typically
   * because the Model version is unpublished, a default version tag will be used. Since the choice
   * of
   *
   * @param info           the Model manifest
   * @param modelVersion   the target version
   * @param defaultVersion a client-provided definition of the default version, used in the comparison of
   *                       manifests of unpublished Models
   * @return if the given modelVersion matches the version in the manifest, with or without the
   * Model timestamp
   */
  public static boolean matchesVersion(
      @Nonnull final TrisotechFileInfo info,
      @Nonnull final String modelVersion,
      @Nonnull final Supplier<String> defaultVersion) {
    var ver = Optional.ofNullable(info.getVersion()).orElseGet(defaultVersion);
    return Objects.equals(ver, modelVersion)
        || Objects.equals(
        applyTimestampToVersion(ver, parseDateTime(info.getUpdated()).getTime()),
        modelVersion);
  }


  /**
   * @return the default version tag implicitly associated to 'null' (unpublished) Model versions
   */
  protected String defaultVersion() {
    return cfg.getTyped(DEFAULT_VERSION_TAG);
  }
}
