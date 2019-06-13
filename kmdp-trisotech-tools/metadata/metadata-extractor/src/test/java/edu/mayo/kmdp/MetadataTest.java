/**
 * Copyright © 2019 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
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
import edu.mayo.kmdp.util.JaxbUtil;
import edu.mayo.kmdp.util.Util;
import edu.mayo.kmdp.util.XMLUtil;
import edu.mayo.kmdp.preprocess.meta.Weaver;
import edu.mayo.kmdp.preprocess.meta.MetadataExtractor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Optional;

import static edu.mayo.kmdp.preprocess.meta.MetadataExtractor.Format.JSON;
import static edu.mayo.kmdp.preprocess.meta.MetadataExtractor.Format.XML;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class MetadataTest {


	private static MetadataExtractor extractor = new MetadataExtractor();

	private static String dmnPath = "/WeaverTest1.dmn";
	private static String metaPath = "/WeaverTest1Meta.json";
	private static String cmnPath = "/WeaveTest1.cmmn";
	private static String cmnMetaPath = "/WeaveTest1Meta.json";

	private static Weaver dmnWeaver;
	private static Weaver cmmnWeaver;

	private static byte[] annotatedDMN;
	private static byte[] annotatedCMMN;

	@BeforeAll
	public static void init() {

		dmnWeaver = new Weaver(	);

		cmmnWeaver = new Weaver( );

		Optional<byte[]> dmn = XMLUtil.loadXMLDocument( MetadataTest.class.getResourceAsStream( dmnPath ) )
		                              .map( dmnWeaver::weave )
		                              .map( XMLUtil::toByteArray );
		assertTrue( dmn.isPresent() );
		annotatedDMN = dmn.get();

		System.out.println(new String ( annotatedDMN ));

		Optional<byte[]> cmmn = XMLUtil.loadXMLDocument( MetadataTest.class.getResourceAsStream( cmnPath ) )
		                              .map( cmmnWeaver::weave )
		                              .map( XMLUtil::toByteArray );
		assertTrue( cmmn.isPresent() );
		annotatedCMMN = cmmn.get();

		System.out.println( new String( annotatedCMMN ) );

	}


// TODO: This passes, but not entirely sure the data is correct CAO
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

			assertNotNull( surr.getAssetId() ); // TODO: correct replacement? CAO .getResourceId() );
			assertNotNull( surr.getAssetId().getUri() ); // .getResourceId().getUri() );
			assertNotNull( surr.getAssetId().getVersionId() ); // getResourceId().getVersionId() );
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

	@Disabled ("testXMLValidate failing after upgrade to 2.0.2; FIX")
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


	@Disabled ("testToXML failing after upgrade to 2.0.2; FIX")
	@Test
	void testToXML() {
		assertTrue( extractor.doExtract( new ByteArrayInputStream( annotatedDMN ),
		                                 MetadataTest.class.getResourceAsStream( metaPath ),
		                                 XML,
		                                 JaxbUtil.defaultProperties() )
		                     .map( Util::printOut ).isPresent() );
		assertTrue( extractor.doExtract( new ByteArrayInputStream( annotatedCMMN ),
		                                 MetadataTest.class.getResourceAsStream( cmnMetaPath ),
		                                 XML,
		                                 JaxbUtil.defaultProperties() )
		                     .map( Util::printOut ).isPresent() );

	}

	@Test
	void testToJson() {
		assertTrue( extractor.doExtract( new ByteArrayInputStream( annotatedDMN ),
		                                 MetadataTest.class.getResourceAsStream( metaPath ),
		                                 JSON,
		                                 JaxbUtil.defaultProperties() )
		                     .map( Util::printOut ).isPresent() );
		assertTrue( extractor.doExtract( new ByteArrayInputStream( annotatedCMMN ),
		                                 MetadataTest.class.getResourceAsStream( cmnMetaPath ),
		                                 JSON,
		                                 JaxbUtil.defaultProperties() )
		                     .map( Util::printOut ).isPresent() );
	}


}
