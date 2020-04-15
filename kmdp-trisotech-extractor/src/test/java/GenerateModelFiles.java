import static edu.mayo.kmdp.util.XMLUtil.streamXMLDocument;
import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.CMMN_1_1;
import static edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries.DMN_1_2;
import static org.junit.jupiter.api.Assertions.fail;

import edu.mayo.kmdp.ChainConverter;
import edu.mayo.kmdp.ChainConverterConfig;
import edu.mayo.kmdp.ExtractorConfig;
import edu.mayo.kmdp.IdentityMapperConfig;
import edu.mayo.kmdp.Model;
import edu.mayo.ontology.taxonomies.krlanguage.KnowledgeRepresentationLanguageSeries;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * This class is used to generate surrogates from the MEA or MEA-Test models.
 * The surrogates will be output to a local directory.
 * This process isn't ideal as it doesn't download the models, but relies on input files
 * of the downloaded models and their metadata, but it was quick to put together since it
 * was based on ChainTest.
 *
 * Some setup is needed to use this file:
 * 1. Download the XML for the models needed if not already in afib, dictionary or SharedCardiologyDecisions
 *    sub-directories. Basic models are in resources.
 * 2. Copy the meta from postman to <model name>Meta.json
 * 3. Set MEA name and ID in extractor application.properties for MEA.
 *    Set MEA-Test and ID in extractor application.properties for MEA-Test. (default)
 * 4. Add token to the appropriate properties file needed.
 * 5. In printConvertResults, change the date on the extension.
 * 6. Run whichever method is desired to generate the output.
 *
 * The output files can then be used in knew-asset-factory for PD generation testing.
 */

@Disabled("For developer use only")
@SpringBootTest
@SpringJUnitConfig(classes = {ExtractorConfig.class, IdentityMapperConfig.class,
    ChainConverterConfig.class})
@ActiveProfiles("dev")
public class GenerateModelFiles {
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
      // FAILS because missing Asset ID
      dmnPath = "/afib/Anticoagulation Recommendation.dmn";
      dmn = ChainTest.class.getResourceAsStream(dmnPath);
      metaPath = "/afib/Anticoagulation RecommendationMeta.json";
      meta = ChainTest.class.getResourceAsStream(metaPath);
      m = convert(dmn, meta, DMN_1_2);

      printConvertResults(m, "/afib/Anticoagulation Recommendation");

      cmmnPath = "/afib/Atrial Fibrillation.cmmn";
      cmmn = ChainTest.class.getResourceAsStream(cmmnPath);
      cmmnMetaPath = "/afib/Atrial FibrillationMeta.json";
      cmmnMeta = ChainTest.class.getResourceAsStream(cmmnMetaPath);
      m = convert(cmmn, cmmnMeta, CMMN_1_1);

      printConvertResults(m, "/afib/Atrial Fibrillation");

      dmnPath = "/afib/Choice of Atrial Fibrillation Treatment Strategy.dmn";
      dmn = ChainTest.class.getResourceAsStream(dmnPath);
      metaPath = "/afib/Choice of Atrial Fibrillation Treatment StrategyMeta.json";
      meta = ChainTest.class.getResourceAsStream(metaPath);
      m = convert(dmn, meta, DMN_1_2);

      printConvertResults(m, "/afib/Choice of Atrial Fibrillation Treatment Strategy");

      // FAILS -- because not published
      dmnPath = "/afib/Choice of Long-Term Management of Coagulation Status.dmn";
      dmn = ChainTest.class.getResourceAsStream(dmnPath);
      metaPath = "/afib/Choice of Long-Term Management of Coagulation StatusMeta.json";
      meta = ChainTest.class.getResourceAsStream(metaPath);
      m = convert(dmn, meta, DMN_1_2);

      printConvertResults(m, "/afib/Choice of Long-Term Management of Coagulation Status");

      dmnPath = "/afib/Choice of Treatment of Abnormal Heart Rate.dmn";
      dmn = ChainTest.class.getResourceAsStream(dmnPath);
      metaPath = "/afib/Choice of Treatment of Abnormal Heart RateMeta.json";
      meta = ChainTest.class.getResourceAsStream(metaPath);
      m = convert(dmn, meta, DMN_1_2);

