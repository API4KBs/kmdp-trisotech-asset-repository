package edu.mayo.kmdp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;

import edu.mayo.kmdp.language.parsers.surrogate.v2.Surrogate2Parser;
import edu.mayo.kmdp.util.FileUtil;
import edu.mayo.kmdp.util.Util;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;

class SurrogateTest {

  Surrogate2Parser parser = new Surrogate2Parser();

  @Test
  void testValidateSurrogates() {
    Set<String> validModels = loadSurrogates()
        .map(this::validate)
        .collect(Collectors.toSet());
    assertFalse(validModels.isEmpty());
  }

  private String validate(KnowledgeAsset asset) {
    assertNotNull(asset.getAssetId());
    assertEquals(asset.getAssetId().getUuid().toString(), asset.getAssetId().getTag());
    assertEquals(1, asset.getFormalCategory().size());
    assertFalse(asset.getFormalType().isEmpty());

    assertEquals(1, asset.getSurrogate().size());
    KnowledgeArtifact self = asset.getSurrogate().get(0);
    assertNotNull(self.getArtifactId());
    assertNotNull(self.getRepresentation());
    assertTrue(Util.isUUID(self.getArtifactId().getTag()));

    assertEquals(1, asset.getCarriers().size());
    KnowledgeArtifact model = asset.getCarriers().get(0);
    assertNotNull(model.getArtifactId());
    assertNotNull(model.getRepresentation());
    assertTrue(Util.isUUID(model.getArtifactId().getTag()));

    return asset.getAssetId().getTag();
  }

  Stream<KnowledgeAsset> loadSurrogates() {
    return loadBinaries()
        .map(kc -> parser
            .applyLift(kc, Abstract_Knowledge_Expression, codedRep(Knowledge_Asset_Surrogate_2_0),
                null))
        .map(ans -> ans.orElseGet(
            () -> fail(ans.getExplanation().asString()
                .orElse("Unable to parse Surrogate binary"))))
        .map(kc -> kc.as(KnowledgeAsset.class))
        .map(opt -> opt.orElseGet(Assertions::fail));
  }

  Stream<KnowledgeCarrier> loadBinaries() {
    try {
      URI base = SurrogateTest.class.getResource("/").toURI();
      return Files.walk(Path.of(base))
          .filter(path -> path.getFileName().toString().endsWith(".surrogate.xml"))
          .map(path -> FileUtil.readBytes(path)
              .orElseGet(() -> fail("Unable to read " + path.toString())))
          .map(bytes -> AbstractCarrier.of(bytes,
              rep(Knowledge_Asset_Surrogate_2_0, XML_1_1, Charset.defaultCharset(), Encodings.DEFAULT)));
    } catch (IOException | URISyntaxException e) {
      fail(e.getMessage());
      return Stream.empty();
    }
  }
}
