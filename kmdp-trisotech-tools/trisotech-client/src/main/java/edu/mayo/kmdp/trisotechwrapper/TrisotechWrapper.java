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

import edu.mayo.kmdp.trisotechwrapper.models.*;
import edu.mayo.kmdp.util.XMLUtil;
import org.apache.http.client.methods.HttpUriRequest;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.stream.Collectors;

import static edu.mayo.kmdp.trisotechwrapper.TrisotechApiUrls.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.*;

/**
 * Class to wrap the calls to Trisotech in meaningful ways.
 */
public class TrisotechWrapper {
  private static final String PUBLISHED_STATE = "Published";
  // TODO: search for this particular directory or just use the known ID of our known repository and skip the places call? CAO
  // TODO: setup (environment?) so can use MEA-Test for development, but MEA for test?, int, prod CAO
  private static final String ROOT_DIRECTORY = "MEA-Test";


  // TODO: Where to put bearer token so protected? CAO
//  private static TrisotechApiToken token;


  /**
   * Retrieves the LATEST version of the model for the modelId provided.
   *
   * @param modelId
   * @return
   */
  public static Optional<Document> getModelById(String modelId) {
    return getModelById(modelId, null);
  }

  public static Optional<Document> getModelById(String modelId, TrisotechFileInfo trisotechFileInfo) {
    try {
      if (null == trisotechFileInfo) {
        trisotechFileInfo = getModelInfo(modelId);
      }
      return Optional.of(downloadXmlModel(trisotechFileInfo.getUrl()));
    } catch (Exception e) { // TODO: Better exception handling. Do we have a preferred process? CAO
      e.printStackTrace();
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


  public static Optional<Document> getPublishedModelById(String modelId, TrisotechFileInfo trisotechFileInfo) {
    try {
      if (null == trisotechFileInfo) {
        trisotechFileInfo = getModelInfo(modelId);
      }
      if (publishedModel(trisotechFileInfo)) {
        return Optional.of(downloadXmlModel(trisotechFileInfo.getUrl()));
      }
    } catch (Exception e) { // TODO: Better exception handling. Do we have a preferred process? CAO
      e.printStackTrace();
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
        .filter((model) ->  publishedModel(model))
        .collect(Collectors.toList());
  }


  /**
   * 'valid' models are those with a state of 'Published'
   * Retrieves all the published CMMN models
   * @return
   */
  public static List<TrisotechFileInfo> getPublishedCMMNModels() {

    return getCmmnModels().stream()
        .filter((model) -> publishedModel(model))
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
   * data from the query of the repository IS the model info, HOWEVER, if don't know the model TYPE,
   * then need to query again to get the appropriate XML url
   *
   * @param modelID
   * @return
   */
  public static TrisotechFileInfo getModelInfo(String modelID) {
    List<TrisotechFileInfo> fileInfos = getModels(null);
    TrisotechFileInfo file = fileInfos.stream()
        .filter((f) -> f.getId().equals(modelID)).findAny().get();
    // fetch with the appropriate mimetype for XML download
    if (file.getMimetype().contains("dmn")) {
      fileInfos = getModels(DMN_XML_MIMETYPE);
    } else {
      fileInfos = getModels(CMMN_XML_MIMETYPE);
    }

    return fileInfos.stream()
        .filter((f) -> f.getId().equals(modelID)).findAny().get();
  }


  /*** version support ***/

  /**
   * Gets the modelInfo for the version requested.
   * NOTE: Until updates from Trisotech, the URL will not be correct to retrieve XML document
   * TODO: may need updates to code once have updates from Trisotech CAO
   *
   * @param modelId
   * @param version
   * @return
   */
  public static TrisotechFileInfo getModelInfoByIdAndVersion(String modelId, String version) {
    // TODO: figure this out CAO
//    return getModelVersions(modelId).stream()
//        .filter((f) -> f.getVersion().equals(version)).map();

    // long form
    List<TrisotechFileInfo> fileInfos = getModelVersions(modelId, null);
    for (TrisotechFileInfo fileInfo : fileInfos) {
      if(version.equals(fileInfo.getVersion())) {
        return fileInfo;
      }
    }
    return null;
  }


  /**
   * get the model for the modelId and version specified
   * TODO: may need to update once have updates from Trisotech to return XML CAO
   *
   * @param modelId
   * @param version
   * @return
   */
  public static Optional<Document> getModelByIdAndVersion(String modelId, String version) {
    // TODO: Figure this out - failing on findAny CAO
//    TrisotechFileInfo fileInfo =  getModelVersions(modelId).stream()
//        .filter((f) -> f.getVersion().equals(version))
//        .findAny().orElse(null);
    List<TrisotechFileInfo> fileInfos = new ArrayList<>();
    // first get the modelInfo by modelId
    // this provides the data needed to know what type of file it is
    TrisotechFileInfo modelInfo = getModelInfo(modelId);
    if(modelInfo.getMimetype().contains("dmn")) {
      fileInfos = getModelVersions(modelId, DMN_XML_MIMETYPE);
    } else { // CMMN
      fileInfos = getModelVersions(modelId, CMMN_XML_MIMETYPE);
    }
    // long form
    for (TrisotechFileInfo fileInfo : fileInfos
         ) {
      if(version.equals(fileInfo.getVersion())) {
        // TODO: this will fail until Trisotech updates to return by XML CAO
        return Optional.of(downloadXmlModel(fileInfo.getUrl()));
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
  public static List<TrisotechFileInfo> getModelVersions(String repositoryName, String modelId, String mimetype) {
    System.out.println("getModelVersions for model: " + modelId + " in repository: " + repositoryName + " with mimetype: " + mimetype);
    String repositoryId = getRepositoryId(repositoryName);
    String urlString = BASE_URL + String.format(VERSIONS_PATH, repositoryId, modelId, mimetype);
    System.out.println("url string: " + urlString);
    List<TrisotechFileInfo> versions = new ArrayList<>();
    HttpEntity<?> requestEntity = getHttpEntity();
    RestTemplate restTemplate = new RestTemplate();
    // ********* NOTE: MUST send URI here to avoid further encoding, otherwise it will be double-encoded and request
    // will fail to return all the values expected **********
    TrisotechFileData tfd =
        restTemplate.exchange(urlString, HttpMethod.GET, requestEntity, TrisotechFileData.class).getBody();

    // TODO: only unique versions? and then, only latest of each version? CAO
    tfd.getData().stream().forEach((datum -> versions.add(datum.getFile())));

    return versions;
  }

  /**
   * return the URL for the latest revision of the given model
   * TODO: really return URL? Signavio only returned the id of the version, but for Trisotech the id doesn't change
   * TODO cont: returning URL, address with Davide if that is desired CAO
   * <p>
   *
   * @param artifactId
   * @return
   */
  // TODO: These 2 will go away when getLatestVersion updated to return VersionedIdentifier CAO
  public static String getLatestVersion(String artifactId) {
    TrisotechFileInfo trisotechFileInfo = getModelInfo(artifactId);
    return getLatestVersion(trisotechFileInfo);
  }

  public static String getLatestVersionTag(String artifactId) {
    TrisotechFileInfo trisotechFileInfo = getModelInfo(artifactId);
    return trisotechFileInfo.getVersion();
  }


  //return VersionedIdentifier uid, versiontag (1.0) , create .withEstablishedOn for timestamp TODO: CAO
  public static String getLatestVersion(TrisotechFileInfo tfi) {
    TrisotechFileInfo trisotechFileInfo;
    // want to get the XML URL for this file
    if (tfi.getMimetype().contains("cmmn")) {
      trisotechFileInfo = getCmmnModel(tfi.getId());
    } else { // dmn
      trisotechFileInfo = getDmnModel(tfi.getId());
    }
    return trisotechFileInfo.getUrl();
  }


  /**
   * Retrieve all the models based on the mimetype provided
   * @param xmlMimetype
   */
  private static List<TrisotechFileInfo> getModels( String xmlMimetype ) {
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
      for(TrisotechPlace tp : data.getData()) {
        if(tp.getName().equals(repositoryName)) {
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
//    List<TrisotechFileInfo> publishedModels = new ArrayList<>();

    // TODO: can do better streaming here -- direct from getModels... CAO
    return getModels( null).stream()
        .filter((model) -> publishedModel(model))
        .collect(Collectors.toList());
//    return publishedModels.stream()
//        .filter((model) -> publishedModel(model))
//        .collect(Collectors.toList());

//    for (TrisotechFileInfo trisotechFileInfo : models) {
//      if (publishedModel(trisotechFileInfo)) {
//        publishedModels.add(trisotechFileInfo);
//      }
//    }
//    return publishedModels;
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
        restTemplate.exchange(url.toString(), HttpMethod.GET, requestEntity, TrisotechPlaceData.class).getBody();

  }

  /**
   * Retrieve the content of a particular Trisotech repository
   *
   * @param uri
   * @return
   * @throws IOException
   */
  private static TrisotechFileData getRepositoryContent(URI uri) throws IOException {

    HttpEntity<?> requestEntity = getHttpEntity();
    RestTemplate restTemplate = new RestTemplate();
    // ******* NOTE: MUST send URI here to avoid further encoding, otherwise it will be double-encoded and request
    // will fail to return all the values expected ********
    TrisotechFileData tfd =
        restTemplate.exchange(uri, HttpMethod.GET, requestEntity, TrisotechFileData.class).getBody();
    return tfd;
  }

  /**
   * get the content of the repository (place)
   * Content can include files and folders
   * This method will traverse folders to return only files.
   *
   * @param directoryID
   * @param modelsArray
   * @param path
   */
  private static void getRepositoryContent(String directoryID, List<TrisotechFileInfo> modelsArray,
                                           String path, String mimeType) {
    URI uri;
    try {
      // NOTE: MUST Use UriComponentBuilder to handle '+' in the MimeType, otherwise it will be
      // double-encoded and request will fail to return all values expected
      // See: https://docs.spring.io/spring/docs/current/spring-framework-reference/web.html#web-uri-encoding for details
      uri = UriComponentsBuilder.fromHttpUrl(new StringBuilder()
          .append(BASE_URL)
          .append(CONTENT_PATH).toString())
          .build(directoryID, mimeType, path);

      TrisotechFileData fileData = getRepositoryContent(uri);
      fileData.getData().stream().forEach((datum) -> {
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

  /**
   * @param modelId
   * @return
   */
  private static TrisotechFileInfo getDmnModel(String modelId) {
    List<TrisotechFileInfo> trisotechFileInfos = getDmnModels();
    return trisotechFileInfos.stream()
        .filter((f) -> f.getId().equals(modelId)).findFirst().get();
  }

  /**
   * @param modelId
   * @return
   */
  private static TrisotechFileInfo getCmmnModel(String modelId) {
    List<TrisotechFileInfo> trisotechFileInfos = getCmmnModels();
    return trisotechFileInfos.stream()
        .filter((f) -> f.getId().equals(modelId)).findFirst().get();
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
   * @return
   */
  public static Document downloadXmlModel(String fromUrl) {
    try {

      URL url = new URL(fromUrl);
      HttpURLConnection conn = getHttpURLConnection(url);

      // using XMLUtil to load the XML Document properly sets up the document for
      // conversion by setting namespaceaware
      Optional<Document> document = XMLUtil.loadXMLDocument(conn.getInputStream());
      conn.disconnect();
      return document.get();

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
