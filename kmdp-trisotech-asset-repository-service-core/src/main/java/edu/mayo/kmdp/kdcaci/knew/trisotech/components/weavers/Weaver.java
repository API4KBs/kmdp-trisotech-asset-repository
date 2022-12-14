/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <p>
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache 2.0</a>
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package edu.mayo.kmdp.kdcaci.knew.trisotech.components.weavers;

import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.API4KP_PREFIX;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.CMMN;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.CMMN_11_XMLNS;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.DMN;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.KEY;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.TRISOTECH_COM;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.TT_ACCELERATOR_ENTTIY;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.TT_ACCELERATOR_MODEL;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.TT_COPYOFLINK;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.TT_CUSTOM_ATTRIBUTE_ATTR;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.TT_METADATA_NS;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.TT_REUSELINK;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.TT_SEMANTICLINK;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.VALUE;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.W3C_XMLNS;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.W3C_XSI;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.components.weavers.DataBindingManipulator.rewriteInputDataBindings;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.components.weavers.DataBindingManipulator.rewriteOutputDataBindings;
import static edu.mayo.kmdp.util.XMLUtil.asElementStream;
import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.Captures;
import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.Defines;
import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.Has_Primary_Subject;
import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.In_Terms_Of;
import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.Is_About;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newVersionId;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.CMMN_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.DMN_1_2;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;
import static org.omg.spec.api4kp._20200801.taxonomy.krserialization.KnowledgeRepresentationLanguageSerializationSeries.Knowledge_Asset_Surrogate_2_0_XML_Syntax;

import edu.mayo.kmdp.kdcaci.knew.trisotech.NamespaceManager;
import edu.mayo.kmdp.registry.Registry;
import edu.mayo.kmdp.util.NameUtils;
import edu.mayo.kmdp.util.URIUtil;
import edu.mayo.kmdp.util.Util;
import edu.mayo.kmdp.util.XMLUtil;
import edu.mayo.kmdp.util.XPathUtil;
import edu.mayo.ontology.taxonomies.kao.decisiontype.DecisionTypeSeries;
import edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.terminology.v4.server.TermsApiInternal;
import org.omg.spec.api4kp._20200801.id.ConceptIdentifier;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KPComponent;
import org.omg.spec.api4kp._20200801.surrogate.Annotation;
import org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries;
import org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyType;
import org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries;
import org.omg.spec.api4kp._20200801.terms.model.ConceptDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Weaver class is used for replacing/removing Trisotech-specific information in model files with
 * application needs.
 */
@Component
public class Weaver {

  private static final Logger logger = LoggerFactory.getLogger(Weaver.class);
  public static final String XMLNS_PREFIX = "xmlns:";

  @Autowired
  @KPComponent(implementation = "fhir")
  private TermsApiInternal terms;

  @Autowired
  private NamespaceManager names;

  private final MetadataAnnotationHandler metaHandler = new MetadataAnnotationHandler();
  private final AssetIDAnnotationHandler assetIdHandler = new AssetIDAnnotationHandler();

  @PostConstruct
  public void init() {
    if (logger.isDebugEnabled()) {
      logger.debug("The Trisotech Weaver class is initialized");
    }
  }

  public Weaver() {
    //
  }

  public Weaver(NamespaceManager names) {
    this.names = names;
    names.init();
  }

