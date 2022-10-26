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
package edu.mayo.kmdp.kdcaci.knew.trisotech.components.redactors;

import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.DMN_12_XMLNS;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.DMN_EL_DECISION_SERVICE;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.DMN_IMPORT;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.DMN_IMPORTTYPE;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.DROOLS_NS;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.TRISOTECH_COM;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.TT_ATTACHMENT_ITEM;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.TT_CMMN_11_NS;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.TT_COMMENTS;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.TT_COPYOFLINK;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.TT_CUSTOM_ATTRIBUTE_ATTR;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.TT_DMN_12_NS;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.TT_DYNAMIC_DECISION_SERVICE;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.TT_LIBRARIES;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.TT_METADATA_NS;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.TT_META_EXPORTER;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.TT_META_EXPORTER_VERSION;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.TT_RELATIONSHIP;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.TT_REUSELINK;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.TT_SEMANTICLINK;
import static edu.mayo.kmdp.util.XMLUtil.asElementStream;

import edu.mayo.kmdp.util.XMLUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

/**
 * Weaver class is used for replacing/removing Trisotech-specific information in model files with
 * application needs.
 */
@Component
public class Redactor {

  public static final Logger logger = LoggerFactory.getLogger(Redactor.class);

  static final String CMMN_DEFINITION_TYPE_UNSPECIFIED =
      "http://www.omg.org/spec/CMMN/DefinitionType/Unspecified";
  static final String CMMN_DEFINITION_TYPE_XSD =
      "http://www.omg.org/spec/CMMN/DefinitionType/XSDElement";

  /**
   * Weave out Trisotech-specific elements and where necessary, replace with KMDP-specific.
   */
  public Document redact(Document dox) {

    /****** Remove traces of Trisotech  ******/
    // remove spurious model elements
    removeTrisoElementsNotRetaining(dox);

    // verify and remove any invalid caseFileItemDefinition items
    verifyAndRemoveInvalidCaseFileItemDefinition(dox);

    removeUnsupportedImports(dox);

    // remove the namespace attributes
    removeProprietaryAttributesAndNS(dox);

    // remove additional Trisotech tags we don't need
    removeTrisoTagsNotRetaining(dox);

    return dox;
  }

  /**
   * Model imports are lifted into Asset-Asset dependencies
   * <p>
   * However, Models may import resources that are not treated as Assets
   * <p>
   * For the time being, we remove the import, but this logic may be refined in FUTURE
   *
   * @param dox
   */
  private void removeUnsupportedImports(Document dox) {
    XMLUtil.asElementStream(dox.getElementsByTagNameNS(DMN_12_XMLNS, DMN_IMPORT))
        .filter(el -> TT_LIBRARIES.equals(el.getAttribute(DMN_IMPORTTYPE)))
        .forEach(element ->
            element.getParentNode().removeChild(element)
        );
  }

  private void removeTrisoElementsNotRetaining(Document dox) {
    XMLUtil.asElementStream(dox.getElementsByTagNameNS("*", DMN_EL_DECISION_SERVICE))
        .filter(el -> el.hasAttributeNS(TT_METADATA_NS, TT_DYNAMIC_DECISION_SERVICE))
        .forEach(element ->
            element.getParentNode().removeChild(element)
        );
  }


  private void verifyAndRemoveInvalidCaseFileItemDefinition(Document dox) {
    XMLUtil.asElementStream(dox.getElementsByTagName("*"))
        .filter(el -> (el.getLocalName().equals("caseFileItemDefinition")))
        .forEach(element -> {
          Attr attr = element.getAttributeNode("definitionType");
          if (attr == null) {
            element.setAttribute("definitionType", CMMN_DEFINITION_TYPE_UNSPECIFIED);
          } else if (attr.getValue() == null) {
            attr.setValue(CMMN_DEFINITION_TYPE_UNSPECIFIED);
          } else if (attr.getValue().contains(TRISOTECH_COM)) {
            if (attr.getValue().contains("ItemDefinitionType")) {
              logger.info(
                  "Rewriting CMMN* CFI ItemDefinition as an XSD Element - waiting for (S)DMN/CMMN integration");
              attr.setValue(CMMN_DEFINITION_TYPE_XSD);
            } else {
              logger.warn(
                  String.format(
                      "WARNING: Should not have %s in caseFileItemDefinition. Rewriting to default value of Unspecified. Found for %s",
                      TRISOTECH_COM, element.getAttributeNode("name").getValue()));
              attr.setValue(CMMN_DEFINITION_TYPE_UNSPECIFIED);
            }
          } // else leave as is - should be a valid CMMN definition type
        });
  }

