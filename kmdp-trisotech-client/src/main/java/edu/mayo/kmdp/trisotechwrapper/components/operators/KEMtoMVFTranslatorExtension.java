package edu.mayo.kmdp.trisotechwrapper.components.operators;

import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.KemConcept;
import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.KemModel;
import java.util.Map;
import java.util.UUID;
import org.omg.spec.mvf._20220702.mvf.MVFDictionary;
import org.omg.spec.mvf._20220702.mvf.MVFEntry;

public interface KEMtoMVFTranslatorExtension {

  String getApplicableTag();

  void process(KemConcept kc, MVFDictionary dict, KemModel kem);

  default void preProcess(MVFDictionary dict, Map<UUID, KemConcept> kemConcepts, KemModel kem) {
    // do nothing by default
  }

  default void postProcess(MVFDictionary dict, Map<UUID, KemConcept> kemConcepts, KemModel kem) {
    // do nothing by default
  }

  default boolean appliesTo(KemConcept kc) {
    return true;
  }

  default MVFEntry lookup(KemConcept kc, MVFDictionary dict) {
    var kcKey = kc.getResourceId().substring(1);
    return dict.getEntry().stream()
        .filter(e -> e.getUri().contains(kcKey))
        .findFirst()
        .orElseThrow(IllegalStateException::new);
  }

  default MVFEntry lookupRef(MVFEntry ref, MVFDictionary dict) {
    return dict.getEntry().stream()
        .filter(entry -> entry.getUri().equals(ref.getUri()))
        .findFirst()
        .orElse(ref);
  }

}
