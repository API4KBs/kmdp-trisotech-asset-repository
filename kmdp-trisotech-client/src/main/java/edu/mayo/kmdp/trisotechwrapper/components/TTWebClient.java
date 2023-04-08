package edu.mayo.kmdp.trisotechwrapper.components;

import static edu.mayo.kmdp.trisotechwrapper.config.TTApiConstants.CONTENT_PATH_POST;
import static edu.mayo.kmdp.trisotechwrapper.config.TTApiConstants.CONTENT_PATH_POST_WITH_VERSION;
import static edu.mayo.kmdp.trisotechwrapper.config.TTApiConstants.EXEC_ARTIFACTS_PATH;
import static edu.mayo.kmdp.trisotechwrapper.config.TTApiConstants.REPOSITORY_PATH;
import static edu.mayo.kmdp.trisotechwrapper.config.TTApiConstants.SPARQL_PATH;
import static edu.mayo.kmdp.trisotechwrapper.config.TTApiConstants.VERSIONS_PATH;
import static edu.mayo.kmdp.trisotechwrapper.config.TTNotations.KEM_JSON;
import static edu.mayo.kmdp.trisotechwrapper.config.TTNotations.getStandardXmlMimeType;
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
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechExecutionArtifact;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechExecutionArtifactData;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileData;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechPlace;
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
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
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


  private static final String CRLF = "\r\n";

  /**
   * Logger
   */
  private static final Logger logger = LoggerFactory.getLogger(TTWebClient.class);

  /**
   * The public Rest API base URL
   */
  private final URI apiEndpoint;
  /**
   * The SPARQL API endpoint
   */
  private final String sparqlEndpoint;
  /**
   * The API auth token
   */
  private final String token;

  /**
   * Flag. True if this client can establish access to the DES API endpoints
   */
  private final boolean online;

  /**
   * Constructor.
   * <p>
   * Uses the {@link TTWEnvironmentConfiguration} to gather the DES access information
   */
  public TTWebClient(@Nonnull final TTWEnvironmentConfiguration cfg) {
    online = cfg.get(TTWConfigParamsDef.API_ENDPOINT).isPresent()
        && cfg.get(TTWConfigParamsDef.API_TOKEN).isPresent();
    apiEndpoint = cfg.getTyped(TTWConfigParamsDef.API_ENDPOINT);
    sparqlEndpoint = cfg.getTyped(TTWConfigParamsDef.BASE_URL) + SPARQL_PATH;
    token = cfg.getTyped(TTWConfigParamsDef.API_TOKEN);
  }

  @Nonnull
  @Override
  public Optional<TrisotechFileInfo> getModelLatestVersion(
      @Nonnull final String repositoryId,
      @Nonnull final String fileId) {
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
   * {@inheritDoc}
   * <p>
   * Will return all the file info for all the versions of the model in the specified repository
   * requested EXCEPT latest version is not included. Don't expect users to know the ID of the
   * repository, but should know the name.
   */
  @Override
  @Nonnull
  public List<TrisotechFileInfo> getModelPreviousVersions(
      @Nonnull final String repositoryId,
      @Nonnull final String modelUri) {
    if (!online) {
      logger.warn("Client is offline - unable to get model history data");
      return Collections.emptyList();
    }
    if (isEmpty(repositoryId) || isEmpty(modelUri)) {
      logger.warn("Missing repository or model ID, unable to retrieve Model Info");
      return Collections.emptyList();
    }

    var uri = fromHttpUrl(apiEndpoint + VERSIONS_PATH)
        .build(repositoryId, modelUri);

    return collectRepositoryContent(uri)
        .collect(Collectors.toList());
  }


  @Override
  @Nonnull
  public List<TrisotechPlace> getPlaces() {
    if (!online) {
      logger.warn("Client is offline - unable to get Place data");
      return Collections.emptyList();
    }

    try {
      URL url = new URL(apiEndpoint + REPOSITORY_PATH);

      HttpEntity<?> requestEntity = getHttpEntity();
      RestTemplate restTemplate = new RestTemplate();

      return
          Optional.ofNullable(
                  restTemplate.exchange(
                          url.toString(), HttpMethod.GET, requestEntity, TrisotechPlaceData.class)
                      .getBody())
              .map(TrisotechPlaceData::getData)
              .orElseGet(Collections::emptyList);
    } catch (IOException ioe) {
      logger.error(ioe.getMessage(), ioe);
      return Collections.emptyList();
    }
  }


  /**
   * Retrieve the content of a particular Trisotech repository/place/directory
   *
   * @param uri URI of the repository querying content from
   * @return Descriptors of the files and directories found in the requested repository, streaming
   */
  @Nonnull
  protected Stream<TrisotechFileInfo> collectRepositoryContent(
      @Nonnull final URI uri) {
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


  @Override
  @Nonnull
  public Optional<Document> downloadXmlModel(
      @Nonnull final TrisotechFileInfo from) {
    if (!online) {
      logger.warn("Client is offline - unable to download XML model");
      return Optional.empty();
    }
    try {
      var fromUrl = negotiate(from);
      if (decode(fromUrl.toString(), UTF_8).contains(KEM_JSON.getMimeType())) {
        // convert KEM to a more standard form, then process as BPM+
        return tryDownloadKEM(fromUrl);
      } else {
        return tryDownloadXmlModel(fromUrl);
      }
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      return Optional.empty();
    }
  }

  /**
   * Intercepts a request to download a KEM Model, so that it can be translated into a standard form
   * (MVF), with a standard serialization (XML).
   * <p>
   * The choice of XML (vs JSON) is driven by alignment with the other standard languages, all of
   * which have a primary XML-based serialization
   *
   * @param fromUrl the TT DES URL where the model to be downloaded is available
   * @return the KEM model, as a MVF/XML document
   */
  @Nonnull
  protected Optional<Document> tryDownloadKEM(
      @Nonnull final URL fromUrl) {
    try {
      return tryDownloadNativeModel(fromUrl)
          .flatMap(j -> JSonUtil.parseJson(j, KemModel.class))
          .map(k -> new KEMtoMVFTranslator().translate(k))
          .flatMap(mvg -> JaxbUtil.marshallDox(
              List.of(MVFDictionary.class),
              mvg,
              new ObjectFactory()::createMVFDictionary,
              JaxbUtil.defaultProperties()));
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      return Optional.empty();
    }
  }

  @Override
  @Nonnull
  public List<TrisotechExecutionArtifact> getExecutionArtifacts(
      @Nonnull final Set<String> execEnvs) {
    if (!online) {
      logger.warn("Client is offline - unable to get Execution Artifacts data");
      return Collections.emptyList();
    }
    try {
      URL url = new URL(apiEndpoint + EXEC_ARTIFACTS_PATH);
      return execEnvs.stream()
          .flatMap(env -> getExecutionArtifacts(url, env))
          .collect(Collectors.toList());

    } catch (IOException ioe) {
      logger.error(ioe.getMessage(), ioe);
      return Collections.emptyList();
    }
  }

  /**
   * Queries an execution environment within a Service Library, to discover the deployed services
   *
   * @param slUrl   the URL of the Service Library deployment
   * @param execEnv the name of the Execution Environment within the Service Library
   * @return the deployed {@link TrisotechExecutionArtifact}, streaming
   */
  @Nonnull
  private Stream<TrisotechExecutionArtifact> getExecutionArtifacts(
      @Nonnull final URL slUrl,
      @Nonnull final String execEnv) {
    HttpEntity<?> requestEntity = getHttpEntity();
    RestTemplate restTemplate = new RestTemplate();

    return Optional.ofNullable(
            restTemplate.exchange(
                    slUrl.toString(), HttpMethod.GET, requestEntity, TrisotechExecutionArtifactData.class,
                    execEnv)
                .getBody()).stream()
        .flatMap(xc -> xc.getData().stream());
  }

  /**
   * Download the actual model in its XML form. Does NOT care if model is published or not
   *
   * @param fromUrl String representing an ENCODED URI
   * @return XML document
   */
  @Nonnull
  public Optional<Document> tryDownloadXmlModel(
      @Nonnull final URL fromUrl) {
    if (!online) {
      logger.warn("Client is offline - unable to download XML model");
      return Optional.empty();
    }

    try {
      HttpURLConnection conn = getHttpURLConnection(fromUrl);
      Optional<Document> document = XMLUtil.loadXMLDocument(conn.getInputStream());
      conn.disconnect();
      return document;
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      return Optional.empty();
    }
  }

  /**
   * Download the actual model in its TT internal JSON-based form.
   *
   * @param fromUrl String representing an ENCODED URI
   * @return JsonNode
   */
  @Nonnull
  public Optional<JsonNode> tryDownloadNativeModel(
      @Nonnull final URL fromUrl) {
    if (!online) {
      logger.warn("Client is offline - unable to download native JSON model");
      return Optional.empty();
    }
    try {
      HttpURLConnection conn = getHttpURLConnection(fromUrl);

      Optional<JsonNode> document = JSonUtil.readJson(conn.getInputStream());
      conn.disconnect();
      return document;
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      return Optional.empty();
    }
  }

  /**
   * Stub method for content negotiation
   */
  private URL negotiate(TrisotechFileInfo from) throws MalformedURLException {
    var parts = from.getUrl().split("\\?");
    var qry = parts[1];
    var queryParams = Arrays.stream(qry.split("&"))
        .map(c -> c.split("="))
        .collect(Collectors.toMap(
            s -> s[0],
            s -> s[1]
        ));
    var mime = queryParams.getOrDefault("mimetype", from.getMimetype());
    getStandardXmlMimeType(mime).ifPresent(m ->
        queryParams.put("mimetype", encode(m, UTF_8)));
    var query = queryParams.entrySet().stream()
        .map(e -> e.getKey() + "=" + e.getValue())
        .collect(Collectors.joining("&"));
    return new URL(parts[0] + "?" + query);
  }


  /**
   * Prepares the default HTTP headers for a DES Web API request, including auth information
   *
   * @return the Headers
   */
  private HttpHeaders getHttpHeaders() {
    final HttpHeaders requestHeaders = new HttpHeaders();
    requestHeaders.add(ACCEPT, APPLICATION_JSON_VALUE);
    requestHeaders.add(AUTHORIZATION, getBearerTokenHeader());
    requestHeaders.setContentType(APPLICATION_JSON);
    return requestHeaders;
  }

  /**
   * Prepares a default HTTP Entity for a DES Web API request
   *
   * @return the {@link HttpEntity}
   */
  private HttpEntity<?> getHttpEntity() {
    return new HttpEntity<>(getHttpHeaders());
  }

  /**
   * Opens a connection to a DES Web API endpoint
   *
   * @param url the API endpoint
   * @return the {@link HttpURLConnection}
   */
  @Nonnull
  private HttpURLConnection getHttpURLConnection(
      @Nonnull final URL url) throws IOException, HttpException {
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("GET");
    conn.setRequestProperty(ACCEPT, "application/json");
    conn.setRequestProperty(AUTHORIZATION, getBearerTokenHeader());

    conn.setDoInput(true);

    if (conn.getResponseCode() != 200) {
      throw new HttpException("Failed : HTTP error code : "
          + conn.getResponseCode() + " : " + conn.getResponseMessage());
    }
    return conn;
  }


  @Override
  public boolean uploadXmlModel(
      @Nonnull final SemanticModelInfo manifest,
      @Nonnull final byte[] fileContents) {
    if (!online) {
      logger.warn("Client is offline - unable to upload model");
      return false;
    }

    var repositoryId = manifest.getPlaceId();
    var path = manifest.getPath();
    var name = manifest.getName();
    var mimeType = manifest.getMimetype();
    var version = manifest.getVersion();
    var state = manifest.getState();

    // first make sure mimetype is in correct format for API call
    mimeType = getStandardXmlMimeType(mimeType).orElse(mimeType);
    URI uri;

    // NOTE: MUST Use UriComponentBuilder to handle '+' in the MimeType, otherwise it will be
    // double-encoded and request will fail to return all values expected
    // See: https://docs.spring.io/spring/docs/current/spring-framework-reference/web.html#web-uri-encoding for details
    if (null == version || null == state) {
      // using name here allows for the name of the file to be different from the
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

    try {

      MultipartEntityBuilder mb = MultipartEntityBuilder.create();
      mb.addBinaryBody("file", fileContents);

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
      addFileData(name, fileContents, body, fout, boundary);
      addCloseDelimiter(body, boundary);

      conn.getInputStream().close();
      fout.close();
      return true;
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      return false;
    }
  }

  /**
   * Adds a file to a multipart (upload) request
   *
   * @param filename     the file name
   * @param byteStream   the file content
   * @param body         the Body request writer
   * @param directOutput the Body data writer
   * @param boundary     separator
   */
  private void addFileData(
      @Nonnull final String filename,
      @Nonnull final byte[] byteStream,
      @Nonnull final PrintWriter body,
      @Nonnull final OutputStream directOutput,
      @Nonnull final String boundary)
      throws IOException {
    body.append("--")
        .append(boundary)
        .append(CRLF)
        .append("Content-Disposition: form-data; name=\"")
        .append("file").append("\"; filename=\"").append(filename).append("\"")
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

  /**
   * @return the API Bearer token header
   */
  public String getBearerTokenHeader() {
    return "Bearer " + token;
  }

  @Override
  @Nonnull
  public ResultSet askQuery(
      @Nonnull final Query query) {
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

    try (var exec = sparqlService(sparqlEndpoint, query, httpClient)) {
      return ResultSetFactory.copyResults(exec.execSelect());
    }
  }

}
