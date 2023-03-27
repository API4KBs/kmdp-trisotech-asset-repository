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
package edu.mayo.kmdp.trisotechwrapper.components.redactors;

import static edu.mayo.kmdp.util.XMLUtil.asElementStream;

import edu.mayo.kmdp.trisotechwrapper.config.TTConstants;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

/**
 * Default implementation of the {@link Redactor} interface, used to remove/rewrite
 * Trisotech-specific elements from a BPM+ Model exported from the TT server as a standards based
 * XML Document
 * <p>
 * Note: among others, this implementation redacts the DMN decision services auto-generated by TT
 * around Decision Models and Diagrams
 * <p>
 * TODO: this implementation supports DMN 1.2 and CMMN 1.1, and should be refactored/modularized
 * to support other BPM+ languages/versions
 */
@Component
public class TTRedactor implements Redactor {

  /**
   * Logger
   */
  public static final Logger logger = LoggerFactory.getLogger(TTRedactor.class);

  static final String CMMN_DEFINITION_TYPE_UNSPECIFIED =
      "http://www.omg.org/spec/CMMN/DefinitionType/Unspecified";
  static final String CMMN_DEFINITION_TYPE_XSD =
      "http://www.omg.org/spec/CMMN/DefinitionType/XSDElement";

  /**
   * {@inheritDoc}
   * <p>
   * Note: This implementation applies the redaction rules 'in place', to the input Document
   *
   * @param dox the Document to be redacted
   * @return the redacted Document
   */
  @Override
  public Document redact(@Nonnull final Document dox) {

    // ---- Remove traces of Trisotech  ---- //
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
   * Model imports are lifted into Asset-Asset dependencies. However, Models may import resources
   * that are not treated as Assets For the time being, we remove the import, but this logic may be
   * refined in FUTURE.
   * <p>
   * The current implementation removes imports to DMN/FEEL libraries, which are not yet accessible
   * via the TTW
   *
   * @param dox the Document to b processed
   */
  private void removeUnsupportedImports(@Nonnull final Document dox) {
    asElementStream(dox.getElementsByTagNameNS(
        TTConstants.DMN_12_XMLNS, TTConstants.DMN_IMPORT))
        .filter(el -> TTConstants.TT_LIBRARIES.equals(el.getAttribute(TTConstants.DMN_IMPORTTYPE)))
        .forEach(element ->
            element.getParentNode().removeChild(element)
        );
  }

  /**
   * Removes irrelevant elements
   * <p>
   * Trisotech creates 'implied' Decision Services for each DMN model, and each diagram within the
   * model. These services are redacted.
   *
   * @param dox the Document to b processed
   */
  private void removeTrisoElementsNotRetaining(@Nonnull final Document dox) {
    asElementStream(dox.getElementsByTagNameNS("*", TTConstants.DMN_EL_DECISION_SERVICE))
        .filter(el -> el.hasAttributeNS(
            TTConstants.TT_METADATA_NS, TTConstants.TT_DYNAMIC_DECISION_SERVICE))
        .forEach(element ->
            element.getParentNode().removeChild(element)
        );
  }

  /**
   * Legacy: the use of cmmn:CFI as 'datatypes' is ambiguous and evolving. CKE used the non standard
   * 'XSD' mode, but then TT enabled the use of DMN itemDefinitions on CFIs. This will likely
   * converge into the SDM(N) initiative
   *
   * @param dox the Document to b processed
   */
  private void verifyAndRemoveInvalidCaseFileItemDefinition(@Nonnull final Document dox) {
    asElementStream(dox.getElementsByTagName("*"))
        .filter(el -> (el.getLocalName().equals("caseFileItemDefinition")))
        .forEach(element -> {
          Attr attr = element.getAttributeNode("definitionType");
          if (attr == null) {
            element.setAttribute("definitionType", CMMN_DEFINITION_TYPE_UNSPECIFIED);
          } else if (attr.getValue() == null) {
            attr.setValue(CMMN_DEFINITION_TYPE_UNSPECIFIED);
          } else if (attr.getValue().contains(TTConstants.TRISOTECH_COM)) {
            if (attr.getValue().contains("ItemDefinitionType")) {
              logger.info(
                  "Rewriting CMMN* CFI ItemDefinition as an XSD Element - waiting for (S)DMN/CMMN integration");
              attr.setValue(CMMN_DEFINITION_TYPE_XSD);
            } else {
              logger.warn(
                  String.format(
                      "WARNING: Should not have %s in caseFileItemDefinition. Rewriting to default value of Unspecified. Found for %s",
                      TTConstants.TRISOTECH_COM, element.getAttributeNode("name").getValue()));
              attr.setValue(CMMN_DEFINITION_TYPE_UNSPECIFIED);
            }
          } // else leave as is - should be a valid CMMN definition type
        });
  }

  /**
   * This method is to remove any triso: tags that have been used, mapped, rewritten or used to
   * generate a model's Surrogate, and thus are no longer necessary.
   * <p>
   * 'attachment' elements are in the model for design-time reference and do not carry over to the
   * woven document.
   * <p>
   * 'interrelationship' and reuse/copy link elements are not needed in the output as they deal with
   * model->model relationships, which is handled at the metadata level.
   *
   * @param dox the Document to be redacted
   */
  private void removeTrisoTagsNotRetaining(@Nonnull final Document dox) {
    asElementStream(dox.getElementsByTagNameNS(
        TTConstants.TT_METADATA_NS, TTConstants.TT_ATTACHMENT_ITEM))
        .forEach(element ->
            element.getParentNode().removeChild(element)
        );

    asElementStream(dox.getElementsByTagNameNS(
        TTConstants.TT_METADATA_NS, TTConstants.TT_RELATIONSHIP))
        .forEach(element ->
            element.getParentNode().removeChild(element)
        );

    asElementStream(dox.getElementsByTagNameNS(
        TTConstants.TT_METADATA_NS, TTConstants.TT_REUSELINK))
        .forEach(element ->
            element.getParentNode().removeChild(element)
        );

    asElementStream(dox.getElementsByTagNameNS(
        TTConstants.TT_METADATA_NS, TTConstants.TT_COPYOFLINK))
        .forEach(element ->
            element.getParentNode().removeChild(element)
        );

    asElementStream(dox.getElementsByTagNameNS(
        TTConstants.TT_METADATA_NS, TTConstants.TT_CUSTOM_ATTRIBUTE_ATTR))
        .forEach(element ->
            element.getParentNode().removeChild(element)
        );

    asElementStream(dox.getElementsByTagNameNS(
        TTConstants.TT_METADATA_NS, TTConstants.TT_COMMENTS))
        .forEach(element ->
            element.getParentNode().removeChild(element)
        );

    // if any meta items were invalid, want to strip them from the file
    // all valid items should have been converted
    asElementStream(dox.getElementsByTagNameNS(
        TTConstants.TT_METADATA_NS, TTConstants.TT_SEMANTICLINK))
        .forEach(element -> {
          logger.warn(String.format("WARNING: Removing element as it was not found. %s",
              element.getAttribute("uri")));
          element.getParentNode().removeChild(element);
        });

    // cleanup some additional tags not used anywhere
    asElementStream(dox.getElementsByTagNameNS(TTConstants.TT_METADATA_NS, "tags"))
        .forEach(element ->
            element.getParentNode().removeChild(element)
        );

  }

  /**
   * remove the proprietary attributes and the associated namespace
   *
   * @param dox the Document to be redacted =
   */
  private void removeProprietaryAttributesAndNS(@Nonnull final Document dox) {
    NodeList elements = dox.getElementsByTagNameNS("*", "*");
    asElementStream(elements).forEach(
        el -> {
          if (TTConstants.TT_METADATA_NS.equals(el.getNamespaceURI())) {
            el.getParentNode().removeChild(el);
          }
          NamedNodeMap attributes = el.getAttributes();
          int attrSize = attributes.getLength() - 1;
          for (int i = attrSize; i >= 0; i--) {
            Attr attr = (Attr) attributes.item(i);
            if ((attr != null) &&
                // remove any of the Trisotech namespace attributes, and Drools
                (TTConstants.TT_METADATA_NS.equals(attr.getNamespaceURI())
                    || TTConstants.TT_METADATA_NS.equals(attr.getValue())
                    || TTConstants.TT_DMN_12_NS.equals(attr.getNamespaceURI())
                    || TTConstants.TT_DMN_12_NS.equals(attr.getValue())
                    || TTConstants.TT_CMMN_11_NS.equals(attr.getNamespaceURI())
                    || TTConstants.TT_CMMN_11_NS.equals(attr.getValue())
                    || TTConstants.DROOLS_NS.equals(attr.getNamespaceURI())
                    || TTConstants.DROOLS_NS.equals(attr.getValue())
                    || TTConstants.TT_META_EXPORTER.equals(attr.getLocalName())
                    || TTConstants.TT_META_EXPORTER_VERSION.equals(attr.getLocalName())
                    || attr.getValue().contains(TTConstants.TRISOTECH_COM)
                )
            ) {
              el.removeAttributeNode(attr);
            }
          }
        }
    );
  }

}
