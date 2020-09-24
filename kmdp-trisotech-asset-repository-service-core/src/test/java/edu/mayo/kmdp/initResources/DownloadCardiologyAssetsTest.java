package edu.mayo.kmdp.initResources;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;

import edu.mayo.kmdp.TrisotechAssetRepositoryConfig;
import edu.mayo.kmdp.kdcaci.knew.trisotech.TrisotechAssetRepository;
import edu.mayo.kmdp.trisotechwrapper.TrisotechWrapper;
import java.io.File;
import java.util.Collections;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

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
    "edu.mayo.kmdp.trisotechwrapper.repositoryPath=/Shellum/Shared Cardiology Decisions",
    "edu.mayo.kmdp.trisotechwrapper.repositoryId=8773e388-75b7-4061-a489-b788222022d3"})
public class DownloadCardiologyAssetsTest extends AbstractAssetDownloader {

  @Autowired
  TrisotechAssetRepository assetRepository;

  @Autowired
  TrisotechWrapper wrapper;

  File tgtFolder = new File(
      "C:\\Users\\M123110\\Projects\\MEA"
          + "\\kmdp-trisotech-asset-repository-service"
          + "\\kmdp-trisotech-asset-repository-service-core"
          + "\\src\\test\\resources"
          + "\\Shared Cardiology Decisions");

  @Test
  @Disabled
  void testInit() {
    assertNotNull(assetRepository);

    assetRepository.listKnowledgeAssets()
        .ifPresent(l -> l.forEach(assetPtr -> saveArtifacts(assetPtr,tgtFolder)));
  }

  @Override
  protected File getTgtFolder() {
    return tgtFolder;
  }
}