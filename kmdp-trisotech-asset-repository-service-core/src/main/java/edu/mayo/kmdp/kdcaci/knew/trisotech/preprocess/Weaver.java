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
package edu.mayo.kmdp.kdcaci.knew.trisotech.preprocess;

import static edu.mayo.kmdp.util.XMLUtil.asElementStream;
import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.*;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.CMMN_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.DMN_1_2;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;
import static org.omg.spec.api4kp._20200801.taxonomy.krserialization.KnowledgeRepresentationLanguageSerializationSeries.Knowledge_Asset_Surrogate_2_0_XML_Syntax;

import edu.mayo.kmdp.registry.Registry;
import edu.mayo.kmdp.util.JaxbUtil;
import edu.mayo.kmdp.util.Util;
import edu.mayo.kmdp.util.XMLUtil;
import edu.mayo.ontology.taxonomies.clinicaltasks.ClinicalTaskSeries;
import edu.mayo.ontology.taxonomies.kao.decisiontype.DecisionTypeSeries;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBElement;

import edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.terminology.v4.server.TermsApiInternal;
import org.omg.spec.api4kp._20200801.id.ConceptIdentifier;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.surrogate.Annotation;
import org.omg.spec.api4kp._20200801.surrogate.ObjectFactory;
import org.omg.spec.api4kp._20200801.terms.model.ConceptDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

/**
 * Weaver class is used for replacing/removing Trisotech-specific information in model files with
 * application needs.
 */
@Component
public class Weaver {

  private static final String TRISOTECH_COM = "trisotech.com";
  private static final String VALUE = "value";
  private static final Logger logger = LoggerFactory.getLogger(Weaver.class);

  private static final String WWW_W_3_ORG_2000_XMLNS = "http://www.w3.org/2000/xmlns/";
  private static final String MODEL_URI = "modelURI";
  private static final String DMN = "DMN";
  private static final String CMMN = "CMMN";

  @Autowired
  private ModelReader reader;

  @Autowired
  private ReaderConfig config;

  @Autowired
  private TermsApiInternal terms;

  private String decisionEl;
  private String elExporter;
  private String elExporterVersion;
  private String metadataRS;
  private String metadataNS;
  private String metadataEl;
  private String metadataExt;
  private String metadataId;
  private String idAttribute;
  private String diagramNS;
  private String metadataDiagramDmnNS;
  private String metadataDiagramCmmnNS;
  private String droolsNS;
  private String diagramExt;
  private String metadataItemDef;
  private String metadataAttachment;

  private ObjectFactory of = new ObjectFactory();
  private Map<String, BaseAnnotationHandler> handlers = new HashMap<>();

  @PostConstruct
  public void init() {
    logger.debug("Weaver ctor, config is: {}", config);
    logger.debug("Weaver ctor, reader is: {}", reader);

    metadataNS = config.getTyped(ReaderOptions.P_METADATA_NS);
    metadataDiagramDmnNS = config.getTyped(ReaderOptions.P_METADATA_DIAGRAM_DMN_NS);
    metadataDiagramCmmnNS = config.getTyped(ReaderOptions.P_METADATA_DIAGRAM_CMMN_NS);
    droolsNS = config.getTyped(ReaderOptions.P_DROOLS_NS);
    metadataEl = config.getTyped(ReaderOptions.P_EL_ANNOTATION);
    // 04/03/2020: do not need interrelationship anymore; remove with other triso: tags
    metadataRS = config.getTyped(ReaderOptions.P_EL_RELATIONSHIP);
    metadataExt = config.getTyped(ReaderOptions.P_EL_MODEL_EXT);
    // 04/03/2020: not keeping assetId in XML, remove with other triso: tags
    metadataId = config.getTyped(ReaderOptions.P_EL_ANNOTATION_ID);
    idAttribute = config.getTyped(ReaderOptions.P_EL_ID_ATT);
    diagramNS = config.getTyped(ReaderOptions.P_DIAGRAM_NS);
    diagramExt = config.getTyped(ReaderOptions.P_EL_DIAGRAM_EXT);
    elExporter = config.getTyped(ReaderOptions.P_EL_EXPORTER);
    elExporterVersion = config.getTyped(ReaderOptions.P_EL_EXPORTER_VERSION);
    decisionEl = config.getTyped(ReaderOptions.P_EL_DECISION);
    metadataItemDef = config.getTyped(ReaderOptions.P_METADATA_ITEM_DEFINITION);
    metadataAttachment = config.getTyped(ReaderOptions.P_METADATA_ATTACHMENT_ITEM);

    logger.debug("METADATA_EL: {}", metadataEl);
    handlers.put(metadataEl, new MetadataAnnotationHandler());
  }

