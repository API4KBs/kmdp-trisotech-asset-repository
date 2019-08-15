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

import com.fasterxml.jackson.databind.JsonNode;
import edu.mayo.kmdp.SurrogateHelper;
import edu.mayo.kmdp.metadata.annotations.Annotation;
import edu.mayo.kmdp.metadata.annotations.DatatypeAnnotation;
import edu.mayo.kmdp.metadata.annotations.SimpleAnnotation;
import edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.id.helper.DatatypeHelper;
import edu.mayo.kmdp.preprocess.NotLatestVersionException;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.util.JSonUtil;
import edu.mayo.kmdp.util.JaxbUtil;
import edu.mayo.kmdp.util.properties.jaxb.JaxbConfig;
import java.net.URI;
import org.omg.spec.api4kp._1_0.identifiers.URIIdentifier;
import org.w3c.dom.Document;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;

import static edu.mayo.kmdp.util.JaxbUtil.marshall;
import static edu.mayo.kmdp.util.XMLUtil.loadXMLDocument;

// TODO: rework for Trisotech data CAO
// TODO: What does this class do? What is its purpose in life? CAO
/**
 * MetadataExtractor takes the output of the Weaver and the information of the file to create
 * a KnowledgeAsset surrogate.
 */
public class MetadataExtractor {


  public enum Format {
    JSON( ".json" ),
    XML( ".xml" );

    private String ext;

    Format( String ext ) {
      this.ext = ext;
    }

    public String ext() {
      return ext;
    }
  }


  private ExtractionStrategy strategy;

  private IdentityMapper mapper;

  public MetadataExtractor() {
    this( new IdentityMapper() );
  }


  public MetadataExtractor( IdentityMapper mapper ) {
    strategy = new TrisotechExtractionStrategy();
    strategy.setMapper( mapper );
    this.mapper = mapper;
  }

  // TODO: Needed? not used CAO
  public MetadataExtractor( IdentityMapper mapper, Map<String,URIIdentifier> idMap ) {
    strategy = new TrisotechExtractionStrategy();
    strategy.setMapper( mapper );
    this.mapper = mapper;
  }

  public IdentityMapper getMapper() {
    return mapper;
  }

//  public void init( Map<String,URIIdentifier> idMap ) {
//    idMap.forEach( mapper::map );
//  }

  public Optional<KnowledgeAsset> extract(InputStream resource, InputStream meta ) {
    Optional<Document> dox = loadXMLDocument( resource );
    Optional<JsonNode> surrJson = JSonUtil.readJson(meta);

    return dox.map(document -> extract(document, surrJson.get()));
  }

  public Optional<ByteArrayOutputStream> doExtract( String resPath, String metaPath, Format f, Properties p ) {
    InputStream res = MetadataExtractor.class.getResourceAsStream( resPath );
    InputStream met = MetadataExtractor.class.getResourceAsStream( metaPath );

    return doExtract( res, met, f, p );
  }


  public Optional<ByteArrayOutputStream> doExtract( InputStream resource, InputStream meta, Format f, Properties p ) {
    return extract( resource, meta ).flatMap( (surr) -> {
      switch ( f ) {
        case JSON :
          Optional<ByteArrayOutputStream> jsonExtract = JSonUtil.writeJson(surr, p);
          ByteArrayOutputStream baos = jsonExtract.get();
          System.out.println("JSON output from doExtract: " + new String( baos.toByteArray()));
          return JSonUtil.writeJson( surr, p );
        case XML :
        default:
          List<? extends Class<? extends KnowledgeAsset>> surrKA = Collections.singletonList(surr.getClass());
          System.out.println("surrKA: " + surrKA.stream().toString());
          List<? extends Class<? extends KnowledgeAsset>> listSurrKA = Arrays.asList(surr.getClass());
          System.out.println("listSurrKA: " + listSurrKA.stream().toString());
          return marshall( Arrays.asList( surr.getClass() ),
                  surr,
                  SurrogateHelper.getSchema().orElseThrow( UnsupportedOperationException::new ),
                  new JaxbConfig().from( p ) );
      }
    } );
  }

