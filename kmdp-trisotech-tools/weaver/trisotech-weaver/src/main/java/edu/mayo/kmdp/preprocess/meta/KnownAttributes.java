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

import edu.mayo.kmdp.terms.AssetVocabulary;
import edu.mayo.kmdp.util.URIUtil;
import org.omg.spec.api4kp._1_0.identifiers.ConceptIdentifier;
import org.omg.spec.api4kp._1_0.identifiers.NamespaceIdentifier;

import java.net.URI;
import java.util.Arrays;
import java.util.Optional;

/*
 Trisotech ... explain yourself

 This class ensures that the names used in the 'published' artifacts
 are up to date.
*/
public enum KnownAttributes {

	// on models
	ASSET_IDENTIFIER( AssetVocabulary.HAS_ID.getLabel(),
	                  "knowledgeAssetId",
	                  AssetVocabulary.HAS_ID.getRef(),
	                  false,
	                  false ),

	;

	private URI uri;
	private String trisotechLabel;
	private String label;
	private boolean coded;
	private boolean manyValued;

	KnownAttributes(String label, String trisotechLabel, URI uri, boolean coded, boolean manyValued ) {
		this.label = label;
		this.trisotechLabel = trisotechLabel;
		this.uri = uri;
		this.coded = coded;
		this.manyValued = manyValued;
	}

	public boolean isManyValued() {
		return manyValued;
	}

	public static Optional<KnownAttributes> resolve( String trisoName ) {
		return Arrays.stream( KnownAttributes.values() )
		             .filter( (ka) -> ka.trisotechLabel.equals( trisoName ) )
		             .findFirst();
	}

	public ConceptIdentifier asConcept() {
		return new ConceptIdentifier().withLabel( label )
		                              .withTag( URIUtil.detectLocalName( uri ) )
		                              .withRef( uri )
		                              .withNamespace( new NamespaceIdentifier()
				                                              .withId( URIUtil.detectNamespace( uri ) ) );
	}
}
