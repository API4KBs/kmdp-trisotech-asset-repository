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
import edu.mayo.kmdp.trisotechwrapper.components.redactors.Redactor;
import edu.mayo.kmdp.trisotechwrapper.components.weavers.Weaver;
import edu.mayo.kmdp.trisotechwrapper.config.TTWConfigParamsDef;
import edu.mayo.kmdp.trisotechwrapper.config.TTWEnvironmentConfiguration;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechExecutionArtifact;
import edu.mayo.kmdp.util.XPathUtil;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.stream.Collectors;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.surrogate.Dependency;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.Publication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Extract the data from the woven (by the Weaver) document to create KnowledgeAsset from model
 * data.
 */
@Component
public class BPMServiceIntrospector {

  @Autowired
  TTAPIAdapter client;

  @Autowired
  NamespaceManager names;

  @Autowired
  TTWEnvironmentConfiguration cfg;


  private final XPathUtil xPathUtil = new XPathUtil();

  public BPMServiceIntrospector() {
    //
  }

  /**
   * Generates a {@link KnowledgeAsset} Surrogate from the introspection of a BPM+ model, combined
   * with the information in its corresponding Trisotech's internal manifest. In particular, the
   * Surrogate is the surrogate of a technical Knowledge Asset for a Service scoped, defined and/or
   * referenced by the BPM+ model
   * <p>
   * FUTURE: Currently supports DMN Decision Services
   * <p>
   * Note that, at this point, the model document has already been standardized using the
   * {@link Redactor}, and (re)annotated using the {@link Weaver}
   *
   * @param dox      the BPM+ model artifact to extract metadata from
   * @param manifest the model's internal manifest
   * @return a KnowledgeAsset surrogate with metadata for a Service defined within that model
   */
  public KnowledgeAsset extractSurrogateFromDocument(
      Document dox,
      SemanticModelInfo manifest,
      ResourceIdentifier serviceAssetId) {

    var serviceName = manifest.getServiceFragmentName();

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
        .withCarriers(
            buildOpenAPICarrier(serviceAssetId, lifecycle, serviceName, manifest),
            buildSwaggerUICarrier(serviceAssetId, lifecycle, serviceName, manifest))
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

    return surrogate;
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
   * @param lifecycle   the publication status (FIXME - needs improvement)
   * @param serviceName the formal name of the Service (matches the Service Library)
   * @param manifest    the manifest of the model from which the service is built
   * @return the metadata for the service OpenAPI spec
   * @see ServiceLibraryHelper#tryResolveOpenApiUI(TrisotechExecutionArtifact,
   * TTWEnvironmentConfiguration)
   */
  private KnowledgeArtifact buildSwaggerUICarrier(
      ResourceIdentifier assetId, Publication lifecycle,
      String serviceName, SemanticModelInfo manifest) {
    var ka = new KnowledgeArtifact()
        .withArtifactId(defaultArtifactId(assetId, HTML))
        .withName(serviceName)
        .withLifecycle(lifecycle)
        .withLocalization(English)
        .withExpressionCategory(Interactive_Resource)
        .withRepresentation(rep(HTML))
        .withMimeType(codedRep(HTML));
    client.getExecutionArtifact(serviceName, manifest)
        .flatMap(exec -> ServiceLibraryHelper.tryResolveOpenApiUI(exec, cfg))
        .ifPresent(ka::withLocator);
    return ka;
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
   * @see ServiceLibraryHelper#tryResolveOpenApiSpec(TrisotechExecutionArtifact,
   * TTWEnvironmentConfiguration)
   */
  private KnowledgeArtifact buildOpenAPICarrier(
      ResourceIdentifier assetId, Publication lifecycle, String serviceName,
      SemanticModelInfo manifest) {
    //FIXME TT actually uses OAS3.x (3.0.1), but that needs to be registered first
    var synRep = rep(OpenAPI_2_X, JSON, Charset.defaultCharset(), Encodings.DEFAULT);

    var ka = new KnowledgeArtifact()
        .withArtifactId(defaultArtifactId(assetId, OpenAPI_2_X))
        .withName(serviceName)
        .withLifecycle(lifecycle)
        .withLocalization(English)
        .withExpressionCategory(Software)
        .withRepresentation(synRep)
        .withMimeType(codedRep(synRep));
    client.getExecutionArtifact(serviceName, manifest)
        .flatMap(exec -> ServiceLibraryHelper.tryResolveOpenApiSpec(exec, cfg))
        .ifPresent(ka::withLocator);
    return ka;
  }

  /**
   * Determines the Asset Id of the Model that provides the logic for the Service Asset under
   * consideration, which happens to be the same Model defining the Service itself
   *
   * @param manifest the Artifact metadata of the model
   * @return the Asset Id of the Model, if any
   */
  private Optional<ResourceIdentifier> getServiceModelDependency(SemanticModelInfo manifest) {
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
  private Element selectNode(Document dox, ResourceIdentifier serviceAssetId) {
    return selectDecisionServiceNode(dox, serviceAssetId)
        .or(() -> selectProcessNode(dox, serviceAssetId))
        .or(() -> selectAnyNode(dox, serviceAssetId))
        .filter(Element.class::isInstance)
        .map(Element.class::cast)
        .orElseThrow(() -> new IllegalStateException(
            "Unable to find expected serviceAssetId in XML : " + serviceAssetId
        ));
  }

  private Optional<Node> selectDecisionServiceNode(Document dox, ResourceIdentifier assetId) {
    return Optional.ofNullable(xPathUtil.xNode(dox,
            "//dmn:decisionService[.//@resourceId='" + assetId.getResourceId() + "']"))
        .or(() -> findAnonymousMatch("//dmn:decisionService", dox, assetId));
  }

  private Optional<Node> selectProcessNode(Document dox, ResourceIdentifier assetId) {
    return Optional.ofNullable(xPathUtil.xNode(dox,
            "//bpmn:process[.//@resourceId='" + assetId.getResourceId() + "']"))
        .or(() -> findAnonymousMatch("//bpmn:process", dox, assetId));
  }

  private Optional<Element> findAnonymousMatch(String xpath, Document dox,
      ResourceIdentifier assetId) {
    return asElementStream(xPathUtil.xList(dox, xpath))
        .filter(e -> mintAssetIdForAnonymous(
            cfg.getTyped(TTWConfigParamsDef.ASSET_NAMESPACE), e.getAttribute("id"), null)
            .getUuid().equals(assetId.getUuid()))
        .findFirst();
  }

  private Optional<? extends Node> selectAnyNode(Document dox, ResourceIdentifier assetId) {
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