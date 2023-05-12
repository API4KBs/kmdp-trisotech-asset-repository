package edu.mayo.kmdp.trisotechwrapper.components.operators;

import static edu.mayo.kmdp.registry.Registry.BASE_UUID_URN;

import edu.mayo.kmdp.registry.Registry;
import edu.mayo.kmdp.trisotechwrapper.components.operators.KEMtoMVFTranslator.IndexableMVFEntry;
import edu.mayo.kmdp.trisotechwrapper.config.TTConstants;
import edu.mayo.kmdp.trisotechwrapper.config.TTWConfigParamsDef;
import edu.mayo.kmdp.trisotechwrapper.config.TTWEnvironmentConfiguration;
import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.Code;
import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.ExtensionElement;
import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.GraphTerm;
import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.KemConcept;
import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.KemConceptProperties;
import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.KemModel;
import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.Stencil;
import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.Tag;
import edu.mayo.kmdp.util.StreamUtil;
import edu.mayo.kmdp.util.Util;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jsoup.Jsoup;
import org.omg.spec.mvf._20220702.mvf.MVFDictionary;
import org.omg.spec.mvf._20220702.mvf.MVFEntry;
import org.omg.spec.mvf._20220702.mvf.Vocabulary;

/**
 * Helper class used to extract elements from a KEM Model
 */
public final class KEMHelper {

  private KEMHelper() {
    // functions only
  }

  /**
   * Looks up an Asset ID from the custom attribute on the KEM model
   *
   * @param kem              the Model
   * @param assetIdAttribute the name of the custom attribute whose value is the Asset ID
   * @return the asserted Asset ID, if any
   */
  @Nonnull
  public static Optional<String> getAssetId(
      @Nonnull final KemModel kem,
      @Nonnull final String assetIdAttribute) {
    return kem.getProperties().getExtensionElements().stream()
        .filter(m -> "semanticType".equals(m.getSemanticType()))
        .filter(m -> assetIdAttribute.equals(m.getKey()))
        .map(ExtensionElement::getValue)
        .filter(Objects::nonNull)
        .findFirst();
  }

  /**
   * Generates an Enterprise Artifact ID for a KEM model
   * <p>
   * Replaces the Trisotech native namespace with the enterprise namespace
   *
   * @param kem the Model
   * @param cfg the environment configuration, providing the enterprise namespaces
   * @return the Enterprise Artifact ID (preserves the UUID)
   */
  @Nonnull
  public static String getArtifactId(
      @Nonnull final KemModel kem,
      @Nonnull final TTWEnvironmentConfiguration cfg) {
    var internal = getModelUri(kem);
    var ns = cfg.getTyped(TTWConfigParamsDef.ARTIFACT_NAMESPACE, String.class);
    return internal.replace(TTConstants.TT_BASE_MODEL_URI, ns);
  }

  /**
   * Gets the Trisotech internal model URI for a KEM model
   *
   * @param kem the Model
   * @return the Trisotech Model URI
   */
  @Nonnull
  public static String getModelUri(
      @Nonnull final KemModel kem) {
    return kem.getProperties().getTargetNamespace();
  }

  /**
   * Gets the name of a KEM Model
   *
   * @param kem the Model
   * @return the Model name
   */
  @Nonnull
  public static String getVocabName(
      @Nonnull final KemModel kem) {
    return kem.getProperties().getName();
  }


  /**
   * Extracts the KEM concepts from a KEM Model
   *
   * @param kem the Model
   * @return the KEM Concepts, as a Map
   */
  @Nonnull
  public static Map<UUID, KemConcept> getKEMConcepts(
      @Nonnull final KemModel kem) {
    var localConcepts = kem.getNodeModelElements().stream()
        .filter(n -> "term".equals(n.getStencil().getId()));
    var externalConcepts = kem.getItemModelElements().getGraphTerms().stream()
        .map(KEMHelper::graphTermToKemConcept);
    return Stream.concat(localConcepts, externalConcepts)
        .collect(Collectors.toMap(
            KEMHelper::getInternalUUID,
            kc -> kc
        ));
  }

  /**
   * Rewrites a GraphTerm into a KemConcept
   * <p>
   * GraphTerm is used as a foreign KEM Concept reuse, so that Concept 1 defined in model A can be
   * linked from Concept 2 in model B, even if (at this time) model B cannot import model A.
   * GraphTerm has the majority of the necessary information from the referenced Concept, but does
   * not have the same shape - hence the need to normalize
   * <p>
   * Note that this method does not need to resolve the reference, and actually load the referenced
   * model.
   *
   * @param graphTerm the (reference to) the reused Concept
   * @return a KemConcept that mimics the foreign Concept
   */
  public static KemConcept graphTermToKemConcept(
      @Nonnull final GraphTerm graphTerm) {
    var kc = new KemConcept();
    var tgtProperties = new KemConceptProperties()
        .withName(graphTerm.getProperties().getName());

    graphTerm.getProperties().getExtensionElements().stream()
        .filter(x -> "tags".equals(x.getSemanticType()))
        .forEach(tgtProperties.getExtensionElements()::add);

    graphTerm.getProperties().getExtensionElements().stream()
        .filter(x -> "code".equals(x.getSemanticType()))
        .map(x -> new Code()
            .withId(x.getId())
            .withCodingSystem(x.getCodingSystem())
            .withDisplay(x.getDisplay())
            .withValue(x.getValue())
            .withSemanticType("code"))
        .forEach(tgtProperties.getCode()::add);

    graphTerm.getProperties().getExtensionElements().stream()
        .filter(x -> "reuseLink".equals(x.getSemanticType()))
        .map(x -> x.getUri().substring(x.getUri().lastIndexOf('#') + 1))
        .findFirst()
        .ifPresent(kc::withResourceId);

    return kc.withStencil(new Stencil()
            .withId("term"))
        .withProperties(tgtProperties);
  }

