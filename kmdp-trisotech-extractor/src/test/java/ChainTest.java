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
import edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.metadata.surrogate.ObjectFactory;

import edu.mayo.kmdp.util.JaxbUtil;
import edu.mayo.kmdp.util.XMLUtil;
import edu.mayo.kmdp.ChainConverter;
import edu.mayo.kmdp.Model;
import edu.mayo.ontology.taxonomies.krlanguage._20190801.KnowledgeRepresentationLanguage;
import org.junit.jupiter.api.Test;


import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Optional;

import static edu.mayo.kmdp.preprocess.meta.Weaver.CLINICALKNOWLEDGEMANAGEMENT_MAYO_ARTIFACTS_BASE_URI;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ChainTest {


	private Model convert( InputStream model, InputStream meta, KnowledgeRepresentationLanguage type ) {
		Model m = new ChainConverter().convert( meta, model, type );

		assertNotNull( m.getModel() );
		assertNotNull( m.getSurrogate() );

		return m;
	}

	@Test
  void testChainDMN_BasicModel() {
    try {
      String dmnPath = "/Basic Decision Model.dmn";
      InputStream dmn = ChainTest.class.getResourceAsStream( dmnPath );
      String metaPath = "/Basic Decision ModelMeta.json";
      InputStream meta = ChainTest.class.getResourceAsStream( metaPath );

      Model m = convert( dmn, meta, KnowledgeRepresentationLanguage.DMN_1_2 );

      Optional<KnowledgeAsset> s = JaxbUtil.unmarshall( ObjectFactory.class,
          KnowledgeAsset.class,
          m.getSurrogate(),
          JaxbUtil.defaultProperties() );
      assertTrue( s.isPresent() );
      assertEquals( "https://clinicalknowledgemanagement.mayo.edu/assets/735a5764-fe3f-4ab8-b103-650b6e805db2",
          s.get().getAssetId().getUri().toString() );
      assertEquals("735a5764-fe3f-4ab8-b103-650b6e805db2", s.get().getAssetId().getTag());
      assertEquals("1.0.0", s.get().getAssetId().getVersion());
      assertEquals("https://clinicalknowledgemanagement.mayo.edu/assets/735a5764-fe3f-4ab8-b103-650b6e805db2/versions/1.0.0",
          s.get().getAssetId().getVersionId().toString());
			System.out.println("artifactId tag: " + s.get().getCarriers().get(0).getArtifactId().getTag());
			System.out.println("artifactId version: " + s.get().getCarriers().get(0).getArtifactId().getVersion());
			System.out.println("artifactId URI: " + s.get().getCarriers().get(0).getArtifactId().getUri());
			System.out.println("artifactId versionId: " + s.get().getCarriers().get(0).getArtifactId().getVersionId());
      assertEquals(CLINICALKNOWLEDGEMANAGEMENT_MAYO_ARTIFACTS_BASE_URI + "ee0c768a-a0d4-4052-a6ea-fc0a3889b356/versions/1.3.0",
					s.get().getCarriers().get(0).getArtifactId().getUri().toString());

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      XMLUtil.streamXMLDocument( m.getModel(), baos );
      assertTrue( new String( baos.toByteArray() ).contains( "KnowledgeAssetTypes - 20190801 - Semantic Decision Modelin 'KnowledgeAssetTypes - 20190801'" ) );

    } catch ( Exception e ) {
      e.printStackTrace();
      fail( e.getMessage() );
    }

  }
	@Test
	void testChainDMN_SimpleModel() {
		try {
			String dmnPath = "/Weaver Test 1.dmn";
			InputStream dmn = ChainTest.class.getResourceAsStream( dmnPath );
			String metaPath = "/WeaverTest1Meta.json";
			InputStream meta = ChainTest.class.getResourceAsStream( metaPath );

			Model m = convert( dmn, meta, KnowledgeRepresentationLanguage.DMN_1_2 );

			Optional<KnowledgeAsset> s = JaxbUtil.unmarshall( ObjectFactory.class,
			                                                  KnowledgeAsset.class,
			                                                  m.getSurrogate(),
			                                                  JaxbUtil.defaultProperties() );
			assertTrue( s.isPresent() );
			assertEquals( "https://clinicalknowledgemanagement.mayo.edu/assets/3c66cf3a-93c4-4e09-b1aa-14088c76aded",
			              s.get().getAssetId().getUri().toString() );
			assertEquals("3c66cf3a-93c4-4e09-b1aa-14088c76aded", s.get().getAssetId().getTag());
			assertEquals("1.0.0-SNAPSHOT", s.get().getAssetId().getVersion());
			assertEquals("https://clinicalknowledgemanagement.mayo.edu/assets/3c66cf3a-93c4-4e09-b1aa-14088c76aded/versions/1.0.0-SNAPSHOT",
					s.get().getAssetId().getVersionId().toString());
			System.out.println("artifactId tag: " + s.get().getCarriers().get(0).getArtifactId().getTag());
			System.out.println("artifactId version: " + s.get().getCarriers().get(0).getArtifactId().getVersion());
			System.out.println("artifactId URI: " + s.get().getCarriers().get(0).getArtifactId().getUri());
			System.out.println("artifactId versionId: " + s.get().getCarriers().get(0).getArtifactId().getVersionId());
			assertEquals(CLINICALKNOWLEDGEMANAGEMENT_MAYO_ARTIFACTS_BASE_URI + "5682fa26-b064-43c8-9475-1e4281e74068/versions/1.8",
					s.get().getCarriers().get(0).getArtifactId().getUri().toString());

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			XMLUtil.streamXMLDocument( m.getModel(), baos );
			assertTrue( new String( baos.toByteArray() ).contains( "KnowledgeAssetType_Scheme - Semantic Decision Model" ) );

		} catch ( Exception e ) {
			e.printStackTrace();
			fail( e.getMessage() );
		}
	}

	@Test
	void testChainCMMN_BasicModel() {
		try {

			String cmmnPath = "/Basic Case Model.cmmn"; // cao: The xml for the CMMN
			InputStream cmmn = ChainTest.class.getResourceAsStream(cmmnPath);
			String modelInfoPath = "/Basic Case ModelMeta.json";

			InputStream modelInfo = ChainTest.class.getResourceAsStream(modelInfoPath);

			Model m = convert( cmmn, modelInfo, KnowledgeRepresentationLanguage.CMMN_1_1);

			Optional<KnowledgeAsset> s = JaxbUtil.unmarshall( ObjectFactory.class,
			                                                  KnowledgeAsset.class,
			                                                  m.getSurrogate(),
			                                                  JaxbUtil.defaultProperties() );
			assertTrue( s.isPresent() );
			System.out.println("s: " + s.get());
			System.out.println("s assetId: " + s.get().getAssetId());
			// TODO: Should be checking ArtifactId, not AssetId? Not sure where AssetId comes from [IdentityMapper] CAO
			assertEquals( "http://test.ckm.mock.edu/190a29b8-9bbd-4759-9046-6837196da93a",
			              s.get().getAssetId().getUri().toString() );
			// TODO: this doesn't seem right -- not a URI, just a ID value CAO
			//  8/1/2019 update -- failing; assetId is null; should there be a value?
			assertEquals("50e19e36-6746-322f-9dd0-5c4ee4f370ce", s.get().getAssetId().getUri().toString());

			// TODO: Is this right? CAO
			assertEquals(CLINICALKNOWLEDGEMANAGEMENT_MAYO_ARTIFACTS_BASE_URI +"f59708b6-96c0-4aa3-be4a-31e075d76ec9",
					s.get().getCarriers().get(0).getArtifactId().getUri().toString() );
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			XMLUtil.streamXMLDocument( m.getModel(), baos );
			assertTrue( new String( baos.toByteArray() ).contains( "Weave Test 1" ) );


		} catch ( Exception e ) {
			e.printStackTrace();
			fail( e.getMessage() );
		}
	}

  @Test
  void testChainCMMN_SimpleModel() {
    try {

      String cmmnPath = "/Weave Test 1.cmmn"; // cao: The xml for the CMMN
      InputStream cmmn = ChainTest.class.getResourceAsStream(cmmnPath);
      String modelInfoPath = "/WeaveTest1Meta.json";

      InputStream modelInfo = ChainTest.class.getResourceAsStream(modelInfoPath);

      Model m = convert( cmmn, modelInfo, KnowledgeRepresentationLanguage.CMMN_1_1);

      Optional<KnowledgeAsset> s = JaxbUtil.unmarshall( ObjectFactory.class,
          KnowledgeAsset.class,
          m.getSurrogate(),
          JaxbUtil.defaultProperties() );
      assertTrue( s.isPresent() );
      System.out.println("s: " + s.get());
      System.out.println("s assetId: " + s.get().getAssetId());
      // TODO: Should be checking ArtifactId, not AssetId? Not sure where AssetId comes from [IdentityMapper] CAO
      assertEquals( "http://test.ckm.mock.edu/190a29b8-9bbd-4759-9046-6837196da93a",
          s.get().getAssetId().getUri().toString() );
      // TODO: this doesn't seem right -- not a URI, just a ID value CAO
      //  8/1/2019 update -- failing; assetId is null; should there be a value?
      assertEquals("50e19e36-6746-322f-9dd0-5c4ee4f370ce", s.get().getAssetId().getUri().toString());

      assertEquals(CLINICALKNOWLEDGEMANAGEMENT_MAYO_ARTIFACTS_BASE_URI +"f59708b6-96c0-4aa3-be4a-31e075d76ec9",
					s.get().getCarriers().get(0).getArtifactId().getUri().toString() );
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      XMLUtil.streamXMLDocument( m.getModel(), baos );
      assertTrue( new String( baos.toByteArray() ).contains( "Weave Test 1" ) );


    } catch ( Exception e ) {
      e.printStackTrace();
      fail( e.getMessage() );
    }
  }



}
