package edu.mayo.kmdp.preprocess.meta;

import edu.mayo.kmdp.metadata.surrogate.Representation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import java.util.Optional;

import static edu.mayo.kmdp.util.Util.resolveResource;
import static edu.mayo.kmdp.util.XMLUtil.loadXMLDocument;
import static org.junit.jupiter.api.Assertions.*;

class TrisotechExtractionStrategyTest {
  TrisotechExtractionStrategy tes;
  String dmnPath = "/WeaverTest1.dmn";
  String cmmnPath = "/WeaveTest1.cmmn";
  String badPath  = "/R2R.dmn";
  Document dmnDox;
  Document cmmnDox;
  Document badDox;

  @BeforeEach
  void setUp() {
   this.tes = new TrisotechExtractionStrategy();
    dmnDox = loadXMLDocument( resolveResource( dmnPath ) ).orElseGet( () -> fail( "Unable to load document " + dmnPath ) );
    cmmnDox = loadXMLDocument( resolveResource( cmmnPath ) ).orElseGet( () -> fail( "Unable to load document " + cmmnPath ) );
    badDox = loadXMLDocument( resolveResource( badPath ) ).orElseGet( () -> fail( "Unable to load document " + badPath ) );
  }

  @AfterEach
  void tearDown() {
    this.tes = null;
    dmnDox = null;
    cmmnDox = null;
    badDox = null;
  }

  @Test
  void getMapper() {
  }

  @Test
  void setMapper() {
  }

  @Test
  void extractXML() {
  }

  @Test
  void extractXML1() {
  }

  @Test
  void trackRepresentationInfo() {
  }

  @Test
  void addSemanticAnnotations() {
  }

  @Test
  void trackArtifact() {
  }

  @Test
  void getResourceID() {
  }

  @Test
  void getIDAnnotationValue() {
  }

  @Test
  void getVersionTag() {
  }

  @Test
  void getArtifactID() {

    Optional<String> value = this.tes.getArtifactID(dmnDox);
    System.out.println("value: " + value.get());
    assertNotNull(value.get());

    value = this.tes.getArtifactID(cmmnDox);
    System.out.println("value: " + value.get());
    assertNotNull(value.get());

    value = this.tes.getArtifactID(badDox);
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
  }

  @Test
  void getMetadataEntryNameForID() {
  }

  @Test
  void extractAssetID() {
  }
}