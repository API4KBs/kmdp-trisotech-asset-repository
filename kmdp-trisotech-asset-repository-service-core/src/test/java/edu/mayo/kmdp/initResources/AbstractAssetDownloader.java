package edu.mayo.kmdp.initResources;


import static java.nio.charset.Charset.defaultCharset;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.snapshot.SerializationFormat.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.snapshot.KnowledgeRepresentationLanguage.CMMN_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.snapshot.KnowledgeRepresentationLanguage.DMN_1_2;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.snapshot.KnowledgeRepresentationLanguage.Knowledge_Asset_Surrogate_2_0;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel._20200801.ParsingLevel.Serialized_Knowledge_Expression;

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
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Assertions;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.transrepresentation.ModelMIMECoder;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;

/*
  Use this class to re-initialize the test models in the /resources folder

  Downloads the surrogate (native TT and KMDP standard) as well as the models
  (pre and post rewriting of the semantic annotations) to support testing
  of the different components
 */


public abstract class AbstractAssetDownloader {

  @Autowired
  TrisotechAssetRepository assetRepository;

  @Autowired
  TrisotechWrapper wrapper;

  protected abstract File getTgtFolder();

  protected String getParent() {
    Path srcPath = Paths.get("src", "test", "resources");
    return srcPath.toAbsolutePath().toString();
  }

  protected void saveArtifacts(Pointer assetPtr, File tgtFolder) {
    if (! tgtFolder.exists()) {
      tgtFolder.mkdirs();
    }

    KnowledgeAsset surrogate
        = assetRepository
        .getKnowledgeAsset(assetPtr.getUuid(), assetPtr.getVersionTag())
        .orElseGet(Assertions::fail);
    KnowledgeCarrier wovenModel
        = assetRepository
        .getCanonicalKnowledgeAssetCarrier(assetPtr.getUuid(), assetPtr.getVersionTag())
        .orElseGet(Assertions::fail);

    assertFalse(surrogate.getSurrogate().isEmpty());
    ResourceIdentifier nativeId = surrogate.getSurrogate().get(0).getArtifactId();
    TrisotechFileInfo nativeSurrogate =
        wrapper.getFileInfoByIdAndVersion(nativeId.getTag(), nativeId.getVersionTag())
            .orElseGet(Assertions::fail);
    Document nativeArtifact = wrapper.getPublishedModel(nativeSurrogate)
        .orElseGet(Assertions::fail);

    String name = surrogate.getName();
    String ext = getLanguageExtension(surrogate);

    File surrFile = new File(tgtFolder, name + ".surrogate" + ".xml");
    saveSurrogate(surrogate, surrFile);
    File modelFile = new File(tgtFolder, name + ext + ".xml");
    saveModel(wovenModel, modelFile);
    File nativeSurrFile = new File(tgtFolder, name + ".meta" + ".json");
    saveNativeSurrogate(nativeSurrogate, nativeSurrFile);
    File nativeModelFile = new File(tgtFolder, name + ".raw" + ext + ".xml");
    saveNativeModel(nativeArtifact, nativeModelFile);
  }

  private void saveNativeModel(Document nativeArtifact, File nativeModelFile) {
    FileUtil.write(XMLUtil.toByteArray(nativeArtifact), nativeModelFile);
  }

  private void saveNativeSurrogate(TrisotechFileInfo nativeSurrogate, File modelFile) {
    String json = JSonUtil.writeJsonAsString(nativeSurrogate)
        .orElseGet(Assertions::fail);
    FileUtil.write(json,modelFile);
  }

  private void saveModel(KnowledgeCarrier wovenModel, File modelFile) {
    if (modelFile.getName().endsWith("cmmn.xml")) {
      new CMMN11Parser().applyLift(wovenModel,
          Serialized_Knowledge_Expression,
          ModelMIMECoder
              .encode(rep(CMMN_1_1, XML_1_1, defaultCharset())),
          null)
          .flatOpt(AbstractCarrier::asString)
          .ifPresent(str -> FileUtil
              .write(str, modelFile));
    } else if (modelFile.getName().endsWith("dmn.xml")) {
      KnowledgeCarrier lifted = new DMN12Parser().applyLift(wovenModel,
          Serialized_Knowledge_Expression,
          ModelMIMECoder
              .encode(rep(DMN_1_2, XML_1_1, defaultCharset())),
          null).get();

      new DMN12Parser().applyLift(wovenModel,
          Serialized_Knowledge_Expression,
          ModelMIMECoder
              .encode(rep(DMN_1_2, XML_1_1, defaultCharset())),
          null)
          .flatOpt(AbstractCarrier::asString)
          .ifPresent(str -> FileUtil
              .write(str, modelFile));
    }
  }

  private void saveSurrogate(KnowledgeAsset surrogate, File surrFile) {
    new Surrogate2Parser().applyLower(
        AbstractCarrier.ofAst(surrogate, rep(Knowledge_Asset_Surrogate_2_0)),
        Serialized_Knowledge_Expression,
        ModelMIMECoder
            .encode(rep(Knowledge_Asset_Surrogate_2_0, XML_1_1, defaultCharset())),
        null)
        .flatOpt(AbstractCarrier::asString)
        .ifPresent(
            str -> FileUtil.write(str, surrFile));
  }

  private String getLanguageExtension(KnowledgeAsset surrogate) {
    KnowledgeRepresentationLanguage lang = surrogate.getCarriers().get(0).getRepresentation()
        .getLanguage();
    switch (lang.asEnum()) {
      case CMMN_1_1:
        return ".cmmn";
      case DMN_1_2:
        return ".dmn";
      case BPMN_2_0:
        return ".bpmn";
      default:
        throw new IllegalStateException("Unrecognized language " + lang);
    }
  }

}
