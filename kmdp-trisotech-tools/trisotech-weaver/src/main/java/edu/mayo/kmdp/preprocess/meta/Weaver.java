/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.mayo.kmdp.preprocess.meta;

import edu.mayo.kmdp.metadata.annotations.*;
import edu.mayo.kmdp.registry.Registry;
import edu.mayo.kmdp.util.ws.ResponseHelper;
import edu.mayo.ontology.taxonomies.clinicalsituations.ClinicalSituation;
import edu.mayo.ontology.taxonomies.krlanguage._2018._08.KnowledgeRepresentationLanguage;
import edu.mayo.kmdp.util.JaxbUtil;
import edu.mayo.kmdp.util.XMLUtil;
import org.omg.spec.api4kp._1_0.AbstractCarrier;
import org.omg.spec.api4kp._1_0.identifiers.ConceptIdentifier;
import org.omg.spec.api4kp._1_0.services.DocumentCarrier;
import org.omg.spec.api4kp._1_0.services.KnowledgeCarrier;
import org.springframework.http.ResponseEntity;
import org.w3c.dom.*;

import javax.xml.bind.JAXBElement;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static edu.mayo.kmdp.util.XMLUtil.asElementStream;
import static edu.mayo.ontology.taxonomies.krformat._2018._08.SerializationFormat.XML_1_1;
import static edu.mayo.ontology.taxonomies.krlanguage._2018._08.KnowledgeRepresentationLanguage.DMN_1_2;
import static org.omg.spec.api4kp._1_0.AbstractCarrier.rep;

public class Weaver {

  public static final String CLINICALKNOWLEGEMANAGEMENT_MAYO_ARTIFACTS_BASE_URI = "https://clinicalknowlegemanagement.mayo.edu/artifacts/";
  private static String EL_EXPORTER;
  private static String EL_EXPORTER_VERSION;
  private static String METADATA_RS;
  private static String METADATA_NS;
  private static String METADATA_EL;
  private static String METADATA_EXT;
  private static String METADATA_ID;
  private static String DIAGRAM_NS;
  private static String METADATA_DIAGRAM_DMN_NS;
  private static String METADATA_DIAGRAM_CMMN_NS;
  private static String DROOLS_NS;
  private static String DIAGRAM_EXT;
  private static String ANNOTATED_ITEM;
  private static String SURROGATE_SCHEMA = "http://kmdp.mayo.edu/metadata/surrogate";
  private static String ANNOTATIONS_SCHEMA = "http://kmdp.mayo.edu/metadata/annotations";


  private ReaderConfig config;
  private ModelReader reader;

  private ObjectFactory of = new ObjectFactory();
  private Map<String, BaseAnnotationHandler> handlers = new HashMap<>();


  public Weaver() {
    this(false);
  }

  public Weaver(boolean strict) {
    this(null, strict);
  }

  public Weaver(boolean strict, ReaderConfig p) {
    this(null, strict, p);
  }

  public Weaver(InputStream dict, boolean strict) {
    this(dict, strict, new ReaderConfig());
  }

  public Weaver(InputStream dict, boolean strict, ReaderConfig p) {
    this.config = p;
    this.reader = new ModelReader(this.config);

    METADATA_NS = config.getTyped(ReaderOptions.p_METADATA_NS);
    METADATA_DIAGRAM_DMN_NS = config.getTyped(ReaderOptions.p_METADATA_DIAGRAM_DMN_NS);
    METADATA_DIAGRAM_CMMN_NS = config.getTyped(ReaderOptions.p_METADATA_DIAGRAM_CMMN_NS);
    DROOLS_NS = config.getTyped(ReaderOptions.p_DROOLS_NS);
    METADATA_EL = config.getTyped(ReaderOptions.p_EL_ANNOTATION);
    METADATA_RS = config.getTyped(ReaderOptions.p_EL_RELATIONSHIP);
    METADATA_EXT = config.getTyped(ReaderOptions.p_EL_MODEL_EXT);
    METADATA_ID = config.getTyped(ReaderOptions.p_EL_ANNOTATION_ID);
    DIAGRAM_NS = config.getTyped(ReaderOptions.p_DIAGRAM_NS);
    DIAGRAM_EXT = config.getTyped(ReaderOptions.p_EL_DIAGRAM_EXT);
    EL_EXPORTER = config.getTyped(ReaderOptions.p_EL_EXPORTER);
    EL_EXPORTER_VERSION = config.getTyped(ReaderOptions.p_EL_EXPORTER_VERSION);

//		TODO: Needed? CAO [maybe]
    ANNOTATED_ITEM = config.getTyped(ReaderOptions.p_EL_ANNOTATED_ITEM);

    System.out.println("METADATA_EL: " + METADATA_EL);
    handlers.put(METADATA_EL, new MetadataAnnotationHandler(config));
    handlers.put(METADATA_ID, new MetadataAnnotationHandler(config));
    handlers.put(ANNOTATED_ITEM, new AnnotatedFragmentHandler(config));
    handlers.put(METADATA_RS, new MetadataAnnotationHandler(config));
  }

