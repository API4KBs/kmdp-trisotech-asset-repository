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

import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.DMN_EL_DECISION;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.DMN_EL_EXTENSIONS;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.DROOLS_NS;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.TT_ATTACHMENT_ITEM;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.TT_CMMN_11_NS;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.TT_DMN_12_NS;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.TT_METADATA_NS;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.TT_META_EXPORTER;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.TT_META_EXPORTER_VERSION;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.TT_RELATIONSHIP;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.TT_SEMANTICLINK;
import static edu.mayo.kmdp.util.Util.resolveResource;
import static edu.mayo.kmdp.util.XMLUtil.asElementStream;
import static edu.mayo.kmdp.util.XMLUtil.loadXMLDocument;
import static edu.mayo.kmdp.util.XMLUtil.validate;
import static edu.mayo.ontology.taxonomies.kao.decisiontype.DecisionTypeSeries.Actionable_Decision;
import static edu.mayo.ontology.taxonomies.kao.decisiontype.DecisionTypeSeries.Aggregation_Decision;
import static edu.mayo.ontology.taxonomies.kao.decisiontype.DecisionTypeSeries.Assessment_Decision;
import static edu.mayo.ontology.taxonomies.kao.decisiontype.DecisionTypeSeries.Choice_Decision;
import static edu.mayo.ontology.taxonomies.kao.decisiontype.DecisionTypeSeries.Naturalistic_Decision;
import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.Captures;
import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.In_Terms_Of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.CMMN_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.DMN_1_2;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;

import edu.mayo.kmdp.kdcaci.knew.trisotech.components.redactors.Redactor;
import edu.mayo.kmdp.kdcaci.knew.trisotech.components.weavers.Weaver;
import edu.mayo.kmdp.language.LanguageDeSerializer;
import edu.mayo.kmdp.language.common.cmmn.CMMN11Utils;
import edu.mayo.kmdp.language.parsers.cmmn.v1_1.CMMN11Parser;
import edu.mayo.kmdp.language.parsers.dmn.v1_2.DMN12Parser;
import edu.mayo.kmdp.util.JaxbUtil;
import edu.mayo.kmdp.util.StreamUtil;
import edu.mayo.kmdp.util.XMLUtil;
import edu.mayo.kmdp.util.XPathUtil;
import edu.mayo.ontology.taxonomies.kao.decisiontype.DecisionType;
import edu.mayo.ontology.taxonomies.kao.decisiontype.DecisionTypeSeries;
import edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBElement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.id.ConceptIdentifier;
import org.omg.spec.api4kp._20200801.surrogate.Annotation;
import org.omg.spec.cmmn._20151109.model.TCaseFileItem;
import org.omg.spec.cmmn._20151109.model.TDecisionTask;
import org.omg.spec.cmmn._20151109.model.TDefinitions;
import org.omg.spec.dmn._20180521.model.TContext;
import org.omg.spec.dmn._20180521.model.TLiteralExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


@SpringBootTest
@ContextConfiguration(classes = {TrisotechAssetRepositoryTestConfig.class})
class WeaverTest {

  Logger logger = LoggerFactory.getLogger(WeaverTest.class);

  @Autowired
  private Weaver weaver;

  @Autowired
  private Redactor redactor;

  @Test
  void testInit() {
    assertNotNull(weaver);
  }


  @Test
  void testWeave() {

    String path = "/Weaver Test 1.raw.dmn.xml";

    // using XMLUtil loadXMLDocument to load the XML Document properly
    // sets up the document for conversion by setting namespaceaware
    Document dox = loadXMLDocument(resolveResource(path))
        .orElseGet(() -> fail("Unable to load document " + path));
    try {
      redactor.redact(weaver.weave(dox));
      //streamXMLDocument(dox, System.out);

      assertTrue(validate(dox, DMN_1_2.getReferentId()));
    } catch (IllegalStateException ie) {
      logger.error(ie.getMessage(), ie);
      fail(ie.getMessage());
    }
  }

  @Test
  void testWeaveDefault() {
    String path = "/Basic Decision Model.raw.dmn.xml";
    Document dox = loadXMLDocument(resolveResource(path))
        .orElseGet(() -> fail("Unable to load document " + path));

    try {
      redactor.redact(weaver.weave(dox));
      assertTrue(validate(dox, DMN_1_2.getReferentId()));

      //streamXMLDocument(dox, System.out);
    } catch (IllegalStateException ie) {
      logger.error(ie.getMessage(), ie);
      fail(ie.getMessage());
    }
  }

