/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
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
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
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
 *
 * The TTW points to a specific folder in a specific place, as dictated by configuration
 * This assumption could be generalized in the future
 */
@Component
public class TrisotechWrapper {

  public static final String BASE_NAMESPACE = "http://www.trisotech.com/definitions/_";

  private final Logger logger = LoggerFactory.getLogger(TrisotechWrapper.class);

  @Autowired
  private TTWEnvironmentConfiguration cfg;

  // Place in the DES that this Wrapper points to
  private String focusPlaceId;
  // (Folder) Path within the Place that this Wrapper points to
  private String targetPath;

  TTWebClient webClient;

  TTCacheManager cacheManager;

  /**
   *
   */
  public TrisotechWrapper() {
    // empty constructor
  }

  @PostConstruct
  void init() {
    this.webClient = new TTWebClient(cfg);

    this.targetPath = cfg.getPath();

    this.focusPlaceId = Optional.ofNullable(cfg.getRepositoryId())
        .or(() -> getRepositoryId(cfg.getRepositoryName()))
        .orElseThrow(() -> new IllegalStateException("Unable to determine target repository ID"));

    this.cacheManager = new TTCacheManager(webClient, cfg);

  }



  public TTWEnvironmentConfiguration getConfig() {
    return cfg;
  }

  public Map<String, Set<String>> getDependencyMap() {
    return cacheManager.getDependencyMap(focusPlaceId, targetPath);
  }

  public Map<TTGraphTerms, String> getMetadataByModel(String modelUri) {
    return cacheManager.getMetadataByModel(focusPlaceId, targetPath, modelUri);
  }

  public Map<TTGraphTerms, String> getMetadataByAsset(UUID assetId) {
    return cacheManager.getMetadataByAsset(focusPlaceId, targetPath, assetId);
  }


  /**
   * Retrieves the LATEST version of the model for the fileId provided.
   *
   * @param fileId The file ID to identify the model to retrieve
   * @return XML Document for the model or Empty
   */
  public Optional<Document> getModelById(String fileId) {
    return getFileInfo(fileId)
        .flatMap(this::getModel);
  }

  /**
   * Retrieves the latest version of the model for the fileId provided.
   *
   * @param trisotechFileInfo the fileinfo for the model. can be null
   * @return the XML document
   */
  public Optional<Document> getModel(
      TrisotechFileInfo trisotechFileInfo) {
    try {
      return webClient.downloadXmlModel(trisotechFileInfo.getUrl());
    } catch (Exception e) {
      logger.error(String.format("%s %s", e.getMessage(), Arrays.toString(e.getStackTrace())), e);
    }
    return Optional.empty();
  }

  /**
   * Retrieve the PUBLISHED Model given the file ID Will only return if the model has been
   * published, else empty.
   *
   * @param fileId the fileId for the file to find the model information
   * @return DMN XML Document or Optional.empty
   */
  public Optional<Document> getPublishedModelById(String fileId) {
    return getFileInfo(fileId)
        .flatMap(this::getPublishedModel);
  }


  /**
   * Get the XML Document for the model.
   *
   * @param trisotechFileInfo The info of the file being requested. Can be null.
   * @return The XML Document or Optional.empty if model is not published.
   */
  public Optional<Document> getPublishedModel(
      TrisotechFileInfo trisotechFileInfo) {
    try {
      if (publishedModel(trisotechFileInfo)) {
        return webClient.downloadXmlModel(trisotechFileInfo.getUrl());
      }
    } catch (Exception e) {
      logger.error(String.format("%s %s", e.getMessage(), Arrays.toString(e.getStackTrace())), e);
    }
    return Optional.empty();
  }

  /**
   * Get the FileInfo for all the (published) models from Trisotech of a given type
   *
   * @return List of all DMN model file info
   */
  public List<TrisotechFileInfo> getModelsFileInfo(String xmlMimeType, boolean publishedOnly) {
    return getModels(getXmlMimeType(xmlMimeType))
        .filter(x -> !publishedOnly || publishedModel(x))
        .collect(Collectors.toList());
  }


