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
//import edu.mayo.ontology.taxonomies.krlanguage._2018._08.KnowledgeRepresentationLanguage;
import edu.mayo.kmdp.util.JaxbUtil;
import edu.mayo.kmdp.util.Util;
import edu.mayo.kmdp.util.XMLUtil;
import edu.mayo.kmdp.meta.Weaver;
import edu.mayo.kmdp.preprocess.meta.MetadataExtractor;
import edu.mayo.ontology.taxonomies.krlanguage._2018._08.KnowledgeRepresentationLanguage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Optional;

import static edu.mayo.kmdp.preprocess.meta.MetadataExtractor.Format.XML;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class MetadataTest {


	private static MetadataExtractor extractor = new MetadataExtractor();

//	private static String dmnPath = "/R2R.dmn";
//	private static String metaPath = "/R2R_Info.json";
	private static String dmnPath = "/WeaverTest1.dmn";
	private static String metaPath = "/WeaverTest1Meta.json";
//	private static String cmnPath = "/CCPM.cmmn";
//	private static String cmnMetaPath = "/CCPM_Info.json";

	private static Weaver dmnWeaver;
	private static Weaver cmmnWeaver;

	private static byte[] annotatedDMN;
	private static byte[] annotatedCMMN;

	@BeforeAll
	public static void init() {
		// TODO: Update to DMN_1_2 when available CAO
		// TODO: What is TestDictionary.xlsx (export of Signavio Dictionary) and
		//  how does it correspond to what Trisotech offers? good question CAO
		// it doesn't; in Trisotech it is the accelerator; don't need a 'dictionary' weaver; rename/re-architect this code to represent that
		dmnWeaver = new Weaver(
//		        false,
//						Weaver.getWeaverProperties( KnowledgeRepresentationLanguage.DMN_1_2 )
		);
		System.out.println("dmnWeaver: " + dmnWeaver.toString());

		cmmnWeaver = new Weaver(
//		        false,
//		                         Weaver.getWeaverProperties( KnowledgeRepresentationLanguage.CMMN_1_1 )
		);

		System.out.println("cmmnWeaver: " + cmmnWeaver.toString());

		Optional<byte[]> dmn = XMLUtil.loadXMLDocument( MetadataTest.class.getResourceAsStream( dmnPath ) )
		                              .map( dmnWeaver::weave )
		                              .map( XMLUtil::toByteArray );
		assertTrue( dmn.isPresent() );
		annotatedDMN = dmn.get();

		System.out.println(new String ( annotatedDMN ));

// CAO
//		Optional<byte[]> cmmn = XMLUtil.loadXMLDocument( MetadataTest.class.getResourceAsStream( cmnPath ) )
//		                              .map( cmmnWeaver::weave )
//		                              .map( XMLUtil::toByteArray );
//		assertTrue( cmmn.isPresent() );
//		annotatedCMMN = cmmn.get();

//		System.out.println( new String( annotatedCMMN ) );

	}




//	@Disabled("Fix this")
	@Test
	void testExtraction() {
		try {
			Optional<KnowledgeAsset> res = extractor.extract( new ByteArrayInputStream( annotatedDMN ),
			                                                  MetadataTest.class.getResourceAsStream( metaPath ) );
			if ( ! res.isPresent() ) {
				fail( "Unable to instantiate metadata object" );
			}
			KnowledgeAsset surr = res.get();
			assertNotNull( surr );
			assertNotNull( surr.getCarriers() );

			assertNotNull( surr.getResourceId() );
			assertNotNull( surr.getResourceId().getUri() );
			assertNotNull( surr.getResourceId().getVersionId() );
			assertNotNull( surr.getName() );

			// TODO: These surr methods do not exist. Replacement? needed? Appears were commented out in old code as well CAO

//			assertNotNull( surr.getCreationDate() );
//			assertNotNull( surr.getLastChangeDate() );
//
//			assertEquals( "draft", surr.getCurrentPublicationStatus().getLabel() );


		} catch ( Exception e ) {
			e.printStackTrace();
			fail( e.getMessage() );
		}
	}



	@Disabled("fix testXMLValidate for Trisotech")
	@Test
	void testXMLValidate() {
		Optional<ByteArrayOutputStream> baos = extractor.doExtract( new ByteArrayInputStream( annotatedDMN ),
		                                                            MetadataTest.class.getResourceAsStream( metaPath ),
		                                                            XML,
		                                                            JaxbUtil.defaultProperties() );

		if ( ! baos.isPresent() ) {
			fail( "Unable to create metadata" );
		} else {
			boolean ans = baos.map( ByteArrayOutputStream::toByteArray )
			    .map( ByteArrayInputStream::new )
			    .map( StreamSource::new )
			    .map( (dox) -> XMLUtil.validate( dox, SurrogateHelper.getSchema().get() ) )
			                  .orElse( false );
			assertTrue( ans );
		}
	}


	@Disabled("Fix testToXML for Trisotech")
	@Test
	void testToXML() {
		assertTrue( extractor.doExtract( new ByteArrayInputStream( annotatedDMN ),
		                                 MetadataTest.class.getResourceAsStream( metaPath ),
		                                 XML,
		                                 JaxbUtil.defaultProperties() )
		                     .map( Util::printOut ).isPresent() );
//		assertTrue( extractor.doExtract( new ByteArrayInputStream( annotatedCMMN ),
//		                                 MetadataTest.class.getResourceAsStream( cmnMetaPath ),
//		                                 XML,
//		                                 JaxbUtil.defaultProperties() )
//		                     .map( Util::printOut ).isPresent() );

	}

  @Disabled("Fix testToJson for Trisotech")
	@Test
	void testToJson() {
		assertTrue( extractor.doExtract( new ByteArrayInputStream( annotatedDMN ),
		                                 MetadataTest.class.getResourceAsStream( metaPath ),
		                                 XML,
		                                 JaxbUtil.defaultProperties() )
		                     .map( Util::printOut ).isPresent() );
//		assertTrue( extractor.doExtract( new ByteArrayInputStream( annotatedCMMN ),
//		                                 MetadataTest.class.getResourceAsStream( cmnMetaPath ),
//		                                 XML,
//		                                 JaxbUtil.defaultProperties() )
//		                     .map( Util::printOut ).isPresent() );

	}


}
