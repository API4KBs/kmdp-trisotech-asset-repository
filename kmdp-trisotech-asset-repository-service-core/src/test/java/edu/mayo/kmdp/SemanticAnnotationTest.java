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

import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.In_Terms_Of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import edu.mayo.kmdp.kdcaci.knew.trisotech.preprocess.MetadataExtractor;
import edu.mayo.kmdp.kdcaci.knew.trisotech.preprocess.Weaver;
import edu.mayo.kmdp.util.XMLUtil;
import edu.mayo.ontology.taxonomies.clinicalsituations.CommonclinicalClinicalSituationSeries;
import edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries;
import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.terminology.v4.server.TermsApiInternal;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.terms.model.ConceptDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ContextConfiguration(classes = TrisotechAssetRepositoryConfig.class)
@TestPropertySource(properties = {
		"edu.mayo.kmdp.trisotechwrapper.repositoryName=MEA-Test",
		"edu.mayo.kmdp.trisotechwrapper.repositoryId=d4aca01b-d446-4bc8-a6f0-85d84f4c1aaf"})
class SemanticAnnotationTest {

	@Autowired
	MetadataExtractor extractor;

	@Autowired
	Weaver dmnWeaver;

	@Autowired
	TermsApiInternal terms;


	@Disabled("testExtraction: Current_Chronological_Age no longer exists...need input files updated to Trisotech")
	@Test
	void testExtraction() {
		// TODO: when have more annotations and examples CAO
		assertTrue(true); // dummy to keep sonarlint happy
		String dmnPath = "/Computable Decision Model.dmn";
		String metaPath = "/Computable Decision Model.meta.json";

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
			assertEquals( 1, surr.getAnnotation().size() );
			assertEquals(
					SemanticAnnotationRelTypeSeries.In_Terms_Of.asConceptIdentifier(), surr.getAnnotation().get(0).getRel() );

			assertEquals(CommonclinicalClinicalSituationSeries.Situation_With_Known_Patient_Age.getUuid(),
					surr.getAnnotation().get(0).getRef().getUuid());


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

			List<ConceptDescriptor> inputs = surr.getAnnotation().stream()
					.filter( annotation -> In_Terms_Of.isSameEntity(annotation.getRel()) )
					.map( annotation -> annotation.getRef().getUuid().toString())
					.map(terms::lookupTerm)
					.collect(Answer.toList())
					.orElse(Collections.emptyList());

			// FIXME "Dictionary inputs are not resolved (sex, race)"
			// FIXME Known concepts are not resolved "height, weight"
			assertEquals(4, inputs.size());

		} catch ( Exception e ) {
			e.printStackTrace();
			fail( e.getMessage() );
		}
	}

}
