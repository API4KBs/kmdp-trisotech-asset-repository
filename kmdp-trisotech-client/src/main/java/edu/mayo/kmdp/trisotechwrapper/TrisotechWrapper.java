/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.mayo.kmdp.trisotechwrapper;

import static edu.mayo.kmdp.trisotechwrapper.TrisotechApiUrls.BASE_URL;
import static edu.mayo.kmdp.trisotechwrapper.TrisotechApiUrls.CMMN_XML_MIMETYPE;
import static edu.mayo.kmdp.trisotechwrapper.TrisotechApiUrls.CONTENT_PATH;
import static edu.mayo.kmdp.trisotechwrapper.TrisotechApiUrls.DMN_XML_MIMETYPE;
import static edu.mayo.kmdp.trisotechwrapper.TrisotechApiUrls.MEA_TEST;
import static edu.mayo.kmdp.trisotechwrapper.TrisotechApiUrls.REPOSITORY_PATH;
import static edu.mayo.kmdp.trisotechwrapper.TrisotechApiUrls.TOKEN;
import static edu.mayo.kmdp.trisotechwrapper.TrisotechApiUrls.VERSIONS_PATH;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;

import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileData;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechPlace;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechPlaceData;
import edu.mayo.kmdp.util.XMLUtil;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import org.omg.spec.api4kp._1_0.identifiers.VersionIdentifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Class to wrap the calls to Trisotech in meaningful ways.
 */
public class TrisotechWrapper {

  private static Logger logger = LogManager.getLogger(TrisotechWrapper.class);
  private static final String PUBLISHED_STATE = "Published";
  // TODO: search for this particular directory or just use the known ID of our known repository and skip the places call? CAO
  // TODO: setup (environment?) so can use MEA-Test for development, but MEA for test?, int, prod CAO
  private static final String ROOT_DIRECTORY = "MEA-Test";

  private TrisotechWrapper() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Retrieves the LATEST version of the model for the fileId provided.
   *
   * @param fileId The file ID to identify the model to retrieve
   * @return XML Document for the model or Empty
   */
  public static Optional<Document> getModelById(String fileId) {
    return getModelById(fileId, null);
  }

  /**
   * Retrieves the latest version of the model for the fileId provided.
   *
   * @param fileId the id for the model file
   * @param trisotechFileInfo the fileinfo for the model. can be null
   * @return the XML document
   */
  public static Optional<Document> getModelById(String fileId,
      TrisotechFileInfo trisotechFileInfo) {
    try {
      if (null == trisotechFileInfo) {
        trisotechFileInfo = getFileInfo(fileId);
      }
      if (null != trisotechFileInfo) {
        return Optional.of(downloadXmlModel(trisotechFileInfo.getUrl()));
      }
    } catch (Exception e) { // TODO: Better exception handling. Do we have a preferred process? return empty here too? CAO
      logger.error(String.format("%s %s", e.getMessage(), e.getStackTrace()));
    }
    return Optional.empty();
  }

  /**
   * Retrieve the PUBLISHED Model given the file ID
   * Will only return if the model has been published, else empty.
   * TODO: Needed? CAO
   *
   * @param fileId the fileId for the file to find the model information
   * @return DMN XML Document or Optional.empty
   */
  public static Optional<Document> getPublishedModelById(String fileId) {
    return getPublishedModelById(fileId, null);
  }


  /**
   * Get the XML Document for the model.
   *
   * @param fileId the ID of the file being requested
   * @param trisotechFileInfo The info of the file being requested. Can be null.
   * @return The XML Document or Optional.empty if model is not published.
   */
  public static Optional<Document> getPublishedModelById(String fileId,
      TrisotechFileInfo trisotechFileInfo) {
    try {
      if (null == trisotechFileInfo) {
        trisotechFileInfo = getFileInfo(fileId);
      }
      if (publishedModel(trisotechFileInfo)) {
        return Optional.of(downloadXmlModel(trisotechFileInfo.getUrl()));
      }
    } catch (Exception e) { // TODO: Better exception handling. Do we have a preferred process? CAO
      logger.error(String.format("%s%s", e.getMessage(), e.getStackTrace()));
    }
    return Optional.empty();
  }

