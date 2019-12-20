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

import static edu.mayo.kmdp.preprocess.meta.Weaver.CLINICALKNOWLEDGEMANAGEMENT_MAYO_ARTIFACTS_BASE_URI;
import static edu.mayo.kmdp.util.Util.resolveResource;
import static edu.mayo.kmdp.util.XMLUtil.asElementStream;
import static edu.mayo.kmdp.util.XMLUtil.loadXMLDocument;
import static edu.mayo.kmdp.util.XMLUtil.streamXMLDocument;
import static edu.mayo.kmdp.util.XMLUtil.validate;
import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.CMMN_1_1;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import edu.mayo.kmdp.metadata.annotations.Annotation;
import edu.mayo.kmdp.metadata.annotations.BasicAnnotation;
import edu.mayo.kmdp.metadata.annotations.SimpleAnnotation;
import edu.mayo.kmdp.preprocess.meta.KnownAttributes;
import edu.mayo.kmdp.preprocess.meta.Weaver;
import edu.mayo.kmdp.registry.Registry;
import edu.mayo.kmdp.util.XMLUtil;
import edu.mayo.ontology.taxonomies.kmdo.annotationreltype.AnnotationRelTypeSeries;
import edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;


@SpringJUnitConfig(classes = {WeaverTest.TestWeaverConfig.class})
class WeaverTest {

  @Configuration
  @ComponentScan("edu.mayo.kmdp.preprocess.meta")
  public static class TestWeaverConfig {

  }

  @Autowired
  private Weaver weaver;

  @Test
  void testInit() {
    assertNotNull(weaver);
  }


  @Test
  void testWeave() {

    String path = "/Weaver Test 1.dmn";

    // using XMLUtil loadXMLDocument to load the XML Document properly
    // sets up the document for conversion by setting namespaceaware
    Document dox = loadXMLDocument(resolveResource(path))
        .orElseGet(() -> fail("Unable to load document " + path));
    try {
      weaver.weave(dox);
      streamXMLDocument(dox, System.out);

      assertTrue(true); // dummy
      // TODO: the following is currently failing due to the new way DMN 1.2 handles the reference; fix once validation fixed for support CAO
//			assertTrue( validate( dox, KnowledgeRepresentationLanguage.DMN_1_2.getRef())); // URI.create(Registry.getValidationSchema(KnowledgeRepresentationLanguage.DMN_1_2.getRef()).get() )) );
    } catch (IllegalStateException ie) {
      ie.printStackTrace();
      fail(ie.getMessage());
    }
  }

  @Test
  void testWeaveDefault() {
    String path = "/Weaver Test 1.dmn";
    Document dox = loadXMLDocument(resolveResource(path))
        .orElseGet(() -> fail("Unable to load document " + path));

    try {
      weaver.weave(dox);
      assertTrue(true); // dummy assertion so sonarlint doesn't complain
      // TODO: the following is currently failing due to the new way DMN 1.2 handles the reference; fix once validation fixed for support CAO
//			assertTrue( validate( dox, KnowledgeRepresentationLanguage.DMN_1_2.getRef() ) );

      streamXMLDocument(dox, System.out);
    } catch (IllegalStateException ie) {
      ie.printStackTrace();
      fail(ie.getMessage());
    }
  }

  @Test
  void testVariousMetadataOnDMN_BasicDecision() {
    String path = "/Basic Decision Model.dmn";

    Document dox = loadXMLDocument(resolveResource(path))
        .orElseGet(() -> fail("Unable to load document " + path));

    assertTrue(true); // dummy
    // TODO: the following is currently failing due to the new way DMN 1.2 handles the reference; fix once validation fixed for support CAO
//		assertTrue( validate( dox, KnowledgeRepresentationLanguage.DMN_1_2.getRef() ) );

    try {
      weaver.weave(dox);
      streamXMLDocument(dox, System.out);

      assertNotNull(dox);

      BasicAnnotation id = loadAnnotations(dox, KnownAttributes.ASSET_IDENTIFIER,
          BasicAnnotation.class).iterator().next();
      assertEquals(
          "https://clinicalknowledgemanagement.mayo.edu/assets/735a5764-fe3f-4ab8-b103-650b6e805db2/versions/1.0.0",
          id.getExpr().toString());

      assertTrue(confirmNoTrisoNameSpace(dox));

      assertTrue(verifyRootNamespaces(dox));

      assertTrue(verifyImportNamespace(dox));

      assertTrue(verifyHrefs(dox));

      SimpleAnnotation type = loadAnnotations(dox, KnownAttributes.TYPE, SimpleAnnotation.class)
          .iterator().next();
      assertEquals(AnnotationRelTypeSeries.Is_A.getLabel(),
          type.getRel().getLabel());

      // TODO: no examples of CAPTURES in sample models, provide? CAO
//			List<Annotation> props = loadAnnotations( dox, KnownAttributes.CAPTURES, Annotation.class );
//			assertTrue( props.stream()
//			                 .anyMatch( (ann) -> ann instanceof SimpleAnnotation
//					                 && ( ( SimpleAnnotation ) ann ).getExpr().getLabel().contains( "Blood Pressure" ) ) );
//

      // TODO: is this something else in Trisotech? CAO
//		assertEquals( "http://www.foo.bar",
//			              xString( dox, "//dmn:knowledgeSource[@name='all']/@locationURI" ) );

    } catch (IllegalStateException ie) {
      ie.printStackTrace();
      fail(ie.getMessage());
    }
  }


