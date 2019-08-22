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
package edu.mayo.kmdp.preprocess.meta;

import edu.mayo.kmdp.metadata.surrogate.Representation;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.util.JSonUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
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
  // file for testing the negative -- old file in Signavio format
  String badPath  = "/R2R.dmn";
  String badMeta = "/R2R_Info.json";
  Document dmnDox;
  Document cmmnDox;
  Document badDox;
  TrisotechFileInfo dmnFile;
  TrisotechFileInfo cmmnFile;
  TrisotechFileInfo badFile;

  @BeforeEach
  void setUp() {
   this.tes = new TrisotechExtractionStrategy();
    InputStream dmnStream = MetadataExtractor.class.getResourceAsStream( dmnMeta );
    InputStream cmmnStream = MetadataExtractor.class.getResourceAsStream( cmmnMeta );
    InputStream badStream = MetadataExtractor.class.getResourceAsStream( badMeta );

    dmnDox = loadXMLDocument( resolveResource( dmnPath ) ).orElseGet( () -> fail( "Unable to load document " + dmnPath ) );
    cmmnDox = loadXMLDocument( resolveResource( cmmnPath ) ).orElseGet( () -> fail( "Unable to load document " + cmmnPath ) );
    badDox = loadXMLDocument( resolveResource( badPath ) ).orElseGet( () -> fail( "Unable to load document " + badPath ) );
    dmnFile = JSonUtil.readJson( dmnStream )
        .flatMap((j) -> JSonUtil.parseJson(j, TrisotechFileInfo.class)).get();
    cmmnFile = JSonUtil.readJson( cmmnStream )
        .flatMap( (j) -> JSonUtil.parseJson( j, TrisotechFileInfo.class ) ).get();
    badFile = JSonUtil.readJson( badStream )
        .flatMap( (j) -> JSonUtil.parseJson( j, TrisotechFileInfo.class ) ).get();
  }

  @AfterEach
  void tearDown() {
    this.tes = null;
    dmnDox = null;
    cmmnDox = null;
    badDox = null;
  }


  @Test
  void getArtifactID() {

    Optional<String> value = this.tes.getArtifactID(dmnDox, dmnFile);
    assertNotNull(value.get());

    value = this.tes.getArtifactID(cmmnDox, cmmnFile);
    assertNotNull(value.get());

    value = this.tes.getArtifactID(badDox, badFile );
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


}