  public Optional<Document> doExtract( Document dox, JsonNode meta ) {
    KnowledgeAsset surr = extract( dox, meta );
    surr.getSubject().forEach(anno -> {
      System.out.println("before: anno canonical class: " + anno.getClass().getCanonicalName());
      System.out.println("before: anno type class: " + anno.getClass().getTypeName());
      if(edu.mayo.kmdp.metadata.annotations.resources.DatatypeAnnotation.class.isInstance(anno)) {
        System.out.println("anno is resources.DatatypeAnnotation... reinstantiate");
        anno = (Annotation)anno.copyTo(new DatatypeAnnotation());
        // rootToFragment doesn't handle DatatypeAnnotation, but the above line is essentially what it does
//        anno = SurrogateHelper.rootToFragment(anno);
      }
      System.out.println("after: anno canonical class: " + anno.getClass().getCanonicalName());
      System.out.println("after: anno type class: " + anno.getClass().getTypeName());
    });

    return JaxbUtil.marshallDox( Collections.singleton( surr.getClass() ),
            surr,
            JaxbUtil.defaultProperties() );
  }

  public KnowledgeAsset extract( Document dox, JsonNode meta ) {
    return strategy.extractXML( dox, meta );
  }

  public KnowledgeAsset extract( Document dox, TrisotechFileInfo meta ) {
    return strategy.extractXML( dox, meta );
  }


  /**
   * Get the assetId from the Document.
   *
   * @param dox the Document that has the woven value of the assetId.
   * @return the URIIdentifer for the asset
   */
  public URIIdentifier getAssetID( Document dox ) {
    return strategy.extractAssetID( dox );
  }

  public Optional<URI> getEnterpriseAssetIdForAsset( UUID assestId ) {
    return strategy.getEnterpriseAssetIdForAsset(assestId);
  }

  public Optional<URI> getEnterpriseAssetVersionIdForAsset(UUID assetId, String versionTag) {
    return strategy.getEnterpriseAssetVersionIdForAsset(assetId, versionTag);
  }

  public URIIdentifier getAssetID(URI artifact) {
    return strategy.getAssetID(artifact);
  }

  public Optional<URIIdentifier> getAssetID(URIIdentifier artifactId, String versionTag)
      throws NotLatestVersionException {
    return strategy.getAssetID(artifactId, versionTag);
  }

  public String getArtifactId(URIIdentifier assetId, String versionTag)
      throws NotLatestVersionException {
    return strategy.getArtifactID(assetId, versionTag);
  }

  public Optional<String> getMimetype(UUID assetId) {
    return strategy.getMimetype(assetId);
  }

  public Optional<String> getArtifactVersion(UUID assetId) {
    return strategy.getArtifactVersion(assetId);
  }

  /**
   * enterpriseAssetId is the assetId found in the Carrier/model/XML file from Trisotech
   *
   * @param modelId the Trisotech model ID to resolve to an enterprise ID
   * @return
   */
  public URIIdentifier resolveEnterpriseAssetID( String modelId ) {
    return strategy.getMapper().getAssetId( modelId )
            .orElseThrow( () -> new IllegalStateException( "Defensive: Unable to resolve internal ID " + modelId + " to a known Enterprise ID" ) );
  }


  /**
   * internalArtifactID is the id of the Carrier/model in Trisotech
   *
   * @param assetId
   * @param versionTag
   * @return
   */
  public String resolveInternalArtifactID(String assetId, String versionTag)
      throws NotLatestVersionException {
    System.out.println("resolveInternalArtifactID: asset " + assetId + " version: " + versionTag);
    // need to find specific version of artifactId for this version of assetId
    // URIIdentifer built with assetId and versionTag; allows for finding the artifact associated with this asset/version
    URIIdentifier id = DatatypeHelper.uri(assetId, versionTag);
    System.out.println("URIIdentifier created from assetId and versionTag: " + id.getUri().toString());
    try {
      return strategy.getMapper().getArtifactId(id);
    } catch (NotLatestVersionException e) {
      throw e;
    }
  }


  public Optional<String> getFileId(UUID assetId) {
    return strategy.getMapper().getFileId(assetId);
  }

  public Optional<String> getFileId(String internalId) {
    return strategy.getMapper().getFileId(internalId);
  }
}
