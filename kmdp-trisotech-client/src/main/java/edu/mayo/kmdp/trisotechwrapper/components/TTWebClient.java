package edu.mayo.kmdp.trisotechwrapper.components;

import static edu.mayo.kmdp.trisotechwrapper.config.TTApiConstants.CONTENT_PATH_POST;
import static edu.mayo.kmdp.trisotechwrapper.config.TTApiConstants.CONTENT_PATH_POST_WITH_VERSION;
import static edu.mayo.kmdp.trisotechwrapper.config.TTApiConstants.EXEC_ARTIFACTS_PATH;
import static edu.mayo.kmdp.trisotechwrapper.config.TTApiConstants.REPOSITORY_PATH;
import static edu.mayo.kmdp.trisotechwrapper.config.TTApiConstants.SPARQL_PATH;
import static edu.mayo.kmdp.trisotechwrapper.config.TTApiConstants.VERSIONS_PATH;
import static edu.mayo.kmdp.trisotechwrapper.config.TTNotations.KEM_JSON;
import static edu.mayo.kmdp.trisotechwrapper.config.TTNotations.getXmlMimeType;
import static edu.mayo.kmdp.util.Util.isEmpty;
import static java.net.URLDecoder.decode;
import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.jena.query.QueryExecutionFactory.sparqlService;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.util.UriComponentsBuilder.fromHttpUrl;

import com.fasterxml.jackson.databind.JsonNode;
import edu.mayo.kmdp.trisotechwrapper.components.operators.KEMtoMVFTranslator;
import edu.mayo.kmdp.trisotechwrapper.config.TTWConfigParamsDef;
import edu.mayo.kmdp.trisotechwrapper.config.TTWEnvironmentConfiguration;
import edu.mayo.kmdp.trisotechwrapper.models.Datum;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechExecutionArtifactData;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileData;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechPlaceData;
import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.KemModel;
import edu.mayo.kmdp.util.JSonUtil;
import edu.mayo.kmdp.util.JaxbUtil;
import edu.mayo.kmdp.util.XMLUtil;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.client.HttpClient;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.jena.query.Query;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.sparql.resultset.ResultSetMem;
import org.omg.spec.mvf._20220702.mvf.MVFDictionary;
import org.omg.spec.mvf._20220702.mvf.ObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;

/**
 * (ReST) web client for the TT DES Handles auth*ion and web calls
 */
public class TTWebClient implements TTDigitalEnterpriseServerClient {

  // SPARQL Strings

  private static final String CRLF = "\r\n";

  private static final Logger logger = LoggerFactory.getLogger(TTWebClient.class);

  private final URI apiEndpoint;
  private final String sparqlEndpoint;
  private final String token;

  private final boolean online;

  public TTWebClient(TTWEnvironmentConfiguration cfg) {
    online = cfg.get(TTWConfigParamsDef.API_ENDPOINT).isPresent()
        && cfg.get(TTWConfigParamsDef.API_TOKEN).isPresent();
    apiEndpoint = cfg.getTyped(TTWConfigParamsDef.API_ENDPOINT);
    sparqlEndpoint = cfg.getTyped(TTWConfigParamsDef.BASE_URL) + SPARQL_PATH;
    token = cfg.getTyped(TTWConfigParamsDef.API_TOKEN);
  }

  public Optional<TrisotechFileInfo> getModelLatestVersion(
      final String repositoryId,
      final String fileId) {
    if (isEmpty(repositoryId) || isEmpty(fileId)) {
      logger.warn("Missing repository or model ID, unable to retrieve Model Info");
      return Optional.empty();
    }
    var uri = fromHttpUrl(apiEndpoint + REPOSITORY_PATH)
        .build(repositoryId, fileId);

    return collectRepositoryContent(uri)
        .findFirst();
  }

  /**
   * Will return all the file info for all the versions of the model in the specified repository
   * requested EXCEPT latest version is not included. Don't expect users to know the ID of the
   * repository, but should know the name.
   *
   * @param repositoryId - id of the repository holding the model
   * @param fileId       - file id of the model requested
   * @return list of modelFileInfo for all but the latest version of the model
   */
  @Override
  public List<TrisotechFileInfo> getModelPreviousVersions(
      final String repositoryId,
      final String fileId) {
    if (!online) {
      logger.warn("Client is offline - unable to get model history data");
      return Collections.emptyList();
    }
    if (isEmpty(repositoryId) || isEmpty(fileId)) {
      logger.warn("Missing repository or model ID, unable to retrieve Model Info");
      return Collections.emptyList();
    }

    var uri = fromHttpUrl(apiEndpoint + VERSIONS_PATH)
        .build(repositoryId, fileId);

    return collectRepositoryContent(uri)
        .collect(Collectors.toList());
  }


