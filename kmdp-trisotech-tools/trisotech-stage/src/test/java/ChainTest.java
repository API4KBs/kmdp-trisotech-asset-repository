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
import edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.metadata.surrogate.ObjectFactory;
import edu.mayo.ontology.taxonomies.krlanguage._2018._08.KnowledgeRepresentationLanguage;
import edu.mayo.kmdp.util.JaxbUtil;
import edu.mayo.kmdp.util.XMLUtil;
import edu.mayo.kmdp.ChainConverter;
import edu.mayo.kmdp.Model;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;


import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ChainTest {


	private Model convert( InputStream model, InputStream meta,KnowledgeRepresentationLanguage type ) {
		Model m = new ChainConverter().convert( meta, model, type );

		assertNotNull( m.getModel() );
		assertNotNull( m.getSurrogate() );

		return m;
	}

	@Disabled("Fix this")
	@Test
	void testChainDMN() {
		try {
			String dmnPath = "/WeaverTest1.dmn";
			InputStream dmn = ChainTest.class.getResourceAsStream( dmnPath );
			String metaPath = "/WeaverTest1Meta.json";
			InputStream meta = ChainTest.class.getResourceAsStream( metaPath );

			Model m = convert( dmn, meta, KnowledgeRepresentationLanguage.DMN_1_2 );

			Optional<KnowledgeAsset> s = JaxbUtil.unmarshall( ObjectFactory.class,
			                                                  KnowledgeAsset.class,
			                                                  m.getSurrogate(),
			                                                  JaxbUtil.defaultProperties() );
			assertTrue( s.isPresent() );
			assertEquals( "https://clinicalknowledgemanagement.mayo.edu/assets/3c66cf3a-93c4-4e09-b1aa-14088c76aded/versions/1.0.0-SNAPSHOT",
			              s.get().getResourceId().getUri().toString() );

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			XMLUtil.streamXMLDocument( m.getModel(), baos );
			assertTrue( new String( baos.toByteArray() ).contains( "KnowledgeAssetType_Scheme - Semantic Decision Model" ) );

		} catch ( Exception e ) {
			e.printStackTrace();
			fail( e.getMessage() );
		}
	}


	@Disabled("Fix this")
	@Test
	void testChainCMMN() {
		try {

			String cmmnPath = "/Atrial Fibrillation.cmmn"; // cao: The xml for the CMMN
			InputStream cmmn = ChainTest.class.getResourceAsStream(cmmnPath);
			String modelInfoPath = "/AFModel.json";

			InputStream modelInfo = ChainTest.class.getResourceAsStream(modelInfoPath);

			Model m = convert( cmmn, modelInfo, KnowledgeRepresentationLanguage.CMMN_1_1);

			Optional<KnowledgeAsset> s = JaxbUtil.unmarshall( ObjectFactory.class,
			                                                  KnowledgeAsset.class,
			                                                  m.getSurrogate(),
			                                                  JaxbUtil.defaultProperties() );
			assertTrue( s.isPresent() );
			assertEquals( "http://test.ckm.mock.edu/190a29b8-9bbd-4759-9046-6837196da93a",
			              s.get().getResourceId().getUri().toString() );

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			XMLUtil.streamXMLDocument( m.getModel(), baos );
			assertTrue( new String( baos.toByteArray() ).contains( "Mock AFib CCPM" ) );


		} catch ( Exception e ) {
			e.printStackTrace();
			fail( e.getMessage() );
		}
	}



}
