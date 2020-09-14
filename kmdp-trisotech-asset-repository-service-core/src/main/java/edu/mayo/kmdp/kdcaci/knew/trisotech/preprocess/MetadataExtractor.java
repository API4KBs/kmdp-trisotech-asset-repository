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
package edu.mayo.kmdp.kdcaci.knew.trisotech.preprocess;

import static edu.mayo.kmdp.util.JaxbUtil.marshall;
import static edu.mayo.kmdp.util.XMLUtil.loadXMLDocument;
import static java.util.Collections.singletonList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.util.JSonUtil;
import edu.mayo.kmdp.util.JaxbUtil;
import edu.mayo.kmdp.util.properties.jaxb.JaxbConfig;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import javax.annotation.PostConstruct;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.SurrogateHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

/**
 * MetadataExtractor takes the output of the Weaver and the information of the artifact to create
 * a KnowledgeAsset surrogate.
 */
@Component
public class MetadataExtractor {

  private static final Logger logger = LoggerFactory.getLogger(MetadataExtractor.class);

  public enum Format {
    JSON(".json"),
    XML(".xml");

    private String ext;

    Format(String ext) {
      this.ext = ext;
    }

    public String ext() {
      return ext;
    }
  }

  @Autowired
  private TrisotechExtractionStrategy strategy;

  @Autowired
  private IdentityMapper mapper;

  @PostConstruct
  public void init() {
    logger.debug("metadataExtractor postconstruct ctor... with mapper: {}", mapper);
    logger.debug("now have strategy: {}", strategy);
    logger.debug("set mapper on strategy");
    strategy.setMapper(mapper);
  }

  public Optional<KnowledgeAsset> extract(InputStream resource, InputStream meta) {
    Optional<Document> dox = loadXMLDocument(resource);
    Optional<JsonNode> surrJson = JSonUtil.readJson(meta);

    return dox.map(document ->
        extract(document, surrJson
            .orElse(JsonNodeFactory.instance.nullNode())));
  }

  public Optional<ByteArrayOutputStream> doExtract(
      InputStream resource, InputStream meta, Format f, Properties p) {
    return extract(resource, meta).flatMap(surr -> {
      switch (f) {
        case JSON:
          return JSonUtil.writeJson(surr, p);
        case XML:
        default:
          return marshall(
              singletonList(surr.getClass()),
              surr,
              SurrogateHelper.getSchema().orElseThrow(UnsupportedOperationException::new),
              new JaxbConfig().from(p));
      }
    });
  }

  public Optional<Document> doExtract(Document dox, JsonNode meta) {
    KnowledgeAsset surr = extract(dox, meta);

    if(null != surr) {
      return JaxbUtil.marshallDox(Collections.singleton(surr.getClass()),
          surr,
          JaxbUtil.defaultProperties());
    } else {
      return Optional.empty();
    }
  }

  public KnowledgeAsset extract(Document dox, JsonNode meta) {
    return strategy.extractXML(dox, meta);
  }

  public KnowledgeAsset extract(Document dox, TrisotechFileInfo meta) {
    return strategy.extractXML(dox, meta);
  }


  public KnowledgeAsset extract(Document woven, TrisotechFileInfo model, ResourceIdentifier asset) {
    return strategy.extractXML(woven, model, asset);
  }

  public Optional<SyntacticRepresentation> getRepLanguage(Document document) {
    return strategy.getRepLanguage(document, false);
  }

  public Optional<URI> getEnterpriseAssetIdForAsset(UUID assestId) {
    return strategy.getEnterpriseAssetIdForAsset(assestId);
  }

  public Optional<URI> getEnterpriseAssetVersionIdForAsset(UUID assetId, String versionTag,
      boolean any)
      throws NotLatestVersionException {
    return strategy.getEnterpriseAssetVersionIdForAsset(assetId, versionTag, any);
  }

  public Optional<String> getMimetype(UUID assetId) {
    return strategy.getMimetype(assetId);
  }

  public Optional<String> getMimetype(String internalId) {
    return strategy.getMimetype(internalId);
  }

  public Optional<String> getArtifactVersion(UUID assetId) {
    return strategy.getArtifactVersion(assetId);
  }

  public Optional<String> getArtifactIdUpdateTime(UUID assetId) {
    return strategy.getArtifactIdUpdateTime(assetId);
  }

  public List<TrisotechFileInfo> getTrisotechModelVersions(String internalId) {
    return strategy.getTrisotechModelVersions(internalId);
  }


  /**
   * The fileId is the id of the file for the artifact that can be used with the APIs.
   *
   * @param assetId the enterprise asset ID
   * @param any boolean to determine if any model is considered, false to consider only published
   * @return fileId
   */
  public Optional<String> getFileId(UUID assetId, boolean any) {
    return strategy.getFileId(assetId, any);
  }


  /**
   * The fileId is the id of the file for the artifact that can be used with the APIs.
   *
   * @param internalId the internal identifier for teh artifact
   * @return fileId
   */  public Optional<String> getFileId(String internalId) {
    return strategy.getFileId(internalId);
  }

  /**
   * enterpriseAssetId is the assetId found in the Carrier/model/XML file from Trisotech
   *
   * @param fileId the Trisotech file ID to resolve to an enterprise ID
   * @return the enterprise ID
   */
  public ResourceIdentifier resolveEnterpriseAssetID(String fileId) {
    return strategy.getAssetID(fileId)
        // Defensive exception. Published models should have an assetId | CAO | DS
        .orElseThrow(() -> new IllegalStateException(
            "Defensive: Unable to resolve internal ID " + fileId + " to a known Enterprise ID"));
  }


  /**
   * internalArtifactID is the id of the Carrier/model in Trisotech
   *
   * @param assetId the assetId for which an artifact is needed
   * @param versionTag the version of the asset requesting
   * @param any should any model be searched? false means only published models will be searched
   * @return the internalArtifactId or NotLatestVersionException
   *
   * The exception will be thrown if the latest version of the artifact does not
   * map to the requested version of the asset.
   */
  public String resolveInternalArtifactID(String assetId, String versionTag, boolean any)
      throws NotLatestVersionException {
    // need to find the artifactId for this version of assetId
    // ResourceIdentifier built with assetId URI and versionTag; allows for finding the artifact associated with this asset/version
    ResourceIdentifier id = SemanticIdentifier.newNamespaceId(URI.create(assetId),versionTag);
    return strategy.getArtifactID(id, any);
  }

  public ResourceIdentifier convertInternalId(String internalId, String versionTag,
      String timestamp) {
    return strategy.convertInternalId(internalId, versionTag, timestamp);
  }


}
