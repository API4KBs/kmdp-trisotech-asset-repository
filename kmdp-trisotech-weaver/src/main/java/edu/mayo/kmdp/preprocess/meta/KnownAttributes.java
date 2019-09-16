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

import edu.mayo.kmdp.util.URIUtil;
import edu.mayo.ontology.taxonomies.kmdo.annotationreltype._20190801.AnnotationRelType;
import org.omg.spec.api4kp._1_0.identifiers.ConceptIdentifier;
import org.omg.spec.api4kp._1_0.identifiers.NamespaceIdentifier;

import java.net.URI;
import java.util.Arrays;
import java.util.Optional;

/*
 Trisotech ... explain yourself

 This class ensures that the names used in the 'published' artifacts
 are up to date. [original description ... what does it mean?]
*/
public enum KnownAttributes {

	// on models
	ASSET_IDENTIFIER( AnnotationRelType.Has_ID.getLabel(),
	                  "knowledgeAssetId",
	                  AnnotationRelType.Has_ID.getRef() ),


	TYPE( AnnotationRelType.Is_A.getLabel(),
			"assetType",
			AnnotationRelType.Is_A.getRef() ),

	// on expressions (models or fragments)

	// CAPTURES for propositionalconcepts on internal decision (decisionService)
  // TODO: don't have an example of CAPTURES in the test files CAO
	CAPTURES( AnnotationRelType.Captures.getLabel(),
			"capture",
			AnnotationRelType.Captures.getRef()),

	// In_Terms_of for propositionalconcepts of all inputs
	INPUTS( AnnotationRelType.In_Terms_Of.getLabel(),
			"inTermsOf",
			AnnotationRelType.In_Terms_Of.getRef()),

	// DEFINES for all other propositionalconcepts
	DEFINES( AnnotationRelType.Defines.getLabel(),
			"defines",
			AnnotationRelType.Defines.getRef() ),

	;

	private URI uri;
	private String key;
	private String label;


	KnownAttributes(String label, String key, URI uri ) {
		this.label = label;
		this.key = key;
		this.uri = uri;
	}

	public static Optional<KnownAttributes> resolve( String trisoName ) {
		return Arrays.stream( KnownAttributes.values() )
		             .filter( ka -> ka.key.equals( trisoName ) )
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