  /**
   * Get the FileInfo for all the DMN models from Trisotech
   *
   * @return List of all DMN model file info
   */
  public static List<TrisotechFileInfo> getDMNModelsFileInfo() {
    return getModels(DMN_XML_MIMETYPE);
  }

  /**
   * 'valid' models are those with a state of 'Published'
   * Retrieves all the published DMN models
   *
   * @return FileInfo for all the published DMN Models
   */
  public static List<TrisotechFileInfo> getPublishedDMNModelsFileInfo() {

    return getDMNModelsFileInfo().stream()
        .filter(TrisotechWrapper::publishedModel)
        .collect(Collectors.toList());
  }


  /**
   * 'valid' models are those with a state of 'Published'
   * Retrieves all the published CMMN models
   *
   * @return FileInfo for all the Published CMMN Models
   */
  public static List<TrisotechFileInfo> getPublishedCMMNModelsFileInfo() {

    return getCMMNModelsFileInfo().stream()
        .filter(TrisotechWrapper::publishedModel)
        .collect(Collectors.toList());
  }

  /**
   * get all the CMMN models
   *
   * @return FileInfo for all the CMMN Models
   */
  public static List<TrisotechFileInfo> getCMMNModelsFileInfo() {
    return getModels(CMMN_XML_MIMETYPE);
  }

  /**
   * data from the query of the repository IS the model info, HOWEVER, need to know the model TYPE
   * for the query to return the appropriate XML url in the FileInfo
   *
   * @param fileID Trisotech fileID used to query the repository
   * @return Trisotech FileInfo
   */
  public static TrisotechFileInfo getFileInfo(String fileID) {
    // get the mimeType first
    String mimeType = getFileInfoMimeType(fileID);
    // now the modelInfo will return with a URL that can be used to download XML file
    return getFileInfo(fileID, mimeType);
  }

  /**
   * get the ModelInfo for the fileID provided. mimeType is used to return the appropriate
   * information in a format for download
   * TODO: verify mimeType as expected ones?
   * Acceptable values for mimeType are: application/dmn-1-2+xml or application/cmmn-1-1+xml
   *
   * @param fileID the fileID for the model used to query from Trisotech
   * @param mimeType the mimeType to allow for XML-compatible URL
   * @return the Trisotech FileInfo about the model
   */
  public static TrisotechFileInfo getFileInfo(String fileID, String mimeType) {
    List<TrisotechFileInfo> fileInfos = getModels(mimeType);
    return fileInfos.stream()
        .filter(f -> f.getId().equals(fileID)).findAny()
        .orElse(null); // TODO: what to return if doesn't exist? CAO
  }

  /*** version support ***/

  /**
   * Gets the modelInfo for the version requested.
   *
   * @param fileId The ID for the file to be queried from Trisotech.
   * @param version the version of the file in Trisotech.
   * @return TrisotechFileInfo
   */
  public static TrisotechFileInfo getFileInfoByIdAndVersion(String fileId, String version) {
    // first get the mimetype to provide the correct XML-ready URL in the TrisotechFileInfo
    String mimeType = getFileInfoMimeType(fileId);
    return getFileInfoByIdAndVersion(fileId, version, mimeType);
  }

  /**
   * Trisotech by default returns JSON files. In order to get a XML-ready URL for downloading
   * XML-compliant model information, a mimeType must be provided in the query.
   * This method will be used when the mimeType is not provided initially. It will first query all
   * the models, then find the model to be queried and return the appropriate mimetype for that file.
   *
   * @param fileId the ID in Tristoech for the file to be queried
   * @return the mimetype for the file to be used to query needed information for download
   */
  private static String getFileInfoMimeType(String fileId) {
    List<TrisotechFileInfo> fileInfos = getModels(null);
    TrisotechFileInfo fileInfo = fileInfos.stream()
        .filter(f -> f.getId().equals(fileId)
        ).findAny().orElse(null); // TODO: better orElse value? CAO
    // fetch with the appropriate mimetype for XML download
    if (null != fileInfo) {

      // want to return the XML version of the file, so need the fileInfo based on mimetype
      // this has to do with how Trisotech returns the data;
      // we don't get the XML path unless we provide the correct mimetype in the query
      // contains is used as a test as the mimetype returned is not exactly what we need, but
      // can be used to determine which one to use
      if (fileInfo.getMimetype().contains("dmn")) {
        return DMN_XML_MIMETYPE;
      } else if (fileInfo.getMimetype().contains("cmmn")) {
        return CMMN_XML_MIMETYPE;
      } else {
        return null; // TODO: error? undefined mimetype?? shouldn't ever happen CAO
      }
    }
    return null; // TODO: error? undefined mimetype?? shouldn't ever happen CAO
  }

