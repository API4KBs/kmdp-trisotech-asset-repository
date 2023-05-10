package edu.mayo.kmdp.trisotechwrapper.components.operators;

import static edu.mayo.kmdp.trisotechwrapper.components.operators.KEMHelper.ensureVocabulary;
import static edu.mayo.kmdp.trisotechwrapper.components.operators.KEMHelper.getTags;

import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.KemConcept;
import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.KemModel;
import edu.mayo.kmdp.util.NameUtils;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.omg.spec.mvf._20220702.mvf.MVFDictionary;
import org.omg.spec.mvf._20220702.mvf.MVFElement;
import org.omg.spec.mvf._20220702.mvf.MVFEntry;
import org.omg.spec.mvf._20220702.mvf.Vocabulary;
import org.omg.spec.mvf._20220702.mvf.VocabularyEntry;
import org.snomed.SCTCategories;

/**
 * {@link KEMtoMVFTranslatorExtension} that interprets KEM Concepts annotated with SNOMED Codes,
 * inferring a post-coordinated definition for the 'anonymous' terms based on the relationship to
 * other concepts, and expresses it as a SNOMED Compositional Grammar (SCG) expression
 * <p>
 * Applies to concepts that are tagged with a SNOMED qualifier (e.g. 'sct:disorder') but do not have
 * a SNOMEDCT code. Creates a MVF {@link Vocabulary} for the SNOMED terminology, which is
 * supplemented to the original {@link MVFDictionary}. The vocabulary contains one entry per
 * SNOMED-annotated concept, and contains either the original codes, or the post-coordinated
 * expressions
 */
