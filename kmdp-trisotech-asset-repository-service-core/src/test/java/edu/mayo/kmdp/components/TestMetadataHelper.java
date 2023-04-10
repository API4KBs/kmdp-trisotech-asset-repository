package edu.mayo.kmdp.components;

import static edu.mayo.kmdp.trisotechwrapper.config.TTWConfigParamsDef.ASSET_ID_ATTRIBUTE;
import static edu.mayo.kmdp.util.XMLUtil.loadXMLDocument;
import static java.util.Optional.ofNullable;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Encoded_Knowledge_Expression;

import edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.DefaultMetadataIntrospector;
import edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.DocumentHelper;
import edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.MetadataIntrospector;
import edu.mayo.kmdp.language.parsers.surrogate.v2.Surrogate2Parser;
import edu.mayo.kmdp.trisotechwrapper.components.DefaultNamespaceManager;
import edu.mayo.kmdp.trisotechwrapper.components.SemanticModelInfo;
import edu.mayo.kmdp.trisotechwrapper.config.TTWEnvironmentConfiguration;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.util.JSonUtil;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Assertions;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.SurrogateHelper;

public class TestMetadataHelper {

  public static MetadataIntrospector newIntrospector(TTWEnvironmentConfiguration cfg) {
    return new DefaultMetadataIntrospector(
        cfg,
        null,
        new DefaultNamespaceManager(cfg),
        null
    ) {
      @Override
      protected Optional<ResolvedAssetId> categorizeAsset(
          @Nonnull ResourceIdentifier assetId,
          @Nonnull String modelUri) {
        return Optional.of(new ResolvedAssetId(AssetCategory.DOMAIN_ASSET, assetId));
      }
    };
  }

  /**
   * Test method
   *
   * @param resource the Artifact, from an InputStream
   * @param meta     the Artifact manifest, from an InputStream
   * @return the metadata surrogate for the Model
   */
  public static Optional<KnowledgeAsset> extractMetadata(
      InputStream resource,
      InputStream meta,
      MetadataIntrospector extractor,
      TTWEnvironmentConfiguration cfg) {
    var dox = loadXMLDocument(resource);
    var info = ofNullable(meta)
        .flatMap(json -> JSonUtil.readJson(json, TrisotechFileInfo.class))
        .map(SemanticModelInfo::testNewInfo)
        .orElseGet(SemanticModelInfo::new);

    var assetId = DocumentHelper.extractAssetIdFromDocument(
        dox.orElseGet(Assertions::fail),
        cfg.getTyped(ASSET_ID_ATTRIBUTE))
        .orElseGet(Assertions::fail);
    info.setAssetId(assetId.getVersionId().toString());
    info.setAssetKey(assetId.asKey());
    return extractor.introspect(assetId, Map.of(info, dox));
  }


  public static Optional<byte[]> carryBinary(KnowledgeAsset surr, String codedRep) {
      return Optional.of(surr)
        .map(SurrogateHelper::carry)
        .map(ast ->
            new Surrogate2Parser().applyLower(
                ast,
                Encoded_Knowledge_Expression,
                codedRep,
                null)
            .flatOpt(AbstractCarrier::asBinary)
            .orElseGet(() -> new byte[0])
    );
  }

}