  /**
   * get the ModelInfo for the fileID provided. mimeType is used to return the appropriate
   * information in a format for download Acceptable values for mimeType are: DMN_XMl_MIMETYPE or
   * CMMN_XML_MIMETYPE
   *
   * @param modelId the fileID for the model used to query from Trisotech
   * @return the Trisotech FileInfo about the model
   */
  public Optional<TrisotechFileInfo> getFileInfo(String modelId) {
    Optional<TrisotechFileInfo> fileInfo =
        Optional.ofNullable(cacheManager.getModelInfo(focusPlaceId, targetPath, modelId));
    if (fileInfo.isEmpty()) {
      logger.error("Unable to get FileInfo for {}", modelId);
    }
    return fileInfo;
  }

  /* version support ***/

  /**
   * Gets the modelInfo for the version requested.
   *
   * @param fileUUID The ID for the file to be queried from Trisotech.
   * @param version  the version of the file in Trisotech.
   * @return TrisotechFileInfo
   */
  public Optional<TrisotechFileInfo> getFileInfoByIdAndVersion(
      UUID fileUUID, String version) {
    String fileId = BASE_NAMESPACE + fileUUID;
    // first get the mimetype to provide the correct XML-ready URL in the TrisotechFileInfo
    return getFileInfoByIdAndVersion(fileId, version);
  }

  /**
   * Trisotech by default returns JSON files. In order to get a XML-ready URL for downloading
   * XML-compliant model information, a mimeType must be provided in the query. This method will be
   * used when the mimeType is not provided initially. It will first query all the models, then find
   * the model to be queried and return the appropriate mimetype for that file.
   *
   * @param modelId the ID of the model in Trisotech
   * @return the mimetype for the file to be used to query needed information for download
   */
  private Optional<String> getFileInfoMimeType(String modelId) {
    // want to return the XML version of the file, so need the fileInfo based on mimetype
    // this has to do with how Trisotech returns the data
    // we don't get the XML path unless we provide the correct mimetype in the query.
    return getFileInfo(modelId)
        .map(info -> getXmlMimeType(info.getMimetype()));
  }


  /**
   * Get the Trisotech fileInfo for the model and fileVersion provided. This will NOT return the
   * latest. For the latest, use getLatestModelInfo.
   *
   * @param fileId      id of the file
   * @param fileVersion the fileVersion
   * @return TrisotechFileInfo object for the file/fileVersion requested
   */
  public Optional<TrisotechFileInfo> getFileInfoByIdAndVersion(
      String fileId, String fileVersion) {
    return getLatestModelFileInfo(fileId).filter(fileInfo -> compareVersion(fileVersion, fileInfo))
        .or(() ->
            getModelVersions(fileId).stream()
                .filter(fileInfo -> compareVersion(fileVersion, fileInfo))
                .findFirst());
  }

  private boolean compareVersion(String fileVersion, TrisotechFileInfo fileInfo) {
    if (fileVersion.equals(fileInfo.getVersion())) {
      return true;
    }
    if (fileInfo.getVersion() == null && !Util.isEmpty(fileVersion)) {
      return false;
    }
    Date artifactDate = Date.from(Instant.parse(fileInfo.getUpdated()));
    String timeStampedVersion = toSemVer(fileInfo.getVersion()) + "+" + artifactDate.getTime();
    return fileVersion.equals(timeStampedVersion);
  }

  /**
   * Get the Trisotech file info for the latest version of a model.
   *
   * @param modelId id of the model interested in
   * @return TrisotechFileInfo for the model
   */
  public Optional<TrisotechFileInfo> getLatestModelFileInfo(String modelId) {
    return getFileInfo(modelId);
  }

  /**
   * get the model for the fileId and version specified
   *
   * @param fileId  the file id for the model
   * @param version the version for the model
   * @return the XML Document for the specified version of the model or Empty
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
        return webClient.downloadXmlModel(fileInfo.getUrl());
      }
    }
    return Optional.empty();
  }

  /**
   * returns all the file info for all the versions of the model for the default repository
   *
   * @param modelUri The modelUri for the model desired so that it includes the correct URl for
   *               downloading the model version.
   * @return The list of Trisotech FileInfo for all the versions for that model
   */
  public List<TrisotechFileInfo> getModelVersions(String modelUri) {
    return getModelVersions(focusPlaceId, modelUri, getFileInfoMimeType(modelUri).orElseThrow());
  }

  /**
   * returns all the file info for all the versions of the model for the default repository
   *
   * @param modelUri   The modelUri for the model desired
   * @param mimeType The mimeType of the model requested; mimeType is needed to get back the
   *                 information so that it includes the correct URl for downloading the model
   *                 version.
   * @return The list of Trisotech FileInfo for all the versions for that model
   */
  public List<TrisotechFileInfo> getModelVersions(String modelUri, String mimeType) {
    return getModelVersions(focusPlaceId, modelUri, mimeType);
  }

