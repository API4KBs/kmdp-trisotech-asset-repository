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

import edu.mayo.kmdp.id.Term;
import edu.mayo.kmdp.registry.Registry;
//import edu.mayo.ontology.taxonomies.lexicon._2018._08.Lexicon;
import edu.mayo.kmdp.util.JSonUtil;
import edu.mayo.kmdp.util.URIUtil;
import edu.mayo.kmdp.util.Util;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.omg.spec.api4kp._1_0.identifiers.URIIdentifier;
import org.w3c.dom.Document;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static edu.mayo.kmdp.util.XMLUtil.loadXMLDocument;

// TODO: Rework for Trisotech data
// What is the purpose of this class?
public class IdentityMapper {

	private Map<String,URIIdentifier> innerToPublicIDMap = new HashMap<>();

	private Model graph;


	public IdentityMapper() {
		this.graph = ModelFactory.createDefaultModel();
//		URI baseRel = Lexicon..getRef();
//		this.graph.setNsPrefix( Registry.getPrefixforNamespace( baseRel ).orElseThrow( IllegalStateException::new ),
//		                        baseRel.toString() );
	}

	@Deprecated( )
	// Test only
	public void scan( InputStream resource, InputStream meta, ExtractionStrategy strategy ) {
		Optional<Document> dox = loadXMLDocument( resource );
		if ( dox.isPresent() ) {
			Optional<String> docId = strategy.getArtifactID( dox.get() );
			Optional<URIIdentifier> resId = strategy.getResourceID( dox.get(),
			                                                        docId.orElseThrow( IllegalStateException::new ),
			                                                        strategy.getVersionTag( dox.get(), readMeta( meta )
					                                                        .orElse( null ) ).orElse( null ) );

			if ( resId.isPresent() && !  edu.mayo.kmdp.util.Util.isEmpty( docId.get() ) ) {
				map( docId.get(), resId.get() );
			}
		}
	}

	private Optional<TrisotechFileInfo> readMeta(InputStream meta ) {
		return JSonUtil.readJson( meta )
		               .flatMap( (j) -> JSonUtil.parseJson( j, TrisotechFileInfo.class ) );
	}

	public Optional<URIIdentifier> getResourceId( String artifactId ) {
		return Optional.ofNullable( innerToPublicIDMap.get( artifactId ) );
	}

	public Optional<String> getInternalId( URIIdentifier assetId ) {
		return innerToPublicIDMap.entrySet().stream()
		                         .filter((entry) -> entry.getValue().getVersionId().toString().endsWith(assetId.getVersionId().toString()) )
		                         .findFirst()
		                         .map( Map.Entry::getKey );
	}

	public boolean hasIdMapped( String artifactId ) {
		return innerToPublicIDMap.containsKey( artifactId );
	}

	public boolean hasMappedId( URI resourceURI ) {
		return innerToPublicIDMap.values().stream()
		                         .anyMatch( ( uid) -> uid.getUri().equals( resourceURI ) );
	}

	public int numMappedIds() {
		return innerToPublicIDMap.size();
	}

	public void map( String innerId, URIIdentifier publicId ) {
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

	public URI ensureId( final String candidateId, String context ) {
		String id = candidateId;
		if ( Util.isEmpty( id ) ) {
			System.out.println( "WARNING : Inferring Enterprise Asset ID for Artifact " + context );
			id = UUID.nameUUIDFromBytes( context.getBytes() ).toString();
		}
		if ( ! id.startsWith( Registry.MAYO_ASSETS_BASE_URI) && ! URIUtil.isUri(id) ) {
			id = Registry.MAYO_ASSETS_BASE_URI + id;
		}
		return URI.create( id );
	}

	public boolean isEmpty() {
		return innerToPublicIDMap.isEmpty();
	}
}
