package edu.mayo.kmdp.trisotechwrapper.components.operators;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import edu.mayo.kmdp.trisotechwrapper.config.TTWEnvironmentConfiguration;
import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.KemModel;
import edu.mayo.kmdp.util.JSonUtil;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.omg.spec.mvf._20220702.mvf.MVFDictionary;

public class MVFTestHelper {

  public static MVFDictionary loadMVFTestData(String src) {
    return loadMVFTestData(src, List.of());
  }

  public static MVFDictionary loadMVFTestData(String src, List<KEMtoMVFTranslatorExtension> exts) {
    try (var is = KEMtoMVFTranslatorTest.class.getResourceAsStream(src)) {
      var km = JSonUtil.readJson(is)
          .flatMap(j -> JSonUtil.parseJson(j, KemModel.class));
      var mvf = km.map(k -> new KEMtoMVFTranslator(exts, new TTWEnvironmentConfiguration())
              .translate(k))
          .orElseGet(Assertions::fail);
      assertNotNull(mvf);
      return mvf;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