  /**
   * get the folders (places|directories) the application has access to
   *
   * @return Object that contains the list of places in a JSON format
   * @throws IOException if can't make the request
   */
  @Override
  public Optional<TrisotechPlaceData> getPlaces() throws IOException {
    if (!online) {
      logger.warn("Client is offline - unable to get Place data");
      return Optional.empty();
    }
    URL url = new URL(apiEndpoint + REPOSITORY_PATH);

    HttpEntity<?> requestEntity = getHttpEntity();
    RestTemplate restTemplate = new RestTemplate();

    return
        Optional.ofNullable(
            restTemplate.exchange(
                    url.toString(), HttpMethod.GET, requestEntity, TrisotechPlaceData.class)
                .getBody());

  }


  /**
   * Retrieve the content of a particular Trisotech repository/place/directory
   *
   * @param uri URI of the repository querying content from
   * @return object that contains a list of all the files and directories found in the requested
   * repository in a JSON format
   */
  protected Stream<TrisotechFileInfo> collectRepositoryContent(URI uri) {
    if (!online) {
      logger.warn("Client is offline - unable to retrieve Model Info");
      return Stream.empty();
    }

    HttpEntity<?> requestEntity = getHttpEntity();
    RestTemplate restTemplate = new RestTemplate();
    // ******* NOTE: MUST send URI here to avoid further encoding, otherwise it will be double-encoded and request
    // will fail to return all the values expected ********
    var data =
        restTemplate.exchange(uri, HttpMethod.GET, requestEntity, TrisotechFileData.class)
            .getBody();
    return data != null
        ? data.getData().stream().map(Datum::getFile)
        : Stream.empty();
  }

  /**
   * Download the actual model in its XML form. Does NOT care if model is published or not
   *
   * @param from String representing an ENCODED URI
   * @return XML document
   */
  @Override
  public Optional<Document> downloadXmlModel(TrisotechFileInfo from) {
    if (!online) {
      logger.warn("Client is offline - unable to download model");
      return Optional.empty();
    }
    try {
      var fromUrl = negotiate(from);
      if (decode(fromUrl.toString(), UTF_8).contains(KEM_JSON.getMimeType())) {
        // convert KEM to a more standard form, then process as BPM+
        return tryDownloadKEM(fromUrl);
      } else{
        return tryDownloadXmlModel(fromUrl);
      }
    } catch (HttpException | IOException | URISyntaxException e) {
      logger.error(e.getMessage(), e);
      return Optional.empty();
    }
  }

  /**
   * Intercepts a request to download a KEM Model, so that it can be translated
   * into a standard form (MVF), with a standard serialization (XML).
   *
   * The choice of XML (vs JSON) is driven by alignment with the other standard
   * languages, all of which have a primary XML-based serialization
   *
   * @param fromUrl the TT DES URL where the model to be downloaded is available
   * @return the KEM model, as a MVF/XML document
   * @throws HttpException
   * @throws IOException
   */
  private Optional<Document> tryDownloadKEM(URL fromUrl) throws HttpException, IOException {
    return tryDownloadNativeModel(fromUrl)
        .flatMap(j -> JSonUtil.parseJson(j, KemModel.class))
        .map(k -> new KEMtoMVFTranslator().translate(k))
        .flatMap(mvg -> JaxbUtil.marshallDox(
            List.of(MVFDictionary.class),
            mvg,
            new ObjectFactory()::createMVFDictionary,
            JaxbUtil.defaultProperties()));
  }

  /**
   * Retrieves the execution artifacts in a given execution environment
   *
   * @param execEnv the unique name of the environment
   * @return the {@link TrisotechExecutionArtifactData}, if any
   */
  @Override
  public Optional<TrisotechExecutionArtifactData> getExecutionArtifacts(String execEnv)
      throws IOException {
    if (!online) {
      logger.warn("Client is offline - unable to get Execution Artifacts data");
      return Optional.empty();
    }
    URL url = new URL(apiEndpoint + EXEC_ARTIFACTS_PATH);

    HttpEntity<?> requestEntity = getHttpEntity();
    RestTemplate restTemplate = new RestTemplate();

    return Optional.ofNullable(restTemplate.exchange(
            url.toString(), HttpMethod.GET, requestEntity, TrisotechExecutionArtifactData.class,
            execEnv)
        .getBody());

  }