  public String getMetadataNS() {
    return metadataNS;
  }

  public String getMetadataEl() {
    return metadataEl;
  }

  public String getMetadataRS() {
    return metadataRS;
  }

  public String getMetadataDiagramDmnNS() {
    return metadataDiagramDmnNS;
  }

  public String getMetadataDiagramCmmnNS() {
    return metadataDiagramCmmnNS;
  }

  public String getDroolsNS() {
    return droolsNS;
  }

  public String getElExporter() {
    return elExporter;
  }

  public String getElExporterVersion() {
    return elExporterVersion;
  }

  public String getDecisionEl() {
    return decisionEl;
  }

  public String getMetadataItemDef() {
    return metadataItemDef;
  }

  public String getMetadataAttachment() {
    return metadataAttachment;
  }


  /**
   * Weave out Trisotech-specific elements and where necessary, replace with KMDP-specific.
   */
  public Document weave(Document dox) {
    dox.getDocumentElement().setAttributeNS(WWW_W_3_ORG_2000_XMLNS,
        "xmlns:" + "xsi",
        "http://www.w3.org/2001/XMLSchema-instance");

    String surrPrefix = Registry
        .getPrefixforNamespace(Knowledge_Asset_Surrogate_2_0_XML_Syntax.getReferentId())
        .orElseThrow(IllegalStateException::new);
    String surrNamespace = Registry
        .getValidationSchema(Knowledge_Asset_Surrogate_2_0.getReferentId())
        .orElseThrow(IllegalStateException::new);

    dox.getDocumentElement().setAttributeNS(WWW_W_3_ORG_2000_XMLNS,
        "xmlns:" + surrPrefix,
        surrNamespace);

    dox.getDocumentElement().setAttributeNS("http://www.w3.org/2001/XMLSchema-instance",
        "xsi:" + "schemaLocation",
        getSchemaLocations(dox, surrNamespace));

    // get metas
    NodeList metas = dox.getElementsByTagNameNS(metadataNS, metadataEl);
    weaveMetadata(metas);

    /****** Remove traces of Trisotech  ******/
    // remove spurious model elements
    removeTrisoElementsNotRetaining(dox);

    // remove the namespace attributes
    removeProprietaryAttributesAndNS(dox);

    // remove additional Trisotech tags we don't need
    removeTrisoTagsNotRetaining(dox);

    // rewrite namespaces
    weaveNamespaces(dox);

    // rewrite namespace for 'import' tags
    weaveImport(dox);

    // rewrite href for 'inputData' and 'requiredInput' tags
    weaveInputs(dox);

    // verify and remove any invalid caseFileItemDefinition items
    verifyAndRemoveInvalidCaseFileItemDefinition(dox);

    return dox;
  }

  private void removeTrisoElementsNotRetaining(Document dox) {
    XMLUtil.asElementStream(dox.getElementsByTagNameNS("*", "decisionService"))
        .filter(el -> el.hasAttributeNS(metadataNS, "dynamicDecisionService"))
        .forEach(element ->
            element.getParentNode().removeChild(element)
        );
  }

  /**
   * Determine the SemanticAnnotatoin based on the uri.
   *
   * @param el the document element under examination
   * @return the SemanticAnnotationRelTypeSeries to be used in rewriting this element
   */
  private SemanticAnnotationRelTypeSeries getSemanticAnnotation(Element el) {
    String uri = el.getAttribute("uri");

    if (DecisionTypeSeries.resolveId(uri).isPresent()) {
      return Captures;
    } else if (ClinicalTaskSeries.resolveId(uri).isPresent()) {
      return Captures;
    } else if (isDomainConcept(uri)) {
      String grandparent = el.getParentNode().getParentNode().getNodeName();
      if (grandparent.equals("semantic:decision")) {
        return Defines;
      } else if (grandparent.equals("semantic:inputData")) {
        return In_Terms_Of;
      } else {
        return null;
      }
    }

    return null;
  }