  @Test
  void testWeaveRemoveDecisionServices() {
    String path = "/Basic Decision Model.raw.dmn.xml";
    Document dox = loadXMLDocument(resolveResource(path))
        .orElseGet(() -> fail("Unable to load document " + path));

    try {
      redactor.redact(weaver.weave(dox));
      assertTrue(validate(dox, DMN_1_2.getReferentId()));

      assertEquals(0,
          XMLUtil.asElementStream(dox.getElementsByTagNameNS("*", "decisionService"))
              .count());

      assertEquals(1,
          XMLUtil.asElementStream(dox.getElementsByTagNameNS("*", "decision"))
              .count());
      //streamXMLDocument(dox, System.out);
    } catch (IllegalStateException ie) {
      logger.error(ie.getMessage(), ie);
      fail(ie.getMessage());
    }
  }

  @Test
  void testVariousMetadataOnDMNBasicDecision() {
    String path = "/Basic Decision Model.raw.dmn.xml";

    Document dox = loadXMLDocument(resolveResource(path))
        .orElseGet(() -> fail("Unable to load document " + path));

    assertTrue(true); // dummy
    assertTrue(validate(dox, DMN_1_2.getReferentId()));

    try {
      redactor.redact(weaver.weave(dox));
      //streamXMLDocument(dox, System.out);

      assertNotNull(dox);

      assertTrue(confirmNoTrisoNameSpace(dox));

      assertTrue(confirmNoTrisotechTags(dox));

      assertTrue(verifyRootNamespaces(dox));

      assertTrue(verifyImportNamespace(dox));

      assertTrue(verifyHrefs(dox));

      List<Annotation> props = loadAnnotations(dox, Captures, Annotation.class);
      assertEquals(3, props.size());
      Set<DecisionType> decisions = props.stream()
          .map(Annotation::getRef)
          .map(ConceptIdentifier::getUuid)
          .map(DecisionTypeSeries::resolveUUID)
          .flatMap(StreamUtil::trimStream)
          .collect(Collectors.toSet());
      assertEquals(3, decisions.size());
      assertTrue(decisions.contains(Aggregation_Decision));
      assertTrue(decisions.contains(Assessment_Decision));
      assertTrue(decisions.contains(Naturalistic_Decision));


    } catch (IllegalStateException ie) {
      logger.error(ie.getMessage(), ie);
      fail(ie.getMessage());
    }
  }

  @Test
  void testVariousMetadataOnDMN_WeaverTest2() {
    String path = "/Weaver Test 2.raw.dmn.xml";
    Document dox = loadXMLDocument(resolveResource(path))
        .orElseGet(() -> fail("Unable to load document " + path));

    assertTrue(true); // dummy
    assertTrue(validate(dox, DMN_1_2.getReferentId()));

    try {
      redactor.redact(weaver.weave(dox));
      //streamXMLDocument(dox, System.out);

      assertNotNull(dox);

      assertTrue(confirmNoTrisoNameSpace(dox));

      assertTrue(confirmNoTrisotechTags(dox));

      assertTrue(verifyRootNamespaces(dox));

      assertTrue(verifyImportNamespace(dox));

      assertTrue(verifyHrefs(dox));

    } catch (IllegalStateException ie) {
      logger.error(ie.getMessage(), ie);
      fail(ie.getMessage());
    }
  }


