package edu.mayo.kmdp.trisotechwrapper;

import static edu.mayo.kmdp.registry.Registry.MAYO_ARTIFACTS_BASE_URI_URI;
import static edu.mayo.kmdp.trisotechwrapper.config.TrisotechApiUrls.getXmlMimeType;
import static edu.mayo.kmdp.util.Util.isNotEmpty;
import static java.util.Collections.emptyList;
import static org.omg.spec.api4kp._20200801.id.VersionIdentifier.toSemVer;

import edu.mayo.kmdp.trisotechwrapper.components.TTCacheManager;
import edu.mayo.kmdp.trisotechwrapper.components.TTGraphTerms;
import edu.mayo.kmdp.trisotechwrapper.components.TTWebClient;
import edu.mayo.kmdp.trisotechwrapper.config.TTWEnvironmentConfiguration;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechPlace;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechPlaceData;
import edu.mayo.kmdp.util.DateTimeUtil;
import edu.mayo.kmdp.util.Util;
import edu.mayo.kmdp.util.XMLUtil;
import java.io.IOException;
import java.time.Instant;
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
import javax.annotation.PostConstruct;
import org.apache.http.HttpException;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

/**
 * Class to wrap the calls to Trisotech in meaningful ways.
 * <p>
 * The TTW points to a specific folder in a specific place, as dictated by configuration This
 * assumption could be generalized in the future </p> Copyright © 2018 Mayo Clinic
 */
@Component
public class TrisotechWrapper {

  public static final String BASE_NAMESPACE = "http://www.trisotech.com/definitions/_";

  private final Logger logger = LoggerFactory.getLogger(TrisotechWrapper.class);

  @Autowired
  private TTWEnvironmentConfiguration trisoWrapperEnvironmentConfiguration;

  // Place in the DES that this Wrapper points to
  private String focusPlaceId;
  // (Folder) Path within the Place that this Wrapper points to
  private String targetPath;

  TTWebClient webClient;

  TTCacheManager cacheManager;


