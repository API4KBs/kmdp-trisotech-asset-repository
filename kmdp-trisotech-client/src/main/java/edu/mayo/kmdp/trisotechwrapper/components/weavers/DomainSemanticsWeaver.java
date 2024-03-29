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
package edu.mayo.kmdp.trisotechwrapper.components.weavers;

import static edu.mayo.kmdp.trisotechwrapper.components.weavers.DataBindingManipulator.rewriteInputDataBindings;
import static edu.mayo.kmdp.trisotechwrapper.components.weavers.DataBindingManipulator.rewriteOutputDataBindings;
import static edu.mayo.kmdp.trisotechwrapper.config.TTConstants.XMLNS_PREFIX;
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

import edu.mayo.kmdp.registry.Registry;
import edu.mayo.kmdp.trisotechwrapper.components.DefaultNamespaceManager;
import edu.mayo.kmdp.trisotechwrapper.components.NamespaceManager;
import edu.mayo.kmdp.trisotechwrapper.components.operators.KEMSelectorHelper;
import edu.mayo.kmdp.trisotechwrapper.config.TTConstants;
import edu.mayo.kmdp.trisotechwrapper.config.TTLanguages;
import edu.mayo.kmdp.trisotechwrapper.config.TTWConfigParamsDef;
import edu.mayo.kmdp.trisotechwrapper.config.TTWEnvironmentConfiguration;
import edu.mayo.kmdp.util.URIUtil;
import edu.mayo.kmdp.util.XMLUtil;
import edu.mayo.ontology.taxonomies.kao.decisiontype.DecisionTypeSeries;
import edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.omg.spec.api4kp._20200801.id.ConceptIdentifier;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.Term;
import org.omg.spec.api4kp._20200801.surrogate.Annotation;
import org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries;
import org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyType;
import org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

/**
 * Default implementation of the {@link Weaver} interface, used to rewrite Trisotech-specific
 * elements in other standards not directly supported by Trisotech (e.g. API4KP, MVF), and/or inject
 * domain-specific semantics by means of annotations based on domain ontologies.
 * <p>
 * TODO: this implementation supports DMN 1.2 and CMMN 1.1, and should be refactored/modularized
 * to support other BPM+ languages/versions
 */
public class DomainSemanticsWeaver implements Weaver {

  /**
   * Logger
   */
  private static final Logger logger = LoggerFactory.getLogger(DomainSemanticsWeaver.class);

  /**
   * NamespaceManager helper - used to rewrite resource URIs
   */
  @Nonnull
  private final NamespaceManager names;

  /**
   * Delegate
   */
  private final @Nonnull MetadataAnnotationHandler metaHandler = new MetadataAnnotationHandler();
  /**
   * Delegate
   */
  private final @Nonnull AssetIDAnnotationHandler assetIdHandler = new AssetIDAnnotationHandler();

  /**
   * The custom attribute name used for Asset Ids
   */
  private final String assetIDKey;

  /**
   * The custom attribute name used for Service Asset Ids
   */
  private final String serviceAssetIDKey;

  public DomainSemanticsWeaver() {
    this(new TTWEnvironmentConfiguration());
  }

  /**
   * Constructor
   */
  public DomainSemanticsWeaver(
      @Nullable TTWEnvironmentConfiguration cfg) {
    var effectiveCfg =
        cfg != null ? cfg : new TTWEnvironmentConfiguration();
    this.names = new DefaultNamespaceManager(effectiveCfg);

    this.assetIDKey = effectiveCfg.getTyped(TTWConfigParamsDef.ASSET_ID_ATTRIBUTE);
    this.serviceAssetIDKey = effectiveCfg.getTyped(TTWConfigParamsDef.SERVICE_ASSET_ID_ATTRIBUTE);

    if (logger.isDebugEnabled()) {
      logger.debug("The Trisotech Weaver class is initialized");
    }
  }


