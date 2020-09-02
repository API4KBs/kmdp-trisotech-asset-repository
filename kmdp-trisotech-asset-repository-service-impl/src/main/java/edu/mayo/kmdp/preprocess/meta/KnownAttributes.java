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

import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.Captures;
import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.Defines;
import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.In_Terms_Of;
import static org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries.Imports;

import edu.mayo.kmdp.util.URIUtil;
import java.net.URI;
import java.util.Arrays;
import java.util.Optional;
import org.omg.spec.api4kp._20200801.id.ConceptIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;

/*
 Trisotech ... explain yourself

Is this class really needed if it is just 'wrapping' AnnotationRelType?

 This class ensures that the names used in the 'published' artifacts
 are up to date. [original description ... what does it mean?]
*/
public enum KnownAttributes {

	// on models
  DATA(Imports.getLabel(),
      "imports",
      Imports.getReferentId()),


	// on expressions (models or fragments)

	// CAPTURES for propositionalconcepts on internal decision (decisionService)
  // TODO: don't have an example of CAPTURES in the test files CAO
	CAPTURES( Captures.getLabel(),
			"capture",
			Captures.getReferentId()),

	// In_Terms_of for propositionalconcepts of all inputs
	INPUTS( In_Terms_Of.getLabel(),
			"inTermsOf",
			In_Terms_Of.getReferentId()),

	// DEFINES for all other propositionalconcepts
	DEFINES( Defines.getLabel(),
			"defines",
			Defines.getReferentId() ),

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
		return new ConceptIdentifier().withName( label )
		                              .withTag( URIUtil.detectLocalName( uri ) )
		                              .withResourceId( uri )
		                              .withNamespaceUri(SemanticIdentifier.newNamespaceId(uri).getNamespaceUri() );
	}
}
