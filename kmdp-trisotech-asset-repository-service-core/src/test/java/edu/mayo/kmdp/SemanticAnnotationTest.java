/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package edu.mayo.kmdp;

import static edu.mayo.kmdp.components.TestMetadataHelper.extractMetadata;
import static edu.mayo.kmdp.util.FileUtil.read;
import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.Captures;
import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.In_Terms_Of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import edu.mayo.kmdp.components.TestMetadataHelper;
import edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.MetadataIntrospector;
import edu.mayo.kmdp.trisotechwrapper.components.SemanticModelInfo;
import edu.mayo.kmdp.trisotechwrapper.components.redactors.Redactor;
import edu.mayo.kmdp.trisotechwrapper.components.redactors.TTRedactor;
import edu.mayo.kmdp.trisotechwrapper.components.weavers.DomainSemanticsWeaver;
import edu.mayo.kmdp.trisotechwrapper.components.weavers.Weaver;
import edu.mayo.kmdp.trisotechwrapper.config.TTWEnvironmentConfiguration;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.util.JSonUtil;
import edu.mayo.kmdp.util.XMLUtil;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.id.ConceptIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.surrogate.Annotation;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.w3c.dom.Document;

class SemanticAnnotationTest {

  static TTWEnvironmentConfiguration cfg;
  static MetadataIntrospector extractor;

  static Weaver dmnWeaver;

  static Redactor redactor;

  @BeforeAll
  static void init() {
    cfg = new TTWEnvironmentConfiguration();
    dmnWeaver = new DomainSemanticsWeaver(new TTWEnvironmentConfiguration());
    redactor = new TTRedactor();
    extractor = TestMetadataHelper.newIntrospector(cfg);
  }


  @Test
  void testExtraction() {
    assertTrue(true); // dummy to keep sonarlint happy
    String dmnPath = "/Computable Decision Model.raw.dmn.xml";
    String metaPath = "/Computable Decision Model.meta.json";

    Optional<byte[]> dmn = XMLUtil
        .loadXMLDocument(SemanticAnnotationTest.class.getResourceAsStream(dmnPath))
        .map(dmnWeaver::weave)
        .map(redactor::redact)
        .map(XMLUtil::toByteArray);
    assertTrue(dmn.isPresent());

    try {
      Optional<KnowledgeAsset> res = extractMetadata(
          new ByteArrayInputStream(dmn.get()),
          SemanticAnnotationTest.class.getResourceAsStream(metaPath),
          extractor,
          cfg);
      if (res.isEmpty()) {
        fail("Unable to instantiate metadata object");
      }
      KnowledgeAsset surr = res.get();
      assertEquals(3, surr.getAnnotation().size());
      List<ConceptIdentifier> inputs = surr.getAnnotation().stream()
          .filter(annotation -> In_Terms_Of.sameTermAs(annotation.getRel()))
          .map(Annotation::getRef)
          .collect(Collectors.toList());
      assertEquals(2, inputs.size());
      assertTrue(inputs.stream()
          .anyMatch(
              input -> input.getUuid().toString().equals("6c14a53a-7ce2-34aa-9cf1-a800317bebef")));
      assertTrue(inputs.stream()
          .anyMatch(
              input -> input.getUuid().toString().equals("9296f375-a7ed-3c59-a972-4a7eb40c8820")));

      List<ConceptIdentifier> decisions = surr.getAnnotation().stream()
          .filter(annotation -> Captures.sameTermAs(annotation.getRel()))
          .map(Annotation::getRef)
          .collect(Collectors.toList());

      assertEquals(1, decisions.size());
      assertTrue(decisions.stream()
          .anyMatch(decision -> decision.getName().contains("Computable Decision")));

    } catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }


  @Test
  void testExtractionFull() {
    String dmnPath = "/Weaver Test 1.raw.dmn.xml";
    String metaPath = "/Weaver Test 1.meta.json";

    Optional<byte[]> dmn = XMLUtil.loadXMLDocument(
            SemanticAnnotationTest.class.getResourceAsStream(dmnPath))
        .map(dmnWeaver::weave)
        .map(redactor::redact)
        .map(XMLUtil::toByteArray);
    assertTrue(dmn.isPresent());
    Optional<Document> dox = (XMLUtil.loadXMLDocument(
        SemanticAnnotationTest.class.getResourceAsStream(dmnPath)));
    redactor.redact(dmnWeaver.weave(dox.orElseGet(Assertions::fail)));
    //XMLUtil.streamXMLDocument(dox.get(), System.out);

    try {
      Optional<KnowledgeAsset> res = extractMetadata(
          new ByteArrayInputStream(dmn.get()),
          SemanticAnnotationTest.class.getResourceAsStream(metaPath),
          extractor,
          cfg);
      if (res.isEmpty()) {
        fail("Unable to instantiate metadata object");
      }
      KnowledgeAsset surr = res.get();

      List<String> inputs = surr.getAnnotation().stream()
          .filter(annotation -> In_Terms_Of.sameTermAs(annotation.getRel()))
          .map(annotation -> annotation.getRef().getUuid().toString())
          .collect(Collectors.toList());

      // FIXME "Dictionary inputs are not resolved (sex, race)"
      assertEquals(2, inputs.size());

    } catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }


  @Test
  void testExternalProcessDefinition() {
    String dmnPath = "/Case with Process Definition.raw.cmmn.xml";
    String metaPath = "/Case with Process Definition.meta.json";

    Optional<Document> cmmn = XMLUtil.loadXMLDocument(
            SemanticAnnotationTest.class.getResourceAsStream(dmnPath))
        .map(dmnWeaver::weave)
        .map(redactor::redact);
    assertTrue(cmmn.isPresent());

    TrisotechFileInfo info = read(SemanticAnnotationTest.class.getResourceAsStream(metaPath))
        .flatMap(str -> JSonUtil.parseJson(str, TrisotechFileInfo.class))
        .orElseGet(Assertions::fail);

    var asset = extractor.introspect(
        SemanticIdentifier.randomId(),
        Map.of(SemanticModelInfo.testNewInfo(info), cmmn));

    System.out.println(XMLUtil.toString(cmmn.get()));
    JSonUtil.printJson(asset).ifPresent(System.out::println);
  }

}