  /**
   * Get the Trisotech fileInfo for the model and version provided. This will NOT return the latest.
   * For the latest, use getLatestModelInfo.
   *
   * @param fileId id of the file
   * @param version the version
   * @param mimetype mimetype of the file
   * @return TrisotechFileInfo object for the file/version requested
   */
  public static TrisotechFileInfo getFileInfoByIdAndVersion(String fileId, String version,
      String mimetype) {
    List<TrisotechFileInfo> fileInfos = getModelVersions(fileId, mimetype);
    for (TrisotechFileInfo fileInfo : fileInfos) {
      if (version.equals(fileInfo.getVersion())) {
        return fileInfo;
      }
    }
    return null; // TODO: better return value? error? CAO
  }

  /**
   * Get the Trisotech file info for the latest version of a model.
   *
   * @param fileId id of the file for the model interested in
   * @return TrisotechFileInfo for the model
   */
  public static TrisotechFileInfo getLatestModelFileInfo(String fileId) {
    return getFileInfo(fileId);
  }

  /**
   * get the model for the fileId and version specified
   *
   * @param fileId the file id for the model
   * @param version the version for the model
   * @return the XML Document for the specified version of the model or Empty
   */
  public static Optional<Document> getModelByIdAndVersion(String fileId, String version) {
    String mimeType = getFileInfoMimeType(fileId);
    List<TrisotechFileInfo> fileInfos = getModelVersions(fileId, mimeType);

    // long form
    for (TrisotechFileInfo fileInfo : fileInfos) {
      if (version.equals(fileInfo.getVersion())) {
        return Optional.ofNullable(downloadXmlModel(fileInfo.getUrl()));
      }
    }
    return Optional.empty();
  }

  /**
   * returns all the file info for all the versions of the model for the default repository
   * EXCEPT latest version
   *
   * @param fileId The fileId for the model desired
   * @param mimetype The mimetype of the model requested; mimetype is needed to get back the information
   * so that it includes the correct URl for downloading the model version.
   * @return The list of Trisotech FileInfo for all the versions for that model
   */
  public static List<TrisotechFileInfo> getModelVersions(String fileId, String mimetype) {
    // TODO: This needs to be MEA_TEST for testing and MEA for production CAO
    return getModelVersions(MEA_TEST, fileId, mimetype);
  }

  /**
   * Will return all the file info for all the versions of the model in the specified repository requested
   * EXCEPT latest version is not included.
   * Don't expect users to know the ID of the repository, but should know the name.
   *
   * @param repositoryName - name of the repository holding the model
   * @param fileId - file id of the model requested
   * @return list of modelFileInfo for all but the latest version of the model
   */
  public static List<TrisotechFileInfo> getModelVersions(String repositoryName, String fileId,
      String mimetype) {
    List<TrisotechFileInfo> versions = new ArrayList<>();

    if (logger.isDebugEnabled()) {
      logger.debug(String
          .format("getModelVersions for model: %s in repository: %s with mimetype: %s", fileId,
              repositoryName, mimetype));
    }
    if(!DMN_XML_MIMETYPE.equals(mimetype) || !CMMN_XML_MIMETYPE.equals(mimetype)) {
      // not a valid mimetype, but maybe can determine
      if(mimetype.contains("dmn")) {
        mimetype = DMN_XML_MIMETYPE;
      } else if(mimetype.contains("cmmn")) {
        mimetype = CMMN_XML_MIMETYPE;
      } else {
        // TODO: error? invalid mimetype -- shoudln't happen CAO
        return versions;
      }
    }
    String repositoryId = getRepositoryId(repositoryName);
    URI uri;
    uri = UriComponentsBuilder.fromHttpUrl(BASE_URL
        + VERSIONS_PATH)
        .build(repositoryId, fileId, mimetype);

    logger.debug("uri string: {}", uri);
    // TODO: only unique versions? and then, only latest of each version? CAO
    getRepositoryContent(uri).getData().forEach((datum -> versions.add(datum.getFile())));

    return versions;
  }

