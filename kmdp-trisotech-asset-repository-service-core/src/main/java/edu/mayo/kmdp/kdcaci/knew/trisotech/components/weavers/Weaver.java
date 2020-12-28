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
package edu.mayo.kmdp.kdcaci.knew.trisotech.components.weavers;

import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.CMMN;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.DMN;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.TRISOTECH_COM;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.TT_COPYOFLINK;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.TT_METADATA_NS;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.TT_REUSELINK;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.TT_SEMANTICLINK;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.W3C_XMLNS;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.W3C_XSI;
import static edu.mayo.kmdp.util.XMLUtil.asElementStream;
import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.Captures;
import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.Defines;
import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.In_Terms_Of;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.CMMN_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.DMN_1_2;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;
import static org.omg.spec.api4kp._20200801.taxonomy.krserialization.KnowledgeRepresentationLanguageSerializationSeries.Knowledge_Asset_Surrogate_2_0_XML_Syntax;

import edu.mayo.kmdp.kdcaci.knew.trisotech.NamespaceManager;
import edu.mayo.kmdp.registry.Registry;
import edu.mayo.kmdp.util.JaxbUtil;
import edu.mayo.kmdp.util.Util;
import edu.mayo.kmdp.util.XMLUtil;
import edu.mayo.ontology.taxonomies.clinicaltasks.ClinicalTaskSeries;
import edu.mayo.ontology.taxonomies.kao.decisiontype.DecisionTypeSeries;
import edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBElement;
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

  public static final Logger logger = LoggerFactory.getLogger(Weaver.class);

  @Autowired
  private TermsApiInternal terms;

  @Autowired
  private NamespaceManager names;

  private final ObjectFactory of = new ObjectFactory();
  private final Map<String, MetadataAnnotationHandler> handlers = new HashMap<>();

  @PostConstruct
  public void init() {
    logger.debug("Weaver ctor");

    handlers.put(TT_SEMANTICLINK, new MetadataAnnotationHandler());
    handlers.put(TT_REUSELINK, new MetadataAnnotationHandler());
    handlers.put(TT_COPYOFLINK, new MetadataAnnotationHandler());
  }


  /**
   * Weave out Trisotech-specific elements and where necessary, replace with KMDP-specific.
   */
  public Document weave(Document dox) {
    dox.getDocumentElement().setAttributeNS(W3C_XMLNS,
        "xmlns:" + "xsi",
        W3C_XSI);

    String surrPrefix = Registry
        .getPrefixforNamespace(Knowledge_Asset_Surrogate_2_0_XML_Syntax.getReferentId())
        .orElseThrow(IllegalStateException::new);
    String surrNamespace = Registry
        .getValidationSchema(Knowledge_Asset_Surrogate_2_0.getReferentId())
        .orElseThrow(IllegalStateException::new);

    dox.getDocumentElement().setAttributeNS(W3C_XMLNS,
        "xmlns:" + surrPrefix,
        surrNamespace);

    dox.getDocumentElement().setAttributeNS(W3C_XSI,
        "xsi:" + "schemaLocation",
        getSchemaLocations(dox, surrNamespace));

    // get metas
    NodeList metas = dox.getElementsByTagNameNS(TT_METADATA_NS, TT_SEMANTICLINK);
    weaveMetadata(metas);
    // copyLink
    metas = dox.getElementsByTagNameNS(TT_METADATA_NS, TT_COPYOFLINK);
    weaveMetadata(metas);
    // reuseLink
    metas = dox.getElementsByTagNameNS(TT_METADATA_NS, TT_REUSELINK);
    weaveMetadata(metas);

    // rewrite namespaces
    weaveNamespaces(dox);

    // rewrite namespace for 'import' tags
    weaveImport(dox);

    // rewrite href for 'inputData' and 'requiredInput' tags
    weaveInputs(dox);

    return dox;
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

    return names.isDomainConcept(ns);
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
    Attr assetsAttr = dox.createAttributeNS(W3C_XMLNS, "xmlns:assets");
    assetsAttr.setValue(names.getAssetNamespace().toString());
    dox.getDocumentElement().setAttributeNodeNS(assetsAttr);

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
      attr.setValue(names.getArtifactNamespace() + id);
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
    Attr uriAttr = el.getAttributeNode("uri");
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
        if(logger.isWarnEnabled()) {
          logger.warn("WARNING: resource ID {} failed in lookupTerm and will be removed from the file", resourceIdentifier.getUuid().toString());
        }
        return conceptIdentifiers;
      }
    } catch (URISyntaxException | IllegalArgumentException e) {
      logger.error(String.format("%s%s", e.getMessage(), Arrays.toString(e.getStackTrace())));
    }
    conceptIdentifiers.add(concept);

    return conceptIdentifiers;
  }


  private MetadataAnnotationHandler handler(Element el) {
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
    MetadataAnnotationHandler handler = handler(el);

    if (!rows.isEmpty() && rows.stream().anyMatch(Objects::nonNull)) {
      List<Annotation> annos = handler.getAnnotation(defaultRel, rows);
      handler.replaceProprietaryElement(el,
          handler.wrap(toChildElements(annos, el)));
    }
  }

  private List<Element> toChildElements(List<Annotation> annos, Element parent) {
    return annos.stream()
        .map(ann -> toChildElement(ann, parent))
        .collect(Collectors.toList());
  }

  private Element toChildElement(Annotation ann, Element parent) {
    Element el;
    if (ann != null) {
      el = toElement(of,
          ann,
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

    return dox
        .map(Document::getDocumentElement)
        .orElseThrow(IllegalStateException::new);
  }


  private String getSchemaLocations(Document dox, String surrNamespace) {
    StringBuilder sb = new StringBuilder();

    sb.append(surrNamespace)
        .append(" ")
        .append("xsd/API4KP/surrogate/surrogate.xsd");

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


}
