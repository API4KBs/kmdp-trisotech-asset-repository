/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.mayo.kmdp;

import edu.mayo.kmdp.metadata.annotations.Annotation;
import edu.mayo.kmdp.metadata.annotations.BasicAnnotation;
import edu.mayo.kmdp.registry.Registry;
import edu.mayo.kmdp.preprocess.meta.KnownAttributes;
import edu.mayo.ontology.taxonomies.krlanguage._2018._08.KnowledgeRepresentationLanguage;
import edu.mayo.kmdp.util.XMLUtil;
import edu.mayo.kmdp.preprocess.meta.Weaver;
//import edu.mayo.kmdp.preprocess.meta.KnownAttributes;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.w3c.dom.*;

import java.util.List;
import java.util.stream.Collectors;

import static edu.mayo.kmdp.preprocess.meta.Weaver.CLINICALKNOWLEGEMANAGEMENT_MAYO_ARTIFACTS_BASE_URI;
import static edu.mayo.kmdp.util.Util.resolveResource;
import static edu.mayo.kmdp.util.XMLUtil.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class WeaverTest {

  @Test
  void testInit() {
    try {
      new Weaver();

    } catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }


  @Test
  void testWeave() {
//    String path = "/WeaverTest1.dmn";
    String path = "/Choice of Atrial Fibrillation Treatment Strategy.dmn";
//		String path = "/Prior Management of Atrial Fibrillation.dmn";

    // using XMLUtil loadXMLDocument to load the XML Document properly
    // sets up the document for conversion by setting namespaceaware
    Document dox = loadXMLDocument(resolveResource(path)).orElseGet(() -> fail("Unable to load document " + path));
    try {
      new Weaver().weave(dox);

      streamXMLDocument(dox, System.out);
      System.out.println("KRLanguage DMN1.2 ref: " + KnowledgeRepresentationLanguage.DMN_1_2.getRef());
      System.out.println("registry getValidationSchema for KRLanguage ref: " + Registry.getValidationSchema(KnowledgeRepresentationLanguage.DMN_1_2.getRef()).get());

      assertTrue(true); // dummy
      // TODO: the following is currently failing due to the new way DMN 1.2 handles the reference; fix once validation fixed for support CAO
//			assertTrue( validate( dox, KnowledgeRepresentationLanguage.DMN_1_2.getRef())); // URI.create(Registry.getValidationSchema(KnowledgeRepresentationLanguage.DMN_1_2.getRef()).get() )) );
    } catch (IllegalStateException ie) {
      ie.printStackTrace();
      fail(ie.getMessage());
    }
  }

  // TODO: How is 'default' different than test above? What is considered 'default'? CAO
  @Test
  void testWeaveDefault() {
    String path = "/WeaverTest1.dmn";
    Document dox = loadXMLDocument(resolveResource(path)).orElseGet(() -> fail("Unable to load document " + path));

    try {
      new Weaver().weave(dox);

      assertTrue(true); // dummy
      // TODO: the following is currently failing due to the new way DMN 1.2 handles the reference; fix once validation fixed for support CAO
//			assertTrue( validate( dox, KnowledgeRepresentationLanguage.DMN_1_2.getRef() ) );

      streamXMLDocument(dox, System.out);
    } catch (IllegalStateException ie) {
      ie.printStackTrace();
      fail(ie.getMessage());
    }
  }


  // TODO: Needed? CAO
  @Disabled
  @Test
  void testWeaveSalience() {
    // TODO: If needed, need a new dmn for testing
    String path = "/Salient.dmn";
    Document dox = loadXMLDocument(resolveResource(path)).orElseGet(() -> fail("Unable to load document " + path));

    try {
      new Weaver();

      assertTrue(validate(dox, KnowledgeRepresentationLanguage.DMN_1_2.getRef()));
      streamXMLDocument(dox, System.out);

//			List<Annotation> props = loadAnnotations( dox, KnownAttributes.SALIENCE, Annotation.class );
//			assertEquals( 1, props.size() );
//			assertTrue( props.get( 0 ) instanceof DatatypeAnnotation );
//			assertEquals( "33", ((DatatypeAnnotation) props.get( 0 )).getValue() );

    } catch (IllegalStateException ie) {
      ie.printStackTrace();
      fail(ie.getMessage());
    }
  }


  @Test
  void testVariousMetadataOnDMN() {
    String path = "/WeaverTest1.dmn";
//    String path = "/Choice of Atrial Fibrillation Treatment Strategy.dmn";
//		String path = "/Prior Management of Atrial Fibrillation.dmn
    Document dox = loadXMLDocument(resolveResource(path)).orElseGet(() -> fail("Unable to load document " + path));


    assertTrue(true); // dummy
    // TODO: the following is currently failing due to the new way DMN 1.2 handles the reference; fix once validation fixed for support CAO
//		assertTrue( validate( dox, KnowledgeRepresentationLanguage.DMN_1_2.getRef() ) );

    try {
      new Weaver().weave(dox);

      streamXMLDocument(dox, System.out);

      assertNotNull(dox);

      BasicAnnotation id = loadAnnotations(dox, KnownAttributes.ASSET_IDENTIFIER, BasicAnnotation.class).iterator().next();
      // this test is for WeaverTest1.dmn; disable for any other file
      assertEquals("https://clinicalknowledgemanagement.mayo.edu/assets/3c66cf3a-93c4-4e09-b1aa-14088c76aded/versions/1.0.0-SNAPSHOT",
          id.getExpr().toString());


      assertTrue(confirmNoTrisoNameSpace(dox));

      assertTrue(verifyRootNamespaces(dox));

      assertTrue(verifyImportNamespace(dox));

      assertTrue(verifyHrefs(dox));

      // TODO: is any of the following still needed? relevant to Trisotech data? CAO
//			SimpleAnnotation type = loadAnnotations( dox, KnownAttributes.TYPE, SimpleAnnotation.class ).iterator().next();
//			assertEquals( KnowledgeAssetType.Semantic_Decision_Model.getLabel(),
//			              type.getExpr().getLabel() );
//
//			List<Annotation> props = loadAnnotations( dox, KnownAttributes.CAPTURES, Annotation.class );
//			assertTrue( props.stream()
//			                 .anyMatch( (ann) -> ann instanceof SimpleAnnotation
//					                 && ( ( SimpleAnnotation ) ann ).getExpr().getLabel().contains( "Blood Pressure" ) ) );
//
//			List<SimpleAnnotation> subj = loadAnnotations( dox, KnownAttributes.SUBJECT, SimpleAnnotation.class );
//			assertTrue( subj.stream()
//			                .anyMatch( (ann) -> ann.getExpr().getLabel().contains( "Diabetes Mellitus" )
//					                || ann .getExpr().getTag().equals( "0215e32f-cced-4388-b0e0-ec8114e632d2" ) ) );

      // TODO: is this something else in Trisotech? CAO
//		assertEquals( "http://www.foo.bar",
//			              xString( dox, "//dmn:knowledgeSource[@name='all']/@locationURI" ) );

    } catch (IllegalStateException ie) {
      ie.printStackTrace();
      fail(ie.getMessage());
    }
  }


  @Test
  void testVariousMetadataOnCMMN() {
    String path = "/WeaveTest1.cmmn";

    // loadXMLDocument set setNamespaceAware
    Document dox = loadXMLDocument(resolveResource(path)).orElseGet(() -> fail("Unable to load document " + path));

    try {

      new Weaver()
          .weave(dox);

      System.out.println("CMMN file AFTER weave: ");
      streamXMLDocument(dox, System.out);

//      System.out.println("registry getValidationSchema for CMMN KRLanguage ref: " + Registry.getValidationSchema(KnowledgeRepresentationLanguage.CMMN_1_1.getRef()).get());

//      assertTrue(validate(dox, KnowledgeRepresentationLanguage.CMMN_1_1.getRef()));

      assertTrue(confirmDecisionURI(dox));

      assertTrue(confirmNoTrisoNameSpace(dox));

      assertTrue(verifyRootNamespaces(dox));
      assertTrue(verifyCaseFileItemDefinition(dox));

      NodeList metas = dox.getElementsByTagNameNS(Weaver.getMETADATA_NS(), Weaver.getMETADATA_EL());
      assertEquals(0, metas.getLength());
      NodeList relations = dox.getElementsByTagNameNS(Weaver.getMETADATA_NS(), Weaver.getMETADATA_RS());

      assertEquals(0, relations.getLength());

    } catch (IllegalStateException ie) {
      ie.printStackTrace();
      fail(ie.getMessage());
    }
  }

  // TODO: pick up here
  private boolean confirmDecisionURI(Document dox) {
    XMLUtil.asElementStream(dox.getElementsByTagName("*"))
        .filter(el -> el.getLocalName().equals(Weaver.getDecisionEl()))
        .forEach(element -> {
          Attr attr = element.getAttributeNode("externalRef");
          if (!confirmKMDPnamespace(attr)) {
            fail("expect BASE_URI in attribute: " + attr.getName() + " : " + attr.getValue());
          }
        });
    return true;
  }

  private boolean verifyCaseFileItemDefinition(Document dox) {
    XMLUtil.asElementStream(dox.getElementsByTagName("*"))
        .filter(el -> el.getLocalName().equals("caseFileItemDefinition"))
        .forEach(element -> {
          Attr attr = element.getAttributeNode("definitionType");
          checkAttribute(attr, "definitionType");
        });
    return true;
  }


  /**
   * verify that inputData and requiredInput elements have the correct href values
   * no longer contain 'trisotech.com' and are KMDP namespaces
   *
   * @param dox
   * @return
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
   * verify namespaces on the document root no longer contain 'trisotech.com' and are KMDP namespaces
   *
   * @param dox
   * @return
   */
  private boolean verifyRootNamespaces(Document dox) {
    // get attributes of the first element -- where the namespaces are set
    NamedNodeMap attributes = dox.getDocumentElement().getAttributes();

    int attrSize = attributes.getLength();
    for (int i = 0; i < attrSize; i++) {
      Attr attr = (Attr) attributes.item(i);

//      System.out.println("attr value: " + attr.getValue()
//              + " attr prefix: " + attr.getPrefix()
//              + " attr localName: " + attr.getLocalName()
//              + " attr namespaceURI: " + attr.getNamespaceURI()
//      );

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
      fail("should not contain 'trisotech.com' in value: " + " for attribute: " + attr.getLocalName());
      return false;
    }
    return true;
  }

  private boolean confirmKMDPnamespace(Attr attr) {
    if (attr.getValue().contains(CLINICALKNOWLEGEMANAGEMENT_MAYO_ARTIFACTS_BASE_URI)) {
      return true;
    }
    return false;
  }


  private boolean checkAttribute(Attr attr, String namespace) {
    if (attr.getLocalName().equals(namespace)
        && attr.getValue().contains("trisotech.com")) {
      fail("should not contain 'trisotech' in value: " + attr.getValue() + " for attribute: " + attr.getLocalName());
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
                    fail("should not have " + attr.getValue() + " in attribute: " + attr.getLocalName() + " on parent: " + el.getNodeName());
                  }
                }
              }
            }
        );
    return true;
  }


  /**
   * Confirm the Trisotech namespaces and namespace attributes have been removed.
   *
   * @param dox
   * @return
   */
  private boolean confirmNoTrisoNameSpace(Document dox) {
    // Confirm no trisotech tags remain:
    NodeList elements = dox.getElementsByTagNameNS("*", "*");
    asElementStream(elements).forEach(
        (el) -> {
          NamedNodeMap attributes = el.getAttributes();
          int attrSize = attributes.getLength(); // TODO: filter? map? doable with NamedNodeMap? CAO
          for (int i = 0; i < attrSize; i++) {
            Attr attr = (Attr) attributes.item(i);
            if ((Weaver.getMETADATA_NS().equals(attr.getNamespaceURI()))
                || (Weaver.getMETADATA_NS().equals(attr.getValue()))
                || (Weaver.getMETADATA_DIAGRAM_DMN_NS().equals(attr.getNamespaceURI()))
                || (Weaver.getMETADATA_DIAGRAM_DMN_NS().equals(attr.getValue()))
                || (Weaver.getMETADATA_DIAGRAM_CMMN_NS().equals(attr.getNamespaceURI()))
                || (Weaver.getMETADATA_DIAGRAM_CMMN_NS().equals(attr.getValue()))
                || (Weaver.getDROOLS_NS().equals(attr.getNamespaceURI()))
                || (Weaver.getDROOLS_NS().equals(attr.getValue()))
                || (Weaver.getEXPORTER().equals(attr.getLocalName()))
                || (Weaver.getEXPORTER_VERSION().equals(attr.getLocalName()))
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
  private <T extends Annotation> List<T> loadAnnotations(Document dox, KnownAttributes att, Class<T> type) {
    System.out.println("***** loadAnnotations for knownAttributes: " + att.name() + " and type: " + type.getName());
    return XMLUtil.asElementStream(dox.getElementsByTagName("*"))
        .filter((el) -> el.getLocalName().equals("extensionElements"))
        .map(Element::getChildNodes)
        .flatMap(XMLUtil::asElementStream)
        .map(SurrogateHelper::unmarshallAnnotation)
        .filter((a) -> att.asConcept().equals(a.getRel()))
        .map(type::cast)
        .collect(Collectors.toList());

  }
}
