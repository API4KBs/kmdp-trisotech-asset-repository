package edu.mayo.kmdp.trisotechwrapper;

import edu.mayo.kmdp.trisotechwrapper.components.SemanticModelInfo;
import edu.mayo.kmdp.trisotechwrapper.components.cache.CachingTTWKnowledgeStore;
import edu.mayo.kmdp.trisotechwrapper.config.TTWEnvironmentConfiguration;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechExecutionArtifact;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechPlace;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.w3c.dom.Document;

public interface TTAPIAdapter {


  void rescan();
  void rescanPlace(String placeId);
  void rescanModel(String modelUri);

  TTWEnvironmentConfiguration getConfig();

  default Stream<SemanticModelInfo> listModels() {
      return listModels(null);
  }

  Stream<SemanticModelInfo> listModels(
      @Nullable String xmlMimeType);

  Map<String, TrisotechPlace> listAccessiblePlaces();
  Map<TrisotechPlace, Set<String>> getCachedPlaceScopes();
  Map<String, TrisotechPlace> getCacheablePlaces();


  Map<String, TrisotechExecutionArtifact> listExecutionArtifacts(
      String env);

  Optional<TrisotechExecutionArtifact> getExecutionArtifact(
      @Nonnull final String serviceName,
      @Nonnull final TrisotechFileInfo manifest);

  Optional<SemanticModelInfo> getMetadataByModelId(
      String modelUri);

  List<TrisotechFileInfo> getVersionsMetadataByModelId(
      String modelUri);

  Optional<TrisotechFileInfo> getMetadataByModelIdAndVersion(
      String fileId, String fileVersion);

  Stream<SemanticModelInfo> getServicesMetadataByModelId(
      String modelUri);

  Stream<SemanticModelInfo> getMetadataByAssetId(
      UUID assetId, String assetVersionTag);

  Optional<Document> getModelById(
      String modelUri);

  Optional<Document> getModelByIdAndVersion(
      String modelUri, String version);

  Optional<Document> getModel(
      TrisotechFileInfo trisotechFileInfo);

  Optional<CachingTTWKnowledgeStore> getCacheManager();

}
