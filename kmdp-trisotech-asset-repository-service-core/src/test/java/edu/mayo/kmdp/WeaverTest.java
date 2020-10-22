/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package edu.mayo.kmdp;

import static edu.mayo.kmdp.util.Util.resolveResource;
import static edu.mayo.kmdp.util.XMLUtil.asElementStream;
import static edu.mayo.kmdp.util.XMLUtil.loadXMLDocument;
import static edu.mayo.kmdp.util.XMLUtil.streamXMLDocument;
import static edu.mayo.kmdp.util.XMLUtil.validate;
import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.CMMN_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.DMN_1_2;

import edu.mayo.kmdp.kdcaci.knew.trisotech.preprocess.Weaver;
import edu.mayo.kmdp.util.JaxbUtil;
import edu.mayo.kmdp.util.StreamUtil;
import edu.mayo.kmdp.util.XMLUtil;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import edu.mayo.ontology.taxonomies.kao.decisiontype.DecisionType;
import edu.mayo.ontology.taxonomies.kao.decisiontype.DecisionTypeSeries;
import edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.id.ConceptIdentifier;
import org.omg.spec.api4kp._20200801.surrogate.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;


@SpringBootTest
@ContextConfiguration(classes = {TrisotechAssetRepositoryConfig.class})
@TestPropertySource(properties = {
    "edu.mayo.kmdp.trisotechwrapper.repositoryName=MEA-Test",
    "edu.mayo.kmdp.trisotechwrapper.repositoryId=d4aca01b-d446-4bc8-a6f0-85d84f4c1aaf"})
class WeaverTest {
  
  Logger logger = LoggerFactory.getLogger(WeaverTest.class);

  @Autowired
  private Weaver weaver;

  @Test
  void testInit() {
    assertNotNull(weaver);
  }


  @Test
  void testWeave() {

    String path = "/Weaver Test 1.dmn.xml";

    // using XMLUtil loadXMLDocument to load the XML Document properly
    // sets up the document for conversion by setting namespaceaware
    Document dox = loadXMLDocument(resolveResource(path))
        .orElseGet(() -> fail("Unable to load document " + path));
    try {
      weaver.weave(dox);
      streamXMLDocument(dox, System.out);

			assertTrue( validate( dox, DMN_1_2.getReferentId()));
    } catch (IllegalStateException ie) {
      logger.error(ie.getMessage(),ie);
      fail(ie.getMessage());
    }
  }

  @Test
  void testWeaveDefault() {
    String path = "/Basic Decision Model.raw.dmn.xml";
    Document dox = loadXMLDocument(resolveResource(path))
        .orElseGet(() -> fail("Unable to load document " + path));

    try {
      weaver.weave(dox);
      assertTrue( validate( dox, DMN_1_2.getReferentId() ) );

      streamXMLDocument(dox, System.out);
    } catch (IllegalStateException ie) {
      logger.error(ie.getMessage(),ie);
      fail(ie.getMessage());
    }
  }

  @Test
  void testVariousMetadataOnDMNBasicDecision() {
    String path = "/Basic Decision Model.raw.dmn.xml";

    Document dox = loadXMLDocument(resolveResource(path))
        .orElseGet(() -> fail("Unable to load document " + path));

    assertTrue(true); // dummy
		assertTrue( validate( dox, DMN_1_2.getReferentId() ) );

    try {
      weaver.weave(dox);
      streamXMLDocument(dox, System.out);

      assertNotNull(dox);

      assertTrue(confirmNoTrisoNameSpace(dox));

      assertTrue(confirmNoTrisotechTags(dox));

      assertTrue(verifyRootNamespaces(dox));

      assertTrue(verifyImportNamespace(dox));

      assertTrue(verifyHrefs(dox));


			List<Annotation> props = loadAnnotations( dox, Captures, Annotation.class );
			assertEquals(3, props.size());
			Set<DecisionType> decisions = props.stream()
            .map(Annotation::getRef)
            .map(ConceptIdentifier::getUuid)
            .map(DecisionTypeSeries::resolveUUID)
            .flatMap(StreamUtil::trimStream)
            .collect(Collectors.toSet());
			assertEquals(3, decisions.size());
			assertTrue(decisions.contains(DecisionTypeSeries.Aggregation_Decision));
      assertTrue(decisions.contains(DecisionTypeSeries.Assessment_Decision));
      assertTrue(decisions.contains(DecisionTypeSeries.Naturalistic_Decision));


    } catch (IllegalStateException ie) {
      logger.error(ie.getMessage(),ie);
      fail(ie.getMessage());
    }
  }

  @Test
  void testVariousMetadataOnDMN_WeaverTest2() {
    String path = "/Weaver Test 2.raw.dmn.xml";
    Document dox = loadXMLDocument(resolveResource(path))
        .orElseGet(() -> fail("Unable to load document " + path));

    assertTrue(true); // dummy
		assertTrue( validate( dox, DMN_1_2.getReferentId() ) );

    try {
      weaver.weave(dox);
      streamXMLDocument(dox, System.out);

      assertNotNull(dox);

      assertTrue(confirmNoTrisoNameSpace(dox));

      assertTrue(confirmNoTrisotechTags(dox));

      assertTrue(verifyRootNamespaces(dox));

      assertTrue(verifyImportNamespace(dox));

      assertTrue(verifyHrefs(dox));

    } catch (IllegalStateException ie) {
      logger.error(ie.getMessage(),ie);
      fail(ie.getMessage());
    }
  }


  @Test
  void testVariousMetadataOnDMN_Subdecision() {
    String path = "/Decision Subdecision.raw.dmn.xml";
    Document dox = loadXMLDocument(resolveResource(path))
        .orElseGet(() -> fail("Unable to load document " + path));

		assertTrue( validate( dox, DMN_1_2.getReferentId() ) );

    try {
      weaver.weave(dox);
      streamXMLDocument(dox, System.out);

      assertNotNull(dox);

      assertTrue(confirmNoTrisoNameSpace(dox));

      assertTrue(confirmNoTrisotechTags(dox));

      assertTrue(verifyRootNamespaces(dox));

      assertTrue(verifyImportNamespace(dox));

      assertTrue(verifyHrefs(dox));

      List<Annotation> props = loadAnnotations( dox, Captures, Annotation.class );
      assertEquals(6, props.size());
      Set<DecisionType> decisions = props.stream()
          .map(Annotation::getRef)
          .map(ConceptIdentifier::getUuid)
          .map(DecisionTypeSeries::resolveUUID)
          .flatMap(StreamUtil::trimStream)
          .collect(Collectors.toSet());
      assertEquals(5, decisions.size());
      // there are 2 Naturalistic_Decision, that's why only 5 are asserted here
      assertTrue(decisions.contains(DecisionTypeSeries.Aggregation_Decision));
      assertTrue(decisions.contains(DecisionTypeSeries.Assessment_Decision));
      assertTrue(decisions.contains(DecisionTypeSeries.Naturalistic_Decision));
      assertTrue(decisions.contains(DecisionTypeSeries.Choice_Decision));
      assertTrue(decisions.contains(DecisionTypeSeries.Actionable_Decision));

      props = loadAnnotations(dox, In_Terms_Of, Annotation.class);
      assertTrue(props.stream()
          .anyMatch(ann -> ann.getRel().getPrefLabel()
              .equals(In_Terms_Of.getLabel())));
      assertEquals(3, props.size());

      assertTrue( props.stream()
          .anyMatch( ann -> ann.getRef().getName().contains("Test On Medication")));

      assertTrue( props.stream()
          .anyMatch( ann -> ann.getRef().getName().contains("Test Most Recent Observation")));

    } catch (IllegalStateException ie) {
      logger.error(ie.getMessage(),ie);
      fail(ie.getMessage());
    }
  }

