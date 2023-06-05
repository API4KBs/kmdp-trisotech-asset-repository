package edu.mayo.kmdp.trisotechwrapper.components.operators;

import static edu.mayo.kmdp.trisotechwrapper.components.operators.KEMHelper.ensureVocabulary;
import static edu.mayo.kmdp.trisotechwrapper.components.operators.KEMHelper.toRef;

import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.EdgeModelElement;
import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.ExtensionElement;
import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.KemConcept;
import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.KemModel;
import edu.mayo.kmdp.util.NameUtils;
import edu.mayo.kmdp.util.StreamUtil;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.omg.spec.mvf._20220702.mvf.MVFDictionary;
import org.omg.spec.mvf._20220702.mvf.MVFElement;
import org.omg.spec.mvf._20220702.mvf.MVFEntry;
import org.omg.spec.mvf._20220702.mvf.VocabularyEntry;

/**
 * {@link KEMtoMVFTranslatorExtension} that interprets KEM Concepts as Clinical Situations from the
 * CSO ontology.
 * <p>
 * Clinical situations are detected using an annotation from the upper CSO pattern ontology
 * <p>
 * Assumes the presence of a 'prefix:id' value of a custom attribute 'conceptId' as the (CSV)
 * concept URI. It was considered to fall back to the KEM element URI with a default namespace, but
 * there is not enough control over the stability of that ID to meet the stability ad portability
 * requirement of a CSV ontology.
 * <p>
 * Uses the concept primary term as (pref) label. Other alternatives become altLabels.
 * <p>
 * The situation pattern is conveyed with an annotation (semanticLink), which becomes a broader
 * MVFEntry.
 * <p>
 * The situation is related to one (context) concept, which will be treated as a SNOMED focal
 * concept.
 */
public class ClinicalSituationKEMtoMVFTranslatorAddOn
    implements KEMtoMVFTranslatorExtension {


  private static final String CSV = "https://ontology.mayo.edu/taxonomies/clinicalsituations";

  /**
   * Predicate. Applies to KEM concepts annotated with a CSO pattern concept
   *
   * @param kc the KEM concept to be assessed
   * @return true if this extension is supposed to process this concept
   */
  @Override
  public boolean appliesTo(
      @Nonnull final KemConcept kc) {
    return detectSituationPattern(kc).isPresent()
        || detectSituationConceptMapping(kc).isPresent();
  }

  /**
   * Processes the KEM concept as a CSO situation (concept).
   * <p>
   * If realizing a Situation Pattern, adds the CSO pattern class as a broader parent, and the
   * related concept as a focal concept. This is not necessary if the KEM concept is manifesting a
   * Situation Concept.
   * <p>
   * Either way, creates a Vocabulary Entry to capture the associated code (UUID)
   *
   * @param kc   the KEM concept to be processed
   * @param dict the generated MVF model
   * @param kem  the original KEM model, for context
   */
  @Override
  public void process(
      @Nonnull final KemConcept kc,
      @Nonnull final MVFDictionary dict,
      @Nonnull final KemModel kem) {
    var entry = lookup(kc, dict);

    detectSituationPattern(kc)
        .map(uri -> new MVFEntry()
            .withExternalReference(uri))
        .ifPresent(pattern -> {
          entry.withBroader(pattern);
          detectFocalConcept(kc, dict, kem)
              .map(uri -> new MVFEntry()
                  .withUri(uri))
              .ifPresent(entry::withContext);
        });

    if (entry.getExternalReference() != null) {
      var vocab = ensureVocabulary(dict, CSV, "Clinical Situation Vocabulary");
      var vocabEntry = new VocabularyEntry()
          .withMVFEntry(toRef(entry))
          .withTerm(NameUtils.getTrailingPart(entry.getExternalReference()))
          .withName(entry.getName())
          .withDefinition(formalize(entry))
          .withUri(entry.getExternalReference());
      vocab.getEntry().add(vocabEntry);
    }
  }

  /**
   * Provides a semiformal definition of the Clinical Situation, expressed as a post-coordinated
   * term in a Manchester-like syntax
   *
   * @param entry the CSV concept, as a MVF concept
   * @return a definition of the concept, to be used in the construction of the formal definition of
   * the CSO class associated to this concept
   */
  private String formalize(MVFEntry entry) {
    var sup = Optional.ofNullable(entry.getBroader())
        .filter(x -> !x.isEmpty())
        .flatMap(x -> Optional.ofNullable(x.get(0).getExternalReference()));
    var focus = Optional.ofNullable(entry.getContext())
        .filter(x -> !x.isEmpty())
        .flatMap(x -> Optional.ofNullable(x.get(0)));
    var sb = new StringBuilder();
    sup.ifPresent(s -> sb.append("csv:").append(URI.create(s).getFragment()));
    if (sup.isPresent() && focus.isPresent()) {
      sb.append(" that ");
    }
    focus.ifPresent(f -> sb.append("targets some ")
        .append(f.getUri()));
    return sb.toString();
  }

  /**
   * Relates a KEM situation concept to its focal concept
   *
   * @param kc   the KEM concept to be processed
   * @param dict the generated MVF model
   * @param kem  the original KEM model, for context
   * @return the internal URI of the focus concept, if any
   */
  @Nonnull
  private Optional<String> detectFocalConcept(
      @Nonnull final KemConcept kc,
      @Nonnull final MVFDictionary dict,
      @Nonnull final KemModel kem) {
    return kem.getEdgeModelElements().stream()
        .filter(e -> Objects.equals(e.getSourceRef(), kc.getResourceId()))
        .map(EdgeModelElement::getTargetRef)
        .map(x -> KEMHelper.resolveReference(x, dict))
        .flatMap(StreamUtil::trimStream)
        .map(MVFElement::getUri)
        .findFirst();
  }


  /**
   * Detects the CSO pattern on a KEM concept
   *
   * @param kc the KEM concept
   * @return the URI of the concept that maps to a CSO upper class, if any
   */
  @Nonnull
  private Optional<String> detectSituationPattern(
      @Nonnull final KemConcept kc) {
    return kc.getProperties().getExtensionElements().stream()
        .filter(x -> "semanticLink".equals(x.getSemanticType()))
        .filter(x -> "http://ontology.mayo.edu/ontologies/clinicalsituationontology/".equals(
            x.getModelURI()))
        .filter(x -> "graph".equals(x.getType()))
        .map(ExtensionElement::getUri)
        .filter(
            x -> x.startsWith("https://ontology.mayo.edu/taxonomies/clinicalsituationpatterns#"))
        .findFirst();
  }

  /**
   * Detects the CSV annotation on a KEM concept
   *
   * @param kc the KEM concept
   * @return the URI of the concept that maps to a CSO upper class, if any
   */
  @Nonnull
  private Optional<String> detectSituationConceptMapping(
      @Nonnull final KemConcept kc) {
    return kc.getProperties().getExtensionElements().stream()
        .filter(x -> "semanticLink".equals(x.getSemanticType()))
        .filter(x -> "https://ontology.mayo.edu/taxonomies/clinicalsituations".equals(
            x.getModelURI()))
        .filter(x -> "graph".equals(x.getType()))
        .map(ExtensionElement::getUri)
        .filter(
            x -> x.startsWith("https://ontology.mayo.edu/taxonomies/clinicalsituations#"))
        .findFirst();
  }


}
