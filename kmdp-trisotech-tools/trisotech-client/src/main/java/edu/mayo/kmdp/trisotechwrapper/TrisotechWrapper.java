/**
 * Copyright © 2019 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
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
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;

import java.io.IOException;
import java.net.*;
import java.util.*;

/**
 * Class to wrap the calls to Trisotech in meaningful ways.
 */
public class TrisotechWrapper {
  private static final String PUBLISHED_STATE = "Published";
  // TODO: search for this particular directory or just use the known ID of our known repository and skip the places call?
  private static final String ROOT_DIRECTORY = "MEA-Test";


  // TODO: Where to put bearer token so protected?
//  private static TrisotechApiToken token;


  /**
   * Retrieve the versions for the requested model.
   *
   * @param repositoryId - id of the repository holding the model
   * @param modelId
   * @return list of modelFileInfo
   */
  // TODO: Needed?
//  public static List<String> getModelVersions( String repositoryId, String modelId ) {
//    System.out.println("getModelVersions for model: " + modelId + " in repository: " + repositoryId);
//    String urlString = TrisotechApiUrls.BASE_URL + String.format(TrisotechApiUrls.VERSIONS_PATH, repositoryId, modelId);
//    List<TrisotechFileInfo> versions = new ArrayList<TrisotechFileInfo>();
//    HttpEntity<?> requestEntity = getHttpEntity();
//
//    RestTemplate restTemplate = new RestTemplate();
//
//    JsonNode jsonNode =
//            restTemplate.exchange(urlString, HttpMethod.GET,
//                    requestEntity, JsonNode.class).getBody();
//    System.out.println("jsonNode from server: " + jsonNode.toString());
//
////    TODO: figure this out
//    // TODO: do anything about the ones that don't have versions? What about multiple same versions? Take latest only?
//    // Do we want the url? or just the list of versions?
//    for (Iterator<JsonNode> it = jsonNode.elements(); it.hasNext(); ) {
//      JsonNode node = it.next();
//      System.out.println("node: " + node.toString());
//      if ((node.get("version").textValue()).equals("version")) {
//        // TODO: get version here?
//        String versionID = node.get("version").textValue();
//        String url = node.get("url").textValue();
//
//        versions.add(versionID);
//      }
////      }
//    }
//    return versions;
//  }

  //