  /**
   * Extracts the Tags for a KEM Concept
   *
   * @param kc the KEM Concept
   * @return the KEM Tags on that concept, as a unique List
   */
  @Nonnull
  public static List<String> getTags(
      @Nonnull final KemConcept kc) {
    return kc.getProperties().getExtensionElements().stream()
        .filter(x -> "tags".equals(x.getSemanticType()))
        .flatMap(x -> x.getTag().stream())
        .map(Tag::getContent)
        .distinct()
        .collect(Collectors.toList());
  }


  /**
   * Extracts the standard Concept URI for a KEM Concept
   * <p>
   * Looks for a custom attribute 'conceptId' whose value has the form 'prefix:ID', if any
   *
   * @param kc           the KEM Concept
   * @param namespaceMap the prefix/namespace map configured on the model
   * @return the Concept URI, if any
   */
  @Nonnull
  public static Optional<String> getExternalConceptURI(
      @Nonnull final KemConcept kc,
      @Nonnull final Map<String, String> namespaceMap) {
    var fqn = kc.getProperties().getExtensionElements().stream()
        .filter(x -> "customAttribute".equals(x.getSemanticType()))
        .filter(x -> "conceptId".equals(x.getKey()))
        .map(ExtensionElement::getValue)
        .filter(Util::isNotEmpty)
        .findFirst();
    return fqn.map(n -> {
      int i = n.indexOf(':');
      var ns = namespaceMap.getOrDefault(n.substring(0, i), BASE_UUID_URN);
      var id = n.substring(i + 1).trim();
      return ns + id;
    });
  }


  /**
   * Extracts the native UUID associated to a KEM Concept
   *
   * @param kc the KEM Concept
   * @return the Concept UUID, parsed
   */
  @Nonnull
  public static UUID getInternalUUID(
      @Nonnull final KemConcept kc) {
    return getInternalUUID(kc.getResourceId());
  }

  /**
   * Extracts the native UUID associated to a KEM Concept
   *
   * @param conceptId the KEM Concept ID
   * @return the Concept UUID, parsed
   */
  @Nonnull
  public static UUID getInternalUUID(
      @Nonnull final String conceptId) {
    return UUID.fromString(conceptId.substring(1));
  }

  /**
   * Extracts the SemanticLink annotations on a KEM Concept
   *
   * @param kc the KEM Concept
   * @return the SemanticLink (accelerator IDs/URIs), as a unique list
   */
  @Nonnull
  public static List<String> getSemanticAnnotation(
      @Nonnull final KemConcept kc) {
    return kc.getProperties().getExtensionElements().stream()
        .filter(x -> "semanticLink".equals(x.getSemanticType()))
        .filter(x -> "graph".equals(x.getType()))
        .map(ExtensionElement::getUri)
        .distinct()
        .collect(Collectors.toList());
  }

  /**
   * Ensures that an mvf:MVFDictionary is linked to an mvf:Vocabulary with the given name and URI.
   * If not, builds and links an empty one
   *
   * @param dict                the MVF dictionary
   * @param codingSystem        the MVF vocabulary URI
   * @param codingSystemDisplay the MVF vocabulary Name
   * @return the Vocabulary
   */
  @Nonnull
  public static Vocabulary ensureVocabulary(
      @Nonnull final MVFDictionary dict,
      @Nonnull final String codingSystem,
      @Nonnull final String codingSystemDisplay) {
    return dict.getVocabulary().stream()
        .filter(v -> codingSystem.equals(v.getUri()))
        .findFirst()
        .orElseGet(() -> {
          var newVoc = new Vocabulary()
              .withUri(codingSystem)
              .withName(codingSystemDisplay);
          dict.withVocabulary(newVoc);
          return newVoc;
        });
  }


