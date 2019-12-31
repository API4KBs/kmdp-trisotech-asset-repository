/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package edu.mayo.kmdp.trisotechwrapper;

import static edu.mayo.kmdp.trisotechwrapper.TrisotechApiUrls.BASE_URL;
import static edu.mayo.kmdp.trisotechwrapper.TrisotechApiUrls.CMMN_LOWER;
import static edu.mayo.kmdp.trisotechwrapper.TrisotechApiUrls.CMMN_XML_MIMETYPE;
import static edu.mayo.kmdp.trisotechwrapper.TrisotechApiUrls.CONTENT_PATH;
import static edu.mayo.kmdp.trisotechwrapper.TrisotechApiUrls.CONTENT_PATH_POST;
import static edu.mayo.kmdp.trisotechwrapper.TrisotechApiUrls.DMN_LOWER;
import static edu.mayo.kmdp.trisotechwrapper.TrisotechApiUrls.DMN_XML_MIMETYPE;
import static edu.mayo.kmdp.trisotechwrapper.TrisotechApiUrls.REPOSITORY_PATH;
import static edu.mayo.kmdp.trisotechwrapper.TrisotechApiUrls.VERSIONS_PATH;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;

import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileData;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechPlace;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechPlaceData;
import edu.mayo.kmdp.util.DateTimeUtil;
import edu.mayo.kmdp.util.XMLUtil;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.jena.ext.com.google.common.base.Strings;
import org.omg.spec.api4kp._1_0.identifiers.VersionIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;

/**
 * Class to wrap the calls to Trisotech in meaningful ways.
 */
public class TrisotechWrapper {

  private static final Logger logger = LoggerFactory.getLogger(TrisotechWrapper.class);
  private static final String CRLF = "\r\n";
  private static String rootDirectory;

  private static String token;

  private TrisotechWrapper() {
  }

  /**
   * Because static values cannot be set using @Value, these are set through setters
   * Another class needs to provide the mapping to the properties.
   *
   * @param token The token for accessing the repository.
   */
  public static void setToken(String token) {
    TrisotechWrapper.token = token;
  }