      printConvertResults(m, "/afib/Choice of Treatment of Abnormal Heart Rate");

      dmnPath = "/afib/Choice of Treatment of Abnormal Heart Rhythm.dmn";
      dmn = ChainTest.class.getResourceAsStream(dmnPath);
      metaPath = "/afib/Choice of Treatment of Abnormal Heart RhythmMeta.json";
      meta = ChainTest.class.getResourceAsStream(metaPath);
      m = convert(dmn, meta, DMN_1_2);

      printConvertResults(m, "/afib/Choice of Treatment of Abnormal Heart Rhythm");

      dmnPath = "/afib/Prior Management of Atrial Fibrillation.dmn";
      dmn = ChainTest.class.getResourceAsStream(dmnPath);
      metaPath = "/afib/Prior Management of Atrial FibrillationMeta.json";
      meta = ChainTest.class.getResourceAsStream(metaPath);
      m = convert(dmn, meta, DMN_1_2);

      printConvertResults(m, "/afib/Prior Management of Atrial Fibrillation");

      dmnPath = "/afib/Current Status of Atrial Fibrillation.dmn";
      dmn = ChainTest.class.getResourceAsStream(dmnPath);
      metaPath = "/afib/Current Status of Atrial FibrillationMeta.json";
      meta = ChainTest.class.getResourceAsStream(metaPath);
      m = convert(dmn, meta, DMN_1_2);

      printConvertResults(m, "/afib/Current Status of Atrial Fibrillation");


      dmnPath = "/afib/CHA2DS2Vasc Score Model.dmn";
      dmn = ChainTest.class.getResourceAsStream(dmnPath);
      metaPath = "/afib/CHA2DS2Vasc Score ModelMeta.json";
      meta = ChainTest.class.getResourceAsStream(metaPath);
      m = convert(dmn, meta, DMN_1_2);

      printConvertResults(m, "/afib/CHA2DS2Vasc Score Model");

      dmnPath = "/afib/HAS-BLED Score Model.dmn";
      dmn = ChainTest.class.getResourceAsStream(dmnPath);
      metaPath = "/afib/HAS-BLED Score ModelMeta.json";
      meta = ChainTest.class.getResourceAsStream(metaPath);
      m = convert(dmn, meta, DMN_1_2);

      printConvertResults(m, "/afib/HAS-BLED Score Model");

      dmnPath = "/afib/History Pertinent to Atrial Fibrillation.dmn";
      dmn = ChainTest.class.getResourceAsStream(dmnPath);
      metaPath = "/afib/History Pertinent to Atrial FibrillationMeta.json";
      meta = ChainTest.class.getResourceAsStream(metaPath);
      m = convert(dmn, meta, DMN_1_2);

      printConvertResults(m, "/afib/History Pertinent to Atrial Fibrillation");

      dmnPath = "/SharedCardiologyDecisions/Current Cardiac Status.dmn";
      dmn = ChainTest.class.getResourceAsStream(dmnPath);
      metaPath = "/SharedCardiologyDecisions/Current Cardiac StatusMeta.json";
      meta = ChainTest.class.getResourceAsStream(metaPath);
      m = convert(dmn, meta, DMN_1_2);

      printConvertResults(m, "/SharedCardiologyDecisions/Current Cardiac Status");

      dmnPath = "/SharedCardiologyDecisions/History Pertinent to Heart Disease.dmn";
      dmn = ChainTest.class.getResourceAsStream(dmnPath);
      metaPath = "/SharedCardiologyDecisions/History Pertinent to Heart DiseaseMeta.json";
      meta = ChainTest.class.getResourceAsStream(metaPath);
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
    cmmn = ChainTest.class.getResourceAsStream(cmmnPath);
    cmmnMetaPath = "/Basic Case ModelMeta.json";
    cmmnMeta = ChainTest.class.getResourceAsStream(cmmnMetaPath);