  @Test
  void testVariousMetadataOnDMN_Subdecision() {
    String path = "/Decision Subdecision.raw.dmn.xml";
    Document dox = loadXMLDocument(resolveResource(path))
        .orElseGet(() -> fail("Unable to load document " + path));

    assertTrue(validate(dox, DMN_1_2.getReferentId()));

    try {
      redactor.redact(weaver.weave(dox));
      //streamXMLDocument(dox, System.out);

      assertNotNull(dox);

      assertTrue(confirmNoTrisoNameSpace(dox));

      assertTrue(confirmNoTrisotechTags(dox));

      assertTrue(verifyRootNamespaces(dox));

      assertTrue(verifyImportNamespace(dox));

      assertTrue(verifyHrefs(dox));

      List<Annotation> props = loadAnnotations(dox, Captures, Annotation.class);
      assertEquals(6, props.size());
      Set<DecisionType> decisions = props.stream()
          .map(Annotation::getRef)
          .map(ConceptIdentifier::getUuid)
          .map(DecisionTypeSeries::resolveUUID)
          .flatMap(StreamUtil::trimStream)
          .collect(Collectors.toSet());
      assertEquals(5, decisions.size());
      // there are 2 Naturalistic_Decision, that's why only 5 are asserted here
      assertTrue(decisions.contains(Aggregation_Decision));
      assertTrue(decisions.contains(Assessment_Decision));
      assertTrue(decisions.contains(Naturalistic_Decision));
      assertTrue(decisions.contains(Choice_Decision));
      assertTrue(decisions.contains(Actionable_Decision));

      props = loadAnnotations(dox, In_Terms_Of, Annotation.class);
      assertTrue(props.stream()
          .anyMatch(ann -> ann.getRel().getPrefLabel()
              .equals(In_Terms_Of.getLabel())));
      assertEquals(3, props.size());

      assertTrue(props.stream()
          .anyMatch(
              ann -> ann.getRef().getTag().contains("13a3e25c-6848-373e-9676-8ecb62ab3e6a")));

      assertTrue(props.stream()
          .anyMatch(
              ann -> ann.getRef().getTag().contains("102f5949-fa9b-3531-85ba-28fafde21c2d")));

    } catch (IllegalStateException ie) {
      logger.error(ie.getMessage(), ie);
      fail(ie.getMessage());
    }
  }

  @Test
  void testVariousMetadataOnDMNComputableDecision() {
    String path = "/Computable Decision Model.raw.dmn.xml";
    Document dox = loadXMLDocument(resolveResource(path))
        .orElseGet(() -> fail("Unable to load document " + path));

    assertTrue(true); // dummy
    assertTrue(validate(dox, DMN_1_2.getReferentId()));

    try {
      redactor.redact(weaver.weave(dox));
      //streamXMLDocument(dox, System.out);

      assertNotNull(dox);

      assertTrue(confirmNoTrisoNameSpace(dox));

      assertTrue(confirmNoTrisotechTags(dox));

      assertTrue(verifyRootNamespaces(dox));

      assertTrue(verifyImportNamespace(dox));

      assertTrue(verifyHrefs(dox));

      List<Annotation> props = loadAnnotations(dox, In_Terms_Of, Annotation.class);
      assertTrue(props.stream()
          .anyMatch(ann -> ann.getRel().getPrefLabel()
              .equals(In_Terms_Of.getLabel())));
      assertEquals(2, props.size());

      assertTrue(props.stream()
          .anyMatch(ann -> ann.getRef().getUuid().toString()
              .equals("6c14a53a-7ce2-34aa-9cf1-a800317bebef")));
      assertTrue(props.stream()
          .anyMatch(ann -> ann.getRef().getUuid().toString()
              .equals("9296f375-a7ed-3c59-a972-4a7eb40c8820")));

      List<Annotation> props2 = loadAnnotations(dox, Captures, Annotation.class);
      assertTrue(props2.stream()
          .anyMatch(ann -> ann.getRef().getLabel().contains("Computable Decision")));

    } catch (IllegalStateException ie) {
      logger.error(ie.getMessage(), ie);
      fail(ie.getMessage());
    }
  }

  @Test
  void testVariousMetadataOnCMMNWeaveTest1() {
    String path = "/Weave Test 1.raw.cmmn.xml";

    // loadXMLDocument sets NamespaceAware
    Document dox = loadXMLDocument(resolveResource(path))
        .orElseGet(() -> fail("Unable to load document " + path));

    try {
      redactor.redact(weaver.weave(dox));
      //streamXMLDocument(dox, System.out);

      assertTrue(validate(dox, CMMN_1_1.getReferentId()));

      assertTrue(confirmDecisionURI(dox));

      assertTrue(confirmNoTrisoNameSpace(dox));

      assertTrue(confirmNoTrisotechTags(dox));

      assertTrue(verifyRootNamespaces(dox));
      assertTrue(verifyCaseFileItemDefinition(dox));

      NodeList metas = dox.getElementsByTagNameNS(TT_METADATA_NS, TT_SEMANTICLINK);
      assertEquals(0, metas.getLength());
      NodeList relations = dox
          .getElementsByTagNameNS(TT_METADATA_NS, TT_RELATIONSHIP);

      assertEquals(0, relations.getLength());

    } catch (IllegalStateException ie) {
      logger.error(ie.getMessage(), ie);
      fail(ie.getMessage());
    }
  }

