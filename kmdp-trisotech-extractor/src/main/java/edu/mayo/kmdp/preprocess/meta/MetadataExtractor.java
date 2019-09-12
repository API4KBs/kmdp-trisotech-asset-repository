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

import static edu.mayo.kmdp.util.JaxbUtil.marshall;
import static edu.mayo.kmdp.util.XMLUtil.loadXMLDocument;

import com.fasterxml.jackson.databind.JsonNode;
import edu.mayo.kmdp.SurrogateHelper;
import edu.mayo.kmdp.id.helper.DatatypeHelper;
import edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.preprocess.NotLatestVersionException;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.util.JSonUtil;
import edu.mayo.kmdp.util.JaxbUtil;
import edu.mayo.kmdp.util.properties.jaxb.JaxbConfig;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import javax.annotation.PostConstruct;
import org.omg.spec.api4kp._1_0.identifiers.URIIdentifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

/**
 * MetadataExtractor takes the output of the Weaver and the information of the file to create
 * a KnowledgeAsset surrogate.
 */
@Component
public class MetadataExtractor {

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

//  private ExtractionStrategy strategy;

  @Autowired
  private TrisotechExtractionStrategy strategy;

  @Autowired
  private IdentityMapper mapper;

  public MetadataExtractor() {
//    this(new IdentityMapper());
  }

//  public MetadataExtractor(IdentityMapper mapper) {
//    strategy = new TrisotechExtractionStrategy();
//    strategy.setMapper(mapper);
//  }

  @PostConstruct
  public void init() {
    System.out.println("metadataExtractor postconstruct ctor... with mapper: " + mapper);
    System.out.println("now have strategy: " + strategy);
    System.out.println("set mapper on strategy");
    strategy.setMapper(mapper);
  }

  public Optional<KnowledgeAsset> extract(InputStream resource, InputStream meta) {
    Optional<Document> dox = loadXMLDocument(resource);
    Optional<JsonNode> surrJson = JSonUtil.readJson(meta);

    return dox.map(document -> extract(document, surrJson.get()));
  }

  public Optional<ByteArrayOutputStream> doExtract(InputStream resource, InputStream meta, Format f,
      Properties p) {
    return extract(resource, meta).flatMap(surr -> {
      switch (f) {
        case JSON:
          Optional<ByteArrayOutputStream> jsonExtract = JSonUtil.writeJson(surr, p);
          ByteArrayOutputStream baos = jsonExtract.get();
          return JSonUtil.writeJson(surr, p);
        case XML:
        default:
          List<? extends Class<? extends KnowledgeAsset>> surrKA = Collections
              .singletonList(surr.getClass());
          List<? extends Class<? extends KnowledgeAsset>> listSurrKA = Arrays
              .asList(surr.getClass());
          return marshall(Arrays.asList(surr.getClass()),
              surr,
              SurrogateHelper.getSchema().orElseThrow(UnsupportedOperationException::new),
              new JaxbConfig().from(p));
      }
    });
  }

  public Optional<Document> doExtract(Document dox, JsonNode meta) {
    KnowledgeAsset surr = extract(dox, meta);

    return JaxbUtil.marshallDox(Collections.singleton(surr.getClass()),
        surr,
        JaxbUtil.defaultProperties());
  }

  public KnowledgeAsset extract(Document dox, JsonNode meta) {
    return strategy.extractXML(dox, meta);
  }

  public KnowledgeAsset extract(Document dox, TrisotechFileInfo meta) {
    return strategy.extractXML(dox, meta);
  }


  /**
   * Get the assetId from the Document.
   *
   * @param dox the Document that has the woven value of the assetId.
   * @return the URIIdentifer for the asset
   */
  public URIIdentifier getAssetID(Document dox) {
    return strategy.extractAssetID(dox);
  }

  public Optional<URI> getEnterpriseAssetIdForAsset(UUID assestId) {
    return strategy.getEnterpriseAssetIdForAsset(assestId);
  }

  public URI getEnterpriseAssetIdForAssetVersionId(URI enterpriseAssetVersionId) {
    return strategy.getEnterpriseAssetIdForAssetVersionId(enterpriseAssetVersionId);
  }

  public Optional<URI> getEnterpriseAssetVersionIdForAsset(UUID assetId, String versionTag)
      throws NotLatestVersionException {
    return strategy.getEnterpriseAssetVersionIdForAsset(assetId, versionTag);
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


  public Optional<String> getFileId(UUID assetId) {
    return strategy.getFileId(assetId);
  }

  public Optional<String> getFileId(String internalId) {
    return strategy.getFileId(internalId);
  }

  /**
   * enterpriseAssetId is the assetId found in the Carrier/model/XML file from Trisotech
   *
   * @param fileId the Trisotech file ID to resolve to an enterprise ID
   * @return
   */
  public URIIdentifier resolveEnterpriseAssetID(String fileId) {
    // TODO: Need consistency ... return Optional.empty? or throw an error? CAO; should all methods return Optional<T>?
    return strategy.getAssetId(fileId)
        .orElseThrow(() -> new IllegalStateException(
            "Defensive: Unable to resolve internal ID " + fileId + " to a known Enterprise ID"));
  }


  /**
   * internalArtifactID is the id of the Carrier/model in Trisotech
   *
   * @param assetId the assetId for which an artifact is needed
   * @param versionTag the version of the asset requesting
   * @return the internalArtifactId or NotLatestVersionException
   *
   * The exception will be thrown if the latest version of the artifact does not
   * map to the requested version of the asset.
   */
  public String resolveInternalArtifactID(String assetId, String versionTag)
      throws NotLatestVersionException {
    // need to find the artifactId for this version of assetId
    // URIIdentifer built with assetId and versionTag; allows for finding the artifact associated with this asset/version
    URIIdentifier id = DatatypeHelper.uri(assetId, versionTag);
    return strategy.getArtifactId(id);
  }

}
