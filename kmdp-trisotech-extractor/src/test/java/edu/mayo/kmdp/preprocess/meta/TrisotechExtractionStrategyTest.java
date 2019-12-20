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
package edu.mayo.kmdp.preprocess.meta;

import edu.mayo.kmdp.metadata.surrogate.Representation;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.util.JSonUtil;
import edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._1_0.identifiers.URIIdentifier;
import org.w3c.dom.Document;

import java.io.InputStream;
import java.util.Optional;

import static edu.mayo.kmdp.util.Util.resolveResource;
import static edu.mayo.kmdp.util.XMLUtil.loadXMLDocument;
import static org.junit.jupiter.api.Assertions.*;

class TrisotechExtractionStrategyTest {

  TrisotechExtractionStrategy tes;
  String dmnPath = "/Weaver Test 1.dmn";
  String dmnMeta = "/WeaverTest1Meta.json";
  String cmmnPath = "/Weave Test 1.cmmn";
  String cmmnMeta = "/WeaveTest1Meta.json";
  String basicCasePath = "/Basic Case Model.cmmn";
  String basicCaseWovenPath = "/Basic Case Model_afterWeave.cmmn";
  String basicCaseMeta = "/Basic Case ModelMeta.json";
  String basicDecisionPath = "/Basic Decision Model.dmn";
  String basicDecisionWovenPath = "/Basic Decision Model_afterWeave.dmn";
  String basicDecisionMeta = "/Basic Decision ModelMeta.json";
  // file for testing the negative -- old file in Signavio format
  String badPath = "/R2R.dmn";
  String badMeta = "/R2R_Info.json";
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
    this.tes = new TrisotechExtractionStrategy();
    InputStream dmnStream = MetadataExtractor.class.getResourceAsStream(dmnMeta);
    InputStream cmmnStream = MetadataExtractor.class.getResourceAsStream(cmmnMeta);
    InputStream badStream = MetadataExtractor.class.getResourceAsStream(badMeta);
    InputStream baseCaseStream = MetadataExtractor.class.getResourceAsStream(basicCaseMeta);
    InputStream basicDecisionStream = MetadataExtractor.class
        .getResourceAsStream(basicDecisionMeta);

    dmnDox = loadXMLDocument(resolveResource(dmnPath))
        .orElseGet(() -> fail("Unable to load document " + dmnPath));
    cmmnDox = loadXMLDocument(resolveResource(cmmnPath))
        .orElseGet(() -> fail("Unable to load document " + cmmnPath));
    badDox = loadXMLDocument(resolveResource(badPath))
        .orElseGet(() -> fail("Unable to load document " + badPath));
    basicCaseDox = loadXMLDocument(resolveResource(basicCasePath))
        .orElseGet(() -> fail("Unable to load document " + basicCasePath));
    basicDecisionDox = loadXMLDocument(resolveResource(basicDecisionPath))
        .orElseGet(() -> fail("Unable to load document " + basicDecisionPath));
    dmnFile = JSonUtil.readJson(dmnStream)
        .flatMap((j) -> JSonUtil.parseJson(j, TrisotechFileInfo.class)).get();
    cmmnFile = JSonUtil.readJson(cmmnStream)
        .flatMap((j) -> JSonUtil.parseJson(j, TrisotechFileInfo.class)).get();
    badFile = JSonUtil.readJson(badStream)
        .flatMap((j) -> JSonUtil.parseJson(j, TrisotechFileInfo.class)).get();
    basicCaseFile = JSonUtil.readJson(baseCaseStream)
        .flatMap((j) -> JSonUtil.parseJson(j, TrisotechFileInfo.class)).get();
    basicDecisionFile = JSonUtil.readJson(basicDecisionStream)
        .flatMap((j) -> JSonUtil.parseJson(j, TrisotechFileInfo.class)).get();

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

    String expectedDMNId = "http://www.trisotech.com/definitions/_5682fa26-b064-43c8-9475-1e4281e74068";
    Optional<String> value = this.tes.getArtifactID(dmnDox, dmnFile);
    assertNotNull(value.get());
    assertEquals(expectedDMNId, value.get());

    String expectedCMMNId = "http://www.trisotech.com/definitions/_f59708b6-96c0-4aa3-be4a-31e075d76ec9";
    value = this.tes.getArtifactID(cmmnDox, cmmnFile);
    assertNotNull(value.get());
    assertEquals(expectedCMMNId, value.get());

    value = this.tes.getArtifactID(badDox, badFile);
    assertFalse(value.isPresent());

  }

  @Test
  void getRepLanguage() {
    Optional<Representation> dmnRep = this.tes.getRepLanguage(dmnDox, false);
    assertEquals("DMN_1_2", dmnRep.get().getLanguage().toString());

    Optional<Representation> cmmnRep = this.tes.getRepLanguage(cmmnDox, false);
    assertEquals("CMMN_1_1", cmmnRep.get().getLanguage().toString());

    Optional<Representation> badRep = this.tes.getRepLanguage(badDox, false);
    assertEquals(Optional.empty(), badRep);
  }

  @Test
  void detectRepLanguage() {
    Optional<KnowledgeRepresentationLanguageSeries> dmnRep = this.tes.detectRepLanguage(dmnDox);
    assertEquals("DMN 1.2", dmnRep.get().getLabel()); //.getLanguage().toString());
    assertEquals(KnowledgeRepresentationLanguageSeries.DMN_1_2, dmnRep.get());

    Optional<KnowledgeRepresentationLanguageSeries> cmmnRep = this.tes.detectRepLanguage(cmmnDox);
    assertEquals("CMMN 1.1", cmmnRep.get().getLabel()); //.getLanguage().toString());
    assertEquals(KnowledgeRepresentationLanguageSeries.CMMN_1_1, cmmnRep.get());

    Optional<KnowledgeRepresentationLanguageSeries> badRep = this.tes.detectRepLanguage(badDox);
    assertEquals(Optional.empty(), badRep);
  }

  @Test
  void extractAssetID() {
    String expectedBasicCase = "https://clinicalknowledgemanagement.mayo.edu/assets/14321e7c-cb9a-427f-abf5-1420bf26e03c/versions/1.0.1";
    String expectedBasicDecision = "https://clinicalknowledgemanagement.mayo.edu/assets/735a5764-fe3f-4ab8-b103-650b6e805db2/versions/1.0.0";

    basicCaseDox = loadXMLDocument(resolveResource(basicCaseWovenPath))
        .orElseGet(() -> fail("Unable to load document " + basicCaseWovenPath));
    basicDecisionDox = loadXMLDocument(resolveResource(basicDecisionWovenPath))
        .orElseGet(() -> fail("Unable to load document " + basicDecisionWovenPath));

    URIIdentifier uriIdentifier = this.tes.extractAssetID(basicCaseDox);
    assertNotNull(uriIdentifier);
    assertEquals(expectedBasicCase, uriIdentifier.getVersionId().toString());

    uriIdentifier = this.tes.extractAssetID(basicDecisionDox);
    assertNotNull(uriIdentifier);
    assertEquals(expectedBasicDecision, uriIdentifier.getVersionId().toString());

    assertThrows(
        IllegalStateException.class,
        () -> this.tes.extractAssetID(badDox));
  }

}