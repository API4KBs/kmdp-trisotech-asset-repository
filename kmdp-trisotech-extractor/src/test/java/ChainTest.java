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
import edu.mayo.kmdp.ChainConverterConfig;
import edu.mayo.kmdp.ExtractorConfig;
import edu.mayo.kmdp.IdentityMapperConfig;
import edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.metadata.surrogate.ObjectFactory;

import edu.mayo.kmdp.registry.Registry;
import edu.mayo.kmdp.util.JaxbUtil;
import edu.mayo.kmdp.util.XMLUtil;
import edu.mayo.kmdp.ChainConverter;
import edu.mayo.kmdp.Model;
import edu.mayo.kmdp.util.properties.jaxb.JaxbConfig;
import edu.mayo.ontology.taxonomies.krlanguage._20190801.KnowledgeRepresentationLanguage;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;


import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static edu.mayo.kmdp.preprocess.meta.Weaver.CLINICALKNOWLEDGEMANAGEMENT_MAYO_ARTIFACTS_BASE_URI;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

// TODO: fix this. currently the jaxbutil.unmarshall is broken after upgrading to latest libraries from dev CAO
@SpringBootTest
@SpringJUnitConfig(classes = {ExtractorConfig.class, IdentityMapperConfig.class,
    ChainConverterConfig.class})
class ChainTest {

  @Autowired
  ChainConverter chainConverter;

  private Model convert(InputStream model, InputStream meta, KnowledgeRepresentationLanguage type) {
    Model m = chainConverter.convert(meta, model, type); // new ChainConverter().convert(meta, model, type);

    assertNotNull(m.getModel());
    assertNotNull(m.getSurrogate());

    return m;
  }