    m = convert(cmmn, cmmnMeta, CMMN_1_1);
    printConvertResults(m, "/basic/Basic Case Model");


    dmnPath = "/Basic Decision Model.dmn";
    dmn = ChainTest.class.getResourceAsStream(dmnPath);
    metaPath = "/Basic Decision ModelMeta.json";
    meta = ChainTest.class.getResourceAsStream(metaPath);
    m = convert(dmn, meta, DMN_1_2);

    printConvertResults(m, "/basic/Basic Decision Model");

    dmnPath = "/Computable Decision Model.dmn";
    dmn = ChainTest.class.getResourceAsStream(dmnPath);
    metaPath = "/ComputableDecisionModelMeta.json";
    meta = ChainTest.class.getResourceAsStream(metaPath);
    m = convert(dmn, meta, DMN_1_2);

    printConvertResults(m, "/basic/Computable Decision Model");

    dmnPath = "/Decision Reuse.dmn";
    dmn = ChainTest.class.getResourceAsStream(dmnPath);
    metaPath = "/DecisionReuseMeta.json";
    meta = ChainTest.class.getResourceAsStream(metaPath);
    m = convert(dmn, meta, DMN_1_2);

    printConvertResults(m, "/basic/Decision Reuse");

    dmnPath = "/Decision Subdecision.dmn";
    dmn = ChainTest.class.getResourceAsStream(dmnPath);
    metaPath = "/DecisionSubdecisionMeta.json";
    meta = ChainTest.class.getResourceAsStream(metaPath);
    m = convert(dmn, meta, DMN_1_2);

    printConvertResults(m, "/basic/Decision Subdecision");

    dmnPath = "/Operational Definition Model.dmn";
    dmn = ChainTest.class.getResourceAsStream(dmnPath);
    metaPath = "/Operational Definition ModelMeta.json";
    meta = ChainTest.class.getResourceAsStream(metaPath);
    m = convert(dmn, meta, DMN_1_2);

    printConvertResults(m, "/basic/Operational Definition Model");

    dmnPath = "/recommendation chain.dmn";
    dmn = ChainTest.class.getResourceAsStream(dmnPath);
    metaPath = "/recommendationChainMeta.json";
    meta = ChainTest.class.getResourceAsStream(metaPath);
    m = convert(dmn, meta, DMN_1_2);

    printConvertResults(m, "/basic/recommendation chain");
  }

  @Disabled("only for developers as environment needs modification")
  @Test
  void convertModelsWeave() {
    cmmnPath = "/Weave Test 1.cmmn";
    cmmn = ChainTest.class.getResourceAsStream(cmmnPath);
    cmmnMetaPath = "/WeaveTest1Meta.json";
    cmmnMeta = ChainTest.class.getResourceAsStream(cmmnMetaPath);

    m = convert(cmmn, cmmnMeta, CMMN_1_1);
    printConvertResults(m, "/weave/Weave Test 1");

    dmnPath = "/Weaver Test 1.dmn";
    dmn = ChainTest.class.getResourceAsStream(dmnPath);
    metaPath = "/WeaverTest1Meta.json";
    meta = ChainTest.class.getResourceAsStream(metaPath);
    m = convert(dmn, meta, DMN_1_2);

    printConvertResults(m, "/weave/Weaver Test 1");

    dmnPath = "/Weaver Test 2.dmn";
    dmn = ChainTest.class.getResourceAsStream(dmnPath);
    metaPath = "/Weaver Test 2Meta.json";
    meta = ChainTest.class.getResourceAsStream(metaPath);
    m = convert(dmn, meta, DMN_1_2);

    printConvertResults(m, "/weave/Weaver Test 2");

  }

    // Creates files in $HOME (C: on Windows)
  // if a path is defined in the fileName, the directory will be created if it doesn't already exist
  private void printConvertResults(Model m, String fileName) {
    System.out.println("fileName: " + fileName);

    File surrogateFile = new File(fileName + "Surrogate_4_13.xml");
    File modelFile = new File(fileName + "_AfterWeave_4_13.xml");
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