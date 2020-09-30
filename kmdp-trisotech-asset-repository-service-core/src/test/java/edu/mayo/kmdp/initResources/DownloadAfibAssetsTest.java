package edu.mayo.kmdp.initResources;


import static java.nio.charset.Charset.defaultCharset;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.snapshot.SerializationFormat.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.snapshot.KnowledgeRepresentationLanguage.CMMN_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.snapshot.KnowledgeRepresentationLanguage.DMN_1_2;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.snapshot.KnowledgeRepresentationLanguage.Knowledge_Asset_Surrogate_2_0;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel._20200801.ParsingLevel.Serialized_Knowledge_Expression;

import edu.mayo.kmdp.TrisotechAssetRepositoryConfig;
import edu.mayo.kmdp.kdcaci.knew.trisotech.TrisotechAssetRepository;
import edu.mayo.kmdp.language.parsers.cmmn.v1_1.CMMN11Parser;
import edu.mayo.kmdp.language.parsers.dmn.v1_2.DMN12Parser;
import edu.mayo.kmdp.language.parsers.surrogate.v2.Surrogate2Parser;
import edu.mayo.kmdp.trisotechwrapper.TrisotechWrapper;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.util.FileUtil;
import edu.mayo.kmdp.util.JSonUtil;
import edu.mayo.kmdp.util.XMLUtil;
import java.io.File;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.transrepresentation.ModelMIMECoder;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.w3c.dom.Document;

/*
  Use this class to re-initialize the test models in the /resources folder

  Downloads the surrogate (native TT and KMDP standard) as well as the models
  (pre and post rewriting of the semantic annotations) to support testing
  of the different components
 */

@SpringBootTest
@ContextConfiguration(classes = TrisotechAssetRepositoryConfig.class)
@TestPropertySource(properties = {
    "edu.mayo.kmdp.trisotechwrapper.repositoryName=MEA",
    "edu.mayo.kmdp.trisotechwrapper.repositoryPath=/Shellum/Atrial Fibrillation",
    "edu.mayo.kmdp.trisotechwrapper.repositoryId=8773e388-75b7-4061-a489-b788222022d3"})
public class DownloadAfibAssetsTest extends AbstractAssetDownloader {

  @Autowired
  TrisotechAssetRepository assetRepository;

  @Autowired
  TrisotechWrapper wrapper;

  @Test
  @Disabled
  void testInit() {
    assertNotNull(assetRepository);

    assetRepository.listKnowledgeAssets()
        .forEach(Pointer.class, assetPtr -> saveArtifacts(assetPtr,getTgtFolder()));
  }

  @Override
  protected File getTgtFolder() {
    return new File(getParent(), "Atrial Fibrillation");
  }
}
