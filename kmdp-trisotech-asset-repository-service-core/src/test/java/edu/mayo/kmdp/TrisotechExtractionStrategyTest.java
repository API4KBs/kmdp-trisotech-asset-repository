/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package edu.mayo.kmdp;

import static edu.mayo.kmdp.util.Util.resolveResource;
import static edu.mayo.kmdp.util.XMLUtil.loadXMLDocument;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.MetadataIntrospector;
import edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.TrisotechIntrospectionStrategy;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.util.JSonUtil;
import java.io.InputStream;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries;
import org.w3c.dom.Document;

class TrisotechExtractionStrategyTest {

  TrisotechIntrospectionStrategy tes;
  String dmnPath = "/Weaver Test 1.dmn.xml";
  String dmnMeta = "/Weaver Test 1.meta.json";
  String cmmnPath = "/Weave Test 1.cmmn.xml";
  String cmmnMeta = "/Weave Test 1.meta.json";
  String basicCasePath = "/Basic Case Model.raw.cmmn.xml";
  String basicCaseWovenPath = "/Basic Case Model.cmmn.xml";
  String basicCaseMeta = "/Basic Case Model.meta.json";
  String basicDecisionPath = "/Basic Decision Model.raw.dmn.xml";
  String basicDecisionWovenPath = "/Basic Decision Model.dmn.xml";
  String basicDecisionMeta = "/Basic Decision Model.meta.json";

  Document dmnDox;
  Document cmmnDox;
  Document badDox;
  Document basicCaseDox;
  Document basicDecisionDox;
  TrisotechFileInfo dmnFile;
  TrisotechFileInfo cmmnFile;
  TrisotechFileInfo badFile;
  TrisotechFileInfo basicCaseFile;
  TrisotechFileInfo basicDecisionFile;

  @BeforeEach
  void setUp() {
    this.tes = new TrisotechIntrospectionStrategy();
    InputStream dmnStream = MetadataIntrospector.class.getResourceAsStream(dmnMeta);
    InputStream cmmnStream = MetadataIntrospector.class.getResourceAsStream(cmmnMeta);
    InputStream baseCaseStream = MetadataIntrospector.class.getResourceAsStream(basicCaseMeta);
    InputStream basicDecisionStream = MetadataIntrospector.class
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
        .flatMap((j) -> JSonUtil.parseJson(j, TrisotechFileInfo.class)).orElseGet(Assertions::fail);
    cmmnFile = JSonUtil.readJson(cmmnStream)
        .flatMap((j) -> JSonUtil.parseJson(j, TrisotechFileInfo.class)).orElseGet(Assertions::fail);
    basicCaseFile = JSonUtil.readJson(baseCaseStream)
        .flatMap((j) -> JSonUtil.parseJson(j, TrisotechFileInfo.class)).orElseGet(Assertions::fail);
    basicDecisionFile = JSonUtil.readJson(basicDecisionStream)
        .flatMap((j) -> JSonUtil.parseJson(j, TrisotechFileInfo.class)).orElseGet(Assertions::fail);

  }

  @AfterEach
  void tearDown() {
    this.tes = null;
    dmnDox = null;
    cmmnDox = null;
    badDox = null;
    basicCaseDox = null;
    basicDecisionDox = null;
  }


  @Test
  void getArtifactID() {

    String expectedDMNId = "https://clinicalknowledgemanagement.mayo.edu/artifacts/5682fa26-b064-43c8-9475-1e4281e74068";
    Optional<String> value = this.tes.getArtifactID(dmnDox);
    assertNotNull(value.orElseGet(Assertions::fail));
    assertEquals(expectedDMNId, value.orElseGet(Assertions::fail));

    String expectedCMMNId = "https://clinicalknowledgemanagement.mayo.edu/artifacts/f59708b6-96c0-4aa3-be4a-31e075d76ec9";
    value = this.tes.getArtifactID(cmmnDox);
    assertNotNull(value.orElseGet(Assertions::fail));
    assertEquals(expectedCMMNId, value.orElseGet(Assertions::fail));

    value = this.tes.getArtifactID(badDox);
    assertFalse(value.isPresent());

  }

  @Test
  void getRepLanguage() {
    Optional<SyntacticRepresentation> dmnRep = this.tes.getRepLanguage(dmnDox);
    assertEquals("DMN_1_2", dmnRep.orElseGet(Assertions::fail).getLanguage().toString());

    Optional<SyntacticRepresentation> cmmnRep = this.tes.getRepLanguage(cmmnDox);
    assertEquals("CMMN_1_1", cmmnRep.orElseGet(Assertions::fail).getLanguage().toString());

    Optional<SyntacticRepresentation> badRep = this.tes.getRepLanguage(badDox);
    assertEquals(Optional.empty(), badRep);
  }

  @Test
  void detectRepLanguage() {
    Optional<KnowledgeRepresentationLanguage> dmnRep = this.tes.detectRepLanguage(dmnDox);
    assertEquals("DMN 1.2", dmnRep.orElseGet(Assertions::fail).getLabel());
    assertEquals(KnowledgeRepresentationLanguageSeries.DMN_1_2, dmnRep.orElseGet(Assertions::fail));

    Optional<KnowledgeRepresentationLanguage> cmmnRep = this.tes.detectRepLanguage(cmmnDox);
    assertEquals("CMMN 1.1", cmmnRep.orElseGet(Assertions::fail).getLabel());
    assertEquals(KnowledgeRepresentationLanguageSeries.CMMN_1_1, cmmnRep.orElseGet(Assertions::fail));

    Optional<KnowledgeRepresentationLanguage> badRep = this.tes.detectRepLanguage(badDox);
    assertEquals(Optional.empty(), badRep);
  }

}