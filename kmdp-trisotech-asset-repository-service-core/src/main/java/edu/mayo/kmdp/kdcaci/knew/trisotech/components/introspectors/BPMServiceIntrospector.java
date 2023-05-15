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
package edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors;

import static edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.BPMMetadataHelper.addSemanticAnnotations;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.BPMMetadataHelper.extractAnnotations;
import static edu.mayo.kmdp.trisotechwrapper.components.graph.PlacePathIndex.mintAssetIdForAnonymous;
import static edu.mayo.kmdp.trisotechwrapper.config.TTNotations.OPENAPI_JSON;
import static edu.mayo.kmdp.util.XMLUtil.asElementStream;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.defaultArtifactId;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.defaultSurrogateId;
import static org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries.Depends_On;
import static org.omg.spec.api4kp._20200801.taxonomy.iso639_2_languagecode._20190201.Language.English;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeartifactcategory._2020_01_20.KnowledgeArtifactCategory.Interactive_Resource;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeartifactcategory._2020_01_20.KnowledgeArtifactCategory.Software;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries.Plans_Processes_Pathways_And_Protocol_Definitions;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.ReSTful_Service_Specification;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.JSON;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.HTML;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.OpenAPI_2_X;
import static org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatusSeries.Draft;

import edu.mayo.kmdp.trisotechwrapper.TTAPIAdapter;
import edu.mayo.kmdp.trisotechwrapper.components.NamespaceManager;
import edu.mayo.kmdp.trisotechwrapper.components.SemanticModelInfo;
import edu.mayo.kmdp.trisotechwrapper.components.execution.ServiceLibraryHelper;
import edu.mayo.kmdp.trisotechwrapper.components.graph.PlacePathIndex;
import edu.mayo.kmdp.trisotechwrapper.components.redactors.Redactor;
import edu.mayo.kmdp.trisotechwrapper.components.weavers.Weaver;
import edu.mayo.kmdp.trisotechwrapper.config.TTWConfigParamsDef;
import edu.mayo.kmdp.trisotechwrapper.config.TTWEnvironmentConfiguration;
import edu.mayo.kmdp.util.StreamUtil;
import edu.mayo.kmdp.util.XPathUtil;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.surrogate.Dependency;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.Publication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Default implementation of @{@link ServiceIntrospector}
 */
public class BPMServiceIntrospector implements ServiceIntrospector {

  /**
   * Logger
   */
  private static final Logger logger = LoggerFactory
      .getLogger(BPMServiceIntrospector.class);

  /**
   * {@link TTAPIAdapter}, used to resolve model/model dependencies
   */
  @Nonnull
  protected final TTAPIAdapter client;

  /**
   * {@link NamespaceManager}, used to map internal IDs to enterprise IDs
   */
  @Nonnull
  protected final NamespaceManager names;

  /**
   * Environment configuration
   */
  @Nonnull
  protected final TTWEnvironmentConfiguration config;

  /**
   * Utility used to extract elements from a Model document
   */
  private final XPathUtil xPathUtil = new XPathUtil();

  public BPMServiceIntrospector(
      @Nonnull TTWEnvironmentConfiguration config,
      @Nonnull NamespaceManager names,
      @Nonnull TTAPIAdapter client) {
    this.config = config;
    this.client = client;
    this.names = names;
  }