  public static String getMETADATA_NS() {
    return METADATA_NS;
  }

  public static String getMETADATA_EL() {
    return METADATA_EL;
  }

  public static String getMETADATA_EXT() {
    return METADATA_EXT;
  }

  public static String getMETADATA_ID() {
    return METADATA_ID;
  }

  public static String getDIAGRAM_NS() {
    return DIAGRAM_NS;
  }

  public static String getDIAGRAM_EXT() {
    return DIAGRAM_EXT;
  }

  public static String getANNOTATED_ITEM() {
    return ANNOTATED_ITEM;
  }

  public static String getSURROGATE_SCHEMA() {
    return SURROGATE_SCHEMA;
  }

  public static String getANNOTATIONS_SCHEMA() {
    return ANNOTATIONS_SCHEMA;
  }

  public static String getMETADATA_RS() {
    return METADATA_RS;
  }

  public static String getMETADATA_DIAGRAM_DMN_NS() {
    return METADATA_DIAGRAM_DMN_NS;
  }

  public static String getMETADATA_DIAGRAM_CMMN_NS() {
    return METADATA_DIAGRAM_CMMN_NS;
  }

  public static String getDROOLS_NS() {
    return DROOLS_NS;
  }

  public static String getEXPORTER() {
    return EL_EXPORTER;
  }

  public static String getEXPORTER_VERSION() {
    return EL_EXPORTER_VERSION;
  }

  public ReaderConfig getConfig() {
    return config;
  }


  //DocumentCarrier input = new DocumentCarrier().withStructuredExpression(dox).withRepresentation(rep(DMN_1_2, XML_1_1));

  // CAO: TODO: This is how it should be done with the updated classes 06/20 review w/Davide
  public ResponseEntity<? extends KnowledgeCarrier> weave(KnowledgeCarrier toBeWovenInto, KnowledgeCarrier toBeWovenIn) {
    DocumentCarrier input = (DocumentCarrier) toBeWovenInto;
    if (input.getRepresentation().getFormat() != XML_1_1) {
      // exception
    }
    Document out = weave((Document) input.getStructuredExpression());


    // ResponseHelper will handle all the error handling for the response
    return ResponseHelper.attempt(AbstractCarrier.of(out).withRepresentation(rep(DMN_1_2, XML_1_1)));
  }