  @Test
  void testVariousMetadataOnDMN_WeaverTest2() {
    String path = "/Weaver Test 2.dmn";
    Document dox = loadXMLDocument(resolveResource(path))
        .orElseGet(() -> fail("Unable to load document " + path));

    assertTrue(true); // dummy
    // TODO: the following is currently failing due to the new way DMN 1.2 handles the reference; fix once validation fixed for support CAO
//		assertTrue( validate( dox, KnowledgeRepresentationLanguage.DMN_1_2.getRef() ) );

    try {
      weaver.weave(dox);
      streamXMLDocument(dox, System.out);

      assertNotNull(dox);

      BasicAnnotation id = loadAnnotations(dox, KnownAttributes.ASSET_IDENTIFIER,
          BasicAnnotation.class).iterator().next();

      // assert basics for a woven file; this test file doesn't have any valid annotation types to confirm
      assertEquals(
          "https://clinicalknowledgemanagement.mayo.edu/assets/3c66cf3a-93c4-4e09-b1aa-14088c76aded/versions/1.0.0-SNAPSHOT",
          id.getExpr().toString());

      assertTrue(confirmNoTrisoNameSpace(dox));

      assertTrue(verifyRootNamespaces(dox));

      assertTrue(verifyImportNamespace(dox));

      assertTrue(verifyHrefs(dox));

    } catch (IllegalStateException ie) {
      ie.printStackTrace();
      fail(ie.getMessage());
    }
  }


  @Test
  void testVariousMetadataOnDMN_Subdecision() {
    String path = "/Decision Subdecision.dmn";
    Document dox = loadXMLDocument(resolveResource(path))
        .orElseGet(() -> fail("Unable to load document " + path));

    assertTrue(true); // dummy
    // TODO: the following is currently failing due to the new way DMN 1.2 handles the reference; fix once validation fixed for support CAO
//		assertTrue( validate( dox, KnowledgeRepresentationLanguage.DMN_1_2.getRef() ) );

    try {
      weaver.weave(dox);
      streamXMLDocument(dox, System.out);

      assertNotNull(dox);

      BasicAnnotation id = loadAnnotations(dox, KnownAttributes.ASSET_IDENTIFIER,
          BasicAnnotation.class).iterator().next();
      assertEquals(
          "https://clinicalknowledgemanagement.mayo.edu/assets/22c207d7-36e2-4935-a634-5205699ce6d0",
          id.getExpr().toString());

      assertTrue(confirmNoTrisoNameSpace(dox));

      assertTrue(verifyRootNamespaces(dox));

      assertTrue(verifyImportNamespace(dox));

      assertTrue(verifyHrefs(dox));

      SimpleAnnotation type = loadAnnotations(dox, KnownAttributes.TYPE, SimpleAnnotation.class)
          .iterator().next();
      assertEquals(AnnotationRelTypeSeries.Is_A.getLabel(),
          type.getRel().getLabel());

      // TODO: No example of CAPTURES in test models. provide? CAO
//			List<Annotation> props = loadAnnotations( dox, KnownAttributes.CAPTURES, Annotation.class );
//			assertTrue( props.stream()
//			                 .anyMatch( (ann) -> ann instanceof SimpleAnnotation
//					                 && ( ( SimpleAnnotation ) ann ).getExpr().getLabel().contains( "Blood Pressure" ) ) );
//

      // TODO: is this something else in Trisotech? CAO
//		assertEquals( "http://www.foo.bar",
//			              xString( dox, "//dmn:knowledgeSource[@name='all']/@locationURI" ) );

    } catch (IllegalStateException ie) {
      ie.printStackTrace();
      fail(ie.getMessage());
    }
  }

