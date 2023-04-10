package edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors;

import edu.mayo.kmdp.trisotechwrapper.components.SemanticModelInfo;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.w3c.dom.Document;

/**
 * Delegate Introspector
 * <p>
 * Extracts Metadata for Assets carried by BPM+ Models
 */
public interface ModelIntrospector {

  /**
   * Introspects a canonical {@link KnowledgeAsset} surrogate for the Model Knowledge Asset, given
   * the carrier(s) of that Asset
   *
   * @param assetId  the Asset ID
   * @param carriers the Manifest/Model Map of the Carriers of the Asset with the given ID
   * @return a {@link KnowledgeAsset} surrogate for that Asset, given its Carriers, and Manifests,
   * if able to
   */
  @Nonnull
  Optional<KnowledgeAsset> introspectAsModel(
      @Nonnull final ResourceIdentifier assetId,
      @Nonnull final Map<SemanticModelInfo, Document> carriers);

}