  /**
   * Weave out Trisotech-specific elements and where necessary, replace with KMDP-specific.
   */
  public Document weave(Document dox) {
    dox.getDocumentElement().setAttributeNS(W3C_XMLNS,
        XMLNS_PREFIX + "xsi",
        W3C_XSI);

    String surrPrefix = Registry
        .getPrefixforNamespace(Knowledge_Asset_Surrogate_2_0_XML_Syntax.getReferentId())
        .orElseThrow(IllegalStateException::new);
    String surrNamespace = Registry
        .getValidationSchema(Knowledge_Asset_Surrogate_2_0.getReferentId())
        .orElseThrow(IllegalStateException::new);

    dox.getDocumentElement().setAttributeNS(W3C_XMLNS,
        XMLNS_PREFIX + surrPrefix,
        surrNamespace);

    dox.getDocumentElement().setAttributeNS(W3C_XSI,
        "xsi:" + "schemaLocation",
        getSchemaLocations(dox, surrNamespace));

    // get metas
    NodeList metas = dox.getElementsByTagNameNS(TT_METADATA_NS, TT_SEMANTICLINK);
    weaveMetadata(asElementStream(metas));

    // copyLink
    NodeList copies = dox.getElementsByTagNameNS(TT_METADATA_NS, TT_COPYOFLINK);
    weaveMetadata(asElementStream(copies)
        .filter(this::isAcceleratorReuse));
    asElementStream(copies)
        .filter(reuse -> !this.isAcceleratorReuse(reuse))
        .forEach(reuse -> rewriteReuseLinks(reuse, dox));

    // reuseLink
    NodeList reuses = dox.getElementsByTagNameNS(TT_METADATA_NS, TT_REUSELINK);
    weaveMetadata(asElementStream(reuses)
        .filter(this::isAcceleratorReuse));
    asElementStream(reuses)
        .filter(reuse -> !this.isAcceleratorReuse(reuse))
        .forEach(reuse -> rewriteReuseLinks(reuse, dox));

    // rewrite custom attribute 'asset ID'
    // necessary in case the asset ID has to be extracted from the file
    weaveAssetId(dox);

    weaveNonBPMReferences(dox);

    // rewrite namespaces
    weaveNamespaces(dox);

    // rewrite namespace for 'import' tags
    weaveImport(dox);

    // rewrite href for 'inputData' and 'requiredInput' tags
    weaveExternalReferences(dox);

    // BUG ### CMMN Decision task with copy/reuse links do not have
    // a corresponding decisionRef -> decision
    if (isCMMN(dox)) {
      ensureCMMNDecisionTaskIntegrity(dox);
    }

    // CMMN Tasks with Data Mappings, supporting DMN (and BPMN) I/O bindings
    rewriteInputDataBindings(dox);
    rewriteOutputDataBindings(dox);

    return dox;
  }


  private void weaveAssetId(Document dox) {
    // looks for an Asset ID, and rewrites it as an annotation
    // note: avoid 'spurious' asset IDs derived from reuse elements
    asElementStream(dox.getElementsByTagNameNS(TT_METADATA_NS, TT_CUSTOM_ATTRIBUTE_ATTR))
        .filter(el -> names.getAssetIDKey().equals(el.getAttribute(KEY)))
        .filter(el -> asElementStream(el.getParentNode().getChildNodes())
            .noneMatch(sibling -> sibling.getLocalName().equals(TT_REUSELINK)))
        .forEach(this::rewriteAssetId);
  }

  private void weaveNonBPMReferences(Document dox) {
    asElementStream(dox.getElementsByTagNameNS(TT_METADATA_NS, TT_CUSTOM_ATTRIBUTE_ATTR))
        .filter(el -> el.getAttribute(KEY).startsWith(API4KP_PREFIX))
        .forEach(relEl -> {

          Optional<DependencyType> rel = DependencyTypeSeries.resolveTag(
              relEl.getAttribute(KEY).substring(API4KP_PREFIX.length()));

          if (rel.isPresent()) {
            var taskElem = (Element) relEl.getParentNode().getParentNode();
            if ("processTask".equals(taskElem.getLocalName())) {
              ResourceIdentifier tgtAsset = newVersionId(URI.create(relEl.getAttribute(VALUE)));
              Element processRefX = dox.createElementNS(CMMN_11_XMLNS, "processRefExpression");
              processRefX.setAttributeNS(CMMN_11_XMLNS, "language",
                  KnowledgeRepresentationLanguageSeries.API4KP.getReferentId().toString());
              processRefX.setTextContent(
                  rel.get().getTag() + " " + tgtAsset.getVersionId().toString());

              taskElem.appendChild(processRefX);
            }
          }
        });
  }

  private void rewriteAssetId(Element el) {
    String assetId = el.getAttribute(VALUE);
    ResourceIdentifier rid = assetIdHandler.getIdentifier(assetId);
    assetIdHandler.replaceProprietaryElement(el, rid);
  }


  private boolean isAcceleratorReuse(Element element) {
    return TT_ACCELERATOR_MODEL.equals(element.getAttribute("modelType"))
        && TT_ACCELERATOR_ENTTIY.equals(element.getAttribute("graphType"));
  }

