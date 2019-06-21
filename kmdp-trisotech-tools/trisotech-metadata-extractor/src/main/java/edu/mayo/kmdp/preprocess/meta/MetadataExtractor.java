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
import edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.id.helper.DatatypeHelper;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.util.JSonUtil;
import edu.mayo.kmdp.util.JaxbUtil;
import edu.mayo.kmdp.util.properties.jaxb.JaxbConfig;
import org.omg.spec.api4kp._1_0.identifiers.URIIdentifier;
import org.w3c.dom.Document;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static edu.mayo.kmdp.util.JaxbUtil.marshall;
import static edu.mayo.kmdp.util.XMLUtil.loadXMLDocument;
import static edu.mayo.kmdp.util.ZipUtil.readZipEntry;

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

  public MetadataExtractor( IdentityMapper mapper, Map<String,URIIdentifier> idMap ) {
    strategy = new TrisotechExtractionStrategy();
    strategy.setMapper( mapper );
    this.mapper = mapper;
  }

  public IdentityMapper getMapper() {
    return mapper;
  }

  public void init( Map<String,URIIdentifier> idMap ) {
    idMap.forEach( mapper::map );
  }

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
          return marshall( Arrays.asList( surr.getClass() ),
                  surr,
                  SurrogateHelper.getSchema().orElseThrow( UnsupportedOperationException::new ),
                  new JaxbConfig().from( p ) );
      }
    } );
  }

  public Optional<Document> doExtract( Document dox, JsonNode meta ) {
    KnowledgeAsset surr = extract( dox, meta );

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

  private Optional<JsonNode> loadDescriptor( Document document, InputStream meta ) {
    Optional<String> innId = strategy.getArtifactID( document );

    if ( innId.isPresent() ) {
      return readZipEntry( strategy.getMetadataEntryNameForID( innId.get() ), meta )
              .flatMap( JSonUtil::readJson );
    } else {
      return Optional.empty();
    }
  }

  public URIIdentifier getAssetId( Document dox, TrisotechFileInfo info ) {
    return strategy.extractAssetID( dox, info );
  }

  public URIIdentifier resolveEnterpriseAssetID( String internalId ) {
    return strategy.getMapper().getResourceId( internalId )
            .orElseThrow( () -> new IllegalStateException( "Defensive: Unable to resolve internal ID" + internalId + " to a known Enterprise ID" ) );
  }


  public String resolveInternalArtifactID(String assetId, String versionTag) {
    URIIdentifier id = DatatypeHelper.uri(assetId, versionTag);
    return this.mapper.getInternalId(id)
            .orElseThrow(() -> new IllegalStateException( "Defensive: Unable to resolve external ID" + assetId + " to a known internal ID" ));
  }

}
