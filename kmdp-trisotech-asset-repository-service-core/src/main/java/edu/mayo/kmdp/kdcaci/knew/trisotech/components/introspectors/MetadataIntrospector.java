package edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors;

import edu.mayo.kmdp.trisotechwrapper.components.SemanticModelInfo;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.w3c.dom.Document;

public interface MetadataIntrospector {

  @Nonnull
  default Optional<KnowledgeAsset> introspect(
      @Nonnull final UUID assetId,
      @Nonnull final SemanticModelInfo manifest,
      @Nullable final Document carrier) {
    return introspect(assetId, Map.of(manifest, Optional.ofNullable(carrier)));
  }

  @Nonnull
  Optional<KnowledgeAsset> introspect(
      @Nonnull final UUID assetId,
      @Nonnull final Map<SemanticModelInfo, Optional<Document>> carriers);

  @Nonnull
  Optional<KnowledgeAsset> introspectAsService(
      @Nonnull final ResourceIdentifier assetId,
      @Nonnull final SemanticModelInfo manifest,
      @Nonnull final Document carrier);

  @Nonnull
  Optional<KnowledgeAsset> introspectAsModel(
      @Nonnull final ResourceIdentifier assetId,
      @Nonnull final Map<SemanticModelInfo, Document> doxMap);
}