  @Test
  void testVariousMetadataOnCMMNWeaveTest2() {
    String path = "/Weave Test 2.raw.cmmn.xml";
    // loadXMLDocument set setNamespaceAware
    Document dox = loadXMLDocument(resolveResource(path))
        .orElseGet(() -> fail("Unable to load document " + path));

    try {
      redactor.redact(weaver.weave(dox));
      //streamXMLDocument(dox, System.out);

      assertTrue(validate(dox, CMMN_1_1.getReferentId()));

      assertTrue(confirmDecisionURI(dox));

      assertTrue(confirmNoTrisoNameSpace(dox));

      assertTrue(confirmNoTrisotechTags(dox));

      assertTrue(verifyRootNamespaces(dox));
      assertTrue(verifyCaseFileItemDefinition(dox));

      NodeList metas = dox.getElementsByTagNameNS(TT_METADATA_NS, TT_SEMANTICLINK);
      assertEquals(0, metas.getLength());
      NodeList relations = dox
          .getElementsByTagNameNS(TT_METADATA_NS, TT_RELATIONSHIP);

      assertEquals(0, relations.getLength());

    } catch (IllegalStateException ie) {
      logger.error(ie.getMessage(), ie);
      fail(ie.getMessage());
    }
  }


  @Test
  void testVariousMetadataOnCMMNBasicCaseModel() {
    String path = "/Basic Case Model.raw.cmmn.xml";
    // loadXMLDocument sets NamespaceAware
    Document dox = loadXMLDocument(resolveResource(path))
        .orElseGet(() -> fail("Unable to load document " + path));

    try {
      redactor.redact(weaver.weave(dox));
      //streamXMLDocument(dox, System.out);

      assertTrue(validate(dox, CMMN_1_1.getReferentId()));

      assertTrue(confirmDecisionURI(dox));

      assertTrue(confirmNoTrisoNameSpace(dox));

      assertTrue(confirmNoTrisotechTags(dox));

      assertTrue(verifyRootNamespaces(dox));
      assertTrue(verifyCaseFileItemDefinition(dox));

      NodeList metas = dox.getElementsByTagNameNS(TT_METADATA_NS, TT_SEMANTICLINK);
      assertEquals(0, metas.getLength());
      NodeList relations = dox
          .getElementsByTagNameNS(TT_METADATA_NS, TT_RELATIONSHIP);

      assertEquals(0, relations.getLength());

    } catch (IllegalStateException ie) {
      logger.error(ie.getMessage(), ie);
      fail(ie.getMessage());
    }
  }


  @Test
  void testRewriteReuseLinks() {
    // Rewrite the 'reuse' links, preserve the 'copy of'
    String path = "/Decision with Reuse.raw.dmn.xml";
    Document dox = loadXMLDocument(resolveResource(path))
        .orElseGet(() -> fail("Unable to load document " + path));

    try {
      redactor.redact(weaver.weave(dox));
      assertTrue(validate(dox, DMN_1_2.getReferentId()));

      NodeList decisions = dox.getElementsByTagName("semantic:decision");
      assertEquals(1, decisions.getLength());

      NodeList inputs = dox.getElementsByTagName("semantic:inputData");
      assertEquals(1, inputs.getLength());

      Element rootDecision = asElementStream(decisions)
          .filter(el -> el.getAttribute("name").equals("Reusing Decision"))
          .findAny()
          .orElseGet(Assertions::fail);
      NodeList deps = rootDecision.getElementsByTagName("semantic:informationRequirement");
      assertEquals(4, deps.getLength());

      asElementStream(deps).forEach(
          dep -> {
            NodeList reqDec = dep.getElementsByTagName("semantic:requiredDecision");
            NodeList reqInp = dep.getElementsByTagName("semantic:requiredInput");

            if (reqDec.getLength() > 0) {
              String href = ((Element) reqDec.item(0)).getAttribute("href");
              boolean isExternalRefReuse = href.contains("c0ae5c78-c1d6-48ad-bcfb-47bde312b963");
              boolean isInternalRefCopy = href.equals("#_f3da83df-b1bc-4ab8-b2be-ddc4937e3142");
              assertTrue(isExternalRefReuse || isInternalRefCopy);
            }
            if (reqInp.getLength() > 0) {
              String href = ((Element) reqInp.item(0)).getAttribute("href");
              boolean isExternalRefReuse = href.contains("c0ae5c78-c1d6-48ad-bcfb-47bde312b963");
              boolean isInternalRefCopy = href.equals("#_be6be145-e914-4163-9875-4849e7f098f6");
              assertTrue(isExternalRefReuse || isInternalRefCopy);
            }
          }
      );

      NodeList annos = dox.getElementsByTagName("surr:annotation");
      assertEquals(1, annos.getLength());

      //streamXMLDocument(dox, System.out);
    } catch (IllegalStateException ie) {
      logger.error(ie.getMessage(), ie);
      fail(ie.getMessage());
    }
  }


