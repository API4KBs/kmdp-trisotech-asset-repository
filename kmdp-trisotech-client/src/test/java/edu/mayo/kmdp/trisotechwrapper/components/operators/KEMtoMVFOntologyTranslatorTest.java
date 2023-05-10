package edu.mayo.kmdp.trisotechwrapper.components.operators;

import static edu.mayo.kmdp.trisotechwrapper.components.operators.MVFTestHelper.loadMVFTestData;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.mvf._20220702.mvf.MVFDictionary;
import org.omg.spec.mvf._20220702.mvf.MVFEntry;
import org.omg.spec.mvf._20220702.mvf.VocabularyEntry;

class KEMtoMVFOntologyTranslatorTest {

  static MVFDictionary dict = loadMVFTestData("/kem-mixed-sct.json", List.of(
      new ClinicalSituationKEMtoMVFTranslatorAddOn(),
      new ClinicalFocusKEMtoMVFTranslatorAddOn()));


  @Test
  void testKemWithClinicalSituationsInMixedModel() {
    assertNotNull(dict);

    var sits = getSituations();
    assertEquals(14, sits.size());

    var newSits = sits.stream()
        .filter(s -> !s.getBroader().isEmpty())
        .collect(Collectors.toList());
    assertEquals(4, newSits.size());

    newSits.forEach(s -> {
      assertTrue(s.getBroader().get(0).getExternalReference().contains("situationpatterns"));
    });
  }

  @Test
  void testKemWithBasicPostCoord() {
    var postcoord = getSituations().stream()
        .filter(e -> e.getName().equals("Most Recent Infectious Disease Labs"))
        .findFirst().orElseGet(Assertions::fail);
    assertTrue(postcoord.getExternalReference().contains("5fee73fb-78a0-42b7-9cf2-b5c19e9a5559"));
    var ctx = postcoord.getContext().get(0).getUri();
    var focus = dict.getEntry().stream()
        .filter(e -> e.getUri().equals(ctx))
        .findFirst().orElseGet(Assertions::fail);
    assertEquals("Infectious Disease Labs", focus.getName());
    var def = dict.getVocabulary().get(0).getEntry()
        .stream()
        .filter(v -> v.getMVFEntry().getUri().equals(focus.getUri()))
        .map(VocabularyEntry::getDefinition)
        .findFirst()
        .orElseGet(Assertions::fail);
    assertEquals(
        "15220000 | Lab Test | : 363702006 | Has focus | = 40733004 | Infectious Disease |",
        def);
  }

  @Test
  void testKemWithComplexPostCoord() {
    var postcoord = getSituations().stream()
        .filter(e -> e.getName().equals("Most Recent Renal Panel Tests"))
        .findFirst().orElseGet(Assertions::fail);

    var ctx = postcoord.getContext().get(0).getUri();
    var def = dict.getVocabulary().get(0).getEntry()
        .stream()
        .filter(v -> v.getMVFEntry().getUri().equals(ctx))
        .map(VocabularyEntry::getDefinition)
        .findFirst()
        .orElseGet(Assertions::fail);

    assertEquals(
        "396550006 | Blood Test | "
            + ": 363702006 | Has focus | = ( 1148582006 | Renal Function Finding | "
            + ": 363714003 | Interprets | = 11953005 | Kidney Function | )",
        def);
  }


  private List<MVFEntry> getSituations() {
    return dict.getEntry().stream()
        .filter(x -> x.getExternalReference() != null
            && x.getExternalReference().contains("/taxonomies/clinicalsituations"))
        .collect(Collectors.toList());
  }
}