  /**
   * Returns a VersionIdentifier of the model information for the artifactId provided.
   * This versionIdentifier will be for the latest version of the model.
   * TODO: Only return published versions? CAO
   *
   * @param artifactId the id for the artifact requesting latest version of
   * @return
   */
  public static VersionIdentifier getLatestVersion(String artifactId) {
    TrisotechFileInfo trisotechFileInfo = getFileInfo(artifactId);
    return getLatestVersion(trisotechFileInfo);
  }

  /**
   * Given a TrisotechFileInfo, return a versionIdentifier for the latest version of that file.
   * TODO: only return published latest version? CAO
   *
   * @param tfi file info for Trisotech file
   * @return VersionIdentifier
   */
  //return VersionIdentifier uid, versiontag (1.0.0) , create .withEstablishedOn for timestamp
  public static VersionIdentifier getLatestVersion(TrisotechFileInfo tfi) {
    if (null != tfi) {
      try {
        return new VersionIdentifier().withTag(tfi.getId())
            .withVersion(tfi.getVersion())
            .withEstablishedOn(
                DatatypeFactory.newInstance().newXMLGregorianCalendar(tfi.getUpdated()));
      } catch (DatatypeConfigurationException e) {
        logger.error(String.format("%s %s", e.getMessage(), e.getStackTrace()));
      }
    }
    return null; // TODO: better default return value? CAO
  }