  @Test
  void testRewriteCMMNInputs() {
    String path = "/Case with DMN IO.raw.cmmn.xml";
    Document dox = loadXMLDocument(resolveResource(path))
        .orElseGet(() -> fail("Unable to load document " + path));

    try {
      redactor.redact(weaver.weave(dox));

      assertTrue(verifyCaseFileItemDefinition(dox));
      assertTrue(verifyRootNamespaces(dox));
      assertTrue(verifyHrefs(dox));
      assertTrue(verifyImportNamespace(dox));

      assertTrue(confirmNoTrisoNameSpace(dox));

      XPathUtil x = new XPathUtil();
      var varNodes = x.xList(dox,
          "//cmmn:input/cmmn:extensionElements/dmn:context/dmn:contextEntry/dmn:variable");
      assertEquals(3, varNodes.getLength());

      asElementStream(x.xList(dox, "//cmmn:input")).forEach(
          taskInput -> {
            String formalVar = taskInput.getAttribute("name");
            String actualVar = x.xString(taskInput, ".//dmn:variable/@name");
            assertEquals(formalVar, actualVar);
          }
      );

      var valueNode = (Element)
          x.xNode(dox, "//cmmn:input[@name='Age At Admission']//dmn:literalExpression/dmn:text");
      assertEquals("42", valueNode.getTextContent());

    } catch (IllegalStateException ie) {
      logger.error(ie.getMessage(), ie);
      fail(ie.getMessage());
    }
  }

  @Test
  void testParseAfterRewrite() {
    String path = "/Case with DMN IO advanced.raw.cmmn.xml";
    Document dox = loadXMLDocument(resolveResource(path))
        .orElseGet(() -> fail("Unable to load document " + path));
    try {
      redactor.redact(weaver.weave(dox));

      var parser = new LanguageDeSerializer(List.of(new CMMN11Parser(List.of(new DMN12Parser()))));
      var cmmn = parser.applyLift(
          AbstractCarrier.of(dox, rep(CMMN_1_1, XML_1_1)),
              Abstract_Knowledge_Expression,
              codedRep(CMMN_1_1),
              null)
          .flatOpt(kc -> kc.as(TDefinitions.class))
          .orElseGet(Assertions::fail);

      var inputCtx = CMMN11Utils.streamTasks(cmmn)
          .flatMap(StreamUtil.filterAs(TDecisionTask.class))
          .filter(dt -> "Multi-Assessment Decision".equals(dt.getName()))
          .flatMap(d -> d.getInput().stream().filter(i -> i.getExtensionElements() != null))
          .filter(i -> i.getBindingRef() instanceof TCaseFileItem)
          .flatMap(i -> i.getExtensionElements().getAny().stream().findFirst().stream())
          .flatMap(StreamUtil.filterAs(JAXBElement.class))
          .map(JAXBElement::getValue)
          .flatMap(StreamUtil.filterAs(TContext.class))
          .findFirst()
          .orElseGet(Assertions::fail);

      assertEquals(1, inputCtx.getContextEntry().size());
      var entry = inputCtx.getContextEntry().get(0);
      assertEquals("Dec Input", entry.getVariable().getName());
      assertEquals("My Output",
          ((TLiteralExpression) entry.getExpression().getValue()).getText());

    } catch (IllegalStateException ie) {
      logger.error(ie.getMessage(), ie);
      fail(ie.getMessage());
    }
  }

  @Test
  void testRewriteCMMNInputsChaining() {
    String path = "/Case with DMN IO advanced.raw.cmmn.xml";
    Document dox = loadXMLDocument(resolveResource(path))
        .orElseGet(() -> fail("Unable to load document " + path));

    try {
      redactor.redact(weaver.weave(dox));

      XPathUtil x = new XPathUtil();

      var decision1 = x.xNode(dox, "//cmmn:decisionTask[@name='Pt Old Assessment']");
      assertNotNull(decision1);
      var inputs1 = x.xList(decision1, ".//cmmn:input");
      assertEquals(2, inputs1.getLength());

      var decision2 = x.xNode(dox, "//cmmn:decisionTask[@name='Multi-Assessment Decision']");
      assertNotNull(decision2);
      var inputsExpr = x.xString(decision2,
          ".//cmmn:input[@name='Dec Input']//dmn:literalExpression/dmn:text");
      assertEquals("My Output", inputsExpr);
    } catch (IllegalStateException ie) {
      logger.error(ie.getMessage(), ie);
      fail(ie.getMessage());
    }
  }

