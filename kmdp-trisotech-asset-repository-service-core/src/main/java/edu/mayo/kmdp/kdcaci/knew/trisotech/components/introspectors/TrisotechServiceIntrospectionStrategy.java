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

import static edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.TrisotechMetadataHelper.addSemanticAnnotations;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.TrisotechMetadataHelper.extractAnnotations;
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
import static org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatusSeries.Published;

import edu.mayo.kmdp.kdcaci.knew.trisotech.IdentityMapper;
import edu.mayo.kmdp.trisotechwrapper.TrisotechWrapper;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.util.XPathUtil;
import java.nio.charset.Charset;
import java.util.Optional;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.surrogate.Dependency;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.Publication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Extract the data from the woven (by the Weaver) document to create KnowledgeAsset from model
 * data.
 */
@Component
public class TrisotechServiceIntrospectionStrategy {

  private static final Logger logger = LoggerFactory
      .getLogger(TrisotechServiceIntrospectionStrategy.class);

  @Autowired
  TrisotechWrapper client;

  @Autowired
  IdentityMapper mapper;

  private final XPathUtil xPathUtil = new XPathUtil();

  public TrisotechServiceIntrospectionStrategy() {
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
   * {@link edu.mayo.kmdp.kdcaci.knew.trisotech.components.redactors.Redactor}, and (re)annotated
   * using the {@link edu.mayo.kmdp.kdcaci.knew.trisotech.components.weavers.Weaver}
   *
   * @param dox      the BPM+ model artifact to extract metadata from
   * @param manifest the model's internal manifest
   * @return a KnowledgeAsset surrogate with metadata for a Service defined within that model
   */
  public KnowledgeAsset extractSurrogateFromDocument(
      Document dox,
      TrisotechFileInfo manifest,
      ResourceIdentifier serviceAssetId) {

    var serviceNode = selectNode(dox, serviceAssetId);
    var serviceName = mintServiceName(manifest, serviceNode.getAttribute("name"));

    // Publication Status
    var lifecycle = getArtifactPublicationStatus();

    // towards the ideal
    var surrogate = new org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset()
        .withAssetId(serviceAssetId)
        .withName(serviceName)
        .withFormalCategory(Plans_Processes_Pathways_And_Protocol_Definitions)
        .withFormalType(ReSTful_Service_Specification)
        .withLifecycle(lifecycle)
        // carriers
        .withCarriers(
            buildOpenAPICarrier(serviceAssetId, lifecycle, serviceName),
            buildSwaggerUICarrier(serviceAssetId, lifecycle, serviceName))
        .withSurrogate(
            new KnowledgeArtifact()
                .withArtifactId(defaultSurrogateId(serviceAssetId, Knowledge_Asset_Surrogate_2_0))
                .withRepresentation(rep(Knowledge_Asset_Surrogate_2_0, JSON))
                .withMimeType(codedRep(Knowledge_Asset_Surrogate_2_0, JSON))
        );

    var annotations = extractAnnotations(serviceNode);
    addSemanticAnnotations(surrogate, annotations);

    getServiceModelDependency(manifest)
        .ifPresent(x -> surrogate.withLinks(
            new Dependency()
                .withHref(x)
                .withRel(Depends_On)));

    return surrogate;
  }

  private KnowledgeArtifact buildSwaggerUICarrier(
      ResourceIdentifier assetId, Publication lifecycle,
      String serviceName) {
    return new KnowledgeArtifact()
        .withArtifactId(defaultArtifactId(assetId, HTML))
        .withName(serviceName)
        .withLifecycle(lifecycle)
        .withLocalization(English)
        .withExpressionCategory(Interactive_Resource)
        .withRepresentation(rep(HTML))
        .withMimeType(codedRep(HTML));
  }

  private KnowledgeArtifact buildOpenAPICarrier(
      ResourceIdentifier assetId, Publication lifecycle, String serviceName) {
    //FIXME TT actually uses OAS3.x (3.0.1), but that needs to be registered first
    var synRep = rep(OpenAPI_2_X, JSON, Charset.defaultCharset(), Encodings.DEFAULT);

    return new KnowledgeArtifact()
        .withArtifactId(defaultArtifactId(assetId, OpenAPI_2_X))
        .withName(serviceName)
        .withLifecycle(lifecycle)
        .withLocalization(English)
        .withExpressionCategory(Software)
        .withRepresentation(synRep)
        .withMimeType(codedRep(synRep));
  }

  /**
   * Determines the Asset Id of the Model that provides the logic for the Service Asset under
   * consideration, which happens to be the same Model defining the Service itself
   *
   * @param manifest the Artifact metadata of the model
   * @return the Asset Id of the Model, if any
   */
  private Optional<ResourceIdentifier> getServiceModelDependency(TrisotechFileInfo manifest) {
    return mapper.resolveEnterpriseAssetID(manifest.getId());
  }


  /* ----------------------------------------------------------------------------------------- */


  private Element selectNode(Document dox, ResourceIdentifier assetId) {
    return (Element) xPathUtil.xNode(dox,
        "//dmn:decisionService[.//@resourceId='" + assetId.getResourceId() + "']");
  }

  public static String mintServiceName(TrisotechFileInfo manifest, String serviceName) {
    var n = manifest.getName();
    if (serviceName != null) {
      n = n + " - " + serviceName;
    }
    n = n + " (API)";
    return n;
  }

  private Publication getArtifactPublicationStatus() {
    // FIXME Consider Published/Unpublished based on the presence in the SL
    return new Publication()
        .withPublicationStatus(Published);
  }
}