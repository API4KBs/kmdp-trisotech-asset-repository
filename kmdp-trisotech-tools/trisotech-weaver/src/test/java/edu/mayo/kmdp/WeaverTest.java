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
package edu.mayo.kmdp;

import edu.mayo.kmdp.metadata.annotations.Annotation;
import edu.mayo.kmdp.metadata.annotations.BasicAnnotation;
import edu.mayo.kmdp.registry.Registry;
import edu.mayo.kmdp.preprocess.meta.KnownAttributes;
import edu.mayo.ontology.taxonomies.krlanguage._2018._08.KnowledgeRepresentationLanguage;
import edu.mayo.kmdp.util.XMLUtil;
import edu.mayo.kmdp.preprocess.meta.Weaver;
//import edu.mayo.kmdp.preprocess.meta.KnownAttributes;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import static edu.mayo.kmdp.util.Util.resolveResource;
import static edu.mayo.kmdp.util.XMLUtil.*;
import static edu.mayo.kmdp.util.XPathUtil.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class WeaverTest {

	@Test
	void testInit() {
		try {
			new Weaver( );

		} catch ( Exception e ) {
			e.printStackTrace();
			fail( e.getMessage() );
		}
	}

	// using XMLUtil loadXMLDocument to load the XML Document properly
	// sets up the document for conversion by setting namespaceaware

	@Test
	void testWeave() {
		String path = "/WeaverTest1.dmn";
//		String path = "/Choice of Atrial Fibrillation Treatment Strategy.dmn";
//		String path = "/Prior Management of Atrial Fibrillation.dmn";

		Document dox = loadXMLDocument( resolveResource( path ) ).orElseGet( () -> fail( "Unable to load document " + path ) );
		try {
			new Weaver().weave(dox);

			streamXMLDocument( dox, System.out );
			System.out.println("KRLanguage DMN1.2 ref: " + KnowledgeRepresentationLanguage.DMN_1_2.getRef());
			System.out.println("registry getValidationSchema for KRLanguage ref: " + Registry.getValidationSchema(KnowledgeRepresentationLanguage.DMN_1_2.getRef()).get());

			assertTrue(true); // dummy
			// TODO: the following is currently failing due to the new way DMN 1.2 handles the reference; fix once validation fixed for support CAO
//			assertTrue( validate( dox, KnowledgeRepresentationLanguage.DMN_1_2.getRef())); // URI.create(Registry.getValidationSchema(KnowledgeRepresentationLanguage.DMN_1_2.getRef()).get() )) );
		} catch ( IllegalStateException ie ) {
			ie.printStackTrace();
			fail( ie.getMessage() );
		}
	}

	// TODO: How is 'default' different than test above? What is considered 'default'? CAO
	@Test
	void testWeaveDefault() {
		String path = "/WeaverTest1.dmn";
		Document dox = loadXMLDocument( resolveResource( path ) ).orElseGet( () -> fail( "Unable to load document " + path ) );

		try {
			new Weaver( ).weave( dox );

			assertTrue(true); // dummy
			// TODO: the following is currently failing due to the new way DMN 1.2 handles the reference; fix once validation fixed for support
//			assertTrue( validate( dox, KnowledgeRepresentationLanguage.DMN_1_2.getRef() ) );

			streamXMLDocument( dox, System.out );
		} catch ( IllegalStateException ie ) {
			ie.printStackTrace();
			fail( ie.getMessage() );
		}
	}


	// TODO: Needed? CAO
	@Disabled
	@Test
	void testWeaveSalience() {
		// TODO: If needed, need a new dmn for testing
		String path = "/Salient.dmn";
		Document dox = loadXMLDocument( resolveResource( path ) ).orElseGet( () -> fail( "Unable to load document " + path ) );

		try {
			new Weaver( );

			assertTrue( validate( dox, KnowledgeRepresentationLanguage.DMN_1_2.getRef() ) );
			streamXMLDocument( dox, System.out );

//			List<Annotation> props = loadAnnotations( dox, KnownAttributes.SALIENCE, Annotation.class );
//			assertEquals( 1, props.size() );
//			assertTrue( props.get( 0 ) instanceof DatatypeAnnotation );
//			assertEquals( "33", ((DatatypeAnnotation) props.get( 0 )).getValue() );

		} catch ( IllegalStateException ie ) {
			ie.printStackTrace();
			fail( ie.getMessage() );
		}
	}


	// TODO: Update to Trisotech? CAO
	@Disabled
	@Test
	void testVariousMetadata() {
		String path = "/WeaverTest1.dmn";
		Document dox = loadXMLDocument( resolveResource( path ) ).orElseGet( () -> fail( "Unable to load document " + path ) );


		assertTrue(true); // dummy
		// TODO: the following is currently failing due to the new way DMN 1.2 handles the reference; fix once validation fixed for support
//		assertTrue( validate( dox, KnowledgeRepresentationLanguage.DMN_1_2.getRef() ) );

		try {
			new Weaver( ).weave( dox );

			streamXMLDocument( dox, System.out );

			assertNotNull( dox );

			BasicAnnotation id = loadAnnotations( dox, KnownAttributes.ASSET_IDENTIFIER, BasicAnnotation.class ).iterator().next();
			assertEquals( "https://clinicalknowledgemanagement.mayo.edu/assets/3c66cf3a-93c4-4e09-b1aa-14088c76aded/versions/1.0.0-SNAPSHOT",
			              id.getExpr().toString() );

			// TODO: is any of the following still needed? relevant to Trisotech data? CAO
//			SimpleAnnotation type = loadAnnotations( dox, KnownAttributes.TYPE, SimpleAnnotation.class ).iterator().next();
//			assertEquals( KnowledgeAssetType.Semantic_Decision_Model.getLabel(),
//			              type.getExpr().getLabel() );
//
//			List<Annotation> props = loadAnnotations( dox, KnownAttributes.CAPTURES, Annotation.class );
//			assertTrue( props.stream()
//			                 .anyMatch( (ann) -> ann instanceof SimpleAnnotation
//					                 && ( ( SimpleAnnotation ) ann ).getExpr().getLabel().contains( "Blood Pressure" ) ) );
//
//			List<SimpleAnnotation> subj = loadAnnotations( dox, KnownAttributes.SUBJECT, SimpleAnnotation.class );
//			assertTrue( subj.stream()
//			                .anyMatch( (ann) -> ann.getExpr().getLabel().contains( "Diabetes Mellitus" )
//					                || ann .getExpr().getTag().equals( "0215e32f-cced-4388-b0e0-ec8114e632d2" ) ) );

			// TODO: is this something else in Trisotech? CAO
//		assertEquals( "http://www.foo.bar",
//			              xString( dox, "//dmn:knowledgeSource[@name='all']/@locationURI" ) );

		} catch ( IllegalStateException ie ) {
			ie.printStackTrace();
			fail( ie.getMessage() );
		}
	}


	@Disabled("Not ready for CMMN yet")
	@Test
	void testVariousMetadataOnCMMN() {
		String path = "/WeaveTest1.cmmn";
		// loadXMLDocument set setNamespaceAware
		Document dox = loadXMLDocument( resolveResource( path ) ).orElseGet( () -> fail( "Unable to load document " + path ) );

		try {

			new Weaver( )
//							true,
//							Weaver.getWeaverProperties(KnowledgeRepresentationLanguage.CMMN_1_1))
							.weave(dox);

			System.out.println("CMMN file AFTER weave: ");
			streamXMLDocument( dox, System.out );
// CAO
//			List<BasicAnnotation> ids = loadAnnotations( dox,
//			                                             KnownAttributes.ASSET_IDENTIFIER,
//			                                             BasicAnnotation.class );
//			assertEquals( 1, ids.size() );
      System.out.println("registry getValidationSchema for CMMN KRLanguage ref: " + Registry.getValidationSchema(KnowledgeRepresentationLanguage.CMMN_1_1.getRef()).get());

			assertTrue( validate( dox, KnowledgeRepresentationLanguage.CMMN_1_1.getRef()));

// CAO
//			assertEquals( "http://test.ckm.mock.edu/190a29b8-9bbd-4759-9046-6837196da93a",
//			              ids.get( 0 ).getExpr().toString() );

			System.out.println("dox documentElement: " + dox.getDocumentElement());
			System.out.println("dox.getParentNode() " + dox.getParentNode());
			System.out.println("dox.getElementById(semantic:case) " + dox.getElementById("semantic:case"));

			System.out.println("dox.getXmlEncoding() " + dox.getXmlEncoding());
			System.out.println("xNode(dox, case): " + xNode(dox, "case"));
			System.out.println("xNode(dox, definitions/case): " + xNode(dox, "definitions/case"));
			System.out.println("xNode(dox, definitions/case[1]): " + xNode(dox, "definitions/case[1]"));


			System.out.println("xNode(dox, //definitions): " + xNode(dox, "//definitions"));
			System.out.println("xNode(dox, //definitions/@*): " + xNode(dox, "//definitions/@*"));
			System.out.println("xNode(dox, //definitions/*): " + xNode(dox, "//definitions/*"));

			System.out.println("xNode(dox, //definitions/case[1]/@*): " + xNode(dox, "//definitions/case[1]/@*"));
//			System.out.println("xNode(dox, //definitions/*:case): " + xNode(dox, "//definitions/*:case"));
//			System.out.println("xNode(dox, //meta/[@*[local-name() = 'case']]): " + xNode(dox, "//meta/[@*[local-name() = 'case']]"));


			System.out.println("xNode(dox, definitions/case): " + xNode(dox, "definitions/case"));
			System.out.println("xNode(dox, definitions/case[@id]): " + xNode(dox, "definitions/case[@id]"));
			System.out.println("xNode(dox, definitions/case[@id]): " + xNode(dox, "definitions/case[@id]"));
			System.out.println("xNode(dox, //*[@name]): " + xNode(dox, "//*[@name]"));
			System.out.println("xNode(dox, case/*[@name]): " + xNode(dox, "case/*[@name]"));
			System.out.println("xList(dox, /definitions).getLength(): " + xList(dox, "/definitions").getLength());

			NodeList metas = dox.getElementsByTagNameNS( Weaver.getMETADATA_NS(), Weaver.getMETADATA_EL() );
			String expression = "//*/triso:*"; // all tags w/'triso:' TODO: FIXME: this works in a xpath tester, but fails here with "Prefix must resolve to a namespace: triso" CAO
// 			System.out.println("metas length (expect 0): " + metas.getLength());

			NodeList nodes = xList(dox, expression);
			System.out.println("nodes length: " + nodes.getLength()); // expect 0, but right now should be more because weave not complete 6/21 CAO
			assertNotNull( xNode( dox, "definitions/case" ) );
		} catch ( IllegalStateException ie ) {
			ie.printStackTrace();
			fail( ie.getMessage() );
		}
	}


	// TODO: FIXME CAO
	private <T extends Annotation> List<T> loadAnnotations( Document dox, KnownAttributes att, Class<T> type ) {
		System.out.println("***** loadAnnotations for knownAttributes: " + att.name() + " and type: " + type.getName());
		return XMLUtil.asElementStream( dox.getElementsByTagName( "*" ) )
		              .filter( (el) -> el.getLocalName().equals( "extension" ) )
		              .map( Element::getChildNodes )
		              .flatMap( XMLUtil::asElementStream )
		              .map( SurrogateHelper::unmarshallAnnotation )
						      .peek(System.out::println)
		              .filter( (a) -> att.asConcept().equals( a.getRel() ) ) // TODO: NEEDED? CAO
									.peek(System.out::println)
		              .map( type::cast )
		              .collect( Collectors.toList() );
	}
}