  @Test
  void testRewriteCMMNOutputs() {
    String path = "/Case with DMN IO.raw.cmmn.xml";
    Document dox = loadXMLDocument(resolveResource(path))
        .orElseGet(() -> fail("Unable to load document " + path));

    try {
      redactor.redact(weaver.weave(dox));

      XPathUtil x = new XPathUtil();
      var varNodes = x.xList(dox,
          "//cmmn:output/cmmn:extensionElements/dmn:context/dmn:contextEntry/dmn:variable");
      assertEquals(1, varNodes.getLength());

      asElementStream(x.xList(dox, "//cmmn:output")).forEach(
          taskOutput -> {
            String cfiRef = taskOutput.getAttribute("bindingRef");
            assertEquals("_e2e3a8fc-98d6-4170-8d10-c955931d404d", cfiRef);
            String cfiName = x.xString(dox, "//cmmn:caseFileItem[@id='" + cfiRef + "']/@name");

            String formalVar = taskOutput.getAttribute("name");
            assertEquals("Pt Old", formalVar);

            String actualVar = x.xString(taskOutput, ".//dmn:variable/@name");
            assertEquals("My Output", actualVar);
            assertEquals(cfiName, actualVar);
          }
      );
    } catch (IllegalStateException ie) {
      logger.error(ie.getMessage(), ie);
      fail(ie.getMessage());
    }
  }


  @Test
  void testRewriteCMMNMultiOutputs() {
    String path = "/Case with DMN multiO.raw.cmmn.xml";
    Document dox = loadXMLDocument(resolveResource(path))
        .orElseGet(() -> fail("Unable to load document " + path));

    try {
      redactor.redact(weaver.weave(dox));
      XPathUtil x = new XPathUtil();

      System.out.println(XMLUtil.toString(dox));

      var varNodes = x.xList(dox,
          "//cmmn:output/cmmn:extensionElements/dmn:context/dmn:contextEntry/dmn:variable");
      assertEquals(2, varNodes.getLength());

      var parser = new LanguageDeSerializer(List.of(new CMMN11Parser(List.of(new DMN12Parser()))));
      var cmmn = parser.applyLift(
              AbstractCarrier.of(dox, rep(CMMN_1_1, XML_1_1)),
              Abstract_Knowledge_Expression,
              codedRep(CMMN_1_1),
              null)
          .flatOpt(kc -> kc.as(TDefinitions.class))
          .orElseGet(Assertions::fail);

      var outputs = CMMN11Utils.streamTasks(cmmn)
          .flatMap(StreamUtil.filterAs(TDecisionTask.class))
          .flatMap(dt -> dt.getOutput().stream())
          .collect(Collectors.toList());

      var check = outputs.stream().allMatch(
          out -> {
            if (!(out.getBindingRef() instanceof TCaseFileItem)) {
              return false;
            }
            return out.getExtensionElements().getAny().stream()
                .flatMap(StreamUtil.filterAs(JAXBElement.class))
                .map(JAXBElement::getValue)
                .flatMap(StreamUtil.filterAs(TContext.class))
                .allMatch(c -> c.getContextEntry().size() == 1);
          }
      );
      assertTrue(check);

    } catch (IllegalStateException ie) {
      logger.error(ie.getMessage(), ie);
      fail(ie.getMessage());
    }
  }


  @Test
  void testRewriteCMMNOutputs2() {
    String path = "/Case with DMN IO advanced.raw.cmmn.xml";
    Document dox = loadXMLDocument(resolveResource(path))
        .orElseGet(() -> fail("Unable to load document " + path));

    try {
      redactor.redact(weaver.weave(dox));

      XPathUtil x = new XPathUtil();
      Node manyDecision = x.xNode(dox,
          "//cmmn:decisionTask[@name='Multi-Assessment Decision']");
      assertNotNull(manyDecision);

      asElementStream(x.xList(manyDecision, ".//cmmn:output"))
          .forEach(taskOutput -> {
            String formalVar = taskOutput.getAttribute("name");
            String cfiRef = taskOutput.getAttribute("bindingRef");
            String cfiName = x.xString(dox, "//cmmn:caseFileItem[@id='" + cfiRef + "']/@name");
            String actualVar = x.xString(taskOutput, ".//dmn:variable/@name");

            if ("Dec Out 1".equals(formalVar)) {
              assertEquals("My Out 2", actualVar);
              assertEquals(cfiName, actualVar);
            } else if ("Dec Out 2".equals(formalVar)) {
              assertEquals("My Out 3", actualVar);
              assertEquals(cfiName, actualVar);
            } else {
              fail("Unrecognized output " + formalVar);
            }
          });
    } catch (IllegalStateException ie) {
      logger.error(ie.getMessage(), ie);
      fail(ie.getMessage());
    }
  }

