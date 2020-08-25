package edu.mayo.kmdp.preprocess.meta;

import static edu.mayo.kmdp.util.XMLUtil.streamXMLDocument;
import static org.junit.jupiter.api.Assertions.fail;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.CMMN_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.DMN_1_2;

import edu.mayo.kmdp.preprocess.meta.beans.ChainConverter;
import edu.mayo.kmdp.preprocess.meta.beans.MetadataExtractorTestConfig;
import edu.mayo.kmdp.preprocess.meta.beans.Model;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * This class is used to generate surrogates from the MEA or MEA-Test models.
 * The surrogates will be output to a local directory.
 * This process isn't ideal as it doesn't download the models, but relies on input files
 * of the downloaded models and their metadata, but it was quick to put together since it
 * was based on ChainTest.
 * This can also cause some problems because IdentityMapper will still pull the latest information
 * so if the model downloaded and the latest don't match (one is published, one is not, etc).
 * Ideally, this would be more like a jobRunner pipe to download the files and produce the output,
 * but not upload to the server as this is outputting files that can be used in the PD pipeline test.
 *
 * Some setup is needed to use this file:
 * 1. Download the XML for the models needed if not already in afib, dictionary or SharedCardiologyDecisions
 *    sub-directories in resources. Basic models are in resources.
 * 2. Copy the meta from postman to <model name>Meta.json
 * 3. Set MEA name and ID in extractor application.properties for MEA (convertModelsAfib).
 *    Set MEA-Test and ID in extractor application.properties for MEA-Test. (default)
 * 4. Add token to the appropriate properties file needed.
 * 5. In printConvertResults, change the date on the extension.
 * 6. Run whichever method is desired to generate the output.
 *
 * The afib output files can then be used in knew-asset-factory for PD generation testing.
 */

@Disabled("For developer use only")
@SpringBootTest
@SpringJUnitConfig(classes = {MetadataExtractorTestConfig.class})
@ActiveProfiles("dev")
@TestPropertySource(properties = {
    "edu.mayo.kmdp.trisotechwrapper.repositoryName=MEA-Test",
    "edu.mayo.kmdp.trisotechwrapper.repositoryId=d4aca01b-d446-4bc8-a6f0-85d84f4c1aaf"})
public class GenerateModelFilesTest {
  Model m;
  String dmnPath;
  InputStream dmn;
  String metaPath;
  InputStream meta;
  String cmmnPath;
  InputStream cmmn;
  String cmmnMetaPath;
  InputStream cmmnMeta;


  // FYI: The IDE may complain that it can't find a Bean for chainConverter,
  // but it's an issue with IDE. The code works.
  @Autowired
  private ChainConverter chainConverter;

  private Model convert(InputStream model, InputStream meta, KnowledgeRepresentationLanguageSeries type) {
    Model m = chainConverter.convert(meta, model, type);

//    assertNotNull(m.getModel());
//    assertNotNull(m.getSurrogate());

    return m;
  }