public class ClinicalFocusKEMtoMVFTranslatorAddOn
    implements KEMtoMVFTranslatorExtension {

  private static final String SNOMED = "http://snomed.info/sct";
  private static final String SNOMED_NS = "http://snomed.info/id/";
  private static final String SNOMED_NAME = "SNOMEDCT";


  /**
   * @param dict        the generated MVF model
   * @param kemConcepts the KEM concepts in the original KEM model
   * @param kem         the original KEM model, for context
   * @see #referenceSnomedConcepts(MVFDictionary, Map)
   */
  @Override
  public void preProcess(
      @Nonnull final MVFDictionary dict,
      @Nonnull final Map<UUID, KemConcept> kemConcepts,
      @Nonnull final KemModel kem) {
    referenceSnomedConcepts(dict, kemConcepts);
  }

  /**
   * Ensures that SNOMED-annotated MVF Concepts have an externalReference to the corresponding
   * SNOMED class, based on the code in the annotation
   *
   * @param dict        the generated MVF model
   * @param kemConcepts the KEM concepts in the original KEM model
   */
  private void referenceSnomedConcepts(
      @Nonnull final MVFDictionary dict,
      @Nonnull final Map<UUID, KemConcept> kemConcepts) {
    for (KemConcept kc : kemConcepts.values()) {
      var mvfConcept = lookup(kc, dict);
      var snoCode = kc.getProperties().getCode().stream()
          .filter(cd -> SNOMED.equals(cd.getCodingSystem()))
          .findFirst();
      if (snoCode.isPresent()) {
        if (mvfConcept.getExternalReference() != null) {
          throw new IllegalStateException();
        }
        mvfConcept.setExternalReference(SNOMED_NS + snoCode.get().getValue());
      }
    }
  }

  /**
   * @param kc the KEM concept to be assessed
   * @return true if the KEM Concept has a SNOMED qualifier, but not a SNOMED code
   */
  @Override
  public boolean appliesTo(
      @Nonnull final KemConcept kc) {
    return kc.getProperties().getCode().isEmpty()
        && getTags(kc).stream()
        .filter(tag -> tag.startsWith("sct:"))
        .map(tag -> tag.substring(4))
        .anyMatch(t ->
            Arrays.stream(SCTCategories.values())
                .map(SCTCategories::getValue)
                .anyMatch(ct -> ct.equals(t)));
  }

  /**
   * Adds the MVF {@link Vocabulary} with the SNOMED terms and pre/post coordinated definitions
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
    var vocab = ensureVocabulary(dict, SNOMED, SNOMED_NAME);
    var formalTerm = formalize(kc, dict);
    vocab.getEntry().add(formalTerm);
  }


  /**
   * Formalizes a post-coordinated KEM concept, given its MVF mapping and the mapping of its
   * definition and relationships
   *
   * @param kc   the KEM concept to be formalized
   * @param dict the {@link MVFDictionary} context with the {@link MVFEntry} derived from the KEM
   *             mapping
   * @return a {@link VocabularyEntry} that includes a SNOMED term, code and definition
   */
  @Nonnull
  private VocabularyEntry formalize(KemConcept kc, MVFDictionary dict) {
    var mvfConcept = lookup(kc, dict);

    return new VocabularyEntry()
        .withName(mvfConcept.getName())
        .withDefinition(toSCGExpr(mvfConcept, dict))
        .withTerm(toSCGCode(mvfConcept))
        .withMVFEntry(KEMtoMVFTranslator.toRef(mvfConcept));
  }

  /**
   * Creates a SCG expression for a post-coordinated MVF Concept
   *
   * @param mvfConcept the Concept to be defined
   * @param dict       the {@link MVFDictionary}, whose entries will be used to construct the
   *                   expression
   * @return the SCG expression that defines the mvfConcept
   */
  @Nonnull
  private String toSCGExpr(MVFEntry mvfConcept, MVFDictionary dict) {
    StringBuilder sb = new StringBuilder();

    var sups = mvfConcept.getBroader().stream()
        .map(ref -> lookupRef(ref, dict))
        .map(this::toSCGTerm)
        .collect(Collectors.joining(" + "));
    sb.append(sups);

    if (!mvfConcept.getContext().isEmpty()) {
      sb.append(" : ");
      var attrs = mvfConcept.getContext().stream()
          .map(attRef -> toSCGAttribute(attRef, dict))
          .collect(Collectors.joining(", "));
      sb.append(attrs);
    }

    return sb.toString();
  }

  /**
   * Processes an MVF relationship, in the form of a reified predicate term with a link to the
   * target/object concept, to create a SCG attribute
   *
   * @param attRef the Concept referenced by an attribute/relationship
   * @param dict   the {@link MVFDictionary}, whose entries will be used to construct the
   *               expression
   * @return a SCG attribute in the form 'attr = code' if the relationship target is primitive, or
   * 'attr = ( expr )' if the target is post-coordinated.
   */
  @Nonnull
  private String toSCGAttribute(
      @Nonnull final MVFEntry attRef,
      @Nonnull final MVFDictionary dict) {
    var obj = lookupRef(attRef, dict);
    var rel = toSCGTerm(obj);
    if (attRef.getContext().size() != 1) {
      throw new IllegalStateException();
    }

    var tgtRef = attRef.getContext().get(0);
    var tgt = lookupRef(tgtRef, dict);
    boolean simple = tgt.getContext().isEmpty();

    var ref = simple
        ? toSCGTerm(tgt)
        : "( " + toSCGExpr(tgt, dict) + " )";

    return rel + " = " + ref;
  }


  /**
   * Converts a primitive MVF entry to its SCG term form
   *
   * @param mvfConcept the Concept
   * @return the concept as a SCG term (in the form 'code |label|')
   */
  @Nonnull
  private String toSCGTerm(
      @Nonnull final MVFEntry mvfConcept) {
    var code = toSCGCode(mvfConcept);
    var label = mvfConcept.getVocabularyEntry().stream()
        .filter(v -> v.getTerm().equals(code))
        .map(MVFElement::getName)
        .findFirst()
        .orElseGet(mvfConcept::getName);
    return code + " | " + label + " |";
  }

  /**
   * Extracts the SNOMED code for a given MVF entry. Anonymous (post-coordindated) SNOMED concepts
   * do not hvave a code: the concept's UUID is used instead as an unofficial SNOMED code
   *
   * @param mvfConcept the Concept
   * @return the code associated to the concept
   */
  @Nonnull
  private String toSCGCode(
      @Nonnull final MVFEntry mvfConcept) {
    return mvfConcept.getExternalReference() != null
        ? NameUtils.getTrailingPart(mvfConcept.getExternalReference())
        : NameUtils.getTrailingPart(mvfConcept.getUri());
  }


  private Optional<MVFEntry> detectSituationPattern(KemConcept kc) {
    var cso = kc.getProperties().getExtensionElements().stream()
        .filter(x -> "semanticLink".equals(x.getSemanticType()))
        .filter(x -> "http://ontology.mayo.edu/ontologies/clinicalsituationontology/".equals(
            x.getModelURI()))
        .filter(x -> "graph".equals(x.getType()))
        .findFirst();
    return cso.map(x -> new MVFEntry()
        .withName(x.getItemName())
        .withExternalReference(x.getUri()));
  }


}