  /**
   * Generates a {@link KnowledgeAsset} Surrogate from the introspection of a BPM+ model, combined
   * with the information in its corresponding Trisotech internal manifest. In particular, the
   * Surrogate is the surrogate of a technical Knowledge Asset for a Service scoped, defined and/or
   * referenced by the BPM+ model
   * <p>
   * FUTURE: Currently supports DMN Decision Services
   * <p>
   * Note that, at this point, the model document has already been standardized using the
   * {@link Redactor}, and (re)annotated using the {@link Weaver}
   *
   * @param serviceAssetId the Asset ID of the Service
   * @param manifest the model's internal manifest
   * @param dox      the BPM+ model artifact to extract metadata from
   * @return a KnowledgeAsset surrogate with metadata for a Service defined within that model
   */
  @Nonnull
  public Optional<KnowledgeAsset> introspectAsService(
      @Nonnull final ResourceIdentifier serviceAssetId,
      @Nonnull final SemanticModelInfo manifest,
      @Nonnull final Document dox) {

    if (isIncomplete(manifest)) {
      logger.warn("Incomplete manifest - unable to create Surrogate for {}", serviceAssetId);
      return Optional.empty();
    }

    var serviceName = Optional.ofNullable(manifest.getServiceFragmentName())
        .orElse("N/A");

    // Publication Status
    var lifecycle = getArtifactPublicationStatus();

    // towards the ideal
    var surrogate = new org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset()
        .withAssetId(serviceAssetId)
        .withName(manifest.getName())
        .withFormalCategory(Plans_Processes_Pathways_And_Protocol_Definitions)
        .withFormalType(ReSTful_Service_Specification)
        .withLifecycle(lifecycle)
        // carriers
        .withCarriers(buildCarriers(serviceAssetId, lifecycle, serviceName, manifest))
        .withSurrogate(
            new KnowledgeArtifact()
                .withArtifactId(defaultSurrogateId(serviceAssetId, Knowledge_Asset_Surrogate_2_0))
                .withRepresentation(rep(Knowledge_Asset_Surrogate_2_0, JSON))
                .withMimeType(codedRep(Knowledge_Asset_Surrogate_2_0, JSON))
        );

    var serviceNode = selectNode(dox, serviceAssetId);
    var annotations = extractAnnotations(serviceNode)
        .collect(Collectors.toList());
    addSemanticAnnotations(surrogate, annotations);

    getServiceModelDependency(manifest)
        .ifPresent(x -> surrogate.withLinks(
            new Dependency()
                .withHref(x)
                .withRel(Depends_On)));

    return Optional.of(surrogate);
  }

  /**
   * Builds the {@link KnowledgeArtifact} metadata records for the various forms of Service
   * Artifacts. Supports OpenAPI specs and TT's native interactive forms.
   * <p>
   * Records are built only if the Service Artifacts can be located, after a Service has been
   * deployed
   *
   * @param serviceAssetId the Asset ID of the Service
   * @param lifecycle the Publication status of the Service
   * @param serviceName the Name of the Service
   * @param manifest the internal Asset metadata
   * @return KnowledgeArtifact carrier metadata for the Service deployments
   */
  @Nonnull
  private Collection<KnowledgeArtifact> buildCarriers(
      @Nonnull final ResourceIdentifier serviceAssetId,
      @Nonnull final Publication lifecycle,
      @Nonnull final String serviceName,
      @Nonnull final SemanticModelInfo manifest) {
    return client.getExecutionArtifacts(serviceName, manifest)
        .flatMap(exec -> {
          var oas = ServiceLibraryHelper.tryResolveOpenApiSpec(exec)
              .map(loc ->
                  buildOpenAPICarrier(serviceAssetId, lifecycle, serviceName, manifest)
                      .withLocator(loc));
          var swg = ServiceLibraryHelper.tryResolveOpenApiUI(exec)
              .map(loc ->
                  buildSwaggerUICarrier(serviceAssetId, lifecycle, serviceName, manifest)
                      .withLocator(loc));
          return Stream.of(oas, swg);
        })
        .flatMap(StreamUtil::trimStream)
        .collect(Collectors.toList());
  }

  /**
   * Predicate
   *
   * @param manifest a Service Asset Manifest
   * @return true if and only if the Manifest does NOT contain enough information to generate a
   * {@link KnowledgeAsset}
   */
  private boolean isIncomplete(
      @Nonnull final SemanticModelInfo manifest) {
    return manifest.getServiceId() == null
        || manifest.getServiceKey() == null
        || manifest.getServiceFragmentId() == null
        || manifest.getServiceFragmentName() == null;
  }