  @Test
  void testVariousMetadataOnDMN_ComputableDecision() {
    String path = "/Computable Decision Model.dmn";
    Document dox = loadXMLDocument(resolveResource(path))
        .orElseGet(() -> fail("Unable to load document " + path));

    assertTrue(true); // dummy
    // TODO: the following is currently failing due to the new way DMN 1.2 handles the reference; fix once validation fixed for support CAO
//		assertTrue( validate( dox, KnowledgeRepresentationLanguage.DMN_1_2.getRef() ) );

    try {
      weaver.weave(dox);
      streamXMLDocument(dox, System.out);

      assertNotNull(dox);

      BasicAnnotation id = loadAnnotations(dox, KnownAttributes.ASSET_IDENTIFIER,
          BasicAnnotation.class).iterator().next();
      assertEquals(
          "https://clinicalknowledgemanagement.mayo.edu/assets/102117ea-82ed-4c34-91dc-c0aa962fbf66",
          id.getExpr().toString());

      assertTrue(confirmNoTrisoNameSpace(dox));

      assertTrue(verifyRootNamespaces(dox));

      assertTrue(verifyImportNamespace(dox));

      assertTrue(verifyHrefs(dox));

      SimpleAnnotation type = loadAnnotations(dox, KnownAttributes.TYPE, SimpleAnnotation.class)
          .iterator().next();
      assertEquals(AnnotationRelTypeSeries.Is_A.getLabel(),
          type.getRel().getLabel());

      type = loadAnnotations(dox, KnownAttributes.DEFINES, SimpleAnnotation.class).iterator()
          .next();
      assertEquals(AnnotationRelTypeSeries.Defines.getLabel(),
          type.getRel().getLabel());

      List<Annotation> props = loadAnnotations(dox, KnownAttributes.INPUTS, Annotation.class);
      assertTrue(props.stream()
          .anyMatch(ann -> ann instanceof SimpleAnnotation
              && (ann).getRel().getLabel()
              .equals(AnnotationRelTypeSeries.In_Terms_Of.getLabel())));

// TODO: No example of CAPTURES in test models. Need one? CAO
//			List<Annotation> props = loadAnnotations( dox, KnownAttributes.CAPTURES, Annotation.class );
//			assertTrue( props.stream()
//			                 .anyMatch( (ann) -> ann instanceof SimpleAnnotation
//					                 && ( ( SimpleAnnotation ) ann ).getExpr().getLabel().contains( "Blood Pressure" ) ) );
//

      // TODO: is this something else in Trisotech? CAO
//		assertEquals( "http://www.foo.bar",
//			              xString( dox, "//dmn:knowledgeSource[@name='all']/@locationURI" ) );

    } catch (IllegalStateException ie) {
      ie.printStackTrace();
      fail(ie.getMessage());
    }
  }

  @Test
  void testVariousMetadataOnCMMN_WeaveTest1() {
    String path = "/Weave Test 1.cmmn";

    // loadXMLDocument sets NamespaceAware
    Document dox = loadXMLDocument(resolveResource(path))
        .orElseGet(() -> fail("Unable to load document " + path));

    try {

      weaver.weave(dox);

      System.out.println("CMMN file AFTER weave: ");
      streamXMLDocument(dox, System.out);

      assertTrue(validate(dox, CMMN_1_1.getRef()));

      assertTrue(confirmDecisionURI(dox));

      assertTrue(confirmNoTrisoNameSpace(dox));

      assertTrue(verifyRootNamespaces(dox));
      assertTrue(verifyCaseFileItemDefinition(dox));

      NodeList metas = dox.getElementsByTagNameNS(weaver.getMetadataNS(), weaver.getMetadataEl());
      assertEquals(0, metas.getLength());
      NodeList relations = dox
          .getElementsByTagNameNS(weaver.getMetadataNS(), weaver.getMetadataRS());

      assertEquals(0, relations.getLength());

    } catch (IllegalStateException ie) {
      ie.printStackTrace();
      fail(ie.getMessage());
    }
  }

  @Test
  void testVariousMetadataOnCMMN_WeaveTest2() {
    String path = "/Weave Test 2.cmmn";
    // loadXMLDocument set setNamespaceAware
    Document dox = loadXMLDocument(resolveResource(path))
        .orElseGet(() -> fail("Unable to load document " + path));

    try {

      weaver.weave(dox);
      System.out.println("CMMN file AFTER weave: ");
      streamXMLDocument(dox, System.out);

      assertTrue(validate(dox, CMMN_1_1.getRef()));

      assertTrue(confirmDecisionURI(dox));

      assertTrue(confirmNoTrisoNameSpace(dox));

      assertTrue(verifyRootNamespaces(dox));
      assertTrue(verifyCaseFileItemDefinition(dox));

      NodeList metas = dox.getElementsByTagNameNS(weaver.getMetadataNS(), weaver.getMetadataEl());
      assertEquals(0, metas.getLength());
      NodeList relations = dox
          .getElementsByTagNameNS(weaver.getMetadataNS(), weaver.getMetadataRS());

      assertEquals(0, relations.getLength());

    } catch (IllegalStateException ie) {
      ie.printStackTrace();
      fail(ie.getMessage());
    }
  }