  /**
   * DMN: m1:A hasRequirement m1:B* (reuse of m2:B) should be rewritten as m1:A hasRequirement m2:B
   * <p>
   * CMMN: Nothing to do - the XML also includes a proper external reference
   *
   * @param reuseLink the Element wrapping the link
   * @param dox       the model, as an XML document, that owns the element to be rewritten
   */
  private void rewriteReuseLinks(Element reuseLink, Document dox) {
    if (isCMMN(reuseLink.getOwnerDocument())) {
      // the reuseElement will be removed later.
      // Everything else has to stay
      return;
    }

    String targetUri = reuseLink.getAttribute("uri");
    // reuseLink -> extensionElement -> reusing modelElement
    Element reusingElement = ((Element) reuseLink.getParentNode().getParentNode());
    String parentId = reusingElement.getAttribute("id");

    XMLUtil.asElementStream(dox.getElementsByTagName("*"))
        .filter(el -> (el.getLocalName().equals("inputData")
            || el.getLocalName().equals("requiredInput")
            || el.getLocalName().equals("requiredKnowledge")
            || el.getLocalName().equals("encapsulatedDecision")
            || el.getLocalName().equals("outputDecision")
            || el.getLocalName().equals("inputDecision")
            || el.getLocalName().equals("requiredDecision"))
            && el.getAttribute("href").equals("#" + parentId))
        .forEach(element -> {
          Attr attr = element.getAttributeNode("href");
          attr.setValue(targetUri);
        });

    reusingElement.getParentNode().removeChild(reusingElement);
  }

  /**
   * Determine the annotation relationship (the property of the annotation triple) based on the '
   * concept (the object of the triple), and the context of use of the annotation.
   * <p>
   * For example, Data-related elements imply In_Terms_Of; Tasks imply Captures; and Decisions imply
   * Defines.
   *
   * @param el the element holding the annotation information
   * @return the {@link SemanticAnnotationRelTypeSeries} (proeprty) to be used in rewriting this
   * element
   */
  private SemanticAnnotationRelTypeSeries getSemanticAnnotationRelationship(Element el) {
    String uri = el.getAttribute("uri");

    if (DecisionTypeSeries.resolveId(uri).isPresent()) {
      return Captures;
    }
    if (KnowledgeAssetTypeSeries.resolveId(uri)
        .or(() -> ClinicalKnowledgeAssetTypeSeries.resolveId(uri)).isPresent()) {
      // TODO should we materialize api4kp:expresses?
      return null;
    }

    String grandparent = el.getParentNode().getParentNode().getNodeName();
    if (isDomainConcept(uri)) {
      switch (grandparent) {
        case "semantic:decision":
          return Defines;
        case "semantic:inputData":
        case "semantic:caseFileItem":
          return In_Terms_Of;
        case "semantic:casePlanModel":
          return Has_Primary_Subject;
        case "semantic:stage":
        case "semantic:case":
        case "semantic:task":
        case "semantic:decisionTask":
        case "semantic:processTask":
        case "semantic:caseTask":
        case "semantic:humanTask":
          return Captures;
        case "semantic:milestone":
          return Is_About;
        default:
      }
    }
    logger.warn("Unable to establish asset-concept relationship for concept {} on element type {}",
        uri, grandparent);
    return null;
  }


  private boolean isDomainConcept(String uriStr) {
    // the Terms service needs a UUID...
    URI uri = URI.create(uriStr);
    Answer<ConceptDescriptor> cdAns
        = Answer.ofTry(Optional.ofNullable(NameUtils.getTrailingPart(uri.toString())))
        .flatOpt(Util::ensureUUID)
        .flatMap(id -> terms.lookupTerm(id.toString()));
    if (!cdAns.isSuccess()) {
      return false;
    }
    String ns = cdAns.map(ConceptIdentifier::getNamespaceUri)
        .map(URI::toString)
        .orElse("");

    return names.isDomainConcept(ns);
  }


  private void ensureCMMNDecisionTaskIntegrity(Document dox) {
    XPathUtil x = new XPathUtil();
    asElementStream(x.xList(dox, "//cmmn:decisionTask[not(@decisionRef)]"))
        .forEach(decisionTask -> {
          Optional<Element> reuseNode = getReuseNode(x, decisionTask, TT_COPYOFLINK)
              .or(() -> getReuseNode(x, decisionTask, TT_REUSELINK));
          if (reuseNode.isEmpty()) {
            logger.warn("Found decision Task with neither external nor internal reference");
            return;
          }
          URI ref = URI.create(reuseNode.get().getAttribute("uri"));
          String decisionTaskName = reuseNode.get().getAttribute("itemName");
          String id = "_" + Util.uuid(ref.toString()).toString().replace("-", "");

          int numNamespaces = x.xList(dox, "//namespace::*").getLength();
          String prefix = "ns" + (1000 + numNamespaces + 1);

          dox.getDocumentElement()
              .setAttribute(XMLNS_PREFIX + prefix, URIUtil.normalizeURIString(ref));

          decisionTask.setAttribute("decisionRef", id);

          Element el = dox.createElementNS(CMMN_11_XMLNS, "decision");
          el.setAttribute("implementationType", "http://www.omg.org/spec/CMMN/DecisionType/DMN1");
          el.setAttribute("name", decisionTaskName);
          el.setAttribute("id", id);
          el.setAttribute("externalRef", prefix + ":" + ref.getFragment());

          dox.getDocumentElement().appendChild(el);
        });

  }