  /**
   * Retrieve all the models based on the mimetype provided
   *
   * @param xmlMimetype the mimetype for the XML formatting of the URL in the TrisotechFileInfo
   * either DMN_XML_MIMETYPE or CMMN_XML_MIMETYPE
   */
  private static List<TrisotechFileInfo> getModels(String xmlMimetype) {
    List<TrisotechFileInfo> modelsArray = new ArrayList<>();
    try {

      TrisotechPlaceData data = getPlaces();

      // search for the 'place' as that is what modelers will know
      for (TrisotechPlace tp : data.getData()) {
        // pass in modelsArray as getRepositoryContent is recursive
        // TODO: MEA_TEST for testing and MEA for production - put in environment/config CAO
        if (tp.getName().equals(ROOT_DIRECTORY)) {
          getRepositoryContent(tp.getId(), modelsArray, "/", xmlMimetype);
        }
      }

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return modelsArray;
  }


  /**
   * Get the ID of the repository (aka directory|places|folder) in Trisotech
   *
   * @param repositoryName the name of the repository; users should know this, but may not know the ID
   * @return the ID for the repository requested
   */
  private static String getRepositoryId(String repositoryName) {
    try {
      TrisotechPlaceData data = getPlaces();
      for (TrisotechPlace tp : data.getData()) {
        if (tp.getName().equals(repositoryName)) {
          return tp.getId();
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return null;
  }

  /**
   * Files that have a 'Published' state are published and considered valid.
   *
   * @return List of Trisotech FileInfo for all published models
   */
  public static List<TrisotechFileInfo> getPublishedModelsFileInfo() {

    return getModels(null).stream()
        .filter(TrisotechWrapper::publishedModel)
        .collect(Collectors.toList());

  }

  /**
   * get the folders (places|directories) the application has access to
   *
   * @return Object that contains the list of places in a JSON format
   * @throws IOException if can't make the request
   */
  private static TrisotechPlaceData getPlaces() throws IOException {
    URL url = new URL(BASE_URL +
        REPOSITORY_PATH);

    HttpEntity<?> requestEntity = getHttpEntity();
    RestTemplate restTemplate = new RestTemplate();

    return
        restTemplate
            .exchange(url.toString(), HttpMethod.GET, requestEntity, TrisotechPlaceData.class)
            .getBody();

  }

  /**
   * Retrieve the content of a particular Trisotech repository/place/directory
   *
   * @param uri URI of the repository querying content from
   * @return object that contains a list of all the files and directories found in the requested
   * repository in a JSON format
   */
  private static TrisotechFileData getRepositoryContent(URI uri) {

    HttpEntity<?> requestEntity = getHttpEntity();
    RestTemplate restTemplate = new RestTemplate();
    // ******* NOTE: MUST send URI here to avoid further encoding, otherwise it will be double-encoded and request
    // will fail to return all the values expected ********
    return
        restTemplate.exchange(uri, HttpMethod.GET, requestEntity, TrisotechFileData.class)
            .getBody();
  }

  /**
   * get the content of the repository (place)
   * Content can include files and folders
   * This method will traverse folders to return only files.
   * CAUTION: while mimeType can be ignored, in order to retrieve XML files, the mimeType MUST be
   * set to a specific value and will only work for a specific type of file (DMN/CMMN) at a time
   *
   * @param directoryID directory/place/repository ID
   * @param modelsArray array of models found; method is recursive; this will contain the data upon return;
   * it is expected modelsArray will be initialized prior to call
   * @param path path of a folder
   * @param mimeType what type of files requesting from repository; no mimeType will retrieve all file types
   */
  private static void getRepositoryContent(String directoryID, List<TrisotechFileInfo> modelsArray,
      String path, String mimeType) {
    URI uri;
    try {
      // NOTE: MUST Use UriComponentBuilder to handle '+' in the MimeType, otherwise it will be
      // double-encoded and request will fail to return all values expected
      // See: https://docs.spring.io/spring/docs/current/spring-framework-reference/web.html#web-uri-encoding for details
      uri = UriComponentsBuilder.fromHttpUrl(BASE_URL
          + CONTENT_PATH)
          .build(directoryID, mimeType, path);

      TrisotechFileData fileData = getRepositoryContent(uri);
      fileData.getData().forEach(datum -> {
        if (null != datum.getFile()) {
          modelsArray.add(datum.getFile());
        } else { // assume folder?
          getRepositoryContent(directoryID, modelsArray, datum.getFolder().getPath(), mimeType);
        }
      });

    } catch (Exception e) {
      // TODO Auto-generated catch block
      throw new RuntimeException(e);
    }
  }

  /**
   * is the model published? models can be published, but not be in the state of 'Published'.
   * This will only return true for those models that are published AND in a state of 'Published'
   *
   * @param trisotechFileInfo the fileInfo for the model; will contain state if published
   * @return true/false
   */
  private static boolean publishedModel(TrisotechFileInfo trisotechFileInfo) {
    return (Optional.ofNullable(trisotechFileInfo.getState()).isPresent() &&
        trisotechFileInfo.getState().equals(PUBLISHED_STATE));
  }


  // TODO: How to handle bearer token? CAO
  private static HttpHeaders getHttpHeaders() {
    final HttpHeaders requestHeaders = new HttpHeaders();
    requestHeaders.add(ACCEPT, "application/json");
    requestHeaders.add(AUTHORIZATION, "Bearer " + TOKEN);
    requestHeaders.setContentType(APPLICATION_JSON_UTF8);
    return requestHeaders;
  }

  private static HttpEntity<?> getHttpEntity() {
    return new HttpEntity<>(getHttpHeaders());
  }

  /**
   * Download the actual model in its XML form.
   * Does NOT care if model is published or not
   *
   * @param fromUrl String representing an ENCODED URI
   * @return XML document
   */
  public static Document downloadXmlModel(String fromUrl) {
    try {

      URL url = new URL(fromUrl);
      HttpURLConnection conn = getHttpURLConnection(url);

      // using XMLUtil to load the XML Document properly sets up the document for
      // conversion by setting namespaceaware
      Optional<Document> document = XMLUtil.loadXMLDocument(conn.getInputStream());
      conn.disconnect();
      // TODO: better option orElse return? CAO
      return document.orElse(null);

    } catch (IOException e) {

      throw new RuntimeException(e);

    }

  }

  private static HttpURLConnection getHttpURLConnection(URL url) throws IOException {
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("GET");
    conn.setRequestProperty(ACCEPT, "application/json");
    conn.setRequestProperty(AUTHORIZATION, "Bearer " + TOKEN);

    conn.setDoInput(true);

    if (conn.getResponseCode() != 200) {

      switch (conn.getResponseCode()) {
        case 401:
          throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode()
              + String.format("<Token>: %s  ", TOKEN));

        default:
          throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
      }
    }
    return conn;
  }

}
