package edu.mayo.kmdp.kdcaci.knew.trisotech.components;

import static edu.mayo.kmdp.kdcaci.knew.trisotech.TrisotechArtifactRepository.ALL_REPOS;
import static edu.mayo.kmdp.util.PropertiesUtil.serializeProps;
import static edu.mayo.ontology.taxonomies.ws.responsecodes.ResponseCodeSeries.NotAcceptable;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.Answer.failed;
import static org.omg.spec.api4kp._20200801.contrastors.SyntacticRepresentationContrastor.theRepContrastor;
import static org.omg.spec.api4kp._20200801.services.transrepresentation.ModelMIMECoder.decodeAll;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateHelper.getSurrogateMetadata;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.JSON;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.TXT;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.HTML;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;
import static org.omg.spec.api4kp._20200801.taxonomy.krserialization.KnowledgeRepresentationLanguageSerializationSeries.RDF_XML_Syntax;
import static org.omg.spec.api4kp._20200801.taxonomy.krserialization.KnowledgeRepresentationLanguageSerializationSeries.Turtle;

import edu.mayo.kmdp.trisotechwrapper.components.NamespaceManager;
import edu.mayo.kmdp.util.Util;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.Properties;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.TransxionApiInternal;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.services.repository.asset.KARSHrefBuilder;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TTContentNegotiationHelper {

  /**
   * Logger
   */
  protected static final Logger logger = LoggerFactory.getLogger(TTContentNegotiationHelper.class);

  /**
   * The {@link KARSHrefBuilder} used to map URIs to URLs relative to this server's deployment
   */
  protected final KARSHrefBuilder hrefBuilder;

  /**
   * The namespace manager used to rewrite the Trisotech native URIs into platform URIs
   */
  protected final NamespaceManager names;

  /**
   * Translator used to generate artifact/model variants
   */
  protected final TransxionApiInternal translator;

  /**
   * Translator used to generate surrogate variants
   */
  protected final TransxionApiInternal surrogateTranslator;


  public TTContentNegotiationHelper(
      @Nonnull NamespaceManager names,
      @Nullable KARSHrefBuilder hrefBuilder,
      @Nullable TransxionApiInternal translator,
      @Nullable TransxionApiInternal surrogateTranslator) {
    this.names = names;
    this.hrefBuilder = hrefBuilder;
    this.translator = translator;
    this.surrogateTranslator = surrogateTranslator;
  }


  /**
   * Predicate
   * <p>
   * Determines whether an X-Accept's code designates a supported Surrogate variant
   *
   * @param xAccept a negotiation preference, usually client-provided
   * @return true if HTML, RDF or FHIR
   */
  public static boolean needsSurrogateVariant(
      @Nullable final String xAccept) {
    return negotiateHTML(xAccept) || negotiateRDF(xAccept) || negotiateFHIR(xAccept);
  }

  /**
   * Predicate
   * <p>
   * Determines whether an X-Accept's first representation preference is HTML
   *
   * @param xAccept a negotiation preference, usually client-provided
   * @return true if HTML is the first preference
   */
  public static boolean negotiateHTML(
      @Nullable final String xAccept) {
    return decodeAll(xAccept).stream().findFirst()
        .filter(wr -> HTML.sameAs(wr.getRep().getLanguage()))
        .isPresent();
  }

  /**
   * Predicate
   * <p>
   * Determines whether an X-Accept's first representation preference is FHIR
   *
   * @param xAccept a negotiation preference, usually client-provided
   * @return true if FHIR is the first preference
   */
  public static boolean negotiateFHIR(
      @Nullable final String xAccept) {
    return decodeAll(xAccept).stream().findFirst()
        .filter(wr -> FHIR_STU3.sameAs(wr.getRep().getLanguage()))
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
  public static boolean negotiateRDF(
      @Nullable final String xAccept) {
    return "application/rdf+xml".equals(xAccept)
        || "text/turtle".equals(xAccept)
        || decodeAll(xAccept).stream().findFirst()
        .filter(wr -> OWL_2.sameAs(wr.getRep().getLanguage()))
        .isPresent();
  }

  /**
   * Converts a Surrogate, wrapped in a KnowledgeCarrier, to a negotiated variant
   *
   * @param surrogateCarrier the KnowledgeAsset, in a KnowledgeCarrier
   * @param xAccept          the client's preferences, as a MIME code
   * @return the KnowledgeAsset RDF variant, in a KnowledgeCarrier, wrapped by Answer
   */
  @Nonnull
  public Answer<KnowledgeCarrier> negotiateSurrogate(
      @Nonnull final KnowledgeCarrier surrogateCarrier,
      @Nullable final String xAccept) {
    // xAccept should not be null at this point
    if (surrogateTranslator == null || xAccept == null) {
      return failed(NotAcceptable);
    }
    return surrogateTranslator.applyTransrepresent(
            surrogateCarrier,
            adjustSurrogateVariantMimeCode(xAccept),
            configureRedirects())
        .or(() -> failed(NotAcceptable));
  }

  /**
   * Normalizes the (client) provided MIME code to match the requirements of the Surrogate
   * translators.
   * <p>
   * This method should be eventually removed once more standardization around the use of MIME codes
   * can be enforced, and as the experimental translators become more mature and robust
   *
   * @param xAccept a negotiation preference, usually client-provided
   * @return the adjusted xAccept
   */
  @Nullable
  protected String adjustSurrogateVariantMimeCode(
      @Nullable final String xAccept) {
    if (negotiateHTML(xAccept)) {
      return codedRep(HTML);
    }
    if ("application/rdf+xml".equals(xAccept)) {
      return codedRep(OWL_2, RDF_XML_Syntax, XML_1_1, Charset.defaultCharset());
    } else if ("text/turtle".equals(xAccept)) {
      return codedRep(OWL_2, Turtle, TXT, Charset.defaultCharset());
    }
    if (negotiateFHIR(xAccept)) {
      return codedRep(FHIR_STU3, JSON, Charset.defaultCharset());
    }
    return null;
  }

  /**
   * For a given Surrogate variant language, returns a client-friendly, standard but informal MIME
   * code that can be used to designate that surrogate variant.
   * <p>
   * The result of this method is meant to be offered to the client, for them to construct URLs or
   * headers. The code will then be re-normalized on a client's request.
   *
   * @param lang the representation Language used in the denoted Surrogate variant
   * @return a standard but informal MIME code matching that variant's form
   * @see #adjustSurrogateVariantMimeCode(String)
   */
  @Nonnull
  protected String getFriendlySurrogateVariantMimeCode(
      @Nonnull final KnowledgeRepresentationLanguage lang) {
    switch (KnowledgeRepresentationLanguageSeries.asEnum(lang)) {
      case HTML:
        return "text/html";
      case FHIR_STU3:
        return "model/fhir-v3";
      case OWL_2:
        return "text/turtle";
      default:
        return "";
    }
  }


  /**
   * For a given Surrogate variant language, returns the canonical {@link SyntacticRepresentation},
   * such that can be encoded as a formal MIME code.
   * <p>
   * In contrast to {@link #getFriendlySurrogateVariantMimeCode(KnowledgeRepresentationLanguage)},
   * this method is intended for use in the canonical metadata graph
   *
   * @param lang the representation Language used in the denoted Surrogate variant
   * @return the {@link SyntacticRepresentation} of the default variant form of the Surrogate in
   * that language
   */
  protected Optional<SyntacticRepresentation> getSurrogateVariantRep(
      KnowledgeRepresentationLanguage lang) {
    SyntacticRepresentation rep = null;
    switch (KnowledgeRepresentationLanguageSeries.asEnum(lang)) {
      case HTML:
        rep = rep(HTML, TXT, Charset.defaultCharset(), Encodings.DEFAULT);
        break;
      case FHIR_STU3:
        rep = rep(FHIR_STU3, JSON, Charset.defaultCharset(), Encodings.DEFAULT);
        break;
      case OWL_2:
        rep = rep(OWL_2, Turtle, TXT, Charset.defaultCharset(), Encodings.DEFAULT);
        break;
      default:
    }
    return Optional.ofNullable(rep);
  }

  /**
   * Adds {@link KnowledgeArtifact} records for the Surrogate variants of a KnowledgeAsset
   *
   * @param surr the canonical Surrogate for a given Asset
   * @return the canonical Surrogate, with added records for the variants of that canonical
   * surrogate
   */
  @Nonnull
  public KnowledgeAsset addEmphemeralSurrogates(
      @Nonnull final KnowledgeAsset surr) {
    var self = getSurrogateMetadata(surr, Knowledge_Asset_Surrogate_2_0, null);
    self.ifPresent(ka -> {
      ensureEmphemeralSurrogateVariantRecord(surr, ka, OWL_2);
      ensureEmphemeralSurrogateVariantRecord(surr, ka, HTML);
      ensureEmphemeralSurrogateVariantRecord(surr, ka, FHIR_STU3);
    });
    return surr;
  }

  /**
   * Adds the {@link KnowledgeArtifact} record for a specific Surrogate variant of a KnowledgeAsset,
   * if not already present
   *
   * @param surrogate the canonical Surrogate for a given Asset
   * @param self      the {@link KnowledgeArtifact} record for the canonical surrogate itself
   * @param variant   the representation language of the surrogate variant to be recorded
   * @see #addEmphemeralSurrogateVariantRecord(KnowledgeAsset, KnowledgeArtifact,
   * KnowledgeRepresentationLanguage)
   */
  protected void ensureEmphemeralSurrogateVariantRecord(
      @Nonnull final KnowledgeAsset surrogate,
      @Nonnull final KnowledgeArtifact self,
      @Nonnull final KnowledgeRepresentationLanguage variant) {
    getSurrogateMetadata(surrogate, variant, null)
        .ifPresentOrElse(
            x -> {
            },
            () -> addEmphemeralSurrogateVariantRecord(surrogate, self, variant));
  }

  /**
   * Adds the {@link KnowledgeArtifact} record for a specific Surrogate variant of a KnowledgeAsset,
   * if not already present
   *
   * @param surrogate the canonical Surrogate for a given Asset
   * @param self      the {@link KnowledgeArtifact} record for the canonical surrogate itself
   * @param lang      the representation language of the surrogate variant to be recorded
   */
  protected void addEmphemeralSurrogateVariantRecord(
      KnowledgeAsset surrogate,
      KnowledgeArtifact self,
      KnowledgeRepresentationLanguage lang) {
    var repOpt = getSurrogateVariantRep(lang);
    if (repOpt.isEmpty()) {
      return;
    }
    var rep = repOpt.get();
    var codedRep = codedRep(rep);
    var other = new KnowledgeArtifact()
        .withArtifactId(SurrogateBuilder.defaultSurrogateId(surrogate.getAssetId(), lang))
        .withName(surrogate.getName() + " - " + lang + " Metadata Variant")
        .withLocator(
            URI.create(self.getLocator() + "?qAccept=" + getFriendlySurrogateVariantMimeCode(lang)))
        .withMimeType(codedRep)
        .withRepresentation(rep);
    surrogate.withSurrogate(other);
  }


  @Nullable
  protected String configureRedirects() {
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
