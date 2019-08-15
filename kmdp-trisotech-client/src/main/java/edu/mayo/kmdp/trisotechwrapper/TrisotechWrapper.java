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
   * Retrieves the LATEST version of the model for the modelId provided.
   *
   * @param modelId
   * @return
   */
  public static Optional<Document> getModelById(String modelId) {
    return getModelById(modelId, null);
  }

  /**
   * Retrieves the latest version of the model for the modelId provided.
   * Assumes trisostechFileInfo has the appropriate XML file info
   * TODO: make no assumptions about fileInfo? CAO
   *
   * @param modelId the id for the model file
   * @param trisotechFileInfo the fileinfo for the model
   * @return the XML document
   */
  public static Optional<Document> getModelById(String modelId,
      TrisotechFileInfo trisotechFileInfo) {
    try {
      if (null == trisotechFileInfo) {
        trisotechFileInfo = getModelInfo(modelId);
      }
      if (null != trisotechFileInfo) {
        return Optional.of(downloadXmlModel(trisotechFileInfo.getUrl()));
      }
    } catch (Exception e) { // TODO: Better exception handling. Do we have a preferred process? CAO
      logger.error(String.format("%s %s", e.getMessage(), e.getStackTrace()));
    }
    return Optional.empty();
  }

  /**
   * Retrieve the PUBLISHED Model given the model ID
   * Will only return if the model has been published, else empty.
   * TODO: Needed? CAO
   *
   * @param modelId
   * @return DMN XML Document
   */
  public static Optional<Document> getPublishedModelById(String modelId) {
    return getPublishedModelById(modelId, null);
  }


  public static Optional<Document> getPublishedModelById(String modelId,
      TrisotechFileInfo trisotechFileInfo) {
    try {
      if (null == trisotechFileInfo) {
        trisotechFileInfo = getModelInfo(modelId);
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
   * Get the DMN models from Trisotech
   *
   * @return List of all DMN model file info
   */
  public static List<TrisotechFileInfo> getDmnModels() {
    return getModels(DMN_XML_MIMETYPE);
  }

  /**
   * 'valid' models are those with a state of 'Published'
   * Retrieves all the published DMN models
   *
   * @return
   */
  public static List<TrisotechFileInfo> getPublishedDmnModels() {

    return getDmnModels().stream()
        .filter(TrisotechWrapper::publishedModel)
        .collect(Collectors.toList());
  }


  /**
   * 'valid' models are those with a state of 'Published'
   * Retrieves all the published CMMN models
   * @return
   */
  public static List<TrisotechFileInfo> getPublishedCMMNModels() {

    return getCmmnModels().stream()
        .filter(TrisotechWrapper::publishedModel)
        .collect(Collectors.toList());
  }

  /**
   * get all the CMMN models
   *
   * @return
   */
  public static List<TrisotechFileInfo> getCmmnModels() {
    return getModels(CMMN_XML_MIMETYPE);
  }

  /**
   * data from the query of the repository IS the model info, HOWEVER, need to know the model TYPE,
   * for the query to get the appropriate XML url
   *
   * @param modelID
   * @return
   */
  public static TrisotechFileInfo getModelInfo(String modelID) {
    String mimeType = getModelInfoMimeType(modelID);
    return getModelInfo(modelID, mimeType);
  }

  public static TrisotechFileInfo getModelInfo(String modelID, String mimeType) {
    List<TrisotechFileInfo> fileInfos = getModels(mimeType);
    return fileInfos.stream()
        .filter(f -> f.getId().equals(modelID)).findAny()
        .orElse(null); // TODO: what to return if doesn't exist? CAO
  }

  /*** version support ***/

  /**
   * Gets the modelInfo for the version requested.
   *
   * @param modelId
   * @param version
   * @return
   */
  public static TrisotechFileInfo getModelInfoByIdAndVersion(String modelId, String version) {
    String mimeType = getModelInfoMimeType(modelId);
    return getModelInfoByIdAndVersion(modelId, version, mimeType);
  }

  private static String getModelInfoMimeType(String modelId) {
    List<TrisotechFileInfo> fileInfos = getModels(null);
    TrisotechFileInfo fileInfo = fileInfos.stream()
        .filter(f -> f.getId().equals(modelId)
        ).findAny().orElse(null); // TODO: better orElse value? CAO
    // fetch with the appropriate mimetype for XML download
    if (null != fileInfo) {

      // want to return the XML version of the file, so need the fileInfo based on mimetype
      // this has to do with how Trisotech returns the data;
      // we don't get the XML path unless we provide the correct mimetype in the query
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
   * @param modelId id of the model
   * @param version the version
   * @param mimetype mimetype of the model
   * @return TrisotechFileInfo object for the model/version requested
   */
  public static TrisotechFileInfo getModelInfoByIdAndVersion(String modelId, String version,
      String mimetype) {
    List<TrisotechFileInfo> fileInfos = getModelVersions(modelId, mimetype);
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
   * @param modelId id of the model interested in
   * @return
   */
  public static TrisotechFileInfo getLatestModelInfo(String modelId) {
    return getModelInfo(modelId);
  }

  /**
   * get the model for the modelId and version specified
   *
   * @param modelId
   * @param version
   * @return
   */
  public static Optional<Document> getModelByIdAndVersion(String modelId, String version) {
    String mimeType = getModelInfoMimeType(modelId);
    List<TrisotechFileInfo> fileInfos = getModelVersions(modelId, mimeType);

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
   * @param modelId
   * @return
   */
  public static List<TrisotechFileInfo> getModelVersions(String modelId, String mimetype) {
    // TODO: This needs to be MEA_TEST for testing and MEA for production CAO
    return getModelVersions(MEA_TEST, modelId, mimetype);
  }

  /**
   * Will return all the file info for all the versions of the model in the specified repository requested
   * EXCEPT latest version is not included.
   * Don't expect users to know the ID of the repository, but should know the name.
   *
   * @param repositoryName - name of the repository holding the model
   * @param modelId - id of the model requested
   * @return list of modelFileInfo
   */
  public static List<TrisotechFileInfo> getModelVersions(String repositoryName, String modelId,
      String mimetype) {
    if (logger.isDebugEnabled()) {
      logger.debug(String
          .format("getModelVersions for model: %s in repository: %s with mimetype: %s", modelId,
              repositoryName, mimetype));
    }
    String repositoryId = getRepositoryId(repositoryName);
    URI uri;
    uri = UriComponentsBuilder.fromHttpUrl(BASE_URL
        + VERSIONS_PATH)
        .build(repositoryId, modelId, mimetype);

    logger.debug("uri string: {}", uri);
    List<TrisotechFileInfo> versions = new ArrayList<>();
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
    TrisotechFileInfo trisotechFileInfo = getModelInfo(artifactId);
    return getLatestVersion(trisotechFileInfo);
  }

  /**
   * Given a TrisotechFileInfo, return a versionIdentifier for the latest version of that file.
   * TODO: only return published latest version? CAO
   *
   * @param tfi file info for Trisotech file
   * @return VersionIdentifier
   */
  //return VersionIdentifier uid, versiontag (1.0) , create .withEstablishedOn for timestamp
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
   * @param xmlMimetype
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
   * @return
   */
  public static List<TrisotechFileInfo> getPublishedModels() {

    return getModels(null).stream()
        .filter(TrisotechWrapper::publishedModel)
        .collect(Collectors.toList());

  }

  /**
   * get the folders (places) the application has access to
   *
   * @return
   * @throws IOException
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
   * Retrieve the content of a particular Trisotech repository
   *
   * @param uri URI of the repository querying content from
   * @return
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
   * @param modelsArray array of models found; method is recursive
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