  /**
   * Creates a {@link KnowledgeArtifact} metadata component for the Swagger UI variant manifestation
   * of the Service (spec)
   * <p>
   * Since it is always conceivable to create this artifact, the metadata will be generated
   * regardless of its availability. Then, the Service Library will be consulted, to check whether
   * the service is deployed, and a manifestation of the artifact can be acquired at a given URL.
   *
   * @param assetId     The Service Asset ID
   * @param lifecycle   the publication status (FIXME - may need improvement)
   * @param serviceName the formal name of the Service (matches the Service Library)
   * @param manifest    the manifest of the model from which the service is built
   * @return the metadata for the service OpenAPI spec
   */
  @Nonnull
  private KnowledgeArtifact buildSwaggerUICarrier(
      @Nonnull final ResourceIdentifier assetId,
      @Nonnull final Publication lifecycle,
      @Nonnull final String serviceName,
      @Nonnull final SemanticModelInfo manifest) {
    return new KnowledgeArtifact()
        .withArtifactId(defaultArtifactId(assetId, HTML, manifest.getVersion()))
        .withName(serviceName)
        .withLifecycle(lifecycle)
        .withLocalization(English)
        .withExpressionCategory(Interactive_Resource)
        .withRepresentation(rep(HTML))
        .withMimeType(codedRep(HTML));
  }

  /**
   * Creates a {@link KnowledgeArtifact} metadata component for the OpenAPI3+JSON manifestation of
   * the Service (spec)
   * <p>
   * Since it is always conceivable to create this artifact, the metadata will be generated
   * regardless of its availability. Then, the Service Library will be consulted, to check whether
   * the service is deployed, and a manifestation of the artifact can be acquired at a given URL.
   *
   * @param assetId     The Service Asset ID
   * @param lifecycle   the publication status (FIXME - needs improvement)
   * @param serviceName the formal name of the Service (matches the Service Library)
   * @param manifest    the manifest of the model from which the service is built
   * @return the metadata for the service OpenAPI spec
   */
  @Nonnull
  private KnowledgeArtifact buildOpenAPICarrier(
      @Nonnull final ResourceIdentifier assetId,
      @Nonnull final Publication lifecycle,
      @Nonnull final String serviceName,
      @Nonnull final SemanticModelInfo manifest) {
    //FIXME TT actually uses OAS3.x (3.0.1), but that needs to be registered first
    var synRep = rep(OpenAPI_2_X, JSON, Charset.defaultCharset(), Encodings.DEFAULT);
    return new KnowledgeArtifact()
        .withArtifactId(defaultArtifactId(assetId, OpenAPI_2_X, manifest.getVersion()))
        .withName(serviceName)
        .withLifecycle(lifecycle)
        .withLocalization(English)
        .withExpressionCategory(Software)
        .withRepresentation(synRep)
        .withMimeType(OPENAPI_JSON.getMimeType());
  }

  /**
   * Determines the Asset ID of the Model that provides the logic for the Service Asset under
   * consideration, which happens to be the same Model defining the Service itself
   *
   * @param manifest the Artifact metadata of the model
   * @return the Asset ID of the Model, if any
   */
  @Nonnull
  private Optional<ResourceIdentifier> getServiceModelDependency(
      @Nonnull final SemanticModelInfo manifest) {
    return names.modelToAssetId(manifest);
  }


  /* ----------------------------------------------------------------------------------------- */


  /**
   * Selects the node in the XML document that better represents the Service Assets
   * <p>
   * In DMN, dmn:decisionService nodes are mapped to deployable artifacts; in BPMN, bpmn:process
   * nodes are mapped instead. Assumes that the node of interest is annotated with the given
   * serviceAssetId
   *
   * @param dox            the model artifact
   * @param serviceAssetId the serviceAssetId
   * @return the XML Element that represents the part of the model exposed as a service
   */
  @Nonnull
  private Element selectNode(
      @Nonnull final Document dox,
      @Nonnull final ResourceIdentifier serviceAssetId) {
    return selectDecisionServiceNode(dox, serviceAssetId)
        .or(() -> selectProcessNode(dox, serviceAssetId))
        .or(() -> selectAnyNode(dox, serviceAssetId))
        .filter(Element.class::isInstance)
        .map(Element.class::cast)
        .orElseThrow(() -> new IllegalStateException(
            "Unable to find expected serviceAssetId in XML : " + serviceAssetId
        ));
  }

