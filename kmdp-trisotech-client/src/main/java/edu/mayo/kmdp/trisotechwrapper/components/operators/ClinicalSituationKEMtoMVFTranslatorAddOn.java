package edu.mayo.kmdp.trisotechwrapper.components.operators;

import static edu.mayo.kmdp.trisotechwrapper.components.operators.KEMUtils.ensureVocabulary;
import static edu.mayo.kmdp.trisotechwrapper.components.operators.KEMUtils.getTags;

import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.KemConcept;
import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.KemModel;
import edu.mayo.kmdp.util.NameUtils;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.omg.spec.mvf._20220702.mvf.MVFDictionary;
import org.omg.spec.mvf._20220702.mvf.MVFElement;
import org.omg.spec.mvf._20220702.mvf.MVFEntry;
import org.omg.spec.mvf._20220702.mvf.VocabularyEntry;

public class ClinicalSituationKEMtoMVFTranslatorAddOn
    implements KEMtoMVFTranslatorExtension {


  private static final String SNOMED = "http://snomed.info/sct";
  private static final String SNOMED_NS = "http://snomed.info/id/";
  private static final String SNOMED_NAME = "SNOMEDCT";


  @Override
  public String getApplicableTag() {
    return "Situation";
  }

  @Override
  public void preProcess(MVFDictionary dict, Map<UUID,KemConcept> kemConcepts, KemModel kem) {
    referenceSnomedConcepts(dict, kemConcepts);
  }

  @Override
  public boolean appliesTo(KemConcept kc) {
    return getTags(kc).stream().anyMatch(t -> t.equalsIgnoreCase(getApplicableTag()));
  }

  @Override
  public void process(KemConcept kc, MVFDictionary dict, KemModel kem) {
    var vocab = ensureVocabulary(dict, SNOMED, SNOMED_NAME);
    var formalTerm = formalize(kc, dict);
    vocab.getEntry().add(formalTerm);
  }

  private void referenceSnomedConcepts(MVFDictionary dict, Map<UUID, KemConcept> kemConcepts) {
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

  private VocabularyEntry formalize(KemConcept kc, MVFDictionary dict) {
    var mvfConcept = lookup(kc, dict);

    var cso = detectSituationPattern(kc);
    cso.ifPresent(sitp -> mvfConcept.getBroader().add(sitp));

    return new VocabularyEntry()
        .withDefinition(toSCGExpr(mvfConcept, dict))
        .withTerm(toSCGCode(mvfConcept))
        .withMVFEntry(KEMtoMVFTranslator.toRef(mvfConcept));
  }

  private String toSCGTerm(MVFEntry mvfConcept) {
    var code = toSCGCode(mvfConcept);
    var label = mvfConcept.getVocabularyEntry().stream()
        .filter(v -> v.getTerm().equals(code))
        .map(MVFElement::getName)
        .findFirst()
        .orElseGet(mvfConcept::getName);
    return code + " | " + label + " |";
  }

  private String toSCGCode(MVFEntry mvfConcept) {
    return mvfConcept.getExternalReference() != null
        ? NameUtils.getTrailingPart(mvfConcept.getExternalReference())
        : NameUtils.getTrailingPart(mvfConcept.getUri());
  }

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

  private String toSCGAttribute(MVFEntry attRef, MVFDictionary dict) {
    var att = lookupRef(attRef, dict);
    var rel = toSCGTerm(att);
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