  @Test
  void testChainDMN_BasicModel() {
    try {
      String dmnPath = "/Basic Decision Model.dmn";
      InputStream dmn = ChainTest.class.getResourceAsStream(dmnPath);
      String metaPath = "/Basic Decision ModelMeta.json";
      InputStream meta = ChainTest.class.getResourceAsStream(metaPath);
      String expectedAssetTag = "735a5764-fe3f-4ab8-b103-650b6e805db2";
      String expectedAssetId = Registry.MAYO_ASSETS_BASE_URI + expectedAssetTag;
      String expectedAssetVersion = "1.0.0";
      String expectedAssetVersionId = expectedAssetId + "/versions/" + expectedAssetVersion;
      String expectedArtifactTag = "ee0c768a-a0d4-4052-a6ea-fc0a3889b356";
      String expectedArtifactVersion = "1.3.0";
      String expectedArtifactId =
          CLINICALKNOWLEDGEMANAGEMENT_MAYO_ARTIFACTS_BASE_URI + expectedArtifactTag;
      String expectedArtifactVersionId =
          expectedArtifactId + "/versions/" + expectedArtifactVersion;

      Model m = convert(dmn, meta, KnowledgeRepresentationLanguage.DMN_1_2);

      Optional<KnowledgeAsset> s = JaxbUtil.unmarshall(ObjectFactory.class,
          KnowledgeAsset.class,
          m.getSurrogate());
//      .unmarshall(ObjectFactory.class,
//          KnowledgeAsset.class,
//          m.getSurrogate(),
//          JaxbUtil.defaultProperties());
      assertTrue(s.isPresent());
      assertEquals(expectedAssetId,
          s.get().getAssetId().getUri().toString());
      assertEquals(expectedAssetTag, s.get().getAssetId().getTag());
      assertEquals(expectedAssetVersion, s.get().getAssetId().getVersion());
      assertEquals(expectedAssetVersionId,
          s.get().getAssetId().getVersionId().toString());

      assertEquals(expectedArtifactId,
          s.get().getCarriers().get(0).getArtifactId().getUri().toString());
      assertEquals(expectedArtifactVersionId,
          s.get().getCarriers().get(0).getArtifactId().getVersionId().toString());

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      XMLUtil.streamXMLDocument(m.getModel(), baos);
//      System.out.println("what is in baos? " + new String(baos.toByteArray()));
      assertTrue(new String(baos.toByteArray()).contains(
          "KnowledgeAssetTypes - 20190801 - Semantic Decision Modelin 'KnowledgeAssetTypes - 20190801'"));

    } catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }

  }

  @Test
  void testChainDMN_SimpleModel() {
    try {
      String dmnPath = "/Weaver Test 1.dmn";
      InputStream dmn = ChainTest.class.getResourceAsStream(dmnPath);
      String metaPath = "/WeaverTest1Meta.json";
      InputStream meta = ChainTest.class.getResourceAsStream(metaPath);
      String expectedAssetTag = "3c66cf3a-93c4-4e09-b1aa-14088c76aded";
      String expectedVersion = "1.0.0-SNAPSHOT";
      String expectedAssetVersionId =
          Registry.MAYO_ASSETS_BASE_URI + expectedAssetTag + "/versions/" + expectedVersion;
      String expectedArtifactTag = "5682fa26-b064-43c8-9475-1e4281e74068";
      String expectedArtifactId =
          CLINICALKNOWLEDGEMANAGEMENT_MAYO_ARTIFACTS_BASE_URI + expectedArtifactTag;
      String expectedArtifactVersionId = CLINICALKNOWLEDGEMANAGEMENT_MAYO_ARTIFACTS_BASE_URI
          + expectedArtifactTag + "/versions/1.8";

      Model m = convert(dmn, meta, KnowledgeRepresentationLanguage.DMN_1_2);

      Optional<KnowledgeAsset> s = JaxbUtil.unmarshall(ObjectFactory.class,
          KnowledgeAsset.class,
          m.getSurrogate());
      assertTrue(s.isPresent());
      assertEquals(
          "https://clinicalknowledgemanagement.mayo.edu/assets/3c66cf3a-93c4-4e09-b1aa-14088c76aded",
          s.get().getAssetId().getUri().toString());
      assertEquals(expectedAssetTag, s.get().getAssetId().getTag());
      assertEquals(expectedVersion, s.get().getAssetId().getVersion());
      assertEquals(expectedAssetVersionId,
          s.get().getAssetId().getVersionId().toString());

      assertEquals(expectedArtifactId,
          s.get().getCarriers().get(0).getArtifactId().getUri().toString());
      assertEquals(expectedArtifactVersionId,
          s.get().getCarriers().get(0).getArtifactId().getVersionId().toString());

      // TODO: more to verify here? CAO
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      XMLUtil.streamXMLDocument(m.getModel(), baos);
      assertTrue(new String(baos.toByteArray())
          .contains("KnowledgeAssetType_Scheme - Semantic Decision Model"));

    } catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  void testChainCMMN_BasicModel() {
    try {

      String cmmnPath = "/Basic Case Model.cmmn"; // cao: The xml for the CMMN
      InputStream cmmn = ChainTest.class.getResourceAsStream(cmmnPath);
      String modelInfoPath = "/Basic Case ModelMeta.json";
      String expectedAssetVersionId =
          Registry.MAYO_ASSETS_BASE_URI + "14321e7c-cb9a-427f-abf5-1420bf26e03c/versions/1.0.1";
      String expectedAssetId =
          Registry.MAYO_ASSETS_BASE_URI + "14321e7c-cb9a-427f-abf5-1420bf26e03c";
      String expectedAssetTag = "14321e7c-cb9a-427f-abf5-1420bf26e03c";
      String expectedArtifactVersionId = CLINICALKNOWLEDGEMANAGEMENT_MAYO_ARTIFACTS_BASE_URI
          + "16086bb8-c1fc-49b0-800b-c9b995dc5ed5/versions/1.8.0";
      String expectedArtifactId = CLINICALKNOWLEDGEMANAGEMENT_MAYO_ARTIFACTS_BASE_URI
          + "16086bb8-c1fc-49b0-800b-c9b995dc5ed5";

      InputStream modelInfo = ChainTest.class.getResourceAsStream(modelInfoPath);

      Model m = convert(cmmn, modelInfo, KnowledgeRepresentationLanguage.CMMN_1_1);

      Optional<KnowledgeAsset> s = JaxbUtil.unmarshall(ObjectFactory.class,
          KnowledgeAsset.class,
          m.getSurrogate());
      assertTrue(s.isPresent());
      System.out.println("s: " + s.get());
      System.out.println("s assetId: " + s.get().getAssetId());
      assertEquals(expectedAssetVersionId, s.get().getAssetId().getVersionId().toString());
      assertEquals(expectedAssetId, s.get().getAssetId().getUri().toString());
      assertEquals(expectedAssetTag, s.get().getAssetId().getTag());
      assertEquals("1.0.1", s.get().getAssetId().getVersion());

      assertEquals(expectedArtifactId,
          s.get().getCarriers().get(0).getArtifactId().getUri().toString());
      assertEquals(expectedArtifactVersionId,
          s.get().getCarriers().get(0).getArtifactId().getVersionId().toString());
      // TODO: More to check for here??? CAO
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      XMLUtil.streamXMLDocument(m.getModel(), baos);
      assertTrue(new String(baos.toByteArray()).contains("Basic Case Model"));


    } catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  void testChainCMMN_SimpleModel() {
    try {

      String cmmnPath = "/Weave Test 1.cmmn"; // cao: The xml for the CMMN
      InputStream cmmn = ChainTest.class.getResourceAsStream(cmmnPath);
      String modelInfoPath = "/WeaveTest1Meta.json";
      String expectedAssetTag = "3c99cf3a-93c4-4e09-b1aa-14088c76aded";
      String expectedAssetId = Registry.MAYO_ASSETS_BASE_URI + expectedAssetTag;
      String expectedAssetVersionId = expectedAssetId + "/versions/1.0.0-SNAPSHOT";
      String expectedArtifactTag = "f59708b6-96c0-4aa3-be4a-31e075d76ec9";
      String expectedArtifactId =
          CLINICALKNOWLEDGEMANAGEMENT_MAYO_ARTIFACTS_BASE_URI + expectedArtifactTag;
      String expectedArtifactVersionId = expectedArtifactId + "/versions/2.2.1";

      InputStream modelInfo = ChainTest.class.getResourceAsStream(modelInfoPath);

      Model m = convert(cmmn, modelInfo, KnowledgeRepresentationLanguage.CMMN_1_1);

      Optional<KnowledgeAsset> s = JaxbUtil.unmarshall(ObjectFactory.class,
          KnowledgeAsset.class,
          m.getSurrogate());
      assertTrue(s.isPresent());
      System.out.println("s: " + s.get());
      System.out.println("s assetId: " + s.get().getAssetId());
      assertEquals(expectedAssetTag, s.get().getAssetId().getTag());
      assertEquals(expectedAssetId,
          s.get().getAssetId().getUri().toString());
      assertEquals(expectedAssetVersionId,
          s.get().getAssetId().getVersionId().toString());

      assertEquals(expectedArtifactTag,
          s.get().getCarriers().get(0).getArtifactId().getTag());
      assertEquals(expectedArtifactId,
          s.get().getCarriers().get(0).getArtifactId().getUri().toString());
      assertEquals(expectedArtifactVersionId,
          s.get().getCarriers().get(0).getArtifactId().getVersionId().toString());
      // TODO: other things to verify? CAO
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      XMLUtil.streamXMLDocument(m.getModel(), baos);
      assertTrue(new String(baos.toByteArray()).contains("Weave Test 1"));


    } catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }


}
