package edu.mayo.kmdp.trisotechwrapper.components.operators;

import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.ExtensionElement;
import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.KemConcept;
import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.KemModel;
import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.Tag;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.omg.spec.mvf._20220702.mvf.MVFDictionary;
import org.omg.spec.mvf._20220702.mvf.Vocabulary;

public final class KEMHelper {

  private KEMHelper() {
    // functions only
  }

  public static Optional<String> getAssetId(KemModel kem) {
    return kem.getProperties().getExtensionElements().stream()
        .map(Map.class::cast)
        .filter(m -> "customAttribute".equals(m.get("semanticType")))
        .filter(m -> "knowledgeAssetId".equals(m.get("key")))
        .map(m -> m.get("value"))
        .filter(Objects::nonNull)
        .map(String.class::cast)
        .findFirst();
  }

  public static String getArtifactId(KemModel kem) {
    return kem.getProperties().getTargetNamespace();
  }

  public static String getVocabName(KemModel kem) {
    return kem.getProperties().getName();
  }


  public static Map<UUID, KemConcept> getKEMConcepts(KemModel kem) {
    return kem.getNodeModelElements().stream()
        .filter(n -> "term".equals(n.getStencil().getId()))
        .collect(Collectors.toMap(
            kc -> toConceptId(kc.getResourceId()),
            kc -> kc
        ));
  }

  public static List<String> getTags(KemConcept kc) {
    return kc.getProperties().getExtensionElements().stream()
        .filter(x -> "tags".equals(x.getSemanticType()))
        .flatMap(x -> x.getTag().stream())
        .map(Tag::getContent)
        .collect(Collectors.toList());
  }

  public static UUID toConceptId(String kemConceptId) {
    return UUID.fromString(kemConceptId.substring(1));
  }


  public static Optional<String> getSemanticAnnotation(KemConcept kc) {
    return kc.getProperties().getExtensionElements().stream()
        .filter(x -> "semanticLink".equals(x.getSemanticType()))
        .filter(x -> "graph".equals(x.getType()))
        .map(ExtensionElement::getUri)
        .findFirst();
  }

  public static Vocabulary ensureVocabulary(
      MVFDictionary dict, String codingSystem, String codingSystemDisplay) {
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

}