  private Optional<Element> getReuseNode(XPathUtil x, Node local, String linkType) {
    return Optional.ofNullable(x.xNode(local, ".//*[local-name()='" + linkType + "']"))
        .flatMap(n -> Util.as(n, Element.class));
  }


  /**
   * weaveInputs will rewrite the href attribute of the tags given to be KMDP hrefs instead of
   * Trisotech
   *
   * @param dox the XML document being rewritten
   */
  private void weaveExternalReferences(Document dox) {
    XMLUtil.asElementStream(dox.getElementsByTagName("*"))
        .filter(el -> (el.getLocalName().equals("inputData")
            || el.getLocalName().equals("requiredInput")
            || el.getLocalName().equals("requiredKnowledge")
            || el.getLocalName().equals("outputDecision")
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

  /**
   * Rewrites each annotation Element as an {@link Annotation} - a structured (subject, property,
   * object) where the subject is the asset, the object is the annotation concept, and the property
   * is an (optional) relationship that connects the asset to the concept.
   *
   * @param metas a stream of annotation Elements to be rewritten
   */
  private void weaveMetadata(Stream<Element> metas) {
    metas.forEach(
        el -> doInjectTerm(el,
            getSemanticAnnotationRelationship(el),
            getConceptIdentifiers(el))
    );
  }

  private List<ConceptIdentifier> getConceptIdentifiers(Element el) {
    // need to verify any URI values are valid -- no trisotech
    Attr uriAttr = el.getAttributeNode("uri");
    if (uriAttr.getValue().contains(TRISOTECH_COM)) {
      rewriteValue(uriAttr);
    }

    List<ConceptIdentifier> conceptIdentifiers = new ArrayList<>();
    ConceptIdentifier concept = null;
    try {
      ResourceIdentifier resourceIdentifier = SemanticIdentifier
          .newId(new URI(el.getAttribute("uri")));

      Answer<ConceptDescriptor> term = terms.lookupTerm(resourceIdentifier.getUuid().toString());
      if (term.getOptionalValue().isPresent()) {
        concept = term.get()
            .asConceptIdentifier();
      } else {
        if (logger.isWarnEnabled()) {
          logger.warn("WARNING: resource ID {} failed in lookupTerm "
              + "and will be removed from the file", resourceIdentifier.getUuid());
        }
        return conceptIdentifiers;
      }
    } catch (URISyntaxException | IllegalArgumentException e) {
      logger.error(String.format("%s%s", e.getMessage(), Arrays.toString(e.getStackTrace())));
    }
    conceptIdentifiers.add(concept);

    return conceptIdentifiers;
  }


  private void doInjectTerm(Element el,
      SemanticAnnotationRelTypeSeries defaultRel,
      List<ConceptIdentifier> rows) {
    if (!rows.isEmpty() && rows.stream().anyMatch(Objects::nonNull)) {
      List<Annotation> annos = metaHandler.getAnnotation(defaultRel, rows);
      metaHandler.replaceProprietaryElement(el, annos);
    }
  }


  private String getSchemaLocations(Document dox, String surrNamespace) {
    StringBuilder sb = new StringBuilder();

    sb.append(surrNamespace)
        .append(" ")
        .append("xsd/API4KP/surrogate/surrogate.xsd");

    if (isDMN(dox)) {
      sb.append(" ")
          .append(DMN_1_2.getReferentId())
          .append(" ").append(Registry.getValidationSchema(DMN_1_2.getReferentId())
              .orElseThrow(IllegalStateException::new));
    } else if (isCMMN(dox)) {
      sb.append(" ").append(CMMN_1_1)
          .append(" ").append(Registry.getValidationSchema(CMMN_1_1.getReferentId())
              .orElseThrow(IllegalStateException::new));
    }

    return sb.toString();
  }

  private boolean isCMMN(Document dox) {
    String baseNS = dox.getDocumentElement().getNamespaceURI();
    return baseNS.contains(CMMN);
  }

  private boolean isDMN(Document dox) {
    String baseNS = dox.getDocumentElement().getNamespaceURI();
    return baseNS.contains(DMN);
  }

}