  private HttpHeaders getHttpHeaders() {
    final HttpHeaders requestHeaders = new HttpHeaders();
    requestHeaders.add(ACCEPT, APPLICATION_JSON_VALUE);
    requestHeaders.add(AUTHORIZATION, getBearerTokenHeader());
    requestHeaders.setContentType(APPLICATION_JSON);
    return requestHeaders;
  }

  private HttpEntity<?> getHttpEntity() {
    return new HttpEntity<>(getHttpHeaders());
  }

  /**
   * Download the actual model in its XML form. Does NOT care if model is published or not
   *
   * @param fromUrl String representing an ENCODED URI
   * @return XML document
   */
  public Optional<Document> tryDownloadXmlModel(URL fromUrl) throws HttpException, IOException {
    if (!online) {
      logger.warn("Client is offline - unable to download model");
      return Optional.empty();
    }

    HttpURLConnection conn = getHttpURLConnection(fromUrl);

    // using XMLUtil to load the XML Document properly sets up the document for
    // conversion by setting namespaceaware
    Optional<Document> document = XMLUtil.loadXMLDocument(conn.getInputStream());
    conn.disconnect();
    return document;
  }

  /**
   * Download the actual model in its TT internal JSON-based form.
   *
   * @param fromUrl String representing an ENCODED URI
   * @return JsonNode
   */
  public Optional<JsonNode> tryDownloadNativeModel(URL fromUrl)
      throws HttpException, IOException {
    if (!online) {
      logger.warn("Client is offline - unable to download model");
      return Optional.empty();
    }

    HttpURLConnection conn = getHttpURLConnection(fromUrl);

    Optional<JsonNode> document = JSonUtil.readJson(conn.getInputStream());
    conn.disconnect();
    return document;
  }

  private URL oldNegotiate(String url) throws MalformedURLException {
    String[] comps = url.split("&");
    String recomp = Arrays.stream(comps)
        .map(comp -> comp.startsWith("mimetype=")
            ? negotiateMimeType(comp)
            : comp
        ).collect(Collectors.joining("&"));
    return new URL(recomp);
  }

  private String negotiateMimeType(String comp) {
    int idx = comp.indexOf('=');
    String xmlMimeType = getXmlMimeType(comp.substring(idx + 1)).orElse(null);
    return comp.substring(0, idx) + "=" + encode(xmlMimeType, UTF_8);
  }

  /**
   * Stub method for content negotiation
   */
  private URL negotiate(TrisotechFileInfo from) throws MalformedURLException, URISyntaxException {
    var parts = from.getUrl().split("\\?");
    var qry = parts[1];
    var queryParams = Arrays.stream(qry.split("&"))
        .map(c -> c.split("="))
        .collect(Collectors.toMap(
            s -> s[0],
            s -> s[1]
        ));
    var mime = queryParams.getOrDefault("mimetype", from.getMimetype());
    getXmlMimeType(mime).ifPresent(m ->
      queryParams.put("mimetype", encode(m, UTF_8)));
    var query = queryParams.entrySet().stream()
        .map(e -> e.getKey() + "=" + e.getValue())
        .collect(Collectors.joining("&"));
    return new URL(parts[0] + "?" + query);
  }

  private HttpURLConnection getHttpURLConnection(URL url) throws IOException, HttpException {
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("GET");
    conn.setRequestProperty(ACCEPT, "application/json");
    conn.setRequestProperty(AUTHORIZATION, getBearerTokenHeader());

    conn.setDoInput(true);

    checkResponse(conn);
    return conn;
  }

  private void checkResponse(HttpURLConnection conn) throws IOException, HttpException {
    if (conn.getResponseCode() != 200) {
      if (401 == conn.getResponseCode()) {
        throw raiseHttpException(conn.getResponseCode(), "Confirm token value");
      } else {
        throw raiseHttpException(conn.getResponseCode(), conn.getResponseMessage());
      }
    }
  }

