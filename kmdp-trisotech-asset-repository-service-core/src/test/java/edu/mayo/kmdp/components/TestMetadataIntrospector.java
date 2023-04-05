package edu.mayo.kmdp.components;

import static edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.DocumentHelper.extractAssetIdFromDocument;
import static edu.mayo.kmdp.trisotechwrapper.config.TTWConfigParamsDef.ASSET_ID_ATTRIBUTE;
import static edu.mayo.kmdp.util.XMLUtil.loadXMLDocument;
import static java.util.Optional.ofNullable;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.randomAssetId;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Encoded_Knowledge_Expression;

import edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.DefaultMetadataIntrospector;
import edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.BPMModelIntrospector;
import edu.mayo.kmdp.trisotechwrapper.components.DefaultNamespaceManager;
import edu.mayo.kmdp.trisotechwrapper.components.SemanticModelInfo;
import edu.mayo.kmdp.trisotechwrapper.components.redactors.Redactor;
import edu.mayo.kmdp.trisotechwrapper.components.weavers.Weaver;
import edu.mayo.kmdp.trisotechwrapper.config.TTWEnvironmentConfiguration;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.util.JSonUtil;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.SurrogateHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

public class TestMetadataIntrospector extends DefaultMetadataIntrospector {

  private static final Logger logger = LoggerFactory
      .getLogger(TestMetadataIntrospector.class);

  public TestMetadataIntrospector(
      BPMModelIntrospector delegate) {
    super(delegate);
  }

  /**
   * Test method
   *
   * @param dox  the Artifact
   * @param meta the Artifact manifest
   * @return the metadata surrogate for the Model
   */
  public Optional<KnowledgeAsset> extractMetadata(Document dox, TrisotechFileInfo meta) {
    return Optional.of(extractSurrogateFromDocument(dox, SemanticModelInfo.testNewInfo(meta)));
  }


  /**
   * Generates a {@link KnowledgeAsset} Surrogate from the introspection of a BPM+ model, combined
   * with the information in its corresponding Trisotech's internal manifest
   * <p>
   * FUTURE: Currently supports DMN and CMMN models, but not BPMN
   * <p>
   * Note that, at this point, the model document has already been standardized using the
   * {@link Redactor}, and (re)annotated using a {@link Weaver}
   *
   * @param dox      the BPM+ model artifact to extract metadata from
   * @param manifest the model's internal manifest
   * @return a KnowledgeAsset surrogate with metadata for that model
   */
  public KnowledgeAsset extractSurrogateFromDocument(Document dox, SemanticModelInfo manifest) {
    Optional<ResourceIdentifier> tryAssetID =
        extractAssetIdFromDocument(dox,
            new TTWEnvironmentConfiguration().getTyped(ASSET_ID_ATTRIBUTE));

    if (tryAssetID.isEmpty()) {
      logger.warn("Model {} does not have an Asset ID - providing a random one", manifest.getId());
    }
    ResourceIdentifier assetID = tryAssetID
        .orElse(randomAssetId(new DefaultNamespaceManager(new TTWEnvironmentConfiguration()).getAssetNamespace()));

    var id = dox.getDocumentElement().getAttribute("id");

    if (logger.isDebugEnabled()) {
      logger.debug("assetID: {} : {}", assetID.getResourceId(), assetID.getVersionTag());
      logger.debug("the id found in woven document {}", id);
    }

    if (id.isEmpty()) {
      logger.error("No ID found in the woven document");
      return new KnowledgeAsset();
    }

    return strategy.extractSurrogateFromDocument(Map.of(manifest, dox), assetID);
  }

  /**
   * Test method
   *
   * @param resource the Artifact, from an InputStream
   * @param meta     the Artifact manifest, from an InputStream
   * @return the metadata surrogate for the Model
   */
  public Optional<KnowledgeAsset> extractMetadata(
      InputStream resource, InputStream meta) {
    Optional<Document> dox = loadXMLDocument(resource);
    TrisotechFileInfo info = ofNullable(meta)
        .flatMap(json -> JSonUtil.readJson(json, TrisotechFileInfo.class))
        .orElseGet(TrisotechFileInfo::new);

    return dox
        .flatMap(document -> extractMetadata(document, SemanticModelInfo.testNewInfo(info)));
  }


  /**
   * Test method
   *
   * @param resource the Artifact, from an InputStream
   * @param meta     the Artifact manifest, from an InputStream
   * @param codedRep the extended MIME type that specifies how to serialize/encode the Surrogate
   * @return the metadata surrogate for the Model
   */
  public Optional<byte[]> extractBinary(
      InputStream resource, InputStream meta, String codedRep) {
    Optional<KnowledgeCarrier> kc = extractMetadata(resource, meta)
        .map(SurrogateHelper::carry);

    return kc.map(
        ast -> serializer.applyLower(
                ast,
                Encoded_Knowledge_Expression,
                codedRep,
                null)
            .flatOpt(AbstractCarrier::asBinary)
            .orElseGet(() -> new byte[0])
    );
  }

}