  @Disabled("only for developers as environment needs modification")
  @Test
  void convertModelsAfib() {
    try {
      dmnPath = "/afib/Anticoagulation Recommendation.dmn";
      dmn = GenerateModelFilesTest.class.getResourceAsStream(dmnPath);
      metaPath = "/afib/Anticoagulation RecommendationMeta.json";
      meta = GenerateModelFilesTest.class.getResourceAsStream(metaPath);
      m = convert(dmn, meta, DMN_1_2);

      printConvertResults(m, "/afib/Anticoagulation Recommendation");

      cmmnPath = "/afib/Atrial Fibrillation.cmmn";
      cmmn = GenerateModelFilesTest.class.getResourceAsStream(cmmnPath);
      cmmnMetaPath = "/afib/Atrial FibrillationMeta.json";
      cmmnMeta = GenerateModelFilesTest.class.getResourceAsStream(cmmnMetaPath);
      m = convert(cmmn, cmmnMeta, CMMN_1_1);

      printConvertResults(m, "/afib/Atrial Fibrillation");

      dmnPath = "/afib/Choice of Atrial Fibrillation Treatment Strategy.dmn";
      dmn = GenerateModelFilesTest.class.getResourceAsStream(dmnPath);
      metaPath = "/afib/Choice of Atrial Fibrillation Treatment StrategyMeta.json";
      meta = GenerateModelFilesTest.class.getResourceAsStream(metaPath);
      m = convert(dmn, meta, DMN_1_2);

      printConvertResults(m, "/afib/Choice of Atrial Fibrillation Treatment Strategy");

      dmnPath = "/afib/Choice of Long-Term Management of Coagulation Status.dmn";
      dmn = GenerateModelFilesTest.class.getResourceAsStream(dmnPath);
      metaPath = "/afib/Choice of Long-Term Management of Coagulation StatusMeta.json";
      meta = GenerateModelFilesTest.class.getResourceAsStream(metaPath);
      m = convert(dmn, meta, DMN_1_2);

      printConvertResults(m, "/afib/Choice of Long-Term Management of Coagulation Status");

      dmnPath = "/afib/Choice of Treatment of Abnormal Heart Rate.dmn";
      dmn = GenerateModelFilesTest.class.getResourceAsStream(dmnPath);
      metaPath = "/afib/Choice of Treatment of Abnormal Heart RateMeta.json";
      meta = GenerateModelFilesTest.class.getResourceAsStream(metaPath);
      m = convert(dmn, meta, DMN_1_2);

      printConvertResults(m, "/afib/Choice of Treatment of Abnormal Heart Rate");

      dmnPath = "/afib/Choice of Treatment of Abnormal Heart Rhythm.dmn";
      dmn = GenerateModelFilesTest.class.getResourceAsStream(dmnPath);
      metaPath = "/afib/Choice of Treatment of Abnormal Heart RhythmMeta.json";
      meta = GenerateModelFilesTest.class.getResourceAsStream(metaPath);
      m = convert(dmn, meta, DMN_1_2);

      printConvertResults(m, "/afib/Choice of Treatment of Abnormal Heart Rhythm");

      dmnPath = "/afib/Prior Management of Atrial Fibrillation.dmn";
      dmn = GenerateModelFilesTest.class.getResourceAsStream(dmnPath);
      metaPath = "/afib/Prior Management of Atrial FibrillationMeta.json";
      meta = GenerateModelFilesTest.class.getResourceAsStream(metaPath);
      m = convert(dmn, meta, DMN_1_2);

      printConvertResults(m, "/afib/Prior Management of Atrial Fibrillation");

      dmnPath = "/afib/Current Status of Atrial Fibrillation.dmn";
      dmn = GenerateModelFilesTest.class.getResourceAsStream(dmnPath);
      metaPath = "/afib/Current Status of Atrial FibrillationMeta.json";
      meta = GenerateModelFilesTest.class.getResourceAsStream(metaPath);
      m = convert(dmn, meta, DMN_1_2);

      printConvertResults(m, "/afib/Current Status of Atrial Fibrillation");


      dmnPath = "/afib/CHA2DS2Vasc Score Model.dmn";
      dmn = GenerateModelFilesTest.class.getResourceAsStream(dmnPath);
      metaPath = "/afib/CHA2DS2Vasc Score ModelMeta.json";
      meta = GenerateModelFilesTest.class.getResourceAsStream(metaPath);
      m = convert(dmn, meta, DMN_1_2);

      printConvertResults(m, "/afib/CHA2DS2Vasc Score Model");

      dmnPath = "/afib/HAS-BLED Score Model.dmn";
      dmn = GenerateModelFilesTest.class.getResourceAsStream(dmnPath);
      metaPath = "/afib/HAS-BLED Score ModelMeta.json";
      meta = GenerateModelFilesTest.class.getResourceAsStream(metaPath);
      m = convert(dmn, meta, DMN_1_2);

      printConvertResults(m, "/afib/HAS-BLED Score Model");

      dmnPath = "/afib/History Pertinent to Atrial Fibrillation.dmn";
      dmn = GenerateModelFilesTest.class.getResourceAsStream(dmnPath);
      metaPath = "/afib/History Pertinent to Atrial FibrillationMeta.json";
      meta = GenerateModelFilesTest.class.getResourceAsStream(metaPath);
      m = convert(dmn, meta, DMN_1_2);

      printConvertResults(m, "/afib/History Pertinent to Atrial Fibrillation");

      dmnPath = "/SharedCardiologyDecisions/Current Cardiac Status.dmn";
      dmn = GenerateModelFilesTest.class.getResourceAsStream(dmnPath);
      metaPath = "/SharedCardiologyDecisions/Current Cardiac StatusMeta.json";
      meta = GenerateModelFilesTest.class.getResourceAsStream(metaPath);
      m = convert(dmn, meta, DMN_1_2);

      printConvertResults(m, "/SharedCardiologyDecisions/Current Cardiac Status");

      dmnPath = "/SharedCardiologyDecisions/History Pertinent to Heart Disease.dmn";
      dmn = GenerateModelFilesTest.class.getResourceAsStream(dmnPath);
      metaPath = "/SharedCardiologyDecisions/History Pertinent to Heart DiseaseMeta.json";
      meta = GenerateModelFilesTest.class.getResourceAsStream(metaPath);
      m = convert(dmn, meta, DMN_1_2);

      printConvertResults(m, "/SharedCardiologyDecisions/History Pertinent to Heart Disease");


      //      Optional<KnowledgeAsset> s = JaxbUtil.unmarshall(ObjectFactory.class,
      //          KnowledgeAsset.class,
      //          m.getSurrogate());

    } catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }

  }

  @Disabled("only for developers as environment needs modification")
  @Test
  void convertModelsBasic() {
    cmmnPath = "/Basic Case Model.cmmn";
    cmmn = GenerateModelFilesTest.class.getResourceAsStream(cmmnPath);
    cmmnMetaPath = "/Basic Case ModelMeta.json";
    cmmnMeta = GenerateModelFilesTest.class.getResourceAsStream(cmmnMetaPath);

    m = convert(cmmn, cmmnMeta, CMMN_1_1);
    printConvertResults(m, "/basic/Basic Case Model");


    dmnPath = "/Basic Decision Model.dmn";
    dmn = GenerateModelFilesTest.class.getResourceAsStream(dmnPath);
    metaPath = "/Basic Decision ModelMeta.json";
    meta = GenerateModelFilesTest.class.getResourceAsStream(metaPath);
    m = convert(dmn, meta, DMN_1_2);

    printConvertResults(m, "/basic/Basic Decision Model");

    dmnPath = "/Computable Decision Model.dmn";
    dmn = GenerateModelFilesTest.class.getResourceAsStream(dmnPath);
    metaPath = "/ComputableDecisionModelMeta.json";
    meta = GenerateModelFilesTest.class.getResourceAsStream(metaPath);
    m = convert(dmn, meta, DMN_1_2);

    printConvertResults(m, "/basic/Computable Decision Model");

    dmnPath = "/Decision Reuse.dmn";
    dmn = GenerateModelFilesTest.class.getResourceAsStream(dmnPath);
    metaPath = "/DecisionReuseMeta.json";
    meta = GenerateModelFilesTest.class.getResourceAsStream(metaPath);
    m = convert(dmn, meta, DMN_1_2);

    printConvertResults(m, "/basic/Decision Reuse");

    dmnPath = "/Decision Subdecision.dmn";
    dmn = GenerateModelFilesTest.class.getResourceAsStream(dmnPath);
    metaPath = "/DecisionSubdecisionMeta.json";
    meta = GenerateModelFilesTest.class.getResourceAsStream(metaPath);
    m = convert(dmn, meta, DMN_1_2);

    printConvertResults(m, "/basic/Decision Subdecision");

    dmnPath = "/Operational Definition Model.dmn";
    dmn = GenerateModelFilesTest.class.getResourceAsStream(dmnPath);
    metaPath = "/Operational Definition ModelMeta.json";
    meta = GenerateModelFilesTest.class.getResourceAsStream(metaPath);
    m = convert(dmn, meta, DMN_1_2);

    printConvertResults(m, "/basic/Operational Definition Model");

    dmnPath = "/recommendation chain.dmn";
    dmn = GenerateModelFilesTest.class.getResourceAsStream(dmnPath);
    metaPath = "/recommendationChainMeta.json";
    meta = GenerateModelFilesTest.class.getResourceAsStream(metaPath);
    m = convert(dmn, meta, DMN_1_2);

    printConvertResults(m, "/basic/recommendation chain");
  }

  @Disabled("only for developers as environment needs modification")
  @Test
  void convertModelsWeave() {
    cmmnPath = "/Weave Test 1.cmmn";
    cmmn = GenerateModelFilesTest.class.getResourceAsStream(cmmnPath);
    cmmnMetaPath = "/WeaveTest1Meta.json";
    cmmnMeta = GenerateModelFilesTest.class.getResourceAsStream(cmmnMetaPath);

    m = convert(cmmn, cmmnMeta, CMMN_1_1);
    printConvertResults(m, "/weave/Weave Test 1");

    dmnPath = "/Weaver Test 1.dmn";
    dmn = GenerateModelFilesTest.class.getResourceAsStream(dmnPath);
    metaPath = "/WeaverTest1Meta.json";
    meta = GenerateModelFilesTest.class.getResourceAsStream(metaPath);
    m = convert(dmn, meta, DMN_1_2);

    printConvertResults(m, "/weave/Weaver Test 1");

    dmnPath = "/Weaver Test 2.dmn";
    dmn = GenerateModelFilesTest.class.getResourceAsStream(dmnPath);
    metaPath = "/Weaver Test 2Meta.json";
    meta = GenerateModelFilesTest.class.getResourceAsStream(metaPath);
    m = convert(dmn, meta, DMN_1_2);

    printConvertResults(m, "/weave/Weaver Test 2");

  }

    // Creates files in $HOME (C: on Windows)
  // if a path is defined in the fileName, the directory will be created if it doesn't already exist
  private void printConvertResults(Model m, String fileName) {
    if(m.getModel() == null) {
      System.out.println("Model is null for " + fileName + " will NOT generate output files");
      return;
    }
    if(m.getSurrogate() == null) {
      System.out.println("Surrogate is null for " + fileName + " will NOT generate output files");
      return;
    }
    System.out.println("fileName: " + fileName);

    File surrogateFile = new File(fileName + "Surrogate_4_26.xml");
    File modelFile = new File(fileName + "_AfterWeave_4_26.xml");
    System.out.println("surrogate parentFile: " + surrogateFile.getParent());
    System.out.println("model parentFile: " + modelFile.getParent());
    System.out.println("modelFile name: " + modelFile.getName());
    System.out.println("modelFile path: " + modelFile.getPath());
    try {
      surrogateFile.getParentFile().mkdirs();
      modelFile.getParentFile().mkdirs();
      surrogateFile.createNewFile();
      modelFile.createNewFile();
      System.out.println("modelFile canonicalPath: " + modelFile.getCanonicalPath().toString());
      System.out.println("modelFile canonicalFile: " + modelFile.getCanonicalFile().toString());

      FileOutputStream fos = new FileOutputStream(surrogateFile);

      streamXMLDocument(m.getSurrogate(), fos);
      //      System.out.println("surrogate: ");
      //      streamXMLDocument(m.getSurrogate(), System.out);
      fos.flush();
      fos.close();

      fos = new FileOutputStream(modelFile);
      streamXMLDocument(m.getModel(), fos);
      fos.flush();
      fos.close();

      //      System.out.println();
      //      System.out.println("model: ");
      //      streamXMLDocument(m.getModel(), System.out);
      //      System.out.println();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

}
