package edu.mayo.kmdp.trisotechwrapper.components.operators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import edu.mayo.kmdp.trisotechwrapper.config.TTWEnvironmentConfiguration;
import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.KemModel;
import edu.mayo.kmdp.util.JSonUtil;
import edu.mayo.kmdp.util.JaxbUtil;
import edu.mayo.kmdp.util.Util;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.mvf._20220702.mvf.MVFDictionary;
import org.omg.spec.mvf._20220702.mvf.ObjectFactory;

class KEMtoMVFTranslatorTest {

  @Test
  void testBasicTranslate() {
    var dict = load("/kem-basic-test.json");
    assertEquals(2, dict.getEntry().size());

    dict.getEntry().forEach(e -> {
      assertNotNull(e.getUri());
      assertNotNull(e.getName());
      assertEquals(1, e.getVocabularyEntry().size());
    });

    var sct = dict.getVocabulary().stream()
        .filter(v -> v.getUri().contains("sct"))
        .findFirst().orElseGet(Assertions::fail);
    assertEquals(1, sct.getEntry().size());

    var entry = sct.getEntry().get(0);
    assertEquals("773568002", entry.getTerm());
    assertNotNull(entry.getName());
    assertNotNull(entry.getMVFEntry().getUri());

    var termToConceptRef = entry.getMVFEntry().getUri();
    dict.getEntry().stream()
        .filter(e -> e.getUri().equals(termToConceptRef))
        .findFirst().orElseGet(Assertions::fail);
  }

  @Test
  void testKemIntegration() {
    try (var is = KEMtoMVFTranslatorTest.class.getResourceAsStream("/kem-test-example.json")) {
      var km = JSonUtil.readJson(is)
          .flatMap(j -> JSonUtil.parseJson(j, KemModel.class));
      var mvf = km.map(k -> new KEMtoMVFTranslator(new TTWEnvironmentConfiguration())
              .translate(k))
          .orElseGet(Assertions::fail);
      var str = JaxbUtil.marshallToString(
          List.of(mvf.getClass()),
          mvf,
          new ObjectFactory()::createMVFDictionary,
          JaxbUtil.defaultProperties());
      assertFalse(Util.isEmpty(str));
    } catch (IOException e) {
      fail(e);
    }
  }

  private MVFDictionary load(String src) {
    try (var is = KEMtoMVFTranslatorTest.class.getResourceAsStream(src)) {
      var km = JSonUtil.readJson(is)
          .flatMap(j -> JSonUtil.parseJson(j, KemModel.class));
      var mvf = km.map(k -> new KEMtoMVFTranslator(List.of(), new TTWEnvironmentConfiguration())
              .translate(k))
          .orElseGet(Assertions::fail);
      assertNotNull(mvf);
      return mvf;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