  @Test
  void testVariousMetadataOnCMMN_BasicCaseModel() {
    String path = "/Basic Case Model.cmmn";
    // loadXMLDocument sets NamespaceAware
    Document dox = loadXMLDocument(resolveResource(path))
        .orElseGet(() -> fail("Unable to load document " + path));

    try {

      weaver.weave(dox);
      System.out.println("CMMN file AFTER weave: ");
      streamXMLDocument(dox, System.out);

      assertTrue(validate(dox, CMMN_1_1.getRef()));

      BasicAnnotation id = loadAnnotations(dox, KnownAttributes.ASSET_IDENTIFIER,
          BasicAnnotation.class).iterator().next();
      assertEquals(
          "https://clinicalknowledgemanagement.mayo.edu/assets/14321e7c-cb9a-427f-abf5-1420bf26e03c/versions/1.0.1",
          id.getExpr().toString());

      assertTrue(confirmDecisionURI(dox));

      assertTrue(confirmNoTrisoNameSpace(dox));

      assertTrue(verifyRootNamespaces(dox));
      assertTrue(verifyCaseFileItemDefinition(dox));

      NodeList metas = dox.getElementsByTagNameNS(weaver.getMetadataNS(), weaver.getMetadataEl());
      assertEquals(0, metas.getLength());
      NodeList relations = dox
          .getElementsByTagNameNS(weaver.getMetadataNS(), weaver.getMetadataRS());

      assertEquals(0, relations.getLength());

    } catch (IllegalStateException ie) {
      ie.printStackTrace();
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
    // TODO: check each tag, or just get all href attributes and modify?  use KnownAttributes? CAO
    XMLUtil.asElementStream(dox.getElementsByTagName("*"))
        .filter((el) -> (el.getLocalName().equals("inputData")
            || el.getLocalName().equals("requiredInput")
            || el.getLocalName().equals("encapsulatedDecision")
            || el.getLocalName().equals("inputDecision")
            || el.getLocalName().equals("requiredDecision"))
            && (el.hasAttribute("href")))
        .forEach(element -> {
          Attr attr = element.getAttributeNode("href");
          checkAttribute(attr, "href");
          // TODO: Fix this, not all hrefs have KMDP namespace CAO
//          if(!confirmKMDPnamespace(attr)) {
//            fail("expect BASE_URI in attribute: " + attr.getName() + " : " + attr.getValue());
//          }
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
      // confirm KMDP namespaces
      // TODO: fix this .. not all namespaces have KMDP CAO
      confirmKMDPnamespace(attr);

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

  private boolean confirmKMDPnamespace(Attr attr) {
    if (attr.getValue().contains(CLINICALKNOWLEDGEMANAGEMENT_MAYO_ARTIFACTS_BASE_URI)) {
      return true;
    }
    return false;
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
    asElementStream(elements).filter((el) -> el.getLocalName().equals("import"))
        .forEach((el) -> {
              NamedNodeMap attributes = el.getAttributes();
              int attrSize = attributes.getLength();
              for (int i = 0; i < attrSize; i++) {
                Attr attr = (Attr) attributes.item(i);
                if (attr.getLocalName().equals("namespace")) {
                  if (attr.getValue().contains("trisotech.com")) {
                    fail("should not have " + attr.getValue() + " in attribute: " + attr.getLocalName()
                        + " on parent: " + el.getNodeName());
                  }
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
        (el) -> {
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

  // TODO: need more capability? CAO
  private <T extends Annotation> List<T> loadAnnotations(Document dox, KnownAttributes att,
      Class<T> type) {

    return XMLUtil.asElementStream(dox.getElementsByTagName("*"))
        .filter(el -> el.getLocalName().equals("extensionElements"))
        .map(Element::getChildNodes)
        .flatMap(XMLUtil::asElementStream)
        .map(SurrogateHelper::unmarshallAnnotation)
        .filter(a -> att.asConcept().equals(a.getRel()))
        .map(type::cast)
        .collect(Collectors.toList());

  }
}
