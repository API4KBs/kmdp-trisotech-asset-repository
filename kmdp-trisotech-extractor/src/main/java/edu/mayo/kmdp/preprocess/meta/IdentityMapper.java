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

import static edu.mayo.kmdp.util.XMLUtil.loadXMLDocument;

import edu.mayo.kmdp.id.Term;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.util.JSonUtil;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.omg.spec.api4kp._1_0.identifiers.URIIdentifier;
import org.w3c.dom.Document;

// TODO: Rework for Trisotech data CAO
// What is the purpose of this class? CAO

/**
 * IdentityMapper is used to map dependencies between artifacts (and dependencies between assets?)
 */
public class IdentityMapper {

	private Map<String,URIIdentifier> innerToPublicIDMap = new HashMap<>();

	private Model graph;


	public IdentityMapper() {
		this.graph = ModelFactory.createDefaultModel();
		// TODO: use graph from trisotech here CAO

	}

	@Deprecated( )
	// Test only
	public void scan( InputStream resource, InputStream meta, ExtractionStrategy strategy ) {
		Optional<Document> dox = loadXMLDocument( resource );
		Optional<TrisotechFileInfo> metaFile = readMeta(meta);
		TrisotechFileInfo fileInfo = null;
		if ( dox.isPresent() ) {
			if(metaFile.isPresent()) {
				fileInfo = metaFile.get();
			}
			Optional<String> docId = strategy.getArtifactID( dox.get(), fileInfo);
			Optional<URIIdentifier> resId = strategy.getAssetID(dox.get()); // is this enough? CAO -- need to rework IDMapper anyway
//			Optional<URIIdentifier> resId = strategy.getAssetID( dox.get(),
//			                                                        docId.orElseThrow( IllegalStateException::new ),
//			                                                        strategy.getArtifactVersionTag( dox.get(), readMeta( meta )
//					                                                        .orElse( null ) ).orElse( null ) );

			if ( resId.isPresent() && (docId.isPresent() && !edu.mayo.kmdp.util.Util.isEmpty( docId.get() ) ) ) {
				map( docId.get(), resId.get() );
			}
		}
	}

	private Optional<TrisotechFileInfo> readMeta(InputStream meta ) {
		return JSonUtil.readJson( meta )
		               .flatMap( j -> JSonUtil.parseJson( j, TrisotechFileInfo.class ) );
	}

	public Optional<URIIdentifier> getResourceId( String artifactId ) {
		return Optional.ofNullable( innerToPublicIDMap.get( artifactId ) );
	}

	public Optional<String> getInternalId( URIIdentifier assetId ) {
		return innerToPublicIDMap.entrySet().stream()
		                         .filter(entry -> entry.getValue().getVersionId().toString().endsWith(assetId.getVersionId().toString()) )
		                         .findFirst()
		                         .map( Map.Entry::getKey );
	}

	public boolean hasIdMapped( String artifactId ) {
		return innerToPublicIDMap.containsKey( artifactId );
	}

	public boolean hasMappedId( URI resourceURI ) {
		return innerToPublicIDMap.values().stream()
		                         .anyMatch( uid -> uid.getUri().equals( resourceURI ) );
	}

	public int numMappedIds() {
		return innerToPublicIDMap.size();
	}

	void map(String innerId, URIIdentifier publicId) {
		innerToPublicIDMap.put( innerId, publicId );
	}

	public URIIdentifier associate( URIIdentifier srcId, String tgtArtifactId, Term rel ) {
		URIIdentifier tgt = this.getResourceId( tgtArtifactId ).orElseThrow( IllegalStateException::new );
		Resource obj = graph.createResource( tgt.getVersionId().toString() );

		Property property = graph.createProperty( rel.getRef().toString() );

		graph.createResource( srcId.toStringId() ).addProperty( property, obj );
		return tgt;
	}

	public ByteArrayOutputStream serializeModel() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		graph.write( baos );
		return baos;
	}

	public void serializeModel( OutputStream os ) {
		graph.write( os );
	}


	public boolean isEmpty() {
		return innerToPublicIDMap.isEmpty();
	}
}