  @Test
  void testRemoveLibraryImports() {
    String path = "/TestImports.raw.xml";
    Document dox = loadXMLDocument(resolveResource(path))
        .orElseGet(() -> fail("Unable to load document " + path));

    try {
      redactor.redact(weaver.weave(dox));

      XPathUtil x = new XPathUtil();
      NodeList imports = x.xList(dox,
          "//dmn:import");
      assertEquals(1, imports.getLength());

      assertEquals("http://www.omg.org/spec/DMN/20180521/MODEL/",
          imports.item(0).getAttributes().getNamedItem("importType").getNodeValue());
    } catch (IllegalStateException ie) {
      logger.error(ie.getMessage(), ie);
      fail(ie.getMessage());
    }
  }

  private boolean confirmDecisionURI(Document dox) {
    XMLUtil.asElementStream(dox.getElementsByTagName("*"))
        .filter(el -> el.getLocalName().equals(DMN_EL_DECISION))
        .forEach(element -> {
          Attr attr = element.getAttributeNode("externalRef");
          if (!attr.getValue().contains("ns") || !attr.getValue().contains("_")) {
            fail("expect 'ns' and underscore in value: " + attr.getValue());
          }
        });
    return true;
  }

  private boolean verifyCaseFileItemDefinition(Document dox) {
    XMLUtil.asElementStream(dox.getElementsByTagName("*"))
        .filter(el -> el.getLocalName().equals("caseFileItemDefinition"))
        .forEach(element -> {
          Attr attr = element.getAttributeNode("definitionType");
          if (null != attr) {
            checkAttribute(attr, "definitionType");
          }
        });
    return true;
  }


  /**
   * verify that inputData and requiredInput elements have the correct href values no longer contain
   * 'trisotech.com' and are KMDP namespaces
   */
  private boolean verifyHrefs(Document dox) {
    XMLUtil.asElementStream(dox.getElementsByTagName("*"))
        .filter(el -> (el.getLocalName().equals("inputData")
            || el.getLocalName().equals("requiredInput")
            || el.getLocalName().equals("encapsulatedDecision")
            || el.getLocalName().equals("inputDecision")
            || el.getLocalName().equals("requiredDecision"))
            && (el.hasAttribute("href")))
        .forEach(element -> {
          Attr attr = element.getAttributeNode("href");
          checkAttribute(attr, "href");
          if (containsTrisotechNamespace(attr)) {
            fail("should NOT contain trisotech in attribute: " + attr.getName() + " : "
                + attr.getValue());
          }
        });
    return true;
  }


  /**
   * verify namespaces on the document root no longer contain 'trisotech.com' and are KMDP
   * namespaces
   */
  private boolean verifyRootNamespaces(Document dox) {
    // get attributes of the first element -- where the namespaces are set
    NamedNodeMap attributes = dox.getDocumentElement().getAttributes();

    int attrSize = attributes.getLength();
    for (int i = 0; i < attrSize; i++) {
      Attr attr = (Attr) attributes.item(i);

      if (!checkAttribute(attr, "xmlns")) {
        return false;
      }
      if (!checkAttribute(attr, "namespace")) {
        return false;
      }
      if (!checkAttribute(attr, "targetNamespace")) {
        return false;
      }
      if (!checkAttribute(attr, "import")) {
        return false;
      }

      if (!checkContainsAttribute(attr, "include")) {
        return false;
      }
      if (!checkContainsAttribute(attr, "ns")) {
        return false;
      }

      if (containsTrisotechNamespace(attr)) {
        return false;
      }

    }
    return true;
  }

  private boolean checkContainsAttribute(Attr attr, String include) {
    if (attr.getLocalName().contains(include)
        && attr.getValue().contains("trisotech.com")) {
      fail("should not contain 'trisotech.com' in value: " + " for attribute: " + attr
          .getLocalName());
      return false;
    }
    return true;
  }