  // What exactly is the purpose of this method? Is it weaving in KMDP attributes in place of <Signavio> (to be Trisotech)
  // attributes?
  public Document weave(Document dox) {
    Element e = dox.getDocumentElement();
//    System.out.println("documentElement: tagName " + e.getTagName() +
//        " nodeName: " + e.getNodeName() + " localname: " + e.getLocalName() +
//        " baseURI: " + e.getBaseURI() + " NamespaceURI: " + e.getNamespaceURI() +
//        " nodeValue: " + e.getNodeValue());
    dox.getDocumentElement().setAttributeNS("http://www.w3.org/2000/xmlns/",
        "xmlns:" + "xsi",
        "http://www.w3.org/2001/XMLSchema-instance");

    // TODO: remove hardcoded values? CAO
    dox.getDocumentElement().setAttributeNS("http://www.w3.org/2000/xmlns/",
        "xmlns:surr",
        SURROGATE_SCHEMA);
    // TODO: remove hardcoded values? CAO
    dox.getDocumentElement().setAttributeNS("http://www.w3.org/2000/xmlns/",
        "xmlns:ann",
        ANNOTATIONS_SCHEMA);

    dox.getDocumentElement().setAttributeNS("http://www.w3.org/2001/XMLSchema-instance",
        "xsi:" + "schemaLocation",
        getSchemaLocations(dox));

//    System.out.println("What is validation Schema for the NS? " + Registry.getValidationSchema(URI.create(METADATA_NS)).get());

    // first rename and move elements needed out of diagram element and into root element
    NodeList diagramExtension = dox.getElementsByTagNameNS(DIAGRAM_NS, DIAGRAM_EXT);

    weaveDiagramExtension(diagramExtension, dox);

    // get metas after the move so the moved elements are captured
    NodeList metas = dox.getElementsByTagNameNS(METADATA_NS, METADATA_EL);

    // relationships can be in CMMN TODO: Can tell if DMN or CMMNN so don't try to process items only in one? CAO
    NodeList relations = dox.getElementsByTagNameNS(METADATA_NS, METADATA_RS);
    weaveRelations(relations);

    NodeList ids = dox.getElementsByTagNameNS(METADATA_NS, METADATA_ID);

    // Find the Asset ID, if present
    weaveIdentifier(ids);

    // CAO
    weaveMetadata(metas);
//		NodeList dicts = dox.getElementsByTagName( ANNOTATED_ITEM );

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
        .filter((el) -> (el.getLocalName().equals("caseFileItemDefinition")))
        .forEach(element -> {
          Attr attr = element.getAttributeNode("definitionType");
          if(attr.getValue().contains("trisotech.com")) {
            System.out.println("WARNING: Should not have trisotech.com in caseFileItemDefinition. Rewriting to default value of Unspecified." +
                "Found for " + element.getAttributeNode("name").getValue());
            // TODO: a way to do this using the XSD? CAO
            attr.setValue("http://www.omg.org/spec/CMMN/DefinitionType/Unspecified");
          }
        });
  }

