package edu.mayo.kmdp.trisotechwrapper.components.operators;

import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.KemConcept;
import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.KemModel;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nonnull;
import org.omg.spec.mvf._20220702.mvf.MVFDictionary;
import org.omg.spec.mvf._20220702.mvf.MVFEntry;


/**
 * Interface for pluggable domain-specific components that refine the KEM/MVF translation.
 * <p>
 * {@link KEMtoMVFTranslatorExtension} are executed after the main {@link KEMtoMVFTranslator} logic
 * has run, thus having access to the source KEM model and the generated MVF model.
 * <p>
 * The extensions pre- and post-process the MVF model globally. In between, they process each KEM
 * Concept individually, to generate additional MVF information, if the KEM concept meets some
 * applicability criteria.
 */
public interface KEMtoMVFTranslatorExtension {

  /**
   * Applies additional logic to the KEM/MVF pair
   *
   * @param dict        the generated MVF model
   * @param kemConcepts the KEM concepts in the original KEM model
   * @param kem         the original KEM model, for context
   */
  default void apply(
      @Nonnull final MVFDictionary dict,
      @Nonnull final Map<UUID, KemConcept> kemConcepts,
      @Nonnull final KemModel kem) {
    preProcess(dict, kemConcepts, kem);
    kemConcepts.values().stream()
        .filter(this::appliesTo)
        .forEach(kc -> this.process(kc, dict, kem));
    postProcess(dict, kemConcepts, kem);
  }

  /**
   * Pre-processes all the concepts in the KEM/MVF pair, before this extension's operations are
   * applied to each concept
   *
   * @param dict        the generated MVF model
   * @param kemConcepts the KEM concepts in the original KEM model
   * @param kem         the original KEM model, for context
   */
  default void preProcess(
      @Nonnull final MVFDictionary dict,
      @Nonnull final Map<UUID, KemConcept> kemConcepts,
      @Nonnull final KemModel kem) {
    // do nothing by default
  }

  /**
   * Post-processes all the concepts in the KEM/MVF pair, after this extension's operations are
   * applied to each concept   *
   *
   * @param dict        the generated MVF model
   * @param kemConcepts the KEM concepts in the original KEM model
   * @param kem         the original KEM model, for context
   */
  default void postProcess(
      @Nonnull final MVFDictionary dict,
      @Nonnull final Map<UUID, KemConcept> kemConcepts,
      @Nonnull final KemModel kem) {
    // do nothing by default
  }

  /**
   * Predicate. Determines whether this extension is expected or capable to perform additional
   * operations on a given KEM concept
   *
   * @param kc the KEM concept to be assessed
   * @return true if this extension is supposed to process this concept
   */
  default boolean appliesTo(
      @Nonnull final KemConcept kc) {
    return true;
  }


  /**
   * Applies this extension's operations to a given KEM concept
   *
   * @param kc   the KEM concept to be processed
   * @param dict the generated MVF model
   * @param kem  the original KEM model, for context
   */
  void process(
      @Nonnull final KemConcept kc,
      @Nonnull final MVFDictionary dict,
      @Nonnull final KemModel kem);


  /**
   * Retrieves the MVFEntry derived from a given KEM Concept
   *
   * @param kc   the KEM Concept
   * @param dict the MVF Dictionary generated from the KEM Concept's model
   * @return the MVFEntry in dict which derives from kc
   */
  default MVFEntry lookup(
      @Nonnull final KemConcept kc,
      @Nonnull final MVFDictionary dict) {
    var kcKey = kc.getResourceId().substring(1);
    return dict.getEntry().stream()
        .filter(e -> e.getUri().contains(kcKey))
        .findFirst()
        .orElseThrow(IllegalStateException::new);
  }

  /**
   * Looks up the MVFEntry for a given MVFEntry Reference (an MVFEntry that only has the ID of the
   * full MVFEntry)
   *
   * @param ref  the reference
   * @param dict the {@link MVFDictionary} to resolve the reference in
   * @return the full MVFEntry for the ID in ref, or ref itself if not found
   */
  default MVFEntry lookupRef(
      @Nonnull final MVFEntry ref,
      @Nonnull final MVFDictionary dict) {
    return dict.getEntry().stream()
        .filter(entry -> entry.getUri().equals(ref.getUri()))
        .findFirst()
        .orElse(ref);
  }
}
