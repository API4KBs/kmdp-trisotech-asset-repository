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
package edu.mayo.kmdp.preprocess.meta;

import static edu.mayo.kmdp.util.XMLUtil.asElementStream;
import static edu.mayo.ontology.taxonomies.krformat._20190801.SerializationFormat.XML_1_1;
import static edu.mayo.ontology.taxonomies.krlanguage._20190801.KnowledgeRepresentationLanguage.CMMN_1_1;
import static edu.mayo.ontology.taxonomies.krlanguage._20190801.KnowledgeRepresentationLanguage.DMN_1_2;
import static org.omg.spec.api4kp._1_0.AbstractCarrier.rep;

import edu.mayo.kmdp.metadata.annotations.Annotation;
import edu.mayo.kmdp.metadata.annotations.BasicAnnotation;
import edu.mayo.kmdp.metadata.annotations.DatatypeAnnotation;
import edu.mayo.kmdp.metadata.annotations.MultiwordAnnotation;
import edu.mayo.kmdp.metadata.annotations.ObjectFactory;
import edu.mayo.kmdp.metadata.annotations.SimpleAnnotation;
import edu.mayo.kmdp.registry.Registry;
import edu.mayo.kmdp.util.JaxbUtil;
import edu.mayo.kmdp.util.XMLUtil;
import edu.mayo.kmdp.util.ws.ResponseHelper;
import edu.mayo.ontology.taxonomies.kao.rel.dependencyreltype._20190801.DependencyType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBElement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omg.spec.api4kp._1_0.AbstractCarrier;
import org.omg.spec.api4kp._1_0.identifiers.ConceptIdentifier;
import org.omg.spec.api4kp._1_0.services.DocumentCarrier;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;
import org.springframework.http.ResponseEntity;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

/**
 * Weaver class is used for replacing/removing Trisotech-specific information in model files with
 * application needs.
 */
public class Weaver {

  public static final String TRISOTECH_COM = "trisotech.com";
  public static final String VALUE = "value";
  private static Logger logger = LogManager.getLogger(Weaver.class);

  public static final String CLINICALKNOWLEGEMANAGEMENT_MAYO_ARTIFACTS_BASE_URI = "https://clinicalknowlegemanagement.mayo.edu/artifacts/";
  public static final String WWW_W_3_ORG_2000_XMLNS = "http://www.w3.org/2000/xmlns/";
  private String decisionEl;
  private String elExporter;
  private String elExporterVersion;
  private String metadataRS;
  private String metadataNS;
  private String metadataEl;
  private String metadataExt;
  private String metadataId;
  private String diagramNS;
  private String metadataDiagramDmnNS;
  private String metadataDiagramCmmnNS;
  private String droolsNS;
  private String diagramExt;
  private String annotatedItem;
  private static final String SURROGATE_SCHEMA = "http://kmdp.mayo.edu/metadata/surrogate";
  private static final String ANNOTATIONS_SCHEMA = "http://kmdp.mayo.edu/metadata/annotations";

  private ReaderConfig config;
  private ModelReader reader;

  private ObjectFactory of = new ObjectFactory();
  private Map<String, BaseAnnotationHandler> handlers = new HashMap<>();


  public Weaver() {
    this(new ReaderConfig());
  }