  /**
   * Looks up the {@link Node} annotated with a given Service Asset ID in a DMN Document, or, tries
   * to establish a match with the Node whose ID was used to generate an the anonymous Asset ID
   * <p>
   * Looks for a dmn:decisionService Node
   *
   * @param dox     the Document expected to match the Asset ID (which could be 'anonymous')
   * @param assetId the ID to look up
   * @return the Node, if found
   * @see #findAnonymousMatch(String, Document, ResourceIdentifier)
   */
  @Nonnull
  private Optional<Node> selectDecisionServiceNode(
      @Nonnull final Document dox,
      @Nonnull final ResourceIdentifier assetId) {
    return Optional.ofNullable(xPathUtil.xNode(dox,
            "//dmn:decisionService[.//@resourceId='" + assetId.getResourceId() + "']"))
        .or(() -> findAnonymousMatch("//dmn:decisionService", dox, assetId));
  }

  /**
   * Looks up the {@link Node} annotated with a given Service Asset ID in a BPMN Document, or, tries
   * to establish a match with the Node whose ID was used to generate an the anonymous Asset ID
   * <p>
   * Looks for a bpmn:Process Node
   *
   * @param dox     the Document expected to match the Asset ID (which could be 'anonymous')
   * @param assetId the ID to look up
   * @return the Node, if found
   * @see #findAnonymousMatch(String, Document, ResourceIdentifier)
   */
  @Nonnull
  private Optional<Node> selectProcessNode(
      @Nonnull final Document dox,
      @Nonnull final ResourceIdentifier assetId) {
    return Optional.ofNullable(xPathUtil.xNode(dox,
            "//bpmn:process[.//@resourceId='" + assetId.getResourceId() + "']"))
        .or(() -> findAnonymousMatch("//bpmn:process", dox, assetId));
  }

  /**
   * Matches the node(s) selected by an XPath expression against a Service Asset ID. Assumes that
   * the Node id was used to generate an anonymous Service Asset ID, and compares the anonymous ID
   * to the given one, to see if the given Asset ID matches the node
   *
   * @param xpath   the Node selector
   * @param dox     the Document to apply the selector to
   * @param assetId the target Asset ID
   * @return an Element, if the Element's ID, when used to generate an anonymous Service Asset ID,
   * matches the given assetId
   * @see PlacePathIndex#mintAssetIdForAnonymous(URI, String, String, String)
   */
  @Nonnull
  private Optional<Element> findAnonymousMatch(
      @Nonnull final String xpath,
      @Nonnull final Document dox,
      @Nonnull final ResourceIdentifier assetId) {
    return asElementStream(xPathUtil.xList(dox, xpath))
        .filter(e -> {
          var anon = mintAssetIdForAnonymous(
              config.getTyped(TTWConfigParamsDef.ASSET_NAMESPACE),
              e.getAttribute("id"),
              null,
              null);
          return anon.getUuid().equals(assetId.getUuid());
        }).findFirst();
  }

  /**
   * Looks up the {@link Node} annotated with a given Service Asset ID in a XML Document, anywhere
   *
   * @param dox     the Document expected to contain the Asset ID
   * @param assetId the ID to look up
   * @return the Node, if found
   */
  @Nonnull
  private Optional<? extends Node> selectAnyNode(
      @Nonnull final Document dox,
      @Nonnull final ResourceIdentifier assetId) {
    return Optional.ofNullable(xPathUtil.xNode(dox,
        "//*[descendant::*/@resourceId='" + assetId.getResourceId() + "']"));
  }


  /**
   * TODO - revisit based on the publication/versioning strategy being developed
   *
   * @return
   */
  private Publication getArtifactPublicationStatus() {
    // FIXME Consider Published/Unpublished based on the presence in the SL
    return new Publication()
        .withPublicationStatus(Draft);
  }
}