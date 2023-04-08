package edu.mayo.kmdp.kdcaci.knew.trisotech.components;

import static edu.mayo.kmdp.kdcaci.knew.trisotech.TrisotechArtifactRepository.ALL_REPOS;
import static edu.mayo.kmdp.util.PropertiesUtil.serializeProps;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.services.transrepresentation.ModelMIMECoder.decodeAll;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.HTML;

import edu.mayo.kmdp.language.translators.surrogate.v2.SurrogateV2toHTMLTranslator;
import edu.mayo.kmdp.trisotechwrapper.components.NamespaceManager;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.TransxionApiInternal._applyTransrepresent;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.repository.asset.KARSHrefBuilder;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TTContentNegotiationHelper {

  /**
   * Logger
   */
  private static final Logger logger = LoggerFactory.getLogger(TTContentNegotiationHelper.class);

  /**
   * The {@link KARSHrefBuilder} used to map URIs to URLs relative to this server's deployment
   */
  @Autowired(required = false)
  @Nullable
  private KARSHrefBuilder hrefBuilder;

  /**
   * The namespace manager used to rewrite the Trisotech native URIs into platform URIs
   */
  @Autowired
  private NamespaceManager names;

  /**
   * The translator used to provide a basic HTML rendering of the {@link KnowledgeAsset} surrogates,
   * with navigable linked data
   */
  private final _applyTransrepresent htmlTranslator = new SurrogateV2toHTMLTranslator();


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

}
