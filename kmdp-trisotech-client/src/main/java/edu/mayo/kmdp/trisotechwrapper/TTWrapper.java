package edu.mayo.kmdp.trisotechwrapper;

import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newKey;
import static org.omg.spec.api4kp._20200801.id.VersionIdentifier.toSemVer;

import edu.mayo.kmdp.trisotechwrapper.components.SemanticModelInfo;
import edu.mayo.kmdp.trisotechwrapper.components.TTDigitalEnterpriseServerClient;
import edu.mayo.kmdp.trisotechwrapper.components.TTWebClient;
import edu.mayo.kmdp.trisotechwrapper.components.cache.CachingTTWKnowledgeStore;
import edu.mayo.kmdp.trisotechwrapper.components.cache.CaffeineCacheManager;
import edu.mayo.kmdp.trisotechwrapper.components.redactors.Redactor;
import edu.mayo.kmdp.trisotechwrapper.components.weavers.Weaver;
import edu.mayo.kmdp.trisotechwrapper.config.TTWConfigParamsDef;
import edu.mayo.kmdp.trisotechwrapper.config.TTWEnvironmentConfiguration;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechExecutionArtifact;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechPlace;
import edu.mayo.kmdp.util.Util;
import edu.mayo.kmdp.util.XMLUtil;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

/**
 *
 */
@Component
public class TTWrapper implements TTAPIAdapter {

  private final Logger logger = LoggerFactory.getLogger(TTWrapper.class);

  @Autowired
  private TTWEnvironmentConfiguration cfg;

  @Autowired
  private Weaver weaver;
  @Autowired
  private Redactor redactor;

  TTDigitalEnterpriseServerClient webClient;

  CachingTTWKnowledgeStore cacheManager;

  @PostConstruct
  void init() {
    this.webClient = initWebClient(cfg);

    this.cacheManager = initCacheManager(webClient, cfg);
  }

  protected CachingTTWKnowledgeStore initCacheManager(
      TTDigitalEnterpriseServerClient webClient,
      TTWEnvironmentConfiguration cfg) {
    return new CaffeineCacheManager(
        webClient,
        dox -> redactor.redact(weaver.weave(dox)),
        cfg);
  }

  protected TTDigitalEnterpriseServerClient initWebClient(TTWEnvironmentConfiguration cfg) {
    return new TTWebClient(cfg);
  }


  /**
   * methodName: applyTimestampToVersion
   * <p>
   * Combines a versionTag with a timestamp in a consistent way. TT allows to reuse version tags,
   * effectively not making the assumption that versions should be immutable.
   * </p>
   * <p>
   * Timestamps are used to reconcile the approaches, and differentiate between distinct (immutable)
   * "versions" of the same (mutable) "version"
   * </p>
   *
   * @param versionTag The version tag of the Trisotech model
   * @param timeStamp  The timestamp of the retrieved model
   * @return String
   */
  public static String applyTimestampToVersion(String versionTag, long timeStamp) {
    return versionTag + "-" + timeStamp;
  }

  /**
   * Gets the Trisotech Configuration
   *
   * @return TTWEnvironmentConfiguration
   */
  @Override
  public TTWEnvironmentConfiguration getConfig() {
    if (logger.isDebugEnabled()) {
      logger.debug("The current Trisotech configuration is {}", cfg);
    }
    return cfg;
  }


  /**
   * Returns the key Service Asset metadata, as per the TT KG, for a given BPM model carrier
   * <p>
   * (Assumption: one model may define 0..N inference services)
   *
   * @param modelUri The uri of the Trisotech model
   * @return SemanticFileInfo
   */
  @Override
  public Stream<SemanticModelInfo> getServicesMetadataByModelId(String modelUri) {
    return cacheManager.getServiceMetadataByModel(modelUri);
  }

  /**
   * methodName: getMetadataByAsset
   * <p>description: returns asset metadata from the cache</p>
   *
   * @param assetId The id of the asset
   * @return SemanticFileInfo if found
   */
  @Override
  public Stream<SemanticModelInfo> getMetadataByAssetId(UUID assetId, String assetVersionTag) {
    return cacheManager.getMetadataByAssetVersion(newKey(assetId, assetVersionTag));
  }


