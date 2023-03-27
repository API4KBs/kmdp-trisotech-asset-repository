package edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors;

import edu.mayo.kmdp.trisotechwrapper.components.SemanticModelInfo;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.w3c.dom.Document;

public interface MetadataIntrospector {

  default Optional<KnowledgeAsset> introspect(
      UUID assetId, SemanticModelInfo manifest, Document carrier) {
    return introspect(assetId, Map.of(manifest, Optional.ofNullable(carrier)));
  }

  Optional<KnowledgeAsset> introspect(
      UUID assetId, Map<SemanticModelInfo, Optional<Document>> carriers);

  Optional<KnowledgeAsset> introspectAsService(
      ResourceIdentifier assetId, SemanticModelInfo manifest, Document carrier);

  Optional<KnowledgeAsset> introspectAsModel(
      ResourceIdentifier assetId, Map<SemanticModelInfo, Document> doxMap);
}
