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

import edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.metadata.annotations.Annotation;
import edu.mayo.kmdp.metadata.annotations.SimpleAnnotation;
import edu.mayo.kmdp.util.XMLUtil;
import edu.mayo.kmdp.preprocess.meta.Weaver;
//import edu.mayo.kmdp.preprocess.meta.KnownAttributes;
import edu.mayo.kmdp.preprocess.meta.MetadataExtractor;
import edu.mayo.ontology.taxonomies.clinicalsituations._20190801.ClinicalSituation;
import edu.mayo.ontology.taxonomies.kmdo.annotationreltype._20190801.AnnotationRelType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._1_0.identifiers.ConceptIdentifier;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@Disabled("not ready")
class SemanticAnnotationTest {


	private static MetadataExtractor extractor = new MetadataExtractor();
	private static Weaver dmnWeaver;

	@BeforeAll
	public static void init() {
		dmnWeaver = new Weaver( );
//		SemanticAnnotationTest.class.getResourceAsStream("/TestDictionary.xlsx"),
//		                                       false,
//		                                       DictionaryEntryWeaver.getWeaverProperties( KnowledgeRepresentationLanguage.DMN_1_1 ) );
	}




	@Disabled("testExtraction: Current_Chronological_Age no longer exists...need input files updated to Trisotech")
	@Test
	void testExtraction() {
		// TODO: when have more annotations and examples CAO
//		String dmnPath = "/ComputableDM.dmn";
//		String metaPath = "/ComputableDM_Info.json";
//
//		Optional<byte[]> dmn = XMLUtil.loadXMLDocument( SemanticAnnotationTest.class.getResourceAsStream(dmnPath))
//		                              .map( dmnWeaver::weave )
//		                              .map( XMLUtil::toByteArray );
//		assertTrue( dmn.isPresent() );
//
//
//		try {
//			Optional<KnowledgeAsset> res = extractor.extract(new ByteArrayInputStream(dmn.get() ),
//			                                                 SemanticAnnotationTest.class.getResourceAsStream(metaPath));
//			if ( ! res.isPresent() ) {
//				fail( "Unable to instantiate metadata object" );
//			}
//			KnowledgeAsset surr = res.get();
//			assertEquals( 1, surr.getSubject().size() );
//			assertEquals(AnnotationRelType.In_Terms_Of.asConcept(), surr.getSubject().get(0).getRel() );
//			assertEquals(ClinicalSituation.Current_Chronological_Age.asConcept(), ((edu.mayo.kmdp.metadata.annotations.SimpleAnnotation) surr.getSubject().get(0)).getExpr() );
//
//
//		} catch ( Exception e ) {
//			e.printStackTrace();
//			fail( e.getMessage() );
//		}
	}


	@Disabled("testSalienceNonExtraction needs input files updated to Trisotech")
	@Test
	void testSalienceNonExtraction() {
		String dmnPath = "/Salient.dmn";
		String metaPath = "/Salient_Info.json";

		Optional<byte[]> dmn = XMLUtil.loadXMLDocument( SemanticAnnotationTest.class.getResourceAsStream(dmnPath))
		                              .map( dmnWeaver::weave )
		                              .map( XMLUtil::toByteArray );
		assertTrue( dmn.isPresent() );


		try {
			Optional<KnowledgeAsset> res = extractor.extract(new ByteArrayInputStream(dmn.get() ),
			                                                 SemanticAnnotationTest.class.getResourceAsStream(metaPath));
			if ( ! res.isPresent() ) {
				fail( "Unable to instantiate metadata object" );
			}
			KnowledgeAsset surr = res.get();
			List<Annotation> annos = surr.getSubject();
			assertEquals(0, annos.size() );
		} catch ( Exception e ) {
			e.printStackTrace();
			fail( e.getMessage() );
		}
	}