  @Override
  @Nonnull
  public Document weave(
      @Nonnull final Document dox,
      @Nonnull Function<String, Optional<Document>> resolver) {
    // manual annotations
    weaveSemanticLinks(dox);

    // accelerators, KEMs and other model/model reuse
    weaveReuseLinks(dox, resolver);

    // rewrite custom attribute 'asset ID'
    // necessary in case the asset ID has to be extracted from the file
    weaveAssetId(dox);

    // rewrite namespaces
    weaveAPI4KPNamespaces(dox);
    weaveNamespaces(dox);

    // rewrite namespace for 'import' tags
    weaveImport(dox);

    // rewrite href for 'inputData' and 'requiredInput' tags
    weaveExternalReferences(dox);

    // CMMN Tasks with Data Mappings, supporting DMN (and BPMN) I/O bindings
    rewriteInputDataBindings(dox);
    rewriteOutputDataBindings(dox);

    weaveNonBPMReferences(dox);

    return dox;
  }

  /**
   * Rewrites TT semanticLink elements, used to connect models to accelerators, as Annotations
   * <p>
   * Covers the scenario where a BPM model element is annotated manually with a SemanticLink
   *
   * @param dox the Document to be woven
   */
  private void weaveSemanticLinks(
      @Nonnull final Document dox) {
    NodeList metas = dox.getElementsByTagNameNS(
        TTConstants.TT_METADATA_NS, TTConstants.TT_SEMANTICLINK);
    asElementStream(metas)
        .forEach(this::weaveElementMetadata);
  }

  /**
   * Rewrites TT reuse/copy elements as semantic Annotations, under the assumption that the reused
   * model is a Semantic Accelerator or a Graph
   * <p>
   * Covers the scenario where an Accelerator/Graph element is dragged&dropped into a BPM model
   *
   * @param dox      the Document to be woven
   * @param resolver the URI (string) to Document mapper used to pull referenced models, or
   *                 fragments thereof, containing the fragments to be woven
   */
  private void weaveReuseLinks(
      @Nonnull final Document dox,
      @Nonnull final Function<String, Optional<Document>> resolver) {
    NodeList copies = dox.getElementsByTagNameNS(
        TTConstants.TT_METADATA_NS, TTConstants.TT_COPYOFLINK);
    asElementStream(copies)
        .forEach(reuseByCopy -> weaveReuse(reuseByCopy, dox, resolver));

    NodeList reuses = dox.getElementsByTagNameNS(
        TTConstants.TT_METADATA_NS, TTConstants.TT_REUSELINK);
    asElementStream(reuses)
        .forEach(reuseByRef -> weaveReuse(reuseByRef, dox, resolver));
  }

  /**
   *
   */
  private void weaveReuse(
      @Nonnull final Element reuse,
      @Nonnull final Document dox,
      Function<String, Optional<Document>> resolver) {
    boolean isAccelerator = isAcceleratorReuse(reuse);
    boolean isKem = isKEMTerm(reuse);
    if (isAccelerator) {
      weaveElementMetadata(reuse);
    } else if (isKem) {
      weaveElementMetadata(reuse, resolver, KEMSelectorHelper::lookupMVFEntryURI, true);
    } else {
      rewriteReuseLinks(reuse, dox);
    }
  }

  /**
   * Rewrites an annotation Element as an {@link Annotation} - a structured (subject, property,
   * object) where the subject is the asset, the object is the annotation concept, and the property
   * is an (optional) relationship that connects the asset to the concept.
   *
   * @param el an annotation Element to be rewritten
   */
  private void weaveElementMetadata(
      @Nonnull final Element el) {
    weaveElementMetadata(
        el,
        s -> Optional.empty(),
        (d, u) -> Optional.of(u),
        false);
  }

  /**
   * Rewrites an annotation Element as an {@link Annotation} - a structured (subject, property,
   * object) where the subject is the asset, the object is the annotation concept, and the property
   * is an (optional) relationship that connects the asset to the concept.
   *
   * @param el                an annotation Element to be rewritten
   * @param resolver          function used to look up the externally referenced document
   * @param mapper            function to look up the concept ID (fragment) to be woven
   * @param needsRelationship if true, will only weave a concept if an explicit relationship can be
   *                          determined
   */
  private void weaveElementMetadata(
      @Nonnull final Element el,
      @Nonnull final Function<String, Optional<Document>> resolver,
      @Nonnull final BiFunction<Document, URI, Optional<URI>> mapper,
      boolean needsRelationship) {

    var concept = getConceptIdentifier(
        el,
        resolver,
        mapper);

    var rel = concept.map(c -> getSemanticAnnotationRelationship(el, c.getConceptId()));
    if (needsRelationship && rel.isEmpty()) {
      el.getParentNode().removeChild(el);
    } else {
      metaHandler.replaceProprietaryElement(
          el,
          rel.orElse(null),
          concept.orElse(null));
    }

  }