  /**
   * This method is to remove any triso: tags that are not being kept in the woven file, and
   * therefore in the surrogate.
   *
   * 'attachment' elements are in the model for CKE and SME use and SHOULD NOT carry over to the
   * woven document.
   *
   * 'interrelationship' elements are not needed in the output as they deal with model->model
   * relationships and we can get that another way.
   *
   * 'reuseLink' elements are not needed in the output
   *
   * 'copyOfLink' elements are not needed in the output
   *
   * 'itemDefinitions' were an experiment. IGNORE if they are in the file, but provide a warning.
   */
  private void removeTrisoTagsNotRetaining(Document dox) {
    XMLUtil.asElementStream(dox.getElementsByTagNameNS(TT_METADATA_NS, TT_ATTACHMENT_ITEM))
        .forEach(element ->
            element.getParentNode().removeChild(element)
        );

    XMLUtil.asElementStream(dox.getElementsByTagNameNS(TT_METADATA_NS, TT_RELATIONSHIP))
        .forEach(element ->
            element.getParentNode().removeChild(element)
        );

    XMLUtil.asElementStream(dox.getElementsByTagNameNS(TT_METADATA_NS, TT_REUSELINK))
        .forEach(element ->
            element.getParentNode().removeChild(element)
        );

    XMLUtil.asElementStream(dox.getElementsByTagNameNS(TT_METADATA_NS, TT_COPYOFLINK))
        .forEach(element ->
            element.getParentNode().removeChild(element)
        );

    XMLUtil.asElementStream(dox.getElementsByTagNameNS(TT_METADATA_NS, TT_CUSTOM_ATTRIBUTE_ATTR))
        .forEach(element ->
            element.getParentNode().removeChild(element)
        );

    XMLUtil.asElementStream(dox.getElementsByTagNameNS(TT_METADATA_NS, TT_COMMENTS))
        .forEach(element ->
            element.getParentNode().removeChild(element)
        );

    // if any meta items were invalid, want to strip them from the file
    // all valid items should have been converted
    XMLUtil.asElementStream(dox.getElementsByTagNameNS(TT_METADATA_NS, TT_SEMANTICLINK))
        .forEach(element -> {
          logger.warn(String.format("WARNING: Removing element as it was not found. %s",
              element.getAttribute("uri")));
          element.getParentNode().removeChild(element);
        });

    // cleanup some additional tags not used anywhere
    XMLUtil.asElementStream(dox.getElementsByTagNameNS(TT_METADATA_NS, "tags"))
        .forEach(element ->
            element.getParentNode().removeChild(element)
        );

  }

  /**
   * remove the proprietary attributes and the associated namespace
   */
  private void removeProprietaryAttributesAndNS(Document dox) {
    NodeList elements = dox.getElementsByTagNameNS("*", "*");
    asElementStream(elements).forEach(
        el -> {
          if (TT_METADATA_NS.equals(el.getNamespaceURI())) {
            el.getParentNode().removeChild(el);
          }
          NamedNodeMap attributes = el.getAttributes();
          int attrSize = attributes.getLength() - 1;
          for (int i = attrSize; i >= 0; i--) {
            Attr attr = (Attr) attributes.item(i);
            if ((attr != null) &&
                // remove any of the Trisotech namespace attributes, and Drools
                (TT_METADATA_NS.equals(attr.getNamespaceURI())
                    || TT_METADATA_NS.equals(attr.getValue())
                    || TT_DMN_12_NS.equals(attr.getNamespaceURI())
                    || TT_DMN_12_NS.equals(attr.getValue())
                    || TT_CMMN_11_NS.equals(attr.getNamespaceURI())
                    || TT_CMMN_11_NS.equals(attr.getValue())
                    || DROOLS_NS.equals(attr.getNamespaceURI())
                    || DROOLS_NS.equals(attr.getValue())
                    || TT_META_EXPORTER.equals(attr.getLocalName())
                    || TT_META_EXPORTER_VERSION.equals(attr.getLocalName())
                    || attr.getValue().contains(TRISOTECH_COM)
                )
            ) {
              el.removeAttributeNode(attr);
            }
          }
        }
    );
  }

}
