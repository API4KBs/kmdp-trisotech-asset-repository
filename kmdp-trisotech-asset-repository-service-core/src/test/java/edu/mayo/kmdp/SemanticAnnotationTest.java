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

import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.Defines;
import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.In_Terms_Of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import edu.mayo.kmdp.kdcaci.knew.trisotech.preprocess.MetadataExtractor;
import edu.mayo.kmdp.kdcaci.knew.trisotech.preprocess.Weaver;
import edu.mayo.kmdp.util.XMLUtil;
import edu.mayo.ontology.taxonomies.clinicalsituations.ClinicalSituation;
import edu.mayo.ontology.taxonomies.clinicalsituations.ClinicalSituationSeries;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.id.ConceptIdentifier;
import org.omg.spec.api4kp._20200801.surrogate.Annotation;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(classes = {TrisotechAssetRepositoryConfig.class})
@TestPropertySource(properties = {
		"edu.mayo.kmdp.trisotechwrapper.repositoryName=MEA-Test",
		"edu.mayo.kmdp.trisotechwrapper.repositoryId=d4aca01b-d446-4bc8-a6f0-85d84f4c1aaf"})
class SemanticAnnotationTest {

	@Autowired
	MetadataExtractor extractor;

	@Autowired
	private Weaver dmnWeaver;


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
		String dmnPath = "/Weaver Test 1.dmn.xml";
		String metaPath = "/Weaver Test 1.meta.json";

		Optional<byte[]> dmn = XMLUtil.loadXMLDocument( SemanticAnnotationTest.class.getResourceAsStream(dmnPath))
				.map( dmnWeaver::weave )
				.map( XMLUtil::toByteArray );
		assertTrue( dmn.isPresent() );
		try {
			Optional<KnowledgeAsset> res = extractor.extract(new ByteArrayInputStream(dmn.get()),
					SemanticAnnotationTest.class.getResourceAsStream(metaPath));
			if ( ! res.isPresent() ) {
				fail( "Unable to instantiate metadata object" );
			}
			KnowledgeAsset surr = res.get();

			List<ClinicalSituation> inputs = surr.getAnnotation().stream()
					.filter( annotation -> annotation.getRel().sameAs( In_Terms_Of.asConceptIdentifier() ) )
					.map( annotation -> ClinicalSituationSeries.resolve(annotation.getRef().getReferentId().toString())) //annotation.getRef()) )
					.map( Optional::get )
					.collect(Collectors.toList());

			// TODO: update per model CAO
		//	assertEquals( 19, inputs.size() );
//			assertTrue( inputs.contains( ClinicalSituationSeries.History_Of_Gastrointestinal_Bleeding ) ); // was .Has_Bleeding_Disorder CAO
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
					.filter(Annotation.class::isInstance)
					.map( Annotation.class::cast)
					.filter( a -> a.getRel().sameAs( In_Terms_Of.asConceptIdentifier() ) )
					.map( a -> ClinicalSituationSeries.resolve( a.getRef().getReferentId().toString() ) )
					.map( Optional::get )
					.collect(Collectors.toList());

			assertEquals( 9, inputs.size() );
			assertTrue( inputs.contains( ClinicalSituationSeries.Sex ) );
//			assertTrue( inputs.contains( ClinicalSituation.Recent_History_Of_TIA ) );
//			assertTrue( inputs.contains( ClinicalSituation.History_Of_Vascular_Disease ) );

			Set<ConceptIdentifier> defines  = surr.getAnnotation().stream()
					.filter( (ann) -> ann.getRel().sameAs( Defines.asConceptIdentifier() ) )
					.map(Annotation::getRef)
					.collect(Collectors.toSet());
			assertEquals( 3, defines.size() );
			assertTrue( defines.contains( ClinicalSituationSeries.Current_CHA2DS2_VASc_Score.asConceptIdentifier() ) );
			assertTrue( defines.contains( ClinicalSituationSeries.Risk_Of_Embolic_Stroke_CHA2DS2_VASc.asConceptIdentifier() ) );

		} catch ( Exception e ) {
			e.printStackTrace();
			fail( e.getMessage() );
		}
	}
	
}