  private void weaveInputs(Document dox) {
    XMLUtil.asElementStream(dox.getElementsByTagName("*"))
        // TODO: code review -- need to know which tags, or just check all hrefs? CAO
        .filter((el) -> (el.getLocalName().equals("inputData")
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
        .filter((el) -> el.getLocalName().equals("import"))
        .forEach((el) -> {
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
   *
   * @param dox
   */
  private void removeProprietaryAttributesAndNS(Document dox) {
    NodeList elements = dox.getElementsByTagNameNS("*", "*");
    asElementStream(elements).forEach(
        (el) -> {
          NamedNodeMap attributes = el.getAttributes();
          int attrSize = attributes.getLength() - 1;
          for (int i = attrSize; i >= 0; i--) {
            Attr attr = (Attr) attributes.item(i);
            if ((attr != null) &&
                // remove any of the Trisotech namespace attributes, and drools
                (METADATA_NS.equals(attr.getNamespaceURI())
                    || METADATA_NS.equals(attr.getValue())
                    || METADATA_DIAGRAM_DMN_NS.equals(attr.getNamespaceURI())
                    || METADATA_DIAGRAM_DMN_NS.equals(attr.getValue())
                    || METADATA_DIAGRAM_CMMN_NS.equals(attr.getNamespaceURI())
                    || METADATA_DIAGRAM_CMMN_NS.equals(attr.getValue())
                    || DROOLS_NS.equals(attr.getNamespaceURI())
                    || DROOLS_NS.equals(attr.getValue())
                    || EL_EXPORTER.equals(attr.getLocalName())
                    || EL_EXPORTER_VERSION.equals(attr.getLocalName())
                )
            ) {
              el.removeAttributeNode(attr);
            }
          }
        }
    );
  }


  /**
   * weaveDiagramExtension will take data from the diagram section of the DMN that are needed and move them
   * to be parented by the root.
   * Specifically:
   * Extract the <di:extension></di:extension> fragment and rename to
   * <semantic:extensionElements></semantic:extensionElements>
   * Move as a child of the root of the document
   *
   * @param diagramExtension
   * @param dox
   */
  private void weaveDiagramExtension(NodeList diagramExtension, Document dox) {
    System.out.println("weaveDiagramExtension size: " + diagramExtension.getLength());
    asElementStream(diagramExtension)
        .forEach((el) -> {
          System.out.println("el: tagName " + el.getTagName() +
              " nodeName: " + el.getNodeName() + " localname: " + el.getLocalName() +
              " baseURI: " + el.getBaseURI() + " NamespaceURI: " + el.getNamespaceURI() +
              " nodeValue: " + el.getNodeValue());
          reparentDiagramElement(el, dox);
        });
  }

  /**
   * rename and reparent the diagram element
   *
   * @param el
   * @param dox
   */
  private void reparentDiagramElement(Element el, Document dox) {
    NodeList children = el.getChildNodes();
    Element newElement = dox.createElementNS(dox.getDocumentElement().getNamespaceURI(), METADATA_EXT);
    newElement.setPrefix("semantic");
    asElementStream(children)
        .forEach((child) -> newElement.appendChild(child));
    dox.getDocumentElement().appendChild(newElement);
  }


  private void weaveRelations(NodeList relations) {
    System.out.println("weaveRelations.... relations size: " + relations.getLength());
    //
//		// TODO: How to handle CMMN data? [interrelationship should be DatatypeAnnotation]
    asElementStream(relations).forEach(
        (el) -> {
          doRewriteRelations(el);
        }
    );


  }

  private void weaveIdentifier(NodeList metas) {
    System.out.println("weaveIdentifier.... metas size: " + metas.getLength());
    // rewire dictionary-bound attributes
    asElementStream(metas)
        .filter(this::isIdentifier)
        .forEach(
            (el) -> doRewriteId(el));
  }


  private void rewriteValue(Attr attr) {
    String value = attr.getValue();
    if(value.lastIndexOf('/') != -1) {
      String id = value.substring(value.lastIndexOf('/') + 2);
      attr.setValue(CLINICALKNOWLEGEMANAGEMENT_MAYO_ARTIFACTS_BASE_URI + id);
    }
  }


  private void doRewriteRelations(Element el) {
//    System.out.println("\tis CMMN?");
//    System.out.println("\tfileId: " + el.getAttribute("fileId"));
//    System.out.println("\telementId: " + el.getAttribute("elementId"));
//    System.out.println("\tmodelId: " + el.getAttribute("modelId"));
//    System.out.println("\tmimeType: " + el.getAttribute("mimeType"));
    String modelId = el.getAttribute("modelId").substring(1);
    String elementId = el.getAttribute("elementId");
    // TODO: pick up here -- how to rewrite the relationship... CAO
    BaseAnnotationHandler handler = handler(el);
    DatatypeAnnotation dta = new DatatypeAnnotation();
    // TODO: This should be Registry.MAYO_ARTIFACTS_BASE_URI -- Davide is adding CAO
    dta.setValue(CLINICALKNOWLEGEMANAGEMENT_MAYO_ARTIFACTS_BASE_URI + modelId + "#" + elementId);
    // TODO: Confer w/Davide what is needed here CAO
    dta.setRel(new ConceptIdentifier().withLabel("pointsTo?").withTag("tag???").withRef(URI.create("someURIValue???")));
    System.out.println("dta: value: " + dta.getValue() + " rel: tag: " + dta.getRel().getTag() + " rel: label: "
        + dta.getRel().getLabel() + " rel: ref: " + dta.getRel().getRef());

    handler.replaceProprietaryElement(el, toChildElement(dta, el));
//              ConceptIdentifier cid = KnowledgeRepresentationLanguage.resolve(modelId).orElseThrow(IllegalArgumentException::new).asConcept(); // this fails CAO
//              System.out.println("cid for CMMN modelId " + modelId + " is: " + cid.toString());

  }

  private void doRewriteId(Element el) {
//    System.out.println("doRewriteId for element: el: tagName " + el.getTagName() +
//        " nodeName: " + el.getNodeName() + " localname: " + el.getLocalName() +
//        " baseURI: " + el.getBaseURI() + " NamespaceURI: " + el.getNamespaceURI() +
//        " nodeValue: " + el.getNodeValue());
    BaseAnnotationHandler handler = handler(el);
    if (KnownAttributes.resolve(el.getAttribute("key")).orElse(null) != KnownAttributes.ASSET_IDENTIFIER) {
      throw new IllegalStateException("This method should be called only on ID annotations");
    }
    Map<String, String> ids = extractKeyValuePairs(el.getAttribute("value"));
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
        throw new IllegalStateException("More than 1 ID annotations are not supported - found " + ids.size());
    }
  }


//  private void weaveNonDictionaryMetadata(NodeList metas) {
//    // rewire attributes
//    asElementStream(metas)
////				.filter( (x) -> ! isDictionaryElement( x ) )
////				.filter( (x) -> ! isIdentifier( x ) )
//        .forEach(
//            (el) -> doRewrite(el));
//  }

  // CAO
  private void weaveMetadata(NodeList metas) {
    asElementStream(metas)
        .forEach(
            (el) -> doInjectTerm(el,
                getConceptIdentifiers(el))
        );
  }

  private List<ConceptIdentifier> getConceptIdentifiers(Element el) {
//		System.out.println("el in getConceptIdentifiers: " + el);
//		System.out.println("\tid: " + el.getAttribute("id"));
//		System.out.println("\tname: " + el.getAttribute("name"));
//		System.out.println("\tmodelURI: " + el.getAttribute("modelURI"));
//		System.out.println("\turi: " + el.getAttribute("uri"));

		// need to verify any URI values are valid -- no trisotech
    Attr modelUriAttr = el.getAttributeNode("modelURI");
    Attr uriAttr = el.getAttributeNode("uri");
    if(modelUriAttr.getValue().contains("trisotech.com")) {
      rewriteValue(modelUriAttr);
    }
    if(uriAttr.getValue().contains("trisotech.com")) {
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
      e.printStackTrace();
    }
    conceptIdentifiers.add(concept);

    // TODO: shouldn't assume here? id might not have leading '_'? or is that just because of test file? CAO
    String tag = el.getAttribute("id").substring(1);
    // TODO: discuss with Davide -- which of the two examples given below are needed? both? for different reasons? How to tell? CAO
    // TODO: This is failing -- should it work? CAO
//    ConceptIdentifier ciFromCS = ClinicalSituation.resolve(tag).orElseThrow(IllegalStateException::new).??; // CAO -- for now: TODO: fix this -- there will be more support coming

    // TODO: fix this CAO -- this replaces the 'new ConceptIdentifer' code above (need the above statement working first???) this currently FAILS
//    ConceptIdentifier cid = KnowledgeRepresentationLanguage.resolve(tag).orElseThrow(IllegalStateException::new).asConcept();

    return conceptIdentifiers;
  }


  private BaseAnnotationHandler handler(Element el) {
    if (!handlers.containsKey(el.getLocalName())) {
      throw new UnsupportedOperationException("Unable to find handler for annotation " + el.getLocalName());
    }
    return handlers.get(el.getLocalName());
  }


//	private void doInjectTerm( Element el, List<ConceptIdentifier> rows ) {
//		doInjectTerm( el, rows );
//	}

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

//	private void doInjectValue( Element el,
////	                            KnownAttributes defaultRel,
//	                            String value ) {
//		BaseAnnotationHandler handler = handler( el );
//
//		List<Annotation> annos = handler.getDataAnnotation( el.getAttribute( "name" ), defaultRel, value );
//		handler.replaceProprietaryElement( el,
//		                                   handler.wrap( toChildElements( annos, el ) ) );
//
//	}


  private void doRewrite(final Element el) {
    extractKeyValuePairs(el.getAttribute("value"));
//						.forEach( (k,v) -> {
//			KnownAttributes.resolve( el.getAttribute( "name" ) )
//			               .ifPresent( (attr) -> {
//			               	    if ( attr == KnownAttributes.SALIENCE ) {
//									doInjectValue( el, attr, v );
//			                    } else {
//				                    throw new IllegalStateException("Unsupported attribute " + attr );
//			                    }
//			               } );
//		} );
  }

  private Map<String, String> extractKeyValuePairs(String value) {
    HashMap<String, String> map = new HashMap<>();
    Matcher m = reader.getURLPattern().matcher(value);
    if (m.find()) {
      map.put(getUrlKey(m.group(1)), m.group(2));
    } else {
      map.put("value", value);
    }
    return map;
  }


  private boolean isEmptyAttribute(Element el) {
    String value = el.getAttribute("value");
    return value.isEmpty() || "[]".equals(value);
  }


  private boolean isIdentifier(Element el) {
    System.out.println("isIdentifier key: " + el.getAttribute("key") +
        " ASSET_IDENTIFIER: " + KnownAttributes.ASSET_IDENTIFIER);
    System.out.println("isIdentifier for element: el: tagName " + el.getTagName() +
        " nodeName: " + el.getNodeName() + " localname: " + el.getLocalName() +
        " baseURI: " + el.getBaseURI() + " NamespaceURI: " + el.getNamespaceURI() +
        " nodeValue: " + el.getNodeValue());
    return KnownAttributes.resolve(el.getAttribute("key"))
        .filter((x) -> x == KnownAttributes.ASSET_IDENTIFIER)
        .isPresent();
  }

  private String getUrlKey(String value) {
    return value;
  }


  private List<Element> toChildElements(List<Annotation> annos, Element parent) {
    return annos.stream()
        .map((ann) -> toChildElement(ann, parent))
        .collect(Collectors.toList());
  }

  private Element toChildElement(Annotation ann, Element parent) {
    Element el;
    if (ann instanceof MultiwordAnnotation) {
      el = toElement(of,
          (MultiwordAnnotation) ann,
          of::createMultiwordAnnotation
          // CAO: The following 2 lines were for validation of schema, and used to not have hard-coded values in the call
//			                         XMLUtil.getSchemas( "http://kmdp.mayo.edu/metadata/surrogate" )
//			                                .orElseThrow( IllegalStateException::new )
      );
    } else if (ann instanceof SimpleAnnotation) {
      el = toElement(of,
          (SimpleAnnotation) ann,
          of::createSimpleAnnotation
//			                         XMLUtil.getSchemas( "http://kmdp.mayo.edu/metadata/surrogate" )
//			                                .orElseThrow( IllegalStateException::new )
      );
    } else if (ann instanceof BasicAnnotation) {
      el = toElement(of,
          (BasicAnnotation) ann,
          of::createBasicAnnotation
//			                         XMLUtil.getSchemas( "http://kmdp.mayo.edu/metadata/surrogate" )
//			                                .orElseThrow( IllegalStateException::new )
      );
    } else if (ann instanceof DatatypeAnnotation) {
      el = toElement(of,
          (DatatypeAnnotation) ann,
          of::createDatatypeAnnotation
//			                         XMLUtil.getSchemas( "http://kmdp.mayo.edu/metadata/surrogate" )
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

    System.out.println("KnowledgeRepresentationLanguage.DMN_1_2.getRef(): " + DMN_1_2.getRef());
    System.out.println("Registry.getValidationSchema( KnowledgeRepresentationLanguage.CMMN_1_1.getRef() ): " + Registry.getValidationSchema(KnowledgeRepresentationLanguage.CMMN_1_1.getRef()));
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
      sb.append(" ").append(KnowledgeRepresentationLanguage.CMMN_1_1)
          .append(" ").append(Registry.getValidationSchema(KnowledgeRepresentationLanguage.CMMN_1_1.getRef())
          .orElseThrow(IllegalStateException::new));
    }

    return sb.toString();
  }


//	public static ReaderConfig getWeaverProperties( KnowledgeRepresentationLanguage src ) {
//		ReaderConfig prop = new ReaderConfig();
//
//		switch ( src ) {
//			case DMN_1_2: return prop;
//			case CMMN_1_1: return prop.with( p_EL_ANNOTATION,
//			                                 "interrelationship" );
//			default:
//				throw new IllegalArgumentException( "Unexpected source type " + src );
//		}
//	}


}