  /**
   * Adds the API4KP Surrogate namespaces, prefix declaration and schema location.
   * <p>
   * The Surrogate namespace declares the {@link Annotation} class, which is used to rewrite the
   * proprietary Annotations
   *
   * @param dox the Document to be woven
   */
  private void weaveAPI4KPNamespaces(
      @Nonnull final Document dox) {
    dox.getDocumentElement().setAttributeNS(TTConstants.W3C_XMLNS,
        XMLNS_PREFIX + "xsi",
        TTConstants.W3C_XSI);

    String surrPrefix = Registry
        .getPrefixforNamespace(Knowledge_Asset_Surrogate_2_0_XML_Syntax.getReferentId())
        .orElseThrow(IllegalStateException::new);
    String surrNamespace = Registry
        .getValidationSchema(Knowledge_Asset_Surrogate_2_0.getReferentId())
        .orElseThrow(IllegalStateException::new);

    dox.getDocumentElement().setAttributeNS(TTConstants.W3C_XMLNS,
        XMLNS_PREFIX + surrPrefix,
        surrNamespace);

    dox.getDocumentElement().setAttributeNS(TTConstants.W3C_XSI,
        "xsi:" + "schemaLocation",
        getSchemaLocations(dox, surrNamespace));
  }


  /**
   * Rewrites Services and Asset Ids, natively serialized as TT customAttributes, using the API4KP
   * {@link ResourceIdentifier} datatype
   *
   * @param dox the Document to be woven
   */
  private void weaveAssetId(
      @Nonnull final Document dox) {
    // looks for an Asset ID, and rewrites it as an annotation
    // note: avoid 'spurious' asset IDs derived from reuse elements
    asElementStream(dox.getElementsByTagNameNS(
        TTConstants.TT_METADATA_NS, TTConstants.TT_CUSTOM_ATTRIBUTE_ATTR))
        // matches Asset Ids
        .filter(el -> Objects.equals(el.getAttribute(TTConstants.KEY), assetIDKey))
        .filter(el -> asElementStream(el.getParentNode().getChildNodes())
            .noneMatch(sibling -> sibling.getLocalName().equals(TTConstants.TT_REUSELINK)))
        .forEach(assetIdHandler::replaceProprietaryElement);

    asElementStream(dox.getElementsByTagNameNS(
        TTConstants.TT_METADATA_NS, TTConstants.TT_CUSTOM_ATTRIBUTE_ATTR))
        // matches Service Asset Ids
        .filter(el -> Objects.equals(el.getAttribute(TTConstants.KEY), serviceAssetIDKey))
        .filter(el -> asElementStream(el.getParentNode().getChildNodes())
            .noneMatch(sibling -> sibling.getLocalName().equals(TTConstants.TT_REUSELINK)))
        .forEach(assetIdHandler::replaceProprietaryElement);
  }


  /**
   * Predicate
   * <p>
   * Determines whether a copy/reuse element points to an entity in a Semantic Accelerator
   *
   * @param element the Element to be tested
   */
  private boolean isAcceleratorReuse(
      @Nonnull final Element element) {
    return TTConstants.TT_ACCELERATOR_META_CLASS.equals(element.getAttribute("modelType"))
        && TTConstants.TT_ACCELERATOR_ENTTIY.equals(element.getAttribute("graphType"));
  }

  /**
   * Predicate
   * <p>
   * Determines whether a copy/reuse element points to an entity in a KEM Model
   *
   * @param element the Element to be tested
   */
  private boolean isKEMTerm(
      @Nonnull final Element element) {
    return TTConstants.TT_KEM_META_CLASS.equals(element.getAttribute("modelType"))
        && TTConstants.TT_KEM_TERM.equals(element.getAttribute("graphType"));
  }