  @Test
  void testVariousMetadataOnDMNComputableDecision() {
    String path = "/Computable Decision Model.raw.dmn.xml";
    Document dox = loadXMLDocument(resolveResource(path))
        .orElseGet(() -> fail("Unable to load document " + path));

    assertTrue(true); // dummy
 		assertTrue( validate( dox, DMN_1_2.getReferentId() ) );

    try {
      weaver.weave(dox);
      streamXMLDocument(dox, System.out);

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

      assertTrue( props.stream()
          .anyMatch( ann -> ann.getRef().getUuid().toString().equals("6c14a53a-7ce2-34aa-9cf1-a800317bebef")));
      assertTrue(props.stream()
          .anyMatch( ann -> ann.getRef().getUuid().toString().equals("9296f375-a7ed-3c59-a972-4a7eb40c8820")));

      List<Annotation> props2 = loadAnnotations( dox, Captures, Annotation.class );
      assertTrue( props2.stream()
          .anyMatch( ann -> ann.getRef().getLabel().contains( "Computable Decision" ) ) );

    } catch (IllegalStateException ie) {
      logger.error(ie.getMessage(),ie);
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

      weaver.weave(dox);

      streamXMLDocument(dox, System.out);

      assertTrue(validate(dox, CMMN_1_1.getReferentId()));

      assertTrue(confirmDecisionURI(dox));

      assertTrue(confirmNoTrisoNameSpace(dox));

      assertTrue(confirmNoTrisotechTags(dox));

      assertTrue(verifyRootNamespaces(dox));
      assertTrue(verifyCaseFileItemDefinition(dox));

      NodeList metas = dox.getElementsByTagNameNS(weaver.getMetadataNS(), weaver.getMetadataEl());
      assertEquals(0, metas.getLength());
      NodeList relations = dox
          .getElementsByTagNameNS(weaver.getMetadataNS(), weaver.getMetadataRS());

      assertEquals(0, relations.getLength());

    } catch (IllegalStateException ie) {
      logger.error(ie.getMessage(),ie);
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

      weaver.weave(dox);
      streamXMLDocument(dox, System.out);

      assertTrue(validate(dox, CMMN_1_1.getReferentId()));

      assertTrue(confirmDecisionURI(dox));

      assertTrue(confirmNoTrisoNameSpace(dox));

      assertTrue(confirmNoTrisotechTags(dox));

      assertTrue(verifyRootNamespaces(dox));
      assertTrue(verifyCaseFileItemDefinition(dox));

      NodeList metas = dox.getElementsByTagNameNS(weaver.getMetadataNS(), weaver.getMetadataEl());
      assertEquals(0, metas.getLength());
      NodeList relations = dox
          .getElementsByTagNameNS(weaver.getMetadataNS(), weaver.getMetadataRS());

      assertEquals(0, relations.getLength());

    } catch (IllegalStateException ie) {
      logger.error(ie.getMessage(),ie);
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

      weaver.weave(dox);
      streamXMLDocument(dox, System.out);

      assertTrue(validate(dox, CMMN_1_1.getReferentId()));

      assertTrue(confirmDecisionURI(dox));

      assertTrue(confirmNoTrisoNameSpace(dox));

      assertTrue(confirmNoTrisotechTags(dox));

      assertTrue(verifyRootNamespaces(dox));
      assertTrue(verifyCaseFileItemDefinition(dox));

      NodeList metas = dox.getElementsByTagNameNS(weaver.getMetadataNS(), weaver.getMetadataEl());
      assertEquals(0, metas.getLength());
      NodeList relations = dox
          .getElementsByTagNameNS(weaver.getMetadataNS(), weaver.getMetadataRS());

      assertEquals(0, relations.getLength());

    } catch (IllegalStateException ie) {
      logger.error(ie.getMessage(),ie);
      fail(ie.getMessage());
    }
  }

  private boolean confirmDecisionURI(Document dox) {
    XMLUtil.asElementStream(dox.getElementsByTagName("*"))
        .filter(el -> el.getLocalName().equals(weaver.getDecisionEl()))
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
          if(containsTrisotechNamespace(attr)) {
            fail("should NOT contain trisotech in attribute: " + attr.getName() + " : " + attr.getValue());
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
    if (attr.getLocalName().equals(namespace)
        && attr.getValue().contains("trisotech.com")) {
      fail("should not contain 'trisotech' in value: " + attr.getValue() + " for attribute: " + attr
          .getLocalName());
      return false;
    }
    return true;
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
          NamedNodeMap attributes = el.getAttributes();
          int attrSize = attributes.getLength();
          for (int i = 0; i < attrSize; i++) {
            Attr attr = (Attr) attributes.item(i);
            if ((weaver.getMetadataNS().equals(attr.getNamespaceURI()))
                || (weaver.getMetadataNS().equals(attr.getValue()))
                || (weaver.getMetadataDiagramDmnNS().equals(attr.getNamespaceURI()))
                || (weaver.getMetadataDiagramDmnNS().equals(attr.getValue()))
                || (weaver.getMetadataDiagramCmmnNS().equals(attr.getNamespaceURI()))
                || (weaver.getMetadataDiagramCmmnNS().equals(attr.getValue()))
                || (weaver.getDroolsNS().equals(attr.getNamespaceURI()))
                || (weaver.getDroolsNS().equals(attr.getValue()))
                || (weaver.getElExporter().equals(attr.getLocalName()))
                || (weaver.getElExporterVersion().equals(attr.getLocalName()))
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
    NodeList elements = dox.getElementsByTagNameNS(weaver.getMetadataNS(), "*");
    asElementStream(elements).forEach(
        el -> {
          if((weaver.getMetadataItemDef().equals(el.getLocalName()))
          || (weaver.getMetadataAttachment().equals(el.getLocalName()))) {
            fail("should not have " + el.getNodeName() + " elements anymore.");
          }
        }
    );

    return true;
  }

  private <T extends Annotation> List<T> loadAnnotations(Document dox, SemanticAnnotationRelTypeSeries att,
                                                         Class<T> type) {

    return XMLUtil.asElementStream(dox.getElementsByTagName("*"))
        .filter(el -> el.getLocalName().equals("extensionElements"))
        .map(Element::getChildNodes)
        .flatMap(XMLUtil::asElementStream)
        .map(el -> JaxbUtil.unmarshall(Annotation.class,el))
        .flatMap(StreamUtil::trimStream)
        .filter(a -> att.asConceptIdentifier().equals(a.getRel()))
        .map(type::cast)
        .collect(Collectors.toList());

  }

}