  /**
   * methodName: clearCache
   * <p>description: Resets the internal cache, forcing a reload</p>
   */
  @Override
  public void rescan() {
    cacheManager.invalidateCaches();
  }

  /**
   * @param placeId
   */
  @Override
  public void rescanPlace(String placeId) {
    cacheManager.invalidatePlaceCache(placeId);
  }

  /**
   * @param modelUri
   * @return
   */
  @Override
  public void rescanModel(String modelUri) {
    cacheManager.invalidateModelCache(modelUri);
  }

  /**
   * methodName: getModelById
   * <p>description: Retrieves the LATEST version of the model for the fileId provided.</p>
   *
   * @param fileId The file ID to identify the model to retrieve
   * @return XML Document for the model or Empty
   */
  @Override
  public Optional<Document> getModelById(String fileId) {
    return getMetadataByModelId(fileId)
        .flatMap(this::getModel);
  }

  /**
   * methodName: getModel
   * <p>description: Retrieves the latest version of the model for the fileId provided.</p>
   *
   * @param trisotechFileInfo the trisotechFileInfo for the model. can be null
   * @return the XML document
   */
  @Override
  public Optional<Document> getModel(
      TrisotechFileInfo trisotechFileInfo) {
    try {
      Optional<Document> document = cacheManager.downloadXmlModel(trisotechFileInfo);
      if (logger.isTraceEnabled() && document.isPresent()) {
        String documentToString = XMLUtil.toString(document.get());
        logger.trace("The document found from the Trisotech Rest Web Client is {}",
            documentToString);
      }
      return document;
    } catch (Exception e) {
      logger.error(String.format("%s %s", e.getMessage(), Arrays.toString(e.getStackTrace())), e);
    }
    return Optional.empty();
  }


  /**
   * methodName: getModelsFileInfo
   * <p>description: Get the FileInfo for all the (published) models from Trisotech of a given
   * type</p>
   *
   * @param xmlMimeType MimeType from the XML
   * @return List<TrisotechFileInfo>
   */
  @Override
  public Stream<SemanticModelInfo> listModels(@Nullable String xmlMimeType) {
    return cacheManager.listAllModelsInfoByMimeClass(xmlMimeType);
  }


  /**
   * methodName: getFileInfoByIdAndVersion
   * <p>description: Get the Trisotech fileInfo for the model and fileVersion provided. This will
   * NOT return the latest. For the latest, use getLatestModelInfo.</p>
   *
   * @param fileId      id of the file
   * @param fileVersion the fileVersion
   * @return Optional<TrisotechFileInfo> object for the file/fileVersion requested
   */
  @Override
  public Optional<TrisotechFileInfo> getMetadataByModelIdAndVersion(
      String fileId, String fileVersion) {
    return getMetadataByModelId(fileId)
        .map(TrisotechFileInfo.class::cast)
        .filter(trisotechFileInfo -> compareVersion(fileVersion, trisotechFileInfo))
        .or(() ->
            getVersionsMetadataByModelId(fileId).stream()
                .filter(trisotechFileInfo -> compareVersion(fileVersion, trisotechFileInfo))
                .findFirst());
  }

  /**
   * methodName: compareVersion
   * <p>description: Compare the version to the Trisotech file info</p>
   *
   * @param fileVersion       the fileVersion
   * @param trisotechFileInfo The info of the file being requested. Can be null.
   * @return boolean
   */
  private boolean compareVersion(String fileVersion, TrisotechFileInfo trisotechFileInfo) {
    if (fileVersion.equals(trisotechFileInfo.getVersion())) {
      return true;
    }
    if (trisotechFileInfo.getVersion() == null && !Util.isEmpty(fileVersion)) {
      return false;
    }
    Date artifactDate = Date.from(Instant.parse(trisotechFileInfo.getUpdated()));
    String timeStampedVersion =
        applyTimestampToVersion(toSemVer(trisotechFileInfo.getVersion()), artifactDate.getTime());
    return fileVersion.equals(timeStampedVersion);
  }

