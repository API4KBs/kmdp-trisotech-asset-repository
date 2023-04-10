package edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors;

import edu.mayo.kmdp.trisotechwrapper.components.SemanticModelInfo;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.w3c.dom.Document;

/**
 * Introspector
 * <p>
 * Extracts Metadata for BPM+ Models, and Services thereof, from both the standard Model Document
 * and the internal Trisotech Manifest
 * <p>
 * Note: does not implement the API4KP _applyIntrospect API, though the combination of Document and
 * Manifest would constitute the KnowledgeBase that the introspector would be applied to.
 */
public interface MetadataIntrospector {

  /**
   * Introspects a canonical {@link KnowledgeAsset} surrogate for the Asset (either Model or Service
   * Asset) carried by a given Model
   *
   * @param assetId  the Asset ID
   * @param manifest the Model's Manifest
   * @param carrier  the Model that carries the Asset with the given ID
   * @return a {@link KnowledgeAsset} surrogate for that Asset, given its Carrier, and Manifest
   */
  @Nonnull
  default Optional<KnowledgeAsset> introspect(
      @Nonnull final ResourceIdentifier assetId,
      @Nonnull final SemanticModelInfo manifest,
      @Nullable final Document carrier) {
    return introspect(assetId, Map.of(manifest, Optional.ofNullable(carrier)));
  }

  /**
   * Introspects a canonical {@link KnowledgeAsset} surrogate for the Asset (either Model or Service
   * Asset), given the carrier(s) of that Asset
   *
   * @param assetId  the Asset ID
   * @param carriers  the Manifest/Model Map of the Carriers of the Asset with the given ID
   * @return a {@link KnowledgeAsset} surrogate for that Asset, given its Carriers, and Manifests
   */
  @Nonnull
  Optional<KnowledgeAsset> introspect(
      @Nonnull final ResourceIdentifier assetId,
      @Nonnull final Map<SemanticModelInfo, Optional<Document>> carriers);


}