  /**
   * Normalizes the reuse links, removing one level of indirection, as follows:
   * <p>
   * DMN: m1:A hasRequirement m1:B* (reuse of m2:B) should be rewritten as m1:A hasRequirement m2:B
   * <p>
   * CMMN: Nothing to do - the XML also includes a proper external reference
   *
   * @param reuseLink the Element wrapping the link
   * @param dox       the model, as an XML document, that owns the element to be rewritten
   */
  private void rewriteReuseLinks(
      @Nonnull final Element reuseLink,
      @Nonnull final Document dox) {
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
   * Determine the annotation relationship (the property of the annotation triple) based on the
   * concept (the object of the triple), and the context of use of the annotation.
   * <p>
   * For example, Data-related elements imply In_Terms_Of; Tasks imply Captures; and Decisions imply
   * Defines.
   *
   * @param el     the element holding the annotation information
   * @param tgtUri the URI of the entity to be related
   * @return the {@link SemanticAnnotationRelTypeSeries} (property) to be used in rewriting this
   * element
   */
  @Nullable
  private SemanticAnnotationRelTypeSeries getSemanticAnnotationRelationship(
      @Nonnull final Element el,
      @Nonnull final URI tgtUri) {

    if (DecisionTypeSeries.resolveId(tgtUri).isPresent()) {
      return Captures;
    }
    if (KnowledgeAssetTypeSeries.resolveId(tgtUri)
        .or(() -> ClinicalKnowledgeAssetTypeSeries.resolveId(tgtUri)).isPresent()) {
      return null;
    }

    String grandparent = el.getParentNode().getParentNode().getNodeName();
    if (isDomainConcept(tgtUri)) {
      switch (grandparent) {
        case "semantic:decision":
        case "semantic:dataOutput":
          return Defines;
        case "semantic:inputData":
        case "semantic:dataInput":
        case "semantic:caseFileItem":
          return In_Terms_Of;
        case "semantic:casePlanModel":
        case "semantic:relationship": // BPMN top level / canvas annotations
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
        case "triso:graphTerm":
          return null;
        default:
      }
    }
    logger.warn("Unable to establish asset-concept relationship for concept {} on element type {}",
        tgtUri, grandparent);
    return null;
  }


  /**
   * Predicate
   * <p>
   * Determines if the given URI denotes or evokes a domain-specific concept (e.g. clinical)
   *
   * @param uri the concept or referent ID
   * @return true if uriStr resolves to a domain-specific concept
   */
  private boolean isDomainConcept(
      @Nonnull final URI uri) {
    return names.isDomainConcept(Term.newTerm(uri).getNamespaceUri());
  }


  /**
   * Rewrites the href attribute of the tags given to be KMDP hrefs instead of Trisotech
   *
   * @param dox the XML document being rewritten
   */
  private void weaveExternalReferences(
      @Nonnull final Document dox) {
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
          rewriteModelReference(attr);
        });
  }

  /**
   * Rewrites the model/model imports to use the Platform's artifact namespace instead of the vendor
   * native one
   *
   * @param dox the XML Document being processed
   */
  private void weaveImport(
      @Nonnull final Document dox) {
    asElementStream(dox.getElementsByTagName("*"))
        .filter(el -> el.getLocalName().equals(TTConstants.DMN_IMPORT))
        // rewrite the 'import model', but do not rewrite the 'import library'
        // FUTURE: at least not until FEEL libs become Assets
        .filter(el -> !TTConstants.TT_LIBRARIES.equals(el.getAttribute(TTConstants.DMN_IMPORTTYPE)))
        .forEach(el -> {
              Attr attr = el.getAttributeNode("namespace");
              rewriteModelReference(attr);
            }
        );
  }

  /**
   * Rewrites the model target namespaces to use the Platform's artifact namespace instead of the
   * vendor native one
   *
   * @param dox the XML Document being processed
   */
  private void weaveNamespaces(
      @Nonnull final Document dox) {
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
        rewriteModelReference(attr);
      }

    }

    // add attribute for assets namespace
    Attr assetsAttr = dox.createAttributeNS(TTConstants.W3C_XMLNS, "xmlns:assets");
    assetsAttr.setValue(names.getAssetNamespace().toString());
    dox.getDocumentElement().setAttributeNodeNS(assetsAttr);
  }

  /**
   * Used to rewrite the value of an attribute expected to be a URI from Trisotech URI to KMDP URI.
   * Also remove leading underscore of identifier.
   *
   * @param attr the attribute of a Document tag
   * @return the rewritten value, as set on the XML Attribute node
   */
  private String rewriteModelReference(
      @Nonnull final Attr attr) {
    var value = attr.getValue();
    var rewritten =
        value.replace(TTConstants.TT_BASE_MODEL_URI, names.getArtifactNamespace().toString());
    attr.setValue(rewritten);
    return rewritten;
  }


  /**
   * Extracts the ConceptIdentifier from the uri attribute of an element, expected to represent a
   * semantic term
   *
   * @param el       the XML element that evokes a concept via its URI
   * @param resolver the URI (string) to Document mapper used to pull referenced models, or
   *                 fragments thereof, containing the fragments to be woven
   */
  @Nonnull
  private Optional<ConceptIdentifier> getConceptIdentifier(
      @Nonnull final Element el,
      Function<String, Optional<Document>> resolver,
      BiFunction<Document, URI, Optional<URI>> mapper) {
    // need to verify any URI values are valid -- no trisotech
    var fragmentUri = URI.create(el.getAttribute("uri")
        .replace(TTConstants.TT_BASE_MODEL_URI, names.getArtifactNamespace().toString())
        .replace("#_", "#"));
    var modelURI = el.getAttribute("modelURI");
    var rewritten = resolver.apply(modelURI)
        .flatMap(dox -> mapper.apply(dox, fragmentUri))
        .orElse(fragmentUri);

    try {
      var concept = Term.newTerm(rewritten)
          .asConceptIdentifier()
          .withName(el.getAttribute("itemName"));
      concept.withNamespaceUri(URIUtil.normalizeURI(concept.getNamespaceUri()));
      return Optional.of(concept);
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      return Optional.empty();
    }
  }


  /**
   * Creates a schemaLocation String, consisting of URI/URL pairs
   *
   * @param dox           the document, used to determine which schemas need added
   * @param surrNamespace the API4KP surrogate URI
   * @return a schemaLocation attribute value suitable for the dox Document
   */
  @Nonnull
  private String getSchemaLocations(
      @Nonnull final Document dox,
      @Nonnull final String surrNamespace) {
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

  /**
   * Predicate
   * <p>
   * Quick-detects whether a model is a CMMN model, based on the document's root element namespace
   *
   * @param dox the Document to be classified
   * @return true if the root element is an element in the CMMN schema
   */
  private boolean isCMMN(
      @Nonnull final Document dox) {
    String baseNS = dox.getDocumentElement().getNamespaceURI();
    return baseNS.toLowerCase().contains(TTLanguages.CMMN.getTag());
  }

  /**
   * Predicate
   * <p>
   * Quick-detects whether a model is a DMN model, based on the document's root element namespace
   *
   * @param dox the Document to be classified
   * @return true if the root element is an element in the DMN schema
   */
  private boolean isDMN(
      @Nonnull final Document dox) {
    String baseNS = dox.getDocumentElement().getNamespaceURI();
    return baseNS.toLowerCase().contains(TTLanguages.DMN.getTag());
  }


  private void weaveNonBPMReferences(
      @Nonnull final Document dox) {
    asElementStream(dox.getElementsByTagNameNS(
        TTConstants.TT_METADATA_NS, TTConstants.TT_CUSTOM_ATTRIBUTE_ATTR))
        .filter(el -> el.getAttribute(TTConstants.KEY).startsWith(TTConstants.API4KP_PREFIX))
        .forEach(relEl -> {

          Optional<DependencyType> rel = DependencyTypeSeries.resolveTag(
              relEl.getAttribute(TTConstants.KEY).substring(TTConstants.API4KP_PREFIX.length()));

          if (rel.isPresent()) {
            var taskElem = (Element) relEl.getParentNode().getParentNode();
            if ("processTask".equals(taskElem.getLocalName())) {
              ResourceIdentifier tgtAsset = newVersionId(URI.create(relEl.getAttribute(
                  TTConstants.VALUE)));
              Element processRefX = dox.createElementNS(TTConstants.CMMN_11_XMLNS,
                  "processRefExpression");
              processRefX.setAttributeNS(TTConstants.CMMN_11_XMLNS, "language",
                  KnowledgeRepresentationLanguageSeries.API4KP.getReferentId().toString());
              processRefX.setTextContent(
                  rel.get().getTag() + " " + tgtAsset.getVersionId().toString());

              taskElem.appendChild(processRefX);
            }
          }
        });
  }


}