  /**
   * Extracts the ontology prefixes declared in a KEM model.
   * <p>
   * Since KEM does not support prefixes natively, prefixes are asserted as custom attributes, with
   * key "prefix" and a value that follows the pattern {prefix} : <{namespace}>, akin to prefixes
   * declared in a SPARQL query
   *
   * @param kem the KEM model
   * @return the prefixes, in a {prefix}/{namespace} map
   */
  @Nonnull
  public static Map<String, String> getPrefixes(
      @Nonnull final KemModel kem) {
    return kem.getProperties().getExtensionElements().stream()
        .filter(x -> "customAttribute".equals(x.getSemanticType().trim()))
        .filter(x -> "prefix".equals(x.getKey().trim()))
        .map(ExtensionElement::getValue)
        .map(x -> {
          int idx = x.indexOf(':');
          var pref = x.substring(0, idx).trim();
          var val = x.substring(idx + 1);
          var ns = val.substring(val.indexOf('<') + 1, val.indexOf('>')).trim();
          return new String[]{pref, ns};
        })
        .collect(Collectors.toMap(
            s -> s[0].trim(),
            s -> s[1].trim()
        ));
  }

  /**
   * Determines the URI namespace to associate to a KEM Concept.
   * <p>
   * If the Concept is tagged with a known namespace prefix, that prefix will be used. Otherwise,
   * will default to BASE_UUID_URN
   *
   * @param kc           the KEM Concept
   * @param namespaceMap the prefix/namespace map configured on the model
   * @return the namespace the concept has been tagged to, {@link Registry#BASE_UUID_URN} otherwise
   */
  @Nonnull
  public static String resolveNamespace(
      @Nonnull final KemConcept kc,
      @Nonnull final Map<String, String> namespaceMap) {
    return getTags(kc).stream()
        .filter(namespaceMap::containsKey)
        .findFirst()
        .map(namespaceMap::get)
        .orElse(BASE_UUID_URN);
  }

  /**
   * Extracts the Documentation of a KEM Concept
   *
   * @param kc the KEM Concept
   * @return the Documentation text, if any
   */
  @Nonnull
  public static Optional<String> getDocumentation(
      @Nonnull final KemConcept kc) {
    return sanitizeHTML(kc.getProperties().getDocumentation());
  }

  /**
   * Extracts the Examples of a KEM Concept
   *
   * @param kc the KEM Concept
   * @return the Examples text, if any
   */
  @Nonnull
  public static Optional<String> getExamples(
      @Nonnull final KemConcept kc) {
    return sanitizeHTML(kc.getProperties().getExamples());
  }

  /**
   * Extracts the Notes of a KEM Concept
   *
   * @param kc the KEM Concept
   * @return the Notes text, if any
   */
  @Nonnull
  public static Optional<String> getNotes(
      @Nonnull final KemConcept kc) {
    return kc.getProperties().getExtensionElements().stream()
        .filter(x -> "notes".equals(x.getSemanticType()))
        .map(ExtensionElement::getContent)
        .flatMap(x -> sanitizeHTML(x).stream())
        .findFirst();
  }


  /**
   * Gets the primary name of a KEM Concept
   *
   * @param kc the Concept
   * @return the Concept name
   */
  @Nonnull
  public static String getConceptName(
      @Nonnull final KemConcept kc) {
    return kc.getProperties().getName();
  }


  /**
   * The URI of a KEM Concept, as {artifactId}#{concept UUID}
   *
   * @param kem  the KEM Model
   * @param guid the unique UUID of the Concept
   * @return the Concept URI, as a KEM Model scoped fragment
   */
  @Nonnull
  public static String getInternalConceptUri(
      @Nonnull final KemModel kem,
      @Nonnull final UUID guid,
      @Nonnull final TTWEnvironmentConfiguration cfg) {
    return getArtifactId(kem, cfg) + "#" + guid;
  }

  /**
   * Rmoves the HTML markup from the text, if any
   *
   * @return the text, without the HTML markup, if not empty
   */
  @Nonnull
  private static Optional<String> sanitizeHTML(
      @Nullable String html) {
    return Util.isEmpty(html)
        ? Optional.empty()
        : Optional.of(html)
            .map(s -> Jsoup.parse(s).text())
            .filter(Util::isNotEmpty);
  }

  /**
   * Resolves a KEM concept reference, as usually found in a KEM edge, to the {@link MVFEntry}
   * mapped from the KEM concept that the reference is pointing to
   *
   * @param ref  the KEM element UUID
   * @param dict the MVF dictionary
   * @return the referenced MVFEntry, if any
   */
  public static Optional<MVFEntry> resolveReference(String ref, MVFDictionary dict) {
    UUID guid = getInternalUUID(ref);
    return dict.getEntry().stream()
        .flatMap(StreamUtil.filterAs(IndexableMVFEntry.class))
        .filter(e -> e.getGuid().equals(guid))
        .map(MVFEntry.class::cast)
        .findFirst()
        .or(() -> dict.getEntry().stream()
            .filter(x -> x.getReference().stream().anyMatch(r -> r.contains(ref)))
            .findFirst());
  }


  /**
   * Creates an MVFEntry reference from another MVFEntry
   *
   * @param entry the source MVFEntry
   * @return a 'reference' to the source entry
   */
  @Nonnull
  public static MVFEntry toRef(
      @Nonnull final MVFEntry entry) {
    return new MVFEntry()
        .withUri(entry.getUri());
  }
}