  /**
   * returns all the file info for all the versions of the model for the default repository
   *
   * @param modelUri   The fileId for the model desired
   * @param mimeType The mimeType of the model requested; mimeType is needed to get back the
   *                 information so that it includes the correct URl for downloading the model
   *                 version.
   * @return The list of Trisotech FileInfo for all the versions for that model
   */
  public List<TrisotechFileInfo> getModelVersions(String repositoryId, String modelUri,
      String mimeType) {
    return webClient.getModelVersions(repositoryId, modelUri, mimeType);
  }

  /**
   * Returns a ResourceIdentifier of the model information for the artifactId provided. This
   * resourceIdentifier will be for the latest published version of the model.
   *
   * @param artifactId the id for the artifact requesting latest version of
   */
  public Optional<ResourceIdentifier> getLatestVersion(String artifactId) {
    return getFileInfo(artifactId)
        .flatMap(this::getLatestVersion);
  }

  /**
   * Given a TrisotechFileInfo, return a ResourceIdentifier for the latest version of that file.
   *
   * @param tfi file info for Trisotech file
   * @return ResourceIdentifier
   */
  //return ResourceIdentifier uid, versiontag (1.0.0) , create .withEstablishedOn for timestamp
  public Optional<ResourceIdentifier> getLatestVersion(TrisotechFileInfo tfi) {
    // only return for published models
    if (null != tfi.getState()) {
      logger.debug("tfiUpdated: {}", tfi.getUpdated());
      logger.debug("Date from tfiUpdated: {}", DateTimeUtil.parseDateTime(tfi.getUpdated()));

      // id is now the full path, only want the tag, minus the underscore
      String id = tfi.getId().substring(tfi.getId().lastIndexOf('/') + 1).replace("_", "");
      return Optional.of(SemanticIdentifier.newId(MAYO_ARTIFACTS_BASE_URI_URI, id, tfi.getVersion())
          .withEstablishedOn(DateTimeUtil.parseDateTime(tfi.getUpdated())));
    }
    logger.info("No published version for {}", tfi.getName());
    return Optional.empty();
  }

  /**
   * Retrieve all the models based on the mimetype provided
   *
   * @param xmlMimetype the mimetype for the XML formatting of the URL in the TrisotechFileInfo
   *                    either DMN_XML_MIMETYPE or CMMN_XML_MIMETYPE
   */
  private Stream<TrisotechFileInfo> getModels(String xmlMimetype) {
    return cacheManager.getModelInfos(focusPlaceId, targetPath).values()
        .stream()
        .filter(
            info -> xmlMimetype == null || xmlMimetype
                .equals(getXmlMimeType(info.getMimetype())));
  }

  /**
   * is the model published? models can be published, but not be in the state of 'Published'. All
   * models, of any state, that are published will return true here. (per meeting 8/21/2019)
   *
   * @param trisotechFileInfo the fileInfo for the model; will contain state and version if
   *                          published
   * @return true/false
   */
  private boolean publishedModel(TrisotechFileInfo trisotechFileInfo) {
    // using the new POST capabilities, it is possible to get a state and version of ""
    // which can mess up the search for models as the SPARQL query will not return
    // those values
    return isNotEmpty(trisotechFileInfo.getState())
        && isNotEmpty(trisotechFileInfo.getVersion());
  }

  public Optional<Document> downloadXmlModel(String fromUrl) {
    return webClient.downloadXmlModel(fromUrl);
  }

  public void tryDownloadXmlModel(String fromUrl) throws IOException, HttpException {
    webClient.tryDownloadXmlModel(fromUrl);
  }

  public void uploadXmlModel(String path, String name, String mimeType, String artifactVersionTag,
      String state, byte[] exemplar)
      throws IOException, HttpException {
    webClient.uploadXmlModel(focusPlaceId, path, name, mimeType, artifactVersionTag, state, exemplar);
  }

  /**
   * Get the ID of the repository (aka directory|places|folder) in Trisotech
   *
   * @param repositoryName the name of the repository; users should know this, but may not know the
   *                       ID
   * @return the ID for the repository requested
   */
  private Optional<String> getRepositoryId(String repositoryName) {
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
}