  private HttpException raiseHttpException(int code, String msg) {
    return new HttpException("Failed : HTTP error code : " + code + " : " + msg);
  }


  /**
   * Upload model file to Tristotech
   *
   * @param path         The path location for the file to be uploaded to
   * @param name         The name of the model uploading
   * @param version      the version for the file (NOTE: only for published models)
   * @param state        the state for the file (NOTE: only for published models)
   * @param fileContents the file contents
   * @throws IOException   unable to load the source document
   * @throws HttpException if TT Digital Enterprise Server refuses the request
   */
  @Override
  public void uploadXmlModel(String repositoryId, String path, String name,
      String mimeType, String version, String state,
      byte[] fileContents)
      throws IOException, HttpException {
    if (!online) {
      logger.warn("Client is offline - unable to upload model");
      return;
    }

    // first make sure mimetype is in correct format for API call
    mimeType = getXmlMimeType(mimeType).orElse(mimeType);
    URI uri;

    // NOTE: MUST Use UriComponentBuilder to handle '+' in the MimeType, otherwise it will be
    // double-encoded and request will fail to return all values expected
    // See: https://docs.spring.io/spring/docs/current/spring-framework-reference/web.html#web-uri-encoding for details
    if (null == version || null == state) {
      // using name here allows for the name of the file to be different than the
      // name of the model. Ex: model.raw.dmn.xml vs model.dmn
      uri = fromHttpUrl(apiEndpoint + CONTENT_PATH_POST)
          .build(
              repositoryId,
              name,
              mimeType,
              path);
    } else {
      uri = fromHttpUrl(apiEndpoint + CONTENT_PATH_POST_WITH_VERSION)
          .build(
              repositoryId,
              name,
              mimeType,
              path,
              version,
              state);
    }

    MultipartEntityBuilder mb = MultipartEntityBuilder.create();
    mb.addBinaryBody("file", fileContents);

    logger.debug("uri.toURL: {}", uri.toURL());
    HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
    final String boundary = "-".repeat(15) + Long.toHexString(System.currentTimeMillis());

    conn.setDoInput(true);
    conn.setDoOutput(true);
    conn.setRequestMethod("POST");
    conn.setRequestProperty(ACCEPT, "application/json");
    conn.setRequestProperty(AUTHORIZATION, getBearerTokenHeader());
    conn.setRequestProperty(CONTENT_TYPE, "multipart/form-data; boundary=" + boundary);

    OutputStream fout = conn.getOutputStream();
    PrintWriter body = new PrintWriter(new OutputStreamWriter(fout), true);
    body.append(CRLF);
    addFileData("file", name, fileContents, body, fout, boundary);
    addCloseDelimiter(body, boundary);

    checkResponse(conn);
    conn.getInputStream().close();
    fout.close();
  }

  private void addFileData(String paramName, String filename, byte[] byteStream,
      PrintWriter body,
      OutputStream directOutput, final String boundary)
      throws IOException {
    body.append("--")
        .append(boundary)
        .append(CRLF)
        .append("Content-Disposition: form-data; name=\"")
        .append(paramName).append("\"; filename=\"").append(filename).append("\"")
        .append(CRLF)
        .append("Content-Type: application/octed-stream").append(CRLF)
        .append("Content-Transfer-Encoding: binary").append(CRLF)
        .append(CRLF);
    body.flush();

    directOutput.write(byteStream);
    directOutput.flush();

    body.append(CRLF);
    body.flush();

  }

  private void addCloseDelimiter(PrintWriter body, final String boundary) {
    body.append("--").append(boundary).append("--").append(CRLF);
    body.flush();
  }

  public String getBearerTokenHeader() {
    return "Bearer " + token;
  }

  @Override
  public ResultSet askQuery(Query query) {
    if (!online) {
      logger.warn("Client is offline - unable to ask Query, returning empty ResultSet");
      return new ResultSetMem();
    }

    Header header = new BasicHeader(
        org.apache.http.HttpHeaders.AUTHORIZATION,
        getBearerTokenHeader());

    HttpClient httpClient = HttpClientBuilder.create()
        .setDefaultHeaders(Collections.singleton(header))
        .build();

    try(var qexec = sparqlService(sparqlEndpoint, query, httpClient)) {
      return ResultSetFactory.copyResults(qexec.execSelect());
    }
  }

}