  private boolean isDomainConcept(String uriStr) {
    // the Terms service needs a UUID...
    URI uri = URI.create(uriStr);
    Answer<ConceptDescriptor> cdAns
        = Answer.of(Optional.ofNullable(uri.getFragment()))
        .flatOpt(Util::ensureUUID)
        .flatMap(id -> terms.lookupTerm(id.toString())) ;
    if (!cdAns.isSuccess()) {
      return false;
    }
    String ns = cdAns.map(ConceptIdentifier::getNamespaceUri)
        .map(URI::toString)
        .orElse("");
    return (ns.toLowerCase().contains("clinicalsituations") ||
        ns.toLowerCase().contains("propositionalconcepts"));
  }

  private void verifyAndRemoveInvalidCaseFileItemDefinition(Document dox) {
    XMLUtil.asElementStream(dox.getElementsByTagName("*"))
        .filter(el -> (el.getLocalName().equals("caseFileItemDefinition")))
        .forEach(element -> {
          Attr attr = element.getAttributeNode("definitionType");
          // TODO: is it an error if there isn't a definitionType for caseFileItemDefinition? CAO
          if ((null != attr) && (attr.getValue().contains(TRISOTECH_COM))) {
            logger.warn(
                String.format(
                    "WARNING: Should not have %s in caseFileItemDefinition. Rewriting to default value of Unspecified. Found for %s",
                    TRISOTECH_COM, element.getAttributeNode("name").getValue()));
            // TODO: a way to do this using the XSD? CAO
            attr.setValue("http://www.omg.org/spec/CMMN/DefinitionType/Unspecified");
          }
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
   * 'itemDefinitions' were an experiment. IGNORE if they are in the file, but provide a warning.
   */
  private void removeTrisoTagsNotRetaining(Document dox) {
    XMLUtil.asElementStream(dox.getElementsByTagNameNS(metadataNS, metadataAttachment))
        .forEach(element ->
          element.getParentNode().removeChild(element)
        );

    XMLUtil.asElementStream(dox.getElementsByTagNameNS(metadataNS, metadataRS))
        .forEach(element ->
          element.getParentNode().removeChild(element)
        );

    XMLUtil.asElementStream(dox.getElementsByTagNameNS(metadataNS, metadataId))
        .forEach(element ->
          element.getParentNode().removeChild(element)
        );

    XMLUtil.asElementStream(dox.getElementsByTagNameNS(metadataNS, metadataItemDef))
        .forEach(element -> {
          logger.warn(
              String.format(
                  "WARNING: Should not have %s in model. Removing from output file",
                  element.getLocalName()));
          element.getParentNode().removeChild(element);
        });

    // if any meta items were invalid, want to strip them from the file
    // all valid items should have been converted
    XMLUtil.asElementStream(dox.getElementsByTagNameNS(metadataNS, metadataEl))
        .forEach(element -> {
          logger.warn(String.format("WARNING: Removing element as it was not found. %s",
              element.getAttribute("uri")));
          element.getParentNode().removeChild(element);
        });
  }

  /**
   * weaveInputs will rewrite the href attribute of the tags given to be KMDP hrefs instead of
   * Trisotech
   *
   * @param dox the XML document being rewritten
   */
  private void weaveInputs(Document dox) {
    XMLUtil.asElementStream(dox.getElementsByTagName("*"))
        // TODO: code review -- need to know which tags, or just check all hrefs? CAO
        .filter(el -> (el.getLocalName().equals("inputData")
            || el.getLocalName().equals("requiredInput")
            || el.getLocalName().equals("requiredKnowledge")
            || el.getLocalName().equals("encapsulatedDecision")
            || el.getLocalName().equals("inputDecision")
            || el.getLocalName().equals("requiredDecision"))
            && el.hasAttribute("href"))
        .forEach(element -> {
          Attr attr = element.getAttributeNode("href");
          rewriteValue(attr);
        });
  }

  private void weaveImport(Document dox) {
    asElementStream(dox.getElementsByTagName("*"))
        .filter(el -> el.getLocalName().equals("import"))
        .forEach(el -> {
              Attr attr = el.getAttributeNode("namespace");
              rewriteValue(attr);
            }
        );
  }

  private void weaveNamespaces(Document dox) {
    NamedNodeMap attributes = dox.getDocumentElement().getAttributes();

    int attrSize = attributes.getLength();
    for (int i = 0; i < attrSize; i++) {
      Attr attr = (Attr) attributes.item(i);
      String localName = attr.getLocalName();
      if (localName.equals("xmlns")
          || (localName.equals("namespace"))
          || (localName.equals("targetNamespace"))
          || (localName.contains("include"))
          || (localName.contains("ns"))
      ) {
        rewriteValue(attr);
      }

    }
    // add attribute for assets namespace
    Attr assetsAttr = dox.createAttributeNS(WWW_W_3_ORG_2000_XMLNS, "xmlns:assets");
    assetsAttr.setValue("https://clinicalknowledgemanagement.mayo.edu/assets");
    dox.getDocumentElement().setAttributeNodeNS(assetsAttr);

  }

  /**
   * remove the proprietary attributes and the associated namespace
   */
  private void removeProprietaryAttributesAndNS(Document dox) {
    NodeList elements = dox.getElementsByTagNameNS("*", "*");
    asElementStream(elements).forEach(
        el -> {
          NamedNodeMap attributes = el.getAttributes();
          int attrSize = attributes.getLength() - 1;
          for (int i = attrSize; i >= 0; i--) {
            Attr attr = (Attr) attributes.item(i);
            if ((attr != null) &&
                // remove any of the Trisotech namespace attributes, and drools
                (metadataNS.equals(attr.getNamespaceURI())
                    || metadataNS.equals(attr.getValue())
                    || metadataDiagramDmnNS.equals(attr.getNamespaceURI())
                    || metadataDiagramDmnNS.equals(attr.getValue())
                    || metadataDiagramCmmnNS.equals(attr.getNamespaceURI())
                    || metadataDiagramCmmnNS.equals(attr.getValue())
                    || droolsNS.equals(attr.getNamespaceURI())
                    || droolsNS.equals(attr.getValue())
                    || elExporter.equals(attr.getLocalName())
                    || elExporterVersion.equals(attr.getLocalName())
                )
            ) {
              el.removeAttributeNode(attr);
            }
          }
        }
    );
  }

  /**
   * Used to rewrite the value of an attribute expected to be a URI from Trisotech URI to KMDP URI.
   * Also remove leading underscore of identifier.
   *
   * @param attr the attribute of a Document tag
   */
  private void rewriteValue(Attr attr) {
    String value = attr.getValue();
    if (value.lastIndexOf('/') != -1) {
      // get the ids after the last '/'
      // and replace the '_' in the ids
      String id = value.substring(value.lastIndexOf('/') + 1).replace("_", "");
      // reset the value to the KMDP URI
      attr.setValue(Registry.MAYO_ARTIFACTS_BASE_URI + id);
    }
  }

  private void weaveMetadata(NodeList metas) {
    asElementStream(metas)
        .forEach(
            el -> doInjectTerm(el,
                getSemanticAnnotation(el),
                getConceptIdentifiers(el))
        );
  }

  private List<ConceptIdentifier> getConceptIdentifiers(Element el) {
    // need to verify any URI values are valid -- no trisotech
    Attr modelUriAttr = el.getAttributeNode(MODEL_URI);
    Attr uriAttr = el.getAttributeNode("uri");
    if (modelUriAttr.getValue().contains(TRISOTECH_COM)) {
      rewriteValue(modelUriAttr);
    }
    if (uriAttr.getValue().contains(TRISOTECH_COM)) {
      rewriteValue(uriAttr);
    }

    // TODO: would there ever be more than one? CAO maybe
    List<ConceptIdentifier> conceptIdentifiers = new ArrayList<>();
    ConceptIdentifier concept = null;
    try {
      ResourceIdentifier resourceIdentifier = SemanticIdentifier
          .newId(new URI(el.getAttribute("uri")));

      Answer<ConceptDescriptor> term  = terms.lookupTerm(resourceIdentifier.getUuid().toString());
      if(term.getOptionalValue().isPresent()) {
        concept = term.get()
            .asConceptIdentifier();
      } else {
        logger.warn("WARNING: resource ID {} failed in lookupTerm and will be removed from the file", resourceIdentifier.getUuid().toString());
        return conceptIdentifiers;
      }
    } catch (URISyntaxException | IllegalArgumentException e) {
      logger.error(String.format("%s%s", e.getMessage(), Arrays.toString(e.getStackTrace())));
    }
    conceptIdentifiers.add(concept);

    return conceptIdentifiers;
  }


  private BaseAnnotationHandler handler(Element el) {
    if (logger.isDebugEnabled()) {
      logger.debug("el localname:  {}", el.getLocalName());
    }
    if (!handlers.containsKey(el.getLocalName())) {
      throw new UnsupportedOperationException(
          "Unable to find handler for annotation " + el.getLocalName());
    }
    return handlers.get(el.getLocalName());
  }


  private void doInjectTerm(Element el,
      SemanticAnnotationRelTypeSeries defaultRel,
      List<ConceptIdentifier> rows) {
    BaseAnnotationHandler handler = handler(el);

    if (!rows.isEmpty() && !rows.stream().noneMatch(Objects::nonNull)) {
      List<Annotation> annos = handler.getAnnotation(el.getLocalName(), defaultRel, rows);
      handler.replaceProprietaryElement(el,
          handler.wrap(toChildElements(annos, el)));
    }
  }

  private boolean isIdentifier(Element el) {
    return (el.getAttribute(idAttribute) != null);
  }

  private List<Element> toChildElements(List<Annotation> annos, Element parent) {
    return annos.stream()
        .map(ann -> toChildElement(ann, parent))
        .collect(Collectors.toList());
  }

  private Element toChildElement(Annotation ann, Element parent) {
    Element el;
    if (ann instanceof Annotation) {
      el = toElement(of,
          (Annotation) ann,
          of::createAnnotation
      );
    } else {
      throw new IllegalStateException("Unmanaged annotation type" + ann.getClass().getName());
    }
    parent.getOwnerDocument().adoptNode(el);
    parent.appendChild(el);
    return el;
  }


  public static <T> Element toElement(Object ctx,
      T root,
      final Function<T, JAXBElement<? super T>> mapper) {
    Optional<Document> dox = JaxbUtil.marshall(Collections.singleton(ctx.getClass()),
        root,
        mapper,
        JaxbUtil.defaultProperties())
        .map(ByteArrayOutputStream::toByteArray)
        .map(ByteArrayInputStream::new)
        .flatMap(XMLUtil::loadXMLDocument);
    // TODO remove when loadXMLDocument provides normalization
    dox.ifPresent(Document::normalizeDocument);
    return dox
        .map(Document::getDocumentElement)
        .orElseThrow(IllegalStateException::new);
  }


  private String getSchemaLocations(Document dox, String surrNamespace) {
    StringBuilder sb = new StringBuilder();

    logger.debug(
        "KnowledgeRepresentationLanguage.DMN_1_2.getReferentId(): {}", DMN_1_2.getReferentId());
    sb.append(surrNamespace)
        .append(" ").append("xsd/API4KP/surrogate/surrogate.xsd");

    String baseNS = dox.getDocumentElement().getNamespaceURI();
    if (baseNS.contains(DMN)) {
      sb.append(" ")
          .append(DMN_1_2.getReferentId())
          .append(" ").append(Registry.getValidationSchema(DMN_1_2.getReferentId())
          .orElseThrow(IllegalStateException::new));
    } else if (baseNS.contains(CMMN)) {
      sb.append(" ").append(CMMN_1_1)
          .append(" ").append(Registry.getValidationSchema(CMMN_1_1.getReferentId())
          .orElseThrow(IllegalStateException::new));
    }

    return sb.toString();
  }

  /**
   * Get the customAttribute assetID from the document. This is a value that is stripped from a
   * woven file but can be retrieved from an unwoven file.
   *
   * @param dox the model XML document
   * @return ResourceIdenifier of the assetID
   */
  public ResourceIdentifier getAssetID(Document dox) {
    NodeList metas = dox.getElementsByTagNameNS(metadataNS, metadataId);

    List<ResourceIdentifier> ids = asElementStream(metas)
        .filter(this::isIdentifier)
        .map(el -> el.getAttribute(VALUE))
        .map(id -> SemanticIdentifier.newVersionId(URI.create(id)))
        .collect(Collectors.toList());

    return ids.get(0);
  }
}
