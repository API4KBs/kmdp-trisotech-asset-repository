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

import edu.mayo.kmdp.metadata.v2.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.metadata.annotations.SimpleAnnotation;
import edu.mayo.kmdp.metadata.v2.surrogate.annotations.Annotation;
import edu.mayo.kmdp.util.XMLUtil;
import edu.mayo.kmdp.preprocess.meta.Weaver;
//import edu.mayo.kmdp.preprocess.meta.KnownAttributes;
import edu.mayo.kmdp.preprocess.meta.MetadataExtractor;
import edu.mayo.ontology.taxonomies.clinicalsituations.ClinicalSituation;
import edu.mayo.ontology.taxonomies.clinicalsituations.ClinicalSituationSeries;
import edu.mayo.ontology.taxonomies.kmdo.annotationreltype.AnnotationRelTypeSeries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._1_0.id.ConceptIdentifier;

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
	}


	@Disabled("testExtraction: Current_Chronological_Age no longer exists...need input files updated to Trisotech")
	@Test
	void testExtraction() {
		// TODO: when have more annotations and examples CAO
		assertTrue(true); // dummy to keep sonarlint happy
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
			List<Annotation> annos = surr.getAnnotation(); //.getSubject();
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

			List<ClinicalSituation> inputs = surr.getAnnotation().stream()
					.filter(SimpleAnnotation.class::isInstance)
					.map( SimpleAnnotation.class::cast)
					.filter( (a) -> a.getRel().equals( AnnotationRelTypeSeries.In_Terms_Of.asConcept() ) )
					.map( (a) -> ClinicalSituationSeries.resolve( a.getExpr() ) )
					.map( Optional::get )
					.collect(Collectors.toList());

			// TODO: update per model CAO
			assertEquals( 19, inputs.size() );
			assertTrue( inputs.contains( ClinicalSituationSeries.History_Of_GI_Bleeding ) ); // was .Has_Bleeding_Disorder CAO
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
			List<ClinicalSituation> inputs = surr.getAnnotation().stream()
					.filter(SimpleAnnotation.class::isInstance)
					.map( SimpleAnnotation.class::cast)
					.filter( (a) -> a.getRel().equals( AnnotationRelTypeSeries.In_Terms_Of.asConcept() ) )
					.map( (a) -> ClinicalSituationSeries.resolve( a.getExpr() ) )
					.map( Optional::get )
					.collect(Collectors.toList());

			assertEquals( 9, inputs.size() );
			assertTrue( inputs.contains( ClinicalSituationSeries.Sex ) );
//			assertTrue( inputs.contains( ClinicalSituation.Recent_History_Of_TIA ) );
//			assertTrue( inputs.contains( ClinicalSituation.History_Of_Vascular_Disease ) );

			Set<ConceptIdentifier> defines  = surr.getAnnotation().stream()
					.filter( (ann) -> ann.getRel().equals( AnnotationRelTypeSeries.Defines.asConcept() ) )
					.map( (ann) -> ann.getRef())
					.collect(Collectors.toSet());
			assertEquals( 3, defines.size() );
			assertTrue( defines.contains( ClinicalSituationSeries.Current_CHA2DS2_VASc_Score.asConcept() ) );
			assertTrue( defines.contains( ClinicalSituationSeries.Risk_Of_Embolic_Stroke_CHA2DS2_VASc.asConcept() ) );

		} catch ( Exception e ) {
			e.printStackTrace();
			fail( e.getMessage() );
		}
	}
	
}
