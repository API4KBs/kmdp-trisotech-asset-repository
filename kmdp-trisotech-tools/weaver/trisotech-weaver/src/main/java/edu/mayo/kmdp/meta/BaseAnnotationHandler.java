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
package edu.mayo.kmdp.meta;

import edu.mayo.kmdp.metadata.annotations.*;
import org.omg.spec.api4kp._1_0.identifiers.ConceptIdentifier;
import org.w3c.dom.Element;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public abstract class BaseAnnotationHandler {

//	public abstract List<String> getDictionaryIDs( Element el );

	public abstract void replaceProprietaryElement( Element original, Element replace );

	public abstract void replaceProprietaryElement( Element original, List<Element> replace );

	public abstract List<Element> wrap( List<Element> element );

	public List<Annotation> getAnnotation(List<ConceptIdentifier> rows) {
		return rows.stream().map( (row) -> newAnnotation(Collections.singletonList(row))).collect(Collectors.toList());
	}
//	public List<Annotation> getAnnotation( String name,
////	                                       KnownAttributes defaultRel,
//	                                       List<ConceptIdentifier> rows ) {
//
//		KnownAttributes rel = KnownAttributes.resolve( name ).orElse( defaultRel );
//		KnownRoles rol = KnownRoles.resolve( rel );
//
//		return rel.isManyValued()
//				? rows.stream().map( (row) -> newAnnotation( rel, rol, Collections.singletonList( row ) ) ).collect( Collectors.toList() )
//				: Collections.singletonList( newAnnotation( rel, rol, rows ) );
//	}

//	public List<Annotation> getDataAnnotation( String name,
//	                                           KnownAttributes defaultRel,
//	                                           String value ) {
//
//		KnownAttributes rel = KnownAttributes.resolve( name ).orElse( defaultRel );
//
//		return Collections.singletonList( new DatatypeAnnotation().withRel( rel.asConcept() )
//		                                                          .withValue( value ) );
//	}

	protected Annotation newAnnotation(List<ConceptIdentifier> rows) {
		Annotation anno;
		switch (rows.size()) {
			case 0:
				anno = new SimpleAnnotation();
				break;
			case 1:
				anno = new SimpleAnnotation()
						.withExpr(rows.get(0));
				break;
			default:
				anno = new MultiwordAnnotation()
						.withExpr(rows);
		}
		return anno;
	}

//	protected Annotation newAnnotation( KnownAttributes rel, KnownRoles rol, List<ConceptIdentifier> rows ) {
//		Annotation anno;
//		switch ( rows.size() ) {
//			case 0:
//				anno = new SimpleAnnotation();
//				break;
//			case 1:
//				anno = new SimpleAnnotation()
//						.withExpr( rows.get( 0 ) );
//				break;
//			default:
//				anno = new MultiwordAnnotation()
//						.withExpr( rows );
//		}
//		return anno.withRel( rel.asConcept() )
//		           .withRol( rol.asConcept() );
//	}

	public Annotation getBasicAnnotation( KnownAttributes attr, String v ) {
		return new BasicAnnotation().withRel( attr.asConcept() )
		                            .withExpr( URI.create( v ) );
	}

}