  private boolean containsTrisotechNamespace(Attr attr) {
    return attr.getValue().toLowerCase().contains("trisotech");
  }


  private boolean checkAttribute(Attr attr, String namespace) {
    if (getAttributeName(attr).equals(namespace)
        && attr.getValue().contains("trisotech.com")) {
      fail("should not contain 'trisotech' in value: " + attr.getValue() + " for attribute: " + attr
          .getLocalName());
      return false;
    }
    return true;
  }

  private String getAttributeName(Attr attr) {
    return Optional.ofNullable(attr.getLocalName())
        .or(() -> Optional.ofNullable(attr.getName()))
        .orElse("");
  }


  private boolean verifyImportNamespace(Document dox) {
    NodeList elements = dox.getElementsByTagName("*");
    asElementStream(elements).filter(el -> el.getLocalName().equals("import"))
        .forEach(el -> {
              NamedNodeMap attributes = el.getAttributes();
              int attrSize = attributes.getLength();
              for (int i = 0; i < attrSize; i++) {
                Attr attr = (Attr) attributes.item(i);
                if (attr.getLocalName().equals("namespace")
                    && (attr.getValue().contains("trisotech.com"))) {
                  fail("should not have " + attr.getValue() + " in attribute: " + attr.getLocalName()
                      + " on parent: " + el.getNodeName());
                }
              }
            }
        );
    return true;
  }


  /**
   * Confirm the Trisotech namespaces and namespace attributes have been removed.
   */
  private boolean confirmNoTrisoNameSpace(Document dox) {
    // Confirm no trisotech tags remain:
    NodeList elements = dox.getElementsByTagNameNS("*", "*");
    asElementStream(elements).forEach(
        el -> {
          if (TT_METADATA_NS.equals(el.getNamespaceURI())) {
            fail("Proprietary Element " + el.getLocalName() +
                " id=" + el.getAttribute("id") + " has not been stripped");
          }
          NamedNodeMap attributes = el.getAttributes();
          int attrSize = attributes.getLength();
          for (int i = 0; i < attrSize; i++) {
            Attr attr = (Attr) attributes.item(i);
            if ((TT_METADATA_NS.equals(attr.getNamespaceURI()))
                || (TT_METADATA_NS.equals(attr.getValue()))
                || (TT_DMN_12_NS.equals(attr.getNamespaceURI()))
                || (TT_DMN_12_NS.equals(attr.getValue()))
                || (TT_CMMN_11_NS.equals(attr.getNamespaceURI()))
                || (TT_CMMN_11_NS.equals(attr.getValue()))
                || (DROOLS_NS.equals(attr.getNamespaceURI()))
                || (DROOLS_NS.equals(attr.getValue()))
                || (TT_META_EXPORTER.equals(attr.getLocalName()))
                || (TT_META_EXPORTER_VERSION.equals(attr.getLocalName()))
            ) {
              fail("Should not have '" + attr.getPrefix() + "' attributes anymore. Have: " +
                  attr.getLocalName() + " on parent: " + el.getNodeName());
            }
          }
        }
    );
    return true;
  }

  /**
   * Confirm the Trisotech tags not being modified have been removed.
   */
  private boolean confirmNoTrisotechTags(Document dox) {
    // Confirm no trisotech tags remain:
    NodeList elements = dox.getElementsByTagNameNS(TT_METADATA_NS, "*");
    asElementStream(elements).forEach(
        el -> {
          if ((TT_ATTACHMENT_ITEM.equals(el.getLocalName()))) {
            fail("should not have " + el.getNodeName() + " elements anymore.");
          }
        }
    );

    return true;
  }

  private <T extends Annotation> List<T> loadAnnotations(Document dox,
      SemanticAnnotationRelTypeSeries att,
      Class<T> type) {

    return XMLUtil.asElementStream(dox.getElementsByTagName("*"))
        .filter(el -> el.getLocalName().equals(DMN_EL_EXTENSIONS))
        .map(Element::getChildNodes)
        .flatMap(XMLUtil::asElementStream)
        .filter(el -> Annotation.class.getSimpleName().equalsIgnoreCase(el.getLocalName()))
        .map(el -> JaxbUtil.unmarshall(Annotation.class, el))
        .flatMap(StreamUtil::trimStream)
        .filter(a -> att.asConceptIdentifier().equals(a.getRel()))
        .map(type::cast)
        .collect(Collectors.toList());

  }

}