  /**
   * Retrieve the PUBLISHED DMN Model given the model ID
   * TODO: Needed?
   *
   * @param modelId
   * @return DMN XML Document
   */
  public static Optional<Document> getDmnModelById( String modelId ) {
    try {
      TrisotechFileInfo trisotechFileInfo = getDmnModel(modelId);
      if ( publishedModel( trisotechFileInfo ) ) {
        return Optional.of( downloadXmlModel( trisotechFileInfo.getUrl() ) );
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return Optional.empty();
  }

  /**
   * Get the DMN models from Trisotech
   * TODO: return more than just model IDs? Likely need to return the entire file information. Due to different processing between Signavio and Trisotech
   *
   * @return List of all DMN model IDs
   */
  public static List<TrisotechFileInfo> getDmnModels() {

    List<TrisotechFileInfo> modelsArray = new ArrayList<>();
    // TODO: search for this particular directory or just use the known ID of our known repository and skip the places call?

    try {
      URL url = new URL(TrisotechApiUrls.BASE_URL +
          TrisotechApiUrls.REPOSITORY_PATH);

      TrisotechPlaceData data = getPlaces( url );
      for(TrisotechPlace tp: data.getData()) {
        if(tp.getName().equals(ROOT_DIRECTORY)) {
          getRepositoryContent(tp.getId(), modelsArray, "/", TrisotechApiUrls.DMN_XML_MIMETYPE);
        }
      }

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return modelsArray;
  }

  /**
   * Retrieve the CMMN Model given ID and version
   *
   * @param assetId
   * @param versionId
   * @return
   */
  // TODO: Need to be able to get by version? if so update to Trisotech APIs CAO
  // TODO: If needed, discuss the version data w/Davide & how best to handle CAO
//  public static JsonNode getCMMNModel( String assetId, String versionId ) {
//    String urlString;
//    if ( null != versionId ) {
////      System.out.println("getModel for version: " + versionTag);
//
//      urlString = TrisotechApiUrls.BASE_URL + TrisotechApiUrls.VERSION_PATH + versionId + TrisotechApiUrls.JSON_PATH;
//
//      return getModel(urlString);
//    } else {
////      System.out.println("getModel for assetId: " + assetId);
//
//      urlString = TrisotechApiUrls.BASE_URL + TrisotechApiUrls.MODEL_PATH + assetId + TrisotechApiUrls.JSON_PATH;
//      return getModel(urlString);
//    }
//  }


  /**
   * 'valid' models are those with a state of 'Published'
   * @return
   */
  public static Map<String, TrisotechFileInfo> getPublishedCMMNModels() {

    List<TrisotechFileInfo> cmmnModels = getCmmnModels();
    return getPublishedModels(cmmnModels);
  }

  public static List<TrisotechFileInfo> getCmmnModels() {
    List<TrisotechFileInfo> modelsArray = new ArrayList<>();

    try {
      URL url = new URL(TrisotechApiUrls.BASE_URL +
          TrisotechApiUrls.REPOSITORY_PATH);

      TrisotechPlaceData data = getPlaces( url );
      for(TrisotechPlace tp: data.getData()) {
        if(tp.getName().equals(ROOT_DIRECTORY)) {
          getRepositoryContent(tp.getId(), modelsArray, "/", TrisotechApiUrls.CMMN_XML_MIMETYPE);
        }
      }

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return modelsArray;
  }

  /**
   * 'valid' models are those with a state of 'Published'
   * @return
   */
  public static Map<String, TrisotechFileInfo> getPublishedDmnModels() {

    List<TrisotechFileInfo> dmnModels = getDmnModels();
    return getPublishedModels(dmnModels);
  }

  /**
   * used to remove /whatever/ from URL fragments provided from Trisotech
   * allows to get at the values that are needed for API calls
   * @param href
   * @return
   */
  private static String getSubstring(String href) {
    return href.substring(href.lastIndexOf('/') + 1);
  }


  /**
   * TODO: Needed? Similar to getModelById, would still need the repository info; also for Trisotech, the returned
   * data from the query of the repository IS the model info
   *
   * @param modelID
   * @return
   */
  public static TrisotechFileInfo getModelInfo(String modelID) {
    return getDmnModel(modelID);
  }

  /**
   * return the URL for the latest revision of the given model
   * <p>
   *
   * @param artifactId
   * @return
   */
  // TODO: UPdate to Trisotech APIs
//  public static String getLatestVersion( String artifactId ) {
//    TrisotechModelRepInfo smri = getModelInfo(artifactId);
//    // strip off /revision/
//    return getLatestVersion( artifactId, smri );
//  }

  // TODO: Update to Trisotech APIs
//  public static String getLatestVersionTag(String artifactId ) {
//    TrisotechModelRepInfo smri = getModelInfo( artifactId );
//    return "" + smri.getRev();
//  }

//  public static String getLatestVersion( String artifactId, TrisotechModelRepInfo smri ) {
//    // strip off /revision/
//    String versionID = getSubstring( smri.getRevision() );
//    return versionID;
//  }



  /**
   * Files that have a 'Published' state are published and considered valid.
   *
   * @param models
   * @return
   */
  private static Map<String, TrisotechFileInfo> getPublishedModels(List<TrisotechFileInfo> models) {
    Map<String, TrisotechFileInfo> publishedModels = new HashMap<>();

    for (TrisotechFileInfo trisotechFileInfo : models) {
      if (publishedModel(trisotechFileInfo)) {
        publishedModels.put(trisotechFileInfo.getId(), trisotechFileInfo);
      } else {
        // TODO: does this make sense? why throw an error; turning off for now
//        throw new IllegalStateException( "ERROR : Model " + trisotechFileInfo.getName() + " is not published" );
      }
    }
    return publishedModels;
  }

  /**
   * get the folders (places) the application has access to
   *
   * @param url
   * @return
   * @throws IOException
   */
  private static TrisotechPlaceData getPlaces(URL url) throws IOException {

    HttpEntity<?> requestEntity = getHttpEntity();
    RestTemplate restTemplate = new RestTemplate();

    TrisotechPlaceData data =
        restTemplate.exchange(url.toString(), HttpMethod.GET, requestEntity, TrisotechPlaceData.class).getBody();

    return data;
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
    // NOTE: MUST send URI here to avoid further encoding, otherwise it will be double-encoded and request
    // will fail to return all the values expected
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
          .append(TrisotechApiUrls.BASE_URL)
          .append(TrisotechApiUrls.CONTENT_PATH).toString())
          .build(directoryID, mimeType, path);

      TrisotechFileData fileData = getRepositoryContent(uri);
      for(Datum datum : fileData.getData()) {
        if (null != datum.getFile()) {
          // have a file entry, process fileInfo
          TrisotechFileInfo file = datum.getFile();
//          if (Optional.ofNullable(file.getState()).isPresent() && file.getState().equals(PUBLISHED_STATE)) {
//            System.out.println("file: " + file.getName() + " with Id: " + file.getId() + " is published");
//          } else {
//            System.out.println("file: " + file.getName() + " with id: " + file.getId() + " is not published");
//          }
          modelsArray.add(file);
        } else { // folder -- assume folder?
          TrisotechFolderInfo folder = datum.getFolder();
          getRepositoryContent(directoryID, modelsArray, folder.getPath(), mimeType);
        }
      }

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
   * TODO: NEEDED?  Not as direct in Trisotech. Would need repository ID (or default to MEA repo) and would need to query the repository
   * TODO: cont: and find the model with the matching ID; alternatively could query repositoryVersions using the ID given, but would still need to know repository CAO
   * @param modelId
   * @return
   */
  private static TrisotechFileInfo getDmnModel(String modelId) {

    List<TrisotechFileInfo> trisotechFileInfos = getDmnModels();
    for(TrisotechFileInfo fileInfo : trisotechFileInfos) {
      if(fileInfo.getId().equals(modelId)) {
        return fileInfo;
      }
    }
    return null;
  }

  // TODO: How to handle bearer token?
  private static HttpHeaders getHttpHeaders() {
    final HttpHeaders requestHeaders = new HttpHeaders();
    requestHeaders.add("Accept", "application/json");
    requestHeaders.add("Authorization", "Bearer " + TrisotechApiUrls.TOKEN);
    requestHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);
    return requestHeaders;
  }

  private static HttpEntity<?> getHttpEntity() {
    return new HttpEntity<>(getHttpHeaders());
  }

  private static <T> HttpEntity<T> getHttpEntity( T body ) {
    final HttpHeaders requestHeaders = getHttpHeaders();
    if ( body instanceof MultiValueMap || body instanceof Map) {
      requestHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    } else {
      requestHeaders.setContentType(MediaType.APPLICATION_JSON);
    }
    return new HttpEntity<T>(body,requestHeaders);
  }

  private static <T> HttpEntity<T> getHttpEntity( T body, MediaType mime ) {
    final HttpHeaders requestHeaders = getHttpHeaders();
    requestHeaders.setContentType( mime );
    return new HttpEntity<T>(body,requestHeaders);
  }


  /**
   * Download the actual model in its XML form.
   *
   * @param fromUri String representing an ENCODED URI
   * @return
   */
  public static Document downloadXmlModel( String fromUrl ) {
    try {

      URL url = new URL(fromUrl);
      HttpURLConnection conn = getHttpURLConnection(url);

      // using XMLUtil to load the XML Document properly sets up the document for
      // conversion by setting namespaceaware
      Optional<Document> document = XMLUtil.loadXMLDocument(conn.getInputStream());
      conn.disconnect();
      return document.get();

    } catch ( IOException e ) {

      throw new RuntimeException(e);

    }

  }

  private static HttpUriRequest getHttpURIConnection(URI uri) {
    return null;
  }

  private static HttpURLConnection getHttpURLConnection(URL url) throws IOException {
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("GET");
    conn.setRequestProperty("Content-Type", "application/json");
    conn.setRequestProperty("Authorization", "Bearer " + TrisotechApiUrls.TOKEN);

    conn.setDoInput(true);

    if (conn.getResponseCode() != 200) {

      switch (conn.getResponseCode()) {
        case 401:
          throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode()
              + String.format("<Token>: %s  ", TrisotechApiUrls.TOKEN));

        default:
          throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
      }
    }
    return conn;
  }


}