  /**
   * Because static values cannot be set using @Value, these are set through setters
   * Another class needs to provide the mapping to the properties.
   *
   * @param repositoryName The name of the repository for the models
   */
  public static void setRoot(String repositoryName) {
    rootDirectory = repositoryName;
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
   * Acceptable values for mimeType are: DMN_XMl_MIMETYPE or CMMN_XML_MIMETYPE
   *
   * @param fileID the fileID for the model used to query from Trisotech
   * @param mimeType the mimeType to allow for XML-compatible URL
   * @return the Trisotech FileInfo about the model
   */
  private static TrisotechFileInfo getFileInfo(String fileID, String mimeType) {
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
   * Updates the model for the fileId provided with the file provided
   *
   * @param fileId the fileId of the file to be updated
   * @param data the XML data to update the file
   */
  public static void updateModelFile(String fileId, byte[] data) {

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
      // this has to do with how Trisotech returns the data
      // we don't get the XML path unless we provide the correct mimetype in the query.
      // contains() is used as a test as the mimetype returned is not exactly what we need, but
      // can be used to determine which one to use
      return getXmlMimeType(fileInfo.getMimetype());
    }
    return null; // TODO: error? undefined mimetype?? shouldn't ever happen CAO
  }

  /**
   * get the XML-specified mimeType for transferring XML files with Trisotech
   *
   * @param mimetype the mimetype specified through file information
   * @return the XML mimetype specfication to be used in API calls
   */
  private static String getXmlMimeType(String mimetype) {
    if (mimetype.contains(DMN_LOWER)) {
      return DMN_XML_MIMETYPE;
    } else if (mimetype.contains(CMMN_LOWER)) {
      return CMMN_XML_MIMETYPE;
    } else {
      return null; // TODO: error? undefined mimetype?? shouldn't ever happen CAO
    }
  }

  /**
   * Get the Trisotech fileInfo for the model and fileVersion provided. This will NOT return the latest.
   * For the latest, use getLatestModelInfo.
   *
   * @param fileId id of the file
   * @param fileVersion the fileVersion
   * @param mimetype mimetype of the file
   * @return TrisotechFileInfo object for the file/fileVersion requested
   */
  public static TrisotechFileInfo getFileInfoByIdAndVersion(String fileId, String fileVersion,
      String mimetype) {
    List<TrisotechFileInfo> fileInfos = getModelVersions(fileId, mimetype);
    for (TrisotechFileInfo fileInfo : fileInfos) {
      if (fileVersion.equals(fileInfo.getVersion())) {
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
    return getModelVersions(rootDirectory, fileId, mimetype);
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
    if (!DMN_XML_MIMETYPE.equals(mimetype) || !CMMN_XML_MIMETYPE.equals(mimetype)) {
      // not a valid mimetype, but maybe can determine
      mimetype = getXmlMimeType(mimetype);
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
   *
   * @param tfi file info for Trisotech file
   * @return VersionIdentifier
   */
  //return VersionIdentifier uid, versiontag (1.0.0) , create .withEstablishedOn for timestamp
  public static VersionIdentifier getLatestVersion(TrisotechFileInfo tfi) {
    // only return for published models
    if (null != tfi && null != tfi.getState()) {
      System.out.println("tfiUpdated: " + tfi.getUpdated());
      System.out.println("Date from tfiUpdated: " + Date.from(Instant.parse(tfi.getUpdated())).toString());
        return new VersionIdentifier().withTag(tfi.getId())
            .withVersion(tfi.getVersion())
            .withEstablishedOn(DateTimeUtil.parseDateTime(tfi.getUpdated()));
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
        if (tp.getName().equals(rootDirectory)) {
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
      throw new RuntimeException(e);
    }
  }

  /**
   * is the model published? models can be published, but not be in the state of 'Published'.
   * All models, of any state, that are published will return true here. (per meeting 8/21/2019)
   *
   * @param trisotechFileInfo the fileInfo for the model; will contain state and version if published
   * @return true/false
   */
  private static boolean publishedModel(TrisotechFileInfo trisotechFileInfo) {
    // using the new POST capabilities, it is possible to get a state and version of ""
    // which can mess up the search for models as the SPARQL query will not return
    // those values
    return ((Optional.ofNullable(trisotechFileInfo.getState()).isPresent() &&
        !trisotechFileInfo.getState().isEmpty()) &&
        (Optional.ofNullable(trisotechFileInfo.getVersion()).isPresent() &&
        !trisotechFileInfo.getVersion().isEmpty()));
  }


  private static HttpHeaders getHttpHeaders() {
    final HttpHeaders requestHeaders = new HttpHeaders();
    requestHeaders.add(ACCEPT, "application/json");
    requestHeaders.add(AUTHORIZATION, "Bearer " + token);
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
    conn.setRequestProperty(AUTHORIZATION, "Bearer " + token);

    conn.setDoInput(true);

    if (conn.getResponseCode() != 200) {

      if (401 == conn.getResponseCode()) {
        throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode()
            + "Confirm token value");
      } else {
        throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
      }
    }
    return conn;
  }

  /**
   * Upload model file to Tristotech
   * @param path The path location for the file to be uploaded to
   * @param filename The name of the file uploading
   * @param version the version for the file (NOTE: only for published models)
   * @param state the state for the file (NOTE: only for published models)
   * @param fileContents the file contents
   */
  public static void uploadXmlModel(String path, String filename,
      String mimeType, String version, String state,
      byte[] fileContents)
      throws IOException {

    // first make sure mimetype is in correct format for API call
    mimeType = getXmlMimeType(mimeType);
    URI uri;

    // NOTE: MUST Use UriComponentBuilder to handle '+' in the MimeType, otherwise it will be
    // double-encoded and request will fail to return all values expected
    // See: https://docs.spring.io/spring/docs/current/spring-framework-reference/web.html#web-uri-encoding for details
    if(null == version || null == state) {
      uri = UriComponentsBuilder.fromHttpUrl(BASE_URL
          + CONTENT_PATH)
          .build(getRepositoryId(rootDirectory), mimeType, path);
    } else {
      uri = UriComponentsBuilder.fromHttpUrl(BASE_URL
          + CONTENT_PATH_POST)
          .build(getRepositoryId(rootDirectory), mimeType, path, version, state);
    }

    MultipartEntityBuilder mb = MultipartEntityBuilder.create();
    mb.addBinaryBody("file", fileContents);
    org.apache.http.HttpEntity e = mb.build();

    logger.debug("uri.toURL: " + uri.toURL());
    HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
    final String boundary = Strings.repeat("-", 15) + Long.toHexString(System.currentTimeMillis());

    conn.setDoInput(true);
    conn.setDoOutput(true);
    conn.setRequestMethod("POST");
    conn.setRequestProperty(ACCEPT, "application/json");
    conn.setRequestProperty(AUTHORIZATION, "Bearer " + token);
    conn.setRequestProperty(CONTENT_TYPE, "multipart/form-data; boundary=" + boundary);
//    conn.setRequestProperty(e.getContentType().getName(), e.getContentType().getValue());
//    ;
//    conn.addRequestProperty("Content-Length", String.valueOf(e.getContentLength()));
    OutputStream fout = conn.getOutputStream();
    PrintWriter body = new PrintWriter(new OutputStreamWriter(fout), true);
    body.append(CRLF);
    addFileData("file", filename, fileContents, body, fout, boundary);
    addCloseDelimiter(body, boundary);

    if (conn.getResponseCode() != 200) {
      if (401 == conn.getResponseCode()) {
        throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode()
            + "Confirm token value");
      } else {
        throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
      }
    }
    conn.getInputStream().close();
    fout.close();
  }

  private static void addFileData(String paramName, String filename, byte[] byteStream, PrintWriter body,
      OutputStream directOutput, final String boundary)
      throws IOException {
    body.append("--").append(boundary).append(CRLF);
    body.append(
        "Content-Disposition: form-data; name=\"" + paramName + "\"; filename=\"" + filename + "\"")
        .append(CRLF);
    body.append("Content-Type: application/octed-stream").append(CRLF);
    body.append("Content-Transfer-Encoding: binary").append(CRLF);
    body.append(CRLF);
    body.flush();

    directOutput.write(byteStream);
    directOutput.flush();

    body.append(CRLF);
    body.flush();

  }
  private static void addCloseDelimiter(PrintWriter body, final String boundary) {
    body.append("--").append(boundary).append("--").append(CRLF);
    body.flush();
  }
}
