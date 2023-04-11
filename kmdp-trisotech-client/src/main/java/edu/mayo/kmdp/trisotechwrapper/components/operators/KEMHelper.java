package edu.mayo.kmdp.trisotechwrapper.components.operators;

import static edu.mayo.kmdp.registry.Registry.BASE_UUID_URN;

import edu.mayo.kmdp.registry.Registry;
import edu.mayo.kmdp.trisotechwrapper.config.TTConstants;
import edu.mayo.kmdp.trisotechwrapper.config.TTWConfigParamsDef;
import edu.mayo.kmdp.trisotechwrapper.config.TTWEnvironmentConfiguration;
import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.ExtensionElement;
import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.KemConcept;
import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.KemModel;
import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.Tag;
import edu.mayo.kmdp.util.Util;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.jsoup.Jsoup;
import org.omg.spec.mvf._20220702.mvf.MVFDictionary;
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
  public static Optional<String> getAssetId(
      KemModel kem,
      String assetIdAttribute) {
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
  public static String getArtifactId(KemModel kem, TTWEnvironmentConfiguration cfg) {
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
  public static String getModelUri(KemModel kem) {
    return kem.getProperties().getTargetNamespace();
  }

  /**
   * Gets the name of a KEM Model
   *
   * @param kem the Model
   * @return the Model name
   */
  public static String getVocabName(KemModel kem) {
    return kem.getProperties().getName();
  }


  /**
   * Extracts the KEM concepts from a KEM Model
   *
   * @param kem the Model
   * @return the KEM Concepts, as a Map
   */
  public static Map<UUID, KemConcept> getKEMConcepts(KemModel kem) {
    return kem.getNodeModelElements().stream()
        .filter(n -> "term".equals(n.getStencil().getId()))
        .collect(Collectors.toMap(
            KEMHelper::toConceptUUID,
            kc -> kc
        ));
  }

  /**
   * Extracts the Tags for a KEM Concept
   *
   * @param kc the KEM Concept
   * @return the KEM Tags on that concept, as a unique List
   */
  public static List<String> getTags(KemConcept kc) {
    return kc.getProperties().getExtensionElements().stream()
        .filter(x -> "tags".equals(x.getSemanticType()))
        .flatMap(x -> x.getTag().stream())
        .map(Tag::getContent)
        .distinct()
        .collect(Collectors.toList());
  }

  /**
   * Extracts the native UUID associated to a KEM Concept
   *
   * @param kc the KEM Concept
   * @return the Concept UUID, parsed
   */
  public static UUID toConceptUUID(KemConcept kc) {
    return toConceptUUID(kc.getResourceId());
  }

  /**
   * Extracts the native UUID associated to a KEM Concept
   *
   * @param conceptId the KEM Concept ID
   * @return the Concept UUID, parsed
   */
  public static UUID toConceptUUID(String conceptId) {
    return UUID.fromString(conceptId.substring(1));
  }

  /**
   * Extracts the SemanticLink annotations on a KEM Concept
   *
   * @param kc the KEM Concept
   * @return the SemanticLink (accelerator IDs/URIs), as a unique list
   */
  public static List<String> getSemanticAnnotation(KemConcept kc) {
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
  public static Vocabulary ensureVocabulary(
      MVFDictionary dict,
      String codingSystem,
      String codingSystemDisplay) {
    return dict.getVocabulary().stream()
        .filter(v -> codingSystem.equals(v.getLanguageCode()))
        .findFirst()
        .orElseGet(() -> {
          var newVoc = new Vocabulary()
              .withUri(codingSystem)
              .withLanguageCode(codingSystem)
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
  public static Map<String, String> getPrefixes(KemModel kem) {
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
  public static String resolveNamespace(KemConcept kc, Map<String, String> namespaceMap) {
    return kc.getProperties().getExtensionElements().stream()
        .filter(x -> "tags".equalsIgnoreCase(x.getSemanticType()))
        .flatMap(x -> x.getTag().stream())
        .map(Tag::getContent)
        .filter(namespaceMap::containsKey)
        .findFirst()
        .map(namespaceMap::get)
        .orElse(BASE_UUID_URN);
  }

  /**
   * Extracts the Documentation of a KEM Concept
   *
   * @param kc the KEM Concept
   * @return the Documentation text
   */
  public static Optional<String> getDocumentation(KemConcept kc) {
    return sanitizeHTML(kc.getProperties().getDocumentation());
  }

  /**
   * Extracts the Examples of a KEM Concept
   *
   * @param kc the KEM Concept
   * @return the Examples text
   */
  public static Optional<String> getExamples(KemConcept kc) {
    return sanitizeHTML(kc.getProperties().getExamples());
  }

  /**
   * Extracts the Notes of a KEM Concept
   *
   * @param kc the KEM Concept
   * @return the Notes text
   */
  public static Optional<String> getNotes(KemConcept kc) {
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
  public static String getConceptName(KemConcept kc) {
    return kc.getProperties().getName();
  }


  /**
   * Rmoves the HTML markup from the text, if any
   *
   * @return the text, without the HTML markup
   */
  @Nonnull
  private static Optional<String> sanitizeHTML(String html) {
    return Util.isEmpty(html)
        ? Optional.empty()
        : Optional.of(html)
            .map(s -> Jsoup.parse(s).text());
  }

}