  public Weaver(ReaderConfig p) {
    this.config = p;
    this.reader = new ModelReader(this.config);

    metadataNS = config.getTyped(ReaderOptions.P_METADATA_NS);
    metadataDiagramDmnNS = config.getTyped(ReaderOptions.P_METADATA_DIAGRAM_DMN_NS);
    metadataDiagramCmmnNS = config.getTyped(ReaderOptions.P_METADATA_DIAGRAM_CMMN_NS);
    droolsNS = config.getTyped(ReaderOptions.P_DROOLS_NS);
    metadataEl = config.getTyped(ReaderOptions.P_EL_ANNOTATION);
    metadataRS = config.getTyped(ReaderOptions.P_EL_RELATIONSHIP);
    metadataExt = config.getTyped(ReaderOptions.P_EL_MODEL_EXT);
    metadataId = config.getTyped(ReaderOptions.P_EL_ANNOTATION_ID);
    diagramNS = config.getTyped(ReaderOptions.P_DIAGRAM_NS);
    diagramExt = config.getTyped(ReaderOptions.P_EL_DIAGRAM_EXT);
    elExporter = config.getTyped(ReaderOptions.P_EL_EXPORTER);
    elExporterVersion = config.getTyped(ReaderOptions.P_EL_EXPORTER_VERSION);
    decisionEl = config.getTyped(ReaderOptions.P_EL_DECISION);
    annotatedItem = config.getTyped(ReaderOptions.P_EL_ANNOTATED_ITEM);

    logger.debug("METADATA_EL: {}", metadataEl);
    handlers.put(metadataEl, new MetadataAnnotationHandler(config));
    handlers.put(metadataId, new MetadataAnnotationHandler(config));
    handlers.put(annotatedItem, new AnnotatedFragmentHandler(config));
    handlers.put(metadataRS, new MetadataAnnotationHandler(config));
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

  public ReaderConfig getConfig() {
    return config;
  }

  //DocumentCarrier input = new DocumentCarrier().withStructuredExpression(dox).withRepresentation(rep(DMN_1_2, XML_1_1));

  // CAO: TODO: This is how it should be done with the updated classes 06/20 review w/Davide
  public ResponseEntity<KnowledgeCarrier> weave(KnowledgeCarrier toBeWovenInto,
      KnowledgeCarrier toBeWovenIn) {
    DocumentCarrier input = (DocumentCarrier) toBeWovenInto;
    if (input.getRepresentation().getFormat() != XML_1_1) {
      // exception
    }
    Document out = weave((Document) input.getStructuredExpression());

    // ResponseHelper will handle all the error handling for the response
    return ResponseHelper
        .attempt(AbstractCarrier.of(out).withRepresentation(rep(DMN_1_2, XML_1_1)));
  }

  /**
   * Weave out Trisotech-specific elements and where necessary, replace with KMDP-specific.
   */
  public Document weave(Document dox) {
    dox.getDocumentElement().setAttributeNS(WWW_W_3_ORG_2000_XMLNS,
        "xmlns:" + "xsi",
        "http://www.w3.org/2001/XMLSchema-instance");

    // TODO: remove hardcoded values? CAO
    dox.getDocumentElement().setAttributeNS(WWW_W_3_ORG_2000_XMLNS,
        "xmlns:surr",
        SURROGATE_SCHEMA);
    // TODO: remove hardcoded values? CAO
    dox.getDocumentElement().setAttributeNS(WWW_W_3_ORG_2000_XMLNS,
        "xmlns:ann",
        ANNOTATIONS_SCHEMA);

    dox.getDocumentElement().setAttributeNS("http://www.w3.org/2001/XMLSchema-instance",
        "xsi:" + "schemaLocation",
        getSchemaLocations(dox));

    // first rename and move elements needed out of diagram element and into root element
    NodeList diagramExtension = dox.getElementsByTagNameNS(diagramNS, diagramExt);
    weaveDiagramExtension(diagramExtension, dox);

    // relationships can be in CMMN TODO: Can tell if DMN or CMMNN so don't try to process items only in one? CAO
    NodeList relations = dox.getElementsByTagNameNS(metadataNS, metadataRS);
    weaveRelations(relations);

    // Find the Asset ID, if present
    NodeList ids = dox.getElementsByTagNameNS(metadataNS, metadataId);
    weaveIdentifier(ids);

    // get metas after the move so the moved elements are captured
    NodeList metas = dox.getElementsByTagNameNS(metadataNS, metadataEl);
    weaveMetadata(metas);

    /****** Remove traces of Trisotech  ******/
    // remove the namespace attributes
    removeProprietaryAttributesAndNS(dox);

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

  private void weaveInputs(Document dox) {
    XMLUtil.asElementStream(dox.getElementsByTagName("*"))
        // TODO: code review -- need to know which tags, or just check all hrefs? CAO
        .filter(el -> (el.getLocalName().equals("inputData")
            || el.getLocalName().equals("requiredInput")
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
   * weaveDiagramExtension will take data from the diagram section of the DMN that are needed and
   * move them to be parented by the root. Specifically: Extract the <di:extension></di:extension>
   * fragment and rename to <semantic:extensionElements></semantic:extensionElements> Move as a
   * child of the root of the document
   */
  private void weaveDiagramExtension(NodeList diagramExtension, Document dox) {
    logger.debug("weaveDiagramExtension size: {} ", diagramExtension.getLength());
    asElementStream(diagramExtension)
        .forEach(el -> {
          logger.debug(String.format(
              "el: tagName %s nodeName: %s localname: %s baseURI: %s NamespaceURI: %s nodeValue: %s",
              el.getTagName(), el.getNodeName(), el.getLocalName(), el.getBaseURI(),
              el.getNamespaceURI(), el.getNodeValue()));
          reparentDiagramElement(el, dox);
        });
  }

  /**
   * rename and reparent the diagram element
   */
  private void reparentDiagramElement(Element el, Document dox) {
    NodeList children = el.getChildNodes();
    Element newElement = dox
        .createElementNS(dox.getDocumentElement().getNamespaceURI(), metadataExt);
    newElement.setPrefix("semantic");
    asElementStream(children)
        .forEach(newElement::appendChild);
    dox.getDocumentElement().appendChild(newElement);
  }

  private void weaveRelations(NodeList relations) {
    logger.debug("weaveRelations.... relations size: {}", relations.getLength());
    //
//		// TODO: How to handle CMMN data? [interrelationship should be DatatypeAnnotation]
    asElementStream(relations).forEach(
        this::doRewriteRelations
    );
  }

  private void weaveIdentifier(NodeList metas) {
    logger.debug("weaveIdentifier.... metas size: {}", metas.getLength());
    // rewire dictionary-bound attributes
    asElementStream(metas)
        .filter(this::isIdentifier)
        .forEach(
            this::doRewriteId);
  }


  private void rewriteValue(Attr attr) {
    String value = attr.getValue();
    if (value.lastIndexOf('/') != -1) {
      // get the ids after the last '/'
      // and replace the '_' in the ids
      String id = value.substring(value.lastIndexOf('/') + 1).replace("_", "");
      // reset the value to the KMDP URI
      attr.setValue(CLINICALKNOWLEGEMANAGEMENT_MAYO_ARTIFACTS_BASE_URI + id);
    }
  }


  private void doRewriteRelations(Element el) {
    // remove leading '_' from modelId
    String modelId = el.getAttribute("modelId").substring(1);
    // remove leading '_' from elementId
    String elementId = el.getAttribute("elementId").substring(1);
    BaseAnnotationHandler handler = handler(el);
    DatatypeAnnotation dta = new DatatypeAnnotation();
    // TODO: This should be Registry.MAYO_ARTIFACTS_BASE_URI -- Davide is adding CAO
    dta.setValue(CLINICALKNOWLEGEMANAGEMENT_MAYO_ARTIFACTS_BASE_URI + modelId + "#" + elementId);
    dta.setRel(DependencyType.Imports.asConcept());

    handler.replaceProprietaryElement(el, toChildElement(dta, el));

  }

  private void doRewriteId(Element el) {
    BaseAnnotationHandler handler = handler(el);
    if (KnownAttributes.resolve(el.getAttribute("key")).orElse(null)
        != KnownAttributes.ASSET_IDENTIFIER) {
      throw new IllegalStateException("This method should be called only on ID annotations");
    }
    Map<String, String> ids = extractKeyValuePairs(el.getAttribute(VALUE));
    switch (ids.size()) {
      case 0:
        return;
      case 1:
        Annotation idAnn = handler.getBasicAnnotation(KnownAttributes.ASSET_IDENTIFIER,
            ids.values().iterator().next());
        handler.replaceProprietaryElement(el,
            toChildElement(idAnn, el));
        return;
      default:
        throw new IllegalStateException(
            "More than 1 ID annotations are not supported - found " + ids.size());
    }
  }


  private void weaveMetadata(NodeList metas) {
    asElementStream(metas)
        .forEach(
            el -> doInjectTerm(el,
                getConceptIdentifiers(el))
        );
  }

  private List<ConceptIdentifier> getConceptIdentifiers(Element el) {
    // need to verify any URI values are valid -- no trisotech
    Attr modelUriAttr = el.getAttributeNode("modelURI");
    Attr uriAttr = el.getAttributeNode("uri");
    if (modelUriAttr.getValue().contains(TRISOTECH_COM)) {
      rewriteValue(modelUriAttr);
    }
    if (uriAttr.getValue().contains(TRISOTECH_COM)) {
      rewriteValue(uriAttr);
    }

    // TODO: would there ever be more than one? CAO maybe
    List conceptIdentifiers = new ArrayList<ConceptIdentifier>();
    ConceptIdentifier concept = null;
    try {
      concept = new ConceptIdentifier().withLabel(el.getAttribute("name"))
          .withTag(el.getAttribute("id"))
          .withRef(new URI(el.getAttribute("modelURI")))
          .withConceptId(new URI(el.getAttribute("uri")));
    } catch (URISyntaxException e) {
      logger.error(String.format("%s%s", e.getMessage(), e.getStackTrace()));
    }
    conceptIdentifiers.add(concept);

    // TODO: shouldn't assume here? id might not have leading '_'? or is that just because of test file? CAO
    //  String tag = el.getAttribute("id").substring(1);
    // TODO: discuss with Davide -- which of the two examples given below are needed? both? for different reasons? How to tell? CAO
    // TODO: This is failing -- should it work? CAO
//    ConceptIdentifier ciFromCS = ClinicalSituation.resolve(tag).orElseThrow(IllegalStateException::new).??; // CAO -- for now: TODO: fix this -- there will be more support coming

    // TODO: fix this CAO -- this replaces the 'new ConceptIdentifer' code above (need the above statement working first???) this currently FAILS
//    ConceptIdentifier cid = KnowledgeRepresentationLanguage.resolve(tag).orElseThrow(IllegalStateException::new).asConcept();

    return conceptIdentifiers;
  }


  private BaseAnnotationHandler handler(Element el) {
    if (!handlers.containsKey(el.getLocalName())) {
      throw new UnsupportedOperationException(
          "Unable to find handler for annotation " + el.getLocalName());
    }
    return handlers.get(el.getLocalName());
  }


  private void doInjectTerm(Element el,
//	                           KnownAttributes defaultRel,
      List<ConceptIdentifier> rows) {
    BaseAnnotationHandler handler = handler(el);

    if (!rows.isEmpty()) {
      List<Annotation> annos = handler.getAnnotation(rows);
      handler.replaceProprietaryElement(el,
          handler.wrap(toChildElements(annos, el)));
    }
  }

  private Map<String, String> extractKeyValuePairs(String value) {
    HashMap<String, String> map = new HashMap<>();
    Matcher m = reader.getURLPattern().matcher(value);
    if (m.find()) {
      map.put(getUrlKey(m.group(1)), m.group(2));
    } else {
      map.put(VALUE, value);
    }
    return map;
  }

  private boolean isIdentifier(Element el) {
    return KnownAttributes.resolve(el.getAttribute("key"))
        .filter(x -> x == KnownAttributes.ASSET_IDENTIFIER)
        .isPresent();
  }

  private String getUrlKey(String value) {
    return value;
  }

  private List<Element> toChildElements(List<Annotation> annos, Element parent) {
    return annos.stream()
        .map(ann -> toChildElement(ann, parent))
        .collect(Collectors.toList());
  }

  private Element toChildElement(Annotation ann, Element parent) {
    Element el;
    if (ann instanceof MultiwordAnnotation) {
      el = toElement(of,
          (MultiwordAnnotation) ann,
          of::createMultiwordAnnotation
          // TODO: CAO: The following 2 lines were for validation of schema, and used to not have hard-coded values in the call
//			                         XMLUtil.getSchemas( "http://kmdp.mayo.edu/metadata/surrogate" )
//			                                .orElseThrow( IllegalStateException::new )
      );
    } else if (ann instanceof SimpleAnnotation) {
      el = toElement(of,
          (SimpleAnnotation) ann,
          of::createSimpleAnnotation
//			  TODO:  CAO:                     XMLUtil.getSchemas( "http://kmdp.mayo.edu/metadata/surrogate" )
//			                                .orElseThrow( IllegalStateException::new )
      );
    } else if (ann instanceof BasicAnnotation) {
      el = toElement(of,
          (BasicAnnotation) ann,
          of::createBasicAnnotation
//			 TODO: CAO:                        XMLUtil.getSchemas( "http://kmdp.mayo.edu/metadata/surrogate" )
//			                                .orElseThrow( IllegalStateException::new )
      );
    } else if (ann instanceof DatatypeAnnotation) {
      el = toElement(of,
          (DatatypeAnnotation) ann,
          of::createDatatypeAnnotation
//			TODO: CAO                         XMLUtil.getSchemas( "http://kmdp.mayo.edu/metadata/surrogate" )
//			                                .orElseThrow( IllegalStateException::new )
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
    return JaxbUtil.marshall(Collections.singleton(ctx.getClass()),
        root,
        mapper,
        JaxbUtil.defaultProperties())
        .map(ByteArrayOutputStream::toByteArray)
        .map(ByteArrayInputStream::new)
        .flatMap(XMLUtil::loadXMLDocument)
        .map(Document::getDocumentElement)
        .orElseThrow(IllegalStateException::new);
  }


  private String getSchemaLocations(Document dox) {
    StringBuilder sb = new StringBuilder();

    logger.debug(
        "KnowledgeRepresentationLanguage.DMN_1_2.getRef(): {}", DMN_1_2.getRef());
    sb.append(SURROGATE_SCHEMA)
        .append(" ").append("xsd/metadata/surrogate/surrogate.xsd");
    sb.append(" ");
    sb.append(ANNOTATIONS_SCHEMA)
        .append(" ").append("xsd/metadata/annotations/annotations.xsd");

    String baseNS = dox.getDocumentElement().getNamespaceURI();
    if (baseNS.contains("DMN")) {
      sb.append(" ")
          .append(DMN_1_2.getRef())
          .append(" ").append(Registry.getValidationSchema(DMN_1_2.getRef())
          .orElseThrow(IllegalStateException::new));
    } else if (baseNS.contains("CMMN")) {
      sb.append(" ").append(CMMN_1_1)
          .append(" ").append(Registry.getValidationSchema(CMMN_1_1.getRef())
          .orElseThrow(IllegalStateException::new));
    }

    return sb.toString();
  }


}
