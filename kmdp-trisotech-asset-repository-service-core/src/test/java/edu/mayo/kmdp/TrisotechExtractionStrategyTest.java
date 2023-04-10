/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package edu.mayo.kmdp;

import static edu.mayo.kmdp.util.Util.resolveResource;
import static edu.mayo.kmdp.util.XMLUtil.loadXMLDocument;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.DMN_1_2;

import edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.BPMMetadataHelper;
import edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.DefaultMetadataIntrospector;
import edu.mayo.kmdp.trisotechwrapper.components.DefaultNamespaceManager;
import edu.mayo.kmdp.trisotechwrapper.components.NamespaceManager;
import edu.mayo.kmdp.trisotechwrapper.components.SemanticModelInfo;
import edu.mayo.kmdp.trisotechwrapper.config.TTWEnvironmentConfiguration;
import edu.mayo.kmdp.util.JSonUtil;
import java.io.InputStream;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries;
import org.w3c.dom.Document;

class TrisotechExtractionStrategyTest {

  NamespaceManager names;

  String dmnPath = "/Weaver Test 1.dmn.xml";
  String dmnMeta = "/Weaver Test 1.meta.json";
  String cmmnPath = "/Weave Test 1.cmmn.xml";
  String cmmnMeta = "/Weave Test 1.meta.json";
  String basicCasePath = "/Basic Case Model.raw.cmmn.xml";
  String basicCaseMeta = "/Basic Case Model.meta.json";
  String basicDecisionPath = "/Basic Decision Model.raw.dmn.xml";
  String basicDecisionMeta = "/Basic Decision Model.meta.json";

  Document dmnDox;
  Document cmmnDox;
  Document badDox;
  Document basicCaseDox;
  Document basicDecisionDox;
  SemanticModelInfo dmnFile;
  SemanticModelInfo cmmnFile;
  SemanticModelInfo basicCaseFile;
  SemanticModelInfo basicDecisionFile;

  @BeforeEach
  void setUp() {
    var cfg = new TTWEnvironmentConfiguration();
    names = new DefaultNamespaceManager(cfg);

    InputStream dmnStream = DefaultMetadataIntrospector.class.getResourceAsStream(dmnMeta);
    InputStream cmmnStream = DefaultMetadataIntrospector.class.getResourceAsStream(cmmnMeta);
    InputStream baseCaseStream = DefaultMetadataIntrospector.class.getResourceAsStream(
        basicCaseMeta);
    InputStream basicDecisionStream = DefaultMetadataIntrospector.class
        .getResourceAsStream(basicDecisionMeta);

    dmnDox = loadXMLDocument(resolveResource(dmnPath))
        .orElseGet(() -> fail("Unable to load document " + dmnPath));
    cmmnDox = loadXMLDocument(resolveResource(cmmnPath))
        .orElseGet(() -> fail("Unable to load document " + cmmnPath));
    basicCaseDox = loadXMLDocument(resolveResource(basicCasePath))
        .orElseGet(() -> fail("Unable to load document " + basicCasePath));
    basicDecisionDox = loadXMLDocument(resolveResource(basicDecisionPath))
        .orElseGet(() -> fail("Unable to load document " + basicDecisionPath));
    dmnFile = JSonUtil.readJson(dmnStream)
        .flatMap((j) -> JSonUtil.parseJson(j, SemanticModelInfo.class)).orElseGet(Assertions::fail);
    cmmnFile = JSonUtil.readJson(cmmnStream)
        .flatMap((j) -> JSonUtil.parseJson(j, SemanticModelInfo.class)).orElseGet(Assertions::fail);
    basicCaseFile = JSonUtil.readJson(baseCaseStream)
        .flatMap((j) -> JSonUtil.parseJson(j, SemanticModelInfo.class)).orElseGet(Assertions::fail);
    basicDecisionFile = JSonUtil.readJson(basicDecisionStream)
        .flatMap((j) -> JSonUtil.parseJson(j, SemanticModelInfo.class)).orElseGet(Assertions::fail);

  }

  @AfterEach
  void tearDown() {
    dmnDox = null;
    cmmnDox = null;
    badDox = null;
    basicCaseDox = null;
    basicDecisionDox = null;
  }


  @Test
  void getArtifactID() {

    String expectedDMNId = "https://clinicalknowledgemanagement.mayo.edu/artifacts/5682fa26-b064-43c8-9475-1e4281e74068";
    var value = names.modelToArtifactId(dmnFile);
    assertEquals(expectedDMNId, value.getResourceId().toString());

    String expectedCMMNId = "https://clinicalknowledgemanagement.mayo.edu/artifacts/f59708b6-96c0-4aa3-be4a-31e075d76ec9";
    value = names.modelToArtifactId(cmmnFile);
    assertEquals(expectedCMMNId, value.getResourceId().toString());

    assertThrows(Exception.class,
        () -> this.names.modelToArtifactId(new SemanticModelInfo()));
  }

  @Test
  void getRepLanguage() {
    var dmnRep = BPMMetadataHelper.getDefaultRepresentation(dmnFile);
    assertEquals("DMN_1_2", dmnRep.orElseGet(Assertions::fail).getLanguage().toString());

    var cmmnRep = BPMMetadataHelper.getDefaultRepresentation(cmmnFile);
    assertEquals("CMMN_1_1", cmmnRep.orElseGet(Assertions::fail).getLanguage().toString());

    var badRep = BPMMetadataHelper.getDefaultRepresentation(new SemanticModelInfo());
    assertEquals(Optional.empty(), badRep);
  }

  @Test
  void detectRepLanguage() {
    var dmnRep = BPMMetadataHelper.detectRepLanguage(dmnFile);
    assertEquals("DMN 1.2", dmnRep.orElseGet(Assertions::fail).getLabel());
    assertEquals(DMN_1_2, dmnRep.orElseGet(Assertions::fail));

    var cmmnRep = BPMMetadataHelper.detectRepLanguage(cmmnFile);
    assertEquals("CMMN 1.1", cmmnRep.orElseGet(Assertions::fail).getLabel());
    assertEquals(KnowledgeRepresentationLanguageSeries.CMMN_1_1,
        cmmnRep.orElseGet(Assertions::fail));

    var badRep = BPMMetadataHelper.detectRepLanguage(new SemanticModelInfo());
    assertEquals(Optional.empty(), badRep);
  }

}