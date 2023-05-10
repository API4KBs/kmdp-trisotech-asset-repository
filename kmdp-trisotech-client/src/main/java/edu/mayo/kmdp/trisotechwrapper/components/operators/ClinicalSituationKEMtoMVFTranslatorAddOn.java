package edu.mayo.kmdp.trisotechwrapper.components.operators;

import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.EdgeModelElement;
import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.ExtensionElement;
import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.KemConcept;
import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.KemModel;
import edu.mayo.kmdp.util.StreamUtil;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.omg.spec.mvf._20220702.mvf.MVFDictionary;
import org.omg.spec.mvf._20220702.mvf.MVFElement;
import org.omg.spec.mvf._20220702.mvf.MVFEntry;

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


  /**
   * Predicate. Applies to KEM concepts annotated with a CSO pattern concept
   *
   * @param kc the KEM concept to be assessed
   * @return true if this extension is supposed to process this concept
   */
  @Override
  public boolean appliesTo(
      @Nonnull final KemConcept kc) {
    return detectSituationPattern(kc).isPresent();
  }

  /**
   * Processes the KEM concept as a CSO situation (concept).
   * <p>
   * Adds the CSO pattern class as a broader parent, and the related concept as a focal concept
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
        .ifPresent(entry::withBroader);

    detectFocalConcept(kc, dict, kem)
        .map(uri -> new MVFEntry()
            .withUri(uri))
        .ifPresent(entry::withContext);
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


}