	@Test
	void testExtractionFull() {
		String dmnPath = "/Weaver Test 1.dmn";
		String metaPath = "/WeaverTest1Meta.json";

		Optional<byte[]> dmn = XMLUtil.loadXMLDocument( SemanticAnnotationTest.class.getResourceAsStream(dmnPath))
		                              .map( dmnWeaver::weave )
		                              .map( XMLUtil::toByteArray );
		assertTrue( dmn.isPresent() );
		try {
			Optional<KnowledgeAsset> res = extractor.extract(new ByteArrayInputStream(dmn.get() ),
			                                                 SemanticAnnotationTest.class.getResourceAsStream(metaPath));
			if ( ! res.isPresent() ) {
				fail( "Unable to instantiate metadata object" );
			}
			KnowledgeAsset surr = res.get();
			// long form
			List<Annotation> annotations = surr.getSubject();
			for (Annotation anno: annotations
					 ) {
				System.out.println("anno is : " + anno.getClass() + " annotation");
				System.out.println("anno rel: " + anno.getRel().toString());
				System.out.println("AnnotationRelType.In_Terms_Of.asConcept(): " + AnnotationRelType.In_Terms_Of.asConcept());
				System.out.println("((SimpleAnnotation)anno).getExpr()" + ((SimpleAnnotation)anno).getExpr());
				System.out.println("ClinicalSituation.resolve(anno.getExpr()): " + ClinicalSituation.resolve(((SimpleAnnotation)anno).getExpr()));
				System.out.println("ClinicalSituation name: " + ClinicalSituation.resolve(((SimpleAnnotation)anno).getExpr()).get().name());

				if(anno.getClass().isInstance(SimpleAnnotation.class)) {
					System.out.println("anno is SimpleAnnotation: " + anno.toString());
					System.out.println("anno rel: " + anno.getRel().toString());
				}
			}
			List<ClinicalSituation> inputs = surr.getSubject().stream()
			                       .filter(SimpleAnnotation.class::isInstance)
			                       .map( SimpleAnnotation.class::cast)
			                       .filter( (a) -> a.getRel().equals( AnnotationRelType.In_Terms_Of.asConcept() ) )
			                       .map( (a) -> ClinicalSituation.resolve( a.getExpr() ) )
			                       .map( Optional::get )
			                       .collect(Collectors.toList());

			// TODO: update per model CAO
			assertEquals( 19, inputs.size() );
			assertTrue( inputs.contains( ClinicalSituation.History_Of_GI_Bleeding ) ); // was .Has_Bleeding_Disorder CAO
//			assertTrue( inputs.contains( ClinicalSituation.Has_Cirrhosis ) );
		} catch ( Exception e ) {
			e.printStackTrace();
			fail( e.getMessage() );
		}
	}


	@Disabled("testExtractionFull2 can't be tested until input files are created/updated to Trisotech")
	@Test
	void testExtractionFull2() {
		String dmnPath = "/TBD.dmn";
		String metaPath = "/TBD_Info.json";

		Optional<byte[]> dmn = XMLUtil.loadXMLDocument( SemanticAnnotationTest.class.getResourceAsStream(dmnPath))
		                              .map( dmnWeaver::weave )
		                              .map( XMLUtil::toByteArray );
		assertTrue( dmn.isPresent() );
		try {
			Optional<KnowledgeAsset> res = extractor.extract(new ByteArrayInputStream(dmn.get() ),
			                                                 SemanticAnnotationTest.class.getResourceAsStream(metaPath));
			if ( ! res.isPresent() ) {
				fail( "Unable to instantiate metadata object" );
			}
			KnowledgeAsset surr = res.get();
			List<ClinicalSituation> inputs = surr.getSubject().stream()
			                       .filter(SimpleAnnotation.class::isInstance)
			                       .map( SimpleAnnotation.class::cast)
			                       .filter( (a) -> a.getRel().equals( AnnotationRelType.In_Terms_Of.asConcept() ) )
			                       .map( (a) -> ClinicalSituation.resolve( a.getExpr() ) )
			                       .map( Optional::get )
			                       .collect(Collectors.toList());

			assertEquals( 9, inputs.size() );
			assertTrue( inputs.contains( ClinicalSituation.Sex ) );
//			assertTrue( inputs.contains( ClinicalSituation.Recent_History_Of_TIA ) );
//			assertTrue( inputs.contains( ClinicalSituation.History_Of_Vascular_Disease ) );

			Set<ConceptIdentifier> defines  = surr.getSubject().stream()
			                                      .filter( (ann) -> ann.getRel().equals( AnnotationRelType.Defines.asConcept() ) )
			                                      .map( (ann) -> ((SimpleAnnotation) ann).getExpr() )
			                                      .collect(Collectors.toSet());
			assertEquals( 3, defines.size() );
			assertTrue( defines.contains( ClinicalSituation.Current_CHA2DS2_VASc_Score.asConcept() ) );
			assertTrue( defines.contains( ClinicalSituation.Risk_Of_Embolic_Stroke_CHA2DS2_VASc1.asConcept() ) ); // was: Likelihood_Of_Stroke_Based_On_CHA2DS2Vasc_Score CAO

		} catch ( Exception e ) {
			e.printStackTrace();
			fail( e.getMessage() );
		}
	}






}