  /**
   * methodName: getLatestModelFileInfo
   * <p>description: Get the Trisotech file info for the latest version of a model.</p>
   *
   * @param modelId id of the model interested in
   * @return Optional<TrisotechFileInfo> for the model
   */
  @Override
  public Optional<SemanticModelInfo> getMetadataByModelId(String modelId) {
    var info = cacheManager.getMetadataByArtifact(modelId);
    if (info.isEmpty()) {
      logger.warn("Unable to get FileInfo for {}", modelId);
    }
    logger.trace("The info found from the cache manager is {}", info);
    return info;
  }

  /**
   * methodName: getModelByIdAndVersion
   * <p>description: get the model for the fileId and version specified</p>
   *
   * @param fileId  the file id for the model
   * @param version the version for the model
   * @return Optional<Document> the XML Document for the specified version of the model or Empty
   */
  @Override
  public Optional<Document> getModelByIdAndVersion(String fileId, String version) {
    return getVersionsMetadataByModelId(fileId).stream()
        .filter(fi -> version.equals(fi.getVersion()))
        .findFirst()
        .flatMap(this::getModel);
  }


  /**
   * methodName: getModelVersions
   * <p>returns all the file info for all the versions of the model for the default repository</p>
   *
   * @param modelUri The fileId for the model desired
   * @return List<TrisotechFileInfo> for all the versions for that model
   */
  @Override
  public List<TrisotechFileInfo> getVersionsMetadataByModelId(String modelUri) {
    var mostRecent = cacheManager.getMetadataByArtifact(modelUri);
    if (mostRecent.isEmpty()) {
      return Collections.emptyList();
    }
    var previous = webClient.getModelPreviousVersions(mostRecent.get().getPlaceId(), modelUri);
    List<TrisotechFileInfo> history = new ArrayList<>(1 + previous.size());
    mostRecent.ifPresent(history::add);
    history.addAll(previous);
    return history;
  }


  /**
   * Lists the Places available on the DES Server that the client has access to, based on the
   * token's grants
   *
   * @return a Map of place id / place name
   */
  @Override
  public Map<String, TrisotechPlace> listAccessiblePlaces() {
    try {
      return webClient.getPlaces()
          .map(tpd -> tpd.getData().stream()
              .collect(Collectors.toMap(
                  TrisotechPlace::getId,
                  tp -> tp
              )))
          .orElse(Collections.emptyMap());
    } catch (IOException e) {
      logger.error(e.getMessage(), e);
      return Collections.emptyMap();
    }
  }


  @Override
  public Map<TrisotechPlace, Set<String>> getCachedPlaceScopes() {
    return cacheManager.getConfiguredPlacePathScopes();
  }

  public Map<String, TrisotechPlace> getCacheablePlaces() {
    return cacheManager.getCachablePlaces().stream()
        .collect(Collectors.toMap(
            TrisotechPlace::getId,
            tp -> tp
        ));
  }

  /**
   * Lists the TrisotechExecutionArtifact available on a given execution environment
   *
   * @param env the name of the execution environment
   * @return the execution artifacts, indexed by name
   */
  @Override
  public Map<String, TrisotechExecutionArtifact> listExecutionArtifacts(String env) {
    try {
      return webClient.getExecutionArtifacts(env)
          .map(txd -> txd.getData().stream()
              .collect(Collectors.toMap(
                  TrisotechExecutionArtifact::getName,
                  x -> x
              )))
          .orElse(Collections.emptyMap());
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      return Collections.emptyMap();
    }
  }


  /**
   * Determines whether a Service is deployed to an execution environment
   * <p>
   * Note that this implementation targets one specific execution environment, which is configurable
   * but not at runtime. FUTURE?
   *
   * @param serviceName the internal name of the service
   * @param manifest    the artifact metadata of the model exposed as a service
   * @return a descriptor of the deployed artifact, if the service is deployed
   */
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


  public Optional<CachingTTWKnowledgeStore> getCacheManager() {
    return Optional.ofNullable(cacheManager);
  }
}
