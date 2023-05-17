package edu.mayo.kmdp.kdcaci.knew.trisotech.components;

import static edu.mayo.kmdp.kdcaci.knew.trisotech.TrisotechArtifactRepository.ALL_REPOS;
import static edu.mayo.kmdp.util.PropertiesUtil.serializeProps;
import static edu.mayo.ontology.taxonomies.ws.responsecodes.ResponseCodeSeries.NotAcceptable;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.Answer.failed;
import static org.omg.spec.api4kp._20200801.contrastors.SyntacticRepresentationContrastor.theRepContrastor;
import static org.omg.spec.api4kp._20200801.services.transrepresentation.ModelMIMECoder.decodeAll;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.TXT;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.HTML;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;
import static org.omg.spec.api4kp._20200801.taxonomy.krserialization.KnowledgeRepresentationLanguageSerializationSeries.RDF_XML_Syntax;
import static org.omg.spec.api4kp._20200801.taxonomy.krserialization.KnowledgeRepresentationLanguageSerializationSeries.Turtle;

import edu.mayo.kmdp.kdcaci.knew.trisotech.components.translators.MCBKSurrogateV2ToRDFTranslator;
import edu.mayo.kmdp.language.translators.surrogate.v2.SurrogateV2toHTMLTranslator;
import edu.mayo.kmdp.trisotechwrapper.components.NamespaceManager;
import edu.mayo.kmdp.util.Util;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Properties;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.TransxionApiInternal;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.TransxionApiInternal._applyTransrepresent;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.repository.asset.KARSHrefBuilder;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TTContentNegotiationHelper {

  /**
   * Logger
   */
  private static final Logger logger = LoggerFactory.getLogger(TTContentNegotiationHelper.class);

  /**
   * The {@link KARSHrefBuilder} used to map URIs to URLs relative to this server's deployment
   */
  protected final KARSHrefBuilder hrefBuilder;

  /**
   * The namespace manager used to rewrite the Trisotech native URIs into platform URIs
   */
  protected final NamespaceManager names;

  /**
   * The translator used to provide a basic HTML rendering of the {@link KnowledgeAsset} surrogates,
   * with navigable linked data
   */
  private final _applyTransrepresent htmlTranslator = new SurrogateV2toHTMLTranslator();

  private final _applyTransrepresent rdfTranslator = new MCBKSurrogateV2ToRDFTranslator();

  private final TransxionApiInternal translator;


  public TTContentNegotiationHelper(
      @Nonnull NamespaceManager names,
      @Nullable KARSHrefBuilder hrefBuilder,
      @Nullable TransxionApiInternal translator) {
    this.names = names;
    this.hrefBuilder = hrefBuilder;
    this.translator = translator;
  }

  /**
   * Predicate
   * <p>
   * Determines whether an X-Accept's first representation preference is HTML
   *
   * @param xAccept a negotiation preference, usually client-provided
   * @return true if HTML is the first preference
   */
  public static boolean negotiateHTML(String xAccept) {
    return decodeAll(xAccept).stream().findFirst()
        .filter(wr -> HTML.sameAs(wr.getRep().getLanguage()))
        .isPresent();
  }

  /**
   * Predicate
   * <p>
   * Determines whether an X-Accept's first representation preference is RDF+XML
   *
   * @param xAccept a negotiation preference, usually client-provided
   * @return true if RDF+XML is the first preference
   */
  public static boolean negotiateRDF(String xAccept) {
    return "application/rdf+xml".equals(xAccept)
        || "text/turtle".equals(xAccept)
        || decodeAll(xAccept).stream().findFirst()
        .filter(wr -> OWL_2.sameAs(wr.getRep().getLanguage()))
        .isPresent();
  }


  /**
   * Converts a Surrogate, wrapped in a KnowledgeCarrier, to its HTML variant
   * <p>
   * Redirects the Asset namespace base URI to this server, making the links in the HTML more
   * navigable. Note that this redirect is a best effort operation, which is not guaranteed.
   *
   * @param surrogateCarrier the KnowledgeAsset, in a KnowledgeCarrier
   * @return the KnowledgeAsset HTML variant, in a KnowledgeCarrier, wrapped by Answer
   */
  public Answer<KnowledgeCarrier> toHtml(
      @Nonnull final KnowledgeCarrier surrogateCarrier) {
    return htmlTranslator.applyTransrepresent(
        surrogateCarrier,
        codedRep(HTML),
        configureRedirects());
  }

  /**
   * Converts a Surrogate, wrapped in a KnowledgeCarrier, to its RDF variant
   *
   * @param surrogateCarrier the KnowledgeAsset, in a KnowledgeCarrier
   * @return the KnowledgeAsset RDF variant, in a KnowledgeCarrier, wrapped by Answer
   */
  public Answer<KnowledgeCarrier> toRdf(
      @Nonnull final KnowledgeCarrier surrogateCarrier,
      @Nullable final String xAccept) {
    String codedRep;
    if ("application/rdf+xml".equals(xAccept)) {
      codedRep = codedRep(OWL_2, RDF_XML_Syntax, XML_1_1, Charset.defaultCharset());
    } else if ("text/turtle".equals(xAccept)) {
      codedRep = codedRep(OWL_2, Turtle, TXT, Charset.defaultCharset());
    } else {
      throw new IllegalStateException("Not possible");
    }
    return rdfTranslator.applyTransrepresent(
        surrogateCarrier,
        codedRep,
        configureRedirects());
  }

  @Nullable
  private String configureRedirects() {
    try {
      if (hrefBuilder != null) {
        Properties props = new Properties();
        setAssetRedirect(hrefBuilder.getHost(), names.getAssetNamespace(), props);
        setArtifactRedirect(hrefBuilder.getHost(), names.getArtifactNamespace(), props);
        return serializeProps(props);
      }
    } catch (Exception e) {
      // fall back to not rewriting the URIs/URLs
    }
    logger.warn("Unable to detect server deployment, URIs will not be mapped to URLs");
    return null;
  }

  /**
   * Configures the HTML renderer of a {@link KnowledgeAsset} Surrogate, to map URIs linking to the
   * Asset Repository APIs with URLs to the same APIs, but grounded on this server's deployment,
   * assuming that the URIs are not natively dereferenceable
   * <p>
   * Assumes that the APIs are extended out of an enterprise base URI, which acts as a namespace
   *
   * @param host      the baseUrl where this server is deployed
   * @param namespace the enterprise Asset namespace
   * @param props     the configuration to update
   */
  public static void setAssetRedirect(
      @Nonnull final String host,
      @Nonnull final URI namespace,
      @Nonnull final Properties props) throws URISyntaxException {
    var hostUri = URI.create(host);
    var redirect = new URI(hostUri.getScheme(), null, hostUri.getHost(), hostUri.getPort(),
        hostUri.getPath() + "/cat" + namespace.getPath(), null, null);
    props.put(namespace.toString(), redirect.toString());
  }


  /**
   * Configures the HTML renderer of a {@link KnowledgeAsset} Surrogate, to map URIs linking to the
   * Artifact Repository APIs with URLs to the same APIs, but grounded on this server's deployment,
   * assuming that the URIs are not natively dereferenceable
   * <p>
   * Assumes that the APIs are extended out of an enterprise base URI, which acts as a namespace
   *
   * @param host      the baseUrl where this server is deployed
   * @param namespace the enterprise Artifact namespace
   * @param props     the configuration to update
   */
  public static void setArtifactRedirect(
      @Nonnull final String host,
      @Nonnull final URI namespace,
      @Nonnull final Properties props) throws URISyntaxException {
    var hostUri = URI.create(host);
    var redirect = new URI(hostUri.getScheme(), null, hostUri.getHost(), hostUri.getPort(),
        hostUri.getPath() + "/repos/" + ALL_REPOS + namespace.getPath(), null, null);
    props.put(namespace.toString(), redirect.toString());
  }


  /**
   * Determines whether content negotiation is necessary for a given Artifact to be returned.
   * <p>
   * Negotiation is necessary if the client has a preference, and none of the preferences match the
   * current form of the Artifact
   *
   * @param kc      the Artifact in the current form
   * @param xAccept the client preferences
   * @return true if the current form does not match the preferences, if preferences are stated
   */
  public boolean needsVariant(
      @Nonnull final KnowledgeCarrier kc,
      @Nullable final String xAccept) {
    if (Util.isEmpty(xAccept)) {
      return false;
    }
    var preferences = decodeAll(xAccept);
    return preferences.stream().noneMatch(
        rep -> theRepContrastor.isBroaderOrEqual(rep.getRep(), kc.getRepresentation()));
  }

  /**
   * Tries to translate a given Artifact into the requested variant form. Delegates to a Translator,
   * which should have been loaded with the operators that provide the supported language mappings
   *
   * @param kc      the Artifact in the current form
   * @param xAccept the client preferences
   * @return the translated Artifact, or NotAcceptable if unable to honor the request
   */
  public Answer<KnowledgeCarrier> negotiate(
      @Nonnull final KnowledgeCarrier kc,
      @Nullable final String xAccept) {
    if (translator == null) {
      return failed(NotAcceptable);
    }
    if (xAccept == null) {
      return Answer.of(kc);
    }
    return translator.applyTransrepresent(kc, xAccept, null)
        .or(() -> failed(NotAcceptable));
  }

}