  @PostConstruct
  void init() {
    this.webClient = new TTWebClient(trisoWrapperEnvironmentConfiguration);

    this.targetPath = trisoWrapperEnvironmentConfiguration.getPath();

    this.focusPlaceId = Optional.ofNullable(trisoWrapperEnvironmentConfiguration.getRepositoryId())
        .or(() -> getRepositoryId(trisoWrapperEnvironmentConfiguration.getRepositoryName()))
        .orElseThrow(() -> new IllegalStateException("Unable to determine target repository ID"));

    this.cacheManager = new TTCacheManager(webClient, trisoWrapperEnvironmentConfiguration);

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
  public TTWEnvironmentConfiguration getConfig() {
    if (logger.isDebugEnabled()) {
      logger
          .debug("The current Trisotech configuration is {}", trisoWrapperEnvironmentConfiguration);
    }
    return trisoWrapperEnvironmentConfiguration;
  }

  /**
   * methodName: getDependencyMap
   * <p>description: returns dependency map from the cache</p>
   *
   * @return Map<String, Set < String>>
   */
  public Map<String, Set<String>> getDependencyMap() {
    return cacheManager.getDependencyMap(focusPlaceId, targetPath);
  }

  /**
   * methodName: getMetadataByModel
   * <p>description: returns model metadata from the cache</p>
   *
   * @param modelUri The uri of the Trisotech model
   * @return Map<TTGraphTerms, String>
   */
  public Map<TTGraphTerms, String> getMetadataByModel(String modelUri) {
    return cacheManager.getMetadataByModel(focusPlaceId, targetPath, modelUri);
  }

  /**
   * methodName: getMetadataByAsset
   * <p>description: returns asset metadata from the cache</p>
   *
   * @param assetId The Id of the asset
   * @return Map<TTGraphTerms, String>
   */
  public Map<TTGraphTerms, String> getMetadataByAsset(UUID assetId) {
    return cacheManager.getMetadataByAsset(focusPlaceId, targetPath, assetId);
  }


  /**
   * methodName: clearCache
   * <p>description: Resets the internal cache, forcing a reload</p>
   *
   * @return boolean
   */
  public boolean clearCache() {
    return cacheManager.clearCache();
  }


  /**
   * methodName: getModelById
   * <p>description: Retrieves the LATEST version of the model for the fileId provided.</p>
   *
   * @param fileId The file ID to identify the model to retrieve
   * @return XML Document for the model or Empty
   */
  public Optional<Document> getModelById(String fileId) {
    return getFileInfo(fileId)
        .flatMap(this::getModel);
  }

  /**
   * methodName: getModel
   * <p>description: Retrieves the latest version of the model for the fileId provided.</p>
   *
   * @param trisotechFileInfo the trisotechFileInfo for the model. can be null
   * @return the XML document
   */
  public Optional<Document> getModel(
      TrisotechFileInfo trisotechFileInfo) {
    try {
      Optional<Document> document = webClient.downloadXmlModel(trisotechFileInfo.getUrl());
      if (logger.isDebugEnabled() && document.isPresent()) {
        String documentToString = XMLUtil.toString(document.get());
        logger
            .debug("The document found from the Trisotech Rest Web Client is {}", documentToString);
      }
      return document;
    } catch (Exception e) {
      logger.error(String.format("%s %s", e.getMessage(), Arrays.toString(e.getStackTrace())), e);
    }
    return Optional.empty();
  }

  /**
   * methodName: getPublishedModelById
   * <p>description: Retrieve the PUBLISHED Model given the file ID Will only return if the model
   * has been published, else empty.</p>
   *
   * @param fileId the fileId for the file to find the model information
   * @return DMN XML Document or Optional.empty
   */
  public Optional<Document> getPublishedModelById(String fileId) {
    return getFileInfo(fileId)
        .flatMap(this::getPublishedModel);
  }


  /**
   * methodName: getPublishedModel
   * <p>description: Get the XML Document for the model.</p>
   *
   * @param trisotechFileInfo The info of the file being requested. Can be null.
   * @return The XML Document or Optional.empty if model is not published.
   */
  public Optional<Document> getPublishedModel(
      TrisotechFileInfo trisotechFileInfo) {
    try {
      if (publishedModel(trisotechFileInfo)) {
        Optional<Document> document = webClient.downloadXmlModel(trisotechFileInfo.getUrl());
        if (logger.isDebugEnabled() && document.isPresent()) {
          String documentToString = XMLUtil.toString(document.get());
          logger
              .debug("The published document found from the Trisotech Rest Web Client is {}",
                  documentToString);
        }
        return document;
      }
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
   * @param xmlMimeType   MimeType from the XML
   * @param publishedOnly If to return published model only
   * @return List<TrisotechFileInfo>
   */
  public List<TrisotechFileInfo> getModelsFileInfo(String xmlMimeType, boolean publishedOnly) {
    return getModels(getXmlMimeType(xmlMimeType))
        .filter(x -> !publishedOnly || publishedModel(x))
        .collect(Collectors.toList());
  }

  /**
   * methodName: getFileInfo
   * <p>description: get the ModelInfo for the fileID provided. mimeType is used to return the
   * appropriate information in a format for download Acceptable values for mimeType are:
   * DMN_XMl_MIMETYPE or CMMN_XML_MIMETYPE </p>
   *
   * @param modelId the fileID for the model used to query from Trisotech
   * @return Optional<TrisotechFileInfo> the Trisotech FileInfo about the model
   */
  public Optional<TrisotechFileInfo> getFileInfo(String modelId) {
    Optional<TrisotechFileInfo> trisotechFileInfo =
        Optional.ofNullable(cacheManager.getModelInfo(focusPlaceId, targetPath, modelId));
    if (trisotechFileInfo.isEmpty()) {
      logger.error("Unable to get FileInfo for {}", modelId);
    }
    if (logger.isDebugEnabled()) {
      logger
          .debug("The trisotechFileInfo found from the cache manager is {}",
              trisotechFileInfo);
    }
    return trisotechFileInfo;
  }

  /**
   * methodName: getFileInfoByIdAndVersion
   * <p>description: Gets the modelInfo for the version requested.</p>
   *
   * @param fileUUID The ID for the file to be queried from Trisotech.
   * @param version  the version of the file in Trisotech.
   * @return Optional<TrisotechFileInfo>
   */
  public Optional<TrisotechFileInfo> getFileInfoByIdAndVersion(
      UUID fileUUID, String version) {
    String fileId = BASE_NAMESPACE + fileUUID;
    // first get the mimetype to provide the correct XML-ready URL in the TrisotechFileInfo
    return getFileInfoByIdAndVersion(fileId, version);
  }

  /**
   * methodName: getFileInfoMimeType
   * <p>description: Trisotech by default returns JSON files. In order to get a XML-ready URL for
   * downloading XML-compliant model information, a mimeType must be provided in the query. This
   * method will be used when the mimeType is not provided initially. It will first query all the
   * models, then find the model to be queried and return the appropriate mimetype for that
   * file.</p>
   *
   * @param modelId the ID of the model in Trisotech
   * @return Optional<String> the mimetype for the file to be used to query needed information for
   * download
   */
  private Optional<String> getFileInfoMimeType(String modelId) {
    // want to return the XML version of the file, so need the fileInfo based on mimetype
    // this has to do with how Trisotech returns the data
    // we don't get the XML path unless we provide the correct mimetype in the query.
    return getFileInfo(modelId)
        .map(info -> getXmlMimeType(info.getMimetype()));
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
  public Optional<TrisotechFileInfo> getFileInfoByIdAndVersion(
      String fileId, String fileVersion) {
    return getLatestModelFileInfo(fileId)
        .filter(trisotechFileInfo -> compareVersion(fileVersion, trisotechFileInfo))
        .or(() ->
            getModelVersions(fileId).stream()
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
  public Optional<TrisotechFileInfo> getLatestModelFileInfo(String modelId) {
    return getFileInfo(modelId);
  }

  /**
   * methodName: getModelByIdAndVersion
   * <p>description: get the model for the fileId and version specified</p>
   *
   * @param fileId  the file id for the model
   * @param version the version for the model
   * @return Optional<Document> the XML Document for the specified version of the model or Empty
   */
  public Optional<Document> getModelByIdAndVersion(String fileId, String version) {
    Optional<String> mimeType = getFileInfoMimeType(fileId);
    if (mimeType.isEmpty()) {
      return Optional.empty();
    }
    List<TrisotechFileInfo> fileInfos = getModelVersions(fileId, mimeType.get());

    // long form
    for (TrisotechFileInfo fileInfo : fileInfos) {
      if (version.equals(fileInfo.getVersion())) {
        Optional<Document> document = webClient.downloadXmlModel(fileInfo.getUrl());
        if (logger.isDebugEnabled() && document.isPresent()) {
          String documentToString = XMLUtil.toString(document.get());
          logger
              .debug(
                  "The model document found with a given version from the Trisotech Rest Web Client is {}",
                  documentToString);
        }
        return document;
      }
    }
    return Optional.empty();
  }

  /**
   * methodName: getModelVersions
   * <p>description: returns all the file info for all the versions of the model for the default
   * repository</p>
   *
   * @param modelUri The modelUri for the model desired so that it includes the correct URl for
   *                 downloading the model version.
   * @return List<TrisotechFileInfo for all the versions for that model
   */
  public List<TrisotechFileInfo> getModelVersions(String modelUri) {
    return getModelVersions(focusPlaceId, modelUri, getFileInfoMimeType(modelUri).orElseThrow());
  }

  /**
   * methodName: getModelVersions
   * <p>description: returns all the file info for all the versions of the model for the default
   * repository</p>
   *
   * @param modelUri The modelUri for the model desired
   * @param mimeType The mimeType of the model requested; mimeType is needed to get back the
   *                 information so that it includes the correct URl for downloading the model
   *                 version.
   * @return List<TrisotechFileInfo> for all the versions for that model
   */
  public List<TrisotechFileInfo> getModelVersions(String modelUri, String mimeType) {
    return getModelVersions(focusPlaceId, modelUri, mimeType);
  }

  /**
   * methodName: getModelVersions
   * <p>returns all the file info for all the versions of the model for the default repository</p>
   *
   * @param modelUri The fileId for the model desired
   * @param mimeType The mimeType of the model requested; mimeType is needed to get back the
   *                 information so that it includes the correct URl for downloading the model
   *                 version.
   * @return List<TrisotechFileInfo> for all the versions for that model
   */
  public List<TrisotechFileInfo> getModelVersions(String repositoryId, String modelUri,
      String mimeType) {
    return webClient.getModelVersions(repositoryId, modelUri, mimeType);
  }

  /**
   * methodName: getLatestVersion
   * <p>description: Returns a ResourceIdentifier of the model information for the artifactId
   * provided. This resourceIdentifier will be for the latest published version of the model.</p>
   *
   * @param artifactId the id for the artifact requesting latest version of
   * @return Optional<ResourceIdentifier>
   */
  public Optional<ResourceIdentifier> getLatestVersion(String artifactId) {
    return getFileInfo(artifactId)
        .flatMap(this::getLatestVersion);
  }

  /**
   * methodName: getLatestVersion
   * <p>description: Given a TrisotechFileInfo, return a ResourceIdentifier for the latest version
   * of that file.</p>
   *
   * @param trisotechFileInfo The info of the file being requested. Can be null.
   * @return Optional<ResourceIdentifier>
   */
  //return ResourceIdentifier uid, versionTag (1.0.0) , create .withEstablishedOn for timestamp
  public Optional<ResourceIdentifier> getLatestVersion(TrisotechFileInfo trisotechFileInfo) {
    // only return for published models
    if (null != trisotechFileInfo.getState()) {
      if (logger.isDebugEnabled()) {
        logger.debug("Trisotech file info updated is: {}", trisotechFileInfo.getUpdated());
        logger.debug("Date from tfiUpdated: {}",
            DateTimeUtil.parseDateTime(trisotechFileInfo.getUpdated()));
      }

      // id is now the full path, only want the tag, minus the underscore
      String id = trisotechFileInfo.getId()
          .substring(trisotechFileInfo.getId().lastIndexOf('/') + 1).replace("_", "");
      return Optional.of(SemanticIdentifier
          .newId(MAYO_ARTIFACTS_BASE_URI_URI, id, trisotechFileInfo.getVersion())
          .withEstablishedOn(DateTimeUtil.parseDateTime(trisotechFileInfo.getUpdated())));
    }
    logger.info("No published version for {}", trisotechFileInfo.getName());
    return Optional.empty();
  }

  /**
   * methodName: getModels
   * <p>description: Retrieve all the models based on the mimetype provided</p>
   *
   * @param xmlMimetype the mimetype for the XML formatting of the URL in the TrisotechFileInfo
   *                    either DMN_XML_MIMETYPE or CMMN_XML_MIMETYPE.
   * @return Stream<TrisotechFileInfo>
   */
  private Stream<TrisotechFileInfo> getModels(String xmlMimetype) {
    return cacheManager.getModelInfos(focusPlaceId, targetPath).values()
        .stream()
        .filter(
            info -> xmlMimetype == null || xmlMimetype
                .equals(getXmlMimeType(info.getMimetype())));
  }

  /**
   * methodName: publishedModel
   * <p>description: is the model published? models can be published, but not be in the state of
   * 'Published'. All models, of any state, that are published will return true here.</p>
   *
   * @param trisotechFileInfo the fileInfo for the model; will contain state and version if
   *                          published
   * @return boolean
   */
  private boolean publishedModel(TrisotechFileInfo trisotechFileInfo) {
    // using the new POST capabilities, it is possible to get a state and version of ""
    // which can mess up the search for models as the SPARQL query will not return
    // those values
    return isNotEmpty(trisotechFileInfo.getState())
        && isNotEmpty(trisotechFileInfo.getVersion());
  }

  /**
   * methodName: downloadXmlModel
   * <p>description: downloads the xml of the Trisotech model</p>
   *
   * @param fromUrl URL where to request from the client
   * @return Optional<Document>
   */
  public Optional<Document> downloadXmlModel(String fromUrl) {
    Optional<Document> document = webClient.downloadXmlModel(fromUrl);
    if (logger.isDebugEnabled() && document.isPresent()) {
      String documentToString = XMLUtil.toString(document.get());

      logger.debug("The URL passed to download XML from is {}", fromUrl);
      logger.debug("The document XML found is {}", documentToString);

    }
    return document;
  }

  /**
   * methodName: tryDownloadXmlModel
   * <p>description: From Trisotech rest client try to download XML Model</p>
   *
   * @param fromUrl URL where to request from the client
   * @throws IOException   Input/Output exception
   * @throws HttpException Web based exception
   */
  public void tryDownloadXmlModel(String fromUrl) throws IOException, HttpException {
    webClient.tryDownloadXmlModel(fromUrl);
  }

  /**
   * methodName: uploadXmlModel
   * <p>description: Upload XML Model to the Trisotech Server</p>
   *
   * @param path               path to upload
   * @param name               name to upload
   * @param mimeType           The mimeType of the model requested; mimeType is needed to get back
   *                           the *                 information so that it includes the correct URl
   *                           for downloading the model *                 version.
   * @param artifactVersionTag artifact version to upload
   * @param state              state
   * @param exemplar           exemplar
   * @throws IOException   Input/Output exception
   * @throws HttpException Web based exception
   */
  public void uploadXmlModel(String path, String name, String mimeType, String artifactVersionTag,
      String state, byte[] exemplar)
      throws IOException, HttpException {
    webClient
        .uploadXmlModel(focusPlaceId, path, name, mimeType, artifactVersionTag, state, exemplar);
  }

  /**
   * methodName: getRepositoryId
   * <p>description: Get the ID of the repository (aka directory|places|folder) in Trisotech
   *
   * @param repositoryName the name of the repository; users should know this, but may not know the
   *                       ID </p>
   * @return Optional<String> the ID for the repository requested
   */
  public Optional<String> getRepositoryId(String repositoryName) {
    try {
      return webClient.getPlaces()
          .map(TrisotechPlaceData::getData)
          .orElse(emptyList())
          .stream()
          .filter(tp -> tp.getName().equals(repositoryName))
          .map(TrisotechPlace::getId)
          .findFirst();
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    }
    return Optional.empty();
  }

  /**
   * Lists the Places available on the DES Server
   * @return a Map of place id / place name
   */
  public Map<String,String> listPlaces() {
    try {
      return webClient.getPlaces()
          .map(tpd -> tpd.getData().stream()
              .collect(Collectors.toMap(
                  TrisotechPlace::getId,
                  TrisotechPlace::getName
              )))
          .orElse(Collections.emptyMap());
    } catch (IOException e) {
      logger.error(e.getMessage(), e);
      return Collections.emptyMap();
    }
  }
}
