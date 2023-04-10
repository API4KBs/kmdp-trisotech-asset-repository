package edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors;

import edu.mayo.kmdp.trisotechwrapper.components.SemanticModelInfo;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.w3c.dom.Document;

/**
 * Delegate Introspector
 * <p>
 * Extracts Metadata for Service Assets carried by BPM+ Models
 */
public interface ServiceIntrospector {

  /**
   * Introspects a canonical {@link KnowledgeAsset} surrogate for the Service Asset exposed by a
   * given Model
   *
   * @param assetId  the Asset ID
   * @param manifest the Model's Manifest
   * @param carrier  the Model that carries the Asset with the given ID
   * @return a {@link KnowledgeAsset} surrogate for that Asset, given its Carrier, and Manifest, if
   * able to
   */
  @Nonnull
  Optional<KnowledgeAsset> introspectAsService(
      @Nonnull final ResourceIdentifier assetId,
      @Nonnull final SemanticModelInfo manifest,
      @Nonnull final Document carrier);
}
