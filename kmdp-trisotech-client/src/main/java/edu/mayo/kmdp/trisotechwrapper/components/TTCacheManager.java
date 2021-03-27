package edu.mayo.kmdp.trisotechwrapper.components;

import static edu.mayo.kmdp.trisotechwrapper.components.TTGraphTerms.ARTIFACT_NAME;
import static edu.mayo.kmdp.trisotechwrapper.components.TTGraphTerms.ASSET_ID;
import static edu.mayo.kmdp.trisotechwrapper.components.TTGraphTerms.MIME_TYPE;
import static edu.mayo.kmdp.trisotechwrapper.components.TTGraphTerms.MODEL;
import static edu.mayo.kmdp.trisotechwrapper.components.TTGraphTerms.STATE;
import static edu.mayo.kmdp.trisotechwrapper.components.TTGraphTerms.UPDATED;
import static edu.mayo.kmdp.trisotechwrapper.components.TTGraphTerms.VERSION;
import static edu.mayo.kmdp.trisotechwrapper.components.TTWebClient.TRISOTECH_GRAPH;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newVersionId;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import edu.mayo.kmdp.trisotechwrapper.TrisotechWrapper;
import edu.mayo.kmdp.trisotechwrapper.config.TTWEnvironmentConfiguration;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.util.FileUtil;
import edu.mayo.kmdp.util.Util;
import java.net.URI;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cache-able
 */
public class TTCacheManager {

  private static final Logger logger = LoggerFactory.getLogger(TTCacheManager.class);

  TTWebClient webClient;

  // Cache and index by path within the focus Place
  private final LoadingCache<String, PlacePathIndex> pathIndexes;


  public TTCacheManager(TTWebClient webClient, TTWEnvironmentConfiguration cfg) {
    this.webClient = webClient;
    this.pathIndexes = initCache(Long.parseLong(cfg.getCacheExpiration()));
  }

  public boolean clearCache() {
    pathIndexes.invalidateAll();
    return true;
  }

  private LoadingCache<String, PlacePathIndex> initCache(final long expirationMinutes) {
    CacheLoader<String, PlacePathIndex> loader = new CacheLoader<>() {
      @Override
      public PlacePathIndex load(final @Nonnull String placePath) {
        // the place ID is a UUID: 32 hex chars + 4 hypens
        UUID placeId = Util.ensureUUID(placePath.substring(0, 36)).orElseThrow();
        String path = placePath.substring(36);
        return reindex(placeId.toString(), path);
      }
    };
    return CacheBuilder.newBuilder()
        .refreshAfterWrite(expirationMinutes, TimeUnit.MINUTES)
        .removalListener(
            (RemovalListener<String, PlacePathIndex>) removalNotification
                -> removalNotification.getValue().destroy())
        .build(loader);
  }


  public Map<String, TrisotechFileInfo> getModelInfos(String repoId, String path) {
    return pathIndexes.getUnchecked(indexKey(repoId,path))
        .modelInfoCache;
  }

  public TrisotechFileInfo getModelInfo(String repoId, String path, String modelUri) {
    return pathIndexes.getUnchecked(indexKey(repoId,path))
        .modelInfoCache.get(modelUri);
  }

  public Map<String, Set<String>> getDependencyMap(String repoId, String path) {
    return pathIndexes.getUnchecked(indexKey(repoId,path))
        .artifactToArtifactDependencyMap;
  }

  public Map<TTGraphTerms, String> getMetadataByModel(String repoId, String path, String modelUri) {
    return pathIndexes.getUnchecked(indexKey(repoId,path))
        .modelsSolutionsByModelID.get(modelUri);
  }

  public Map<TTGraphTerms, String> getMetadataByAsset(String repoId, String path, UUID assetId) {
    return pathIndexes.getUnchecked(indexKey(repoId,path))
        .modelsSolutionsByAssetID.get(assetId);
  }


  private String indexKey(String repoId, String path) {
    return repoId + path;
  }


  private PlacePathIndex reindex(String focusPlaceId, String path) {
    ResultSet allModels = query(getQueryStringModels(), focusPlaceId);
    ResultSet relations = query(getQueryStringRelations(), focusPlaceId);
    return new PlacePathIndex(focusPlaceId, path, allModels, relations, webClient);
  }


  /**
   * Perform the query
   *
   * @param queryString the queryString to be executed
   * @param place       the place to query models from
   * @return The ResultSet with the results from the query
   */
  private ResultSet query(String queryString, String place) {
    ParameterizedSparqlString sparqlString = new ParameterizedSparqlString(queryString);
    String graph = TRISOTECH_GRAPH + place;
    // set NAMED
    sparqlString.setIri(0, graph);
    // set GRAPH
    sparqlString.setIri(1, graph);
    Query query = sparqlString.asQuery();

    return webClient.askQuery(query);
  }


  /**
   * Build the queryString needed to query the DMN->DMN and CMMN->DMN relations in the place
   * requested. This is specific to Trisotech.
   * <p>
   * Should only get those models that are Published? CAO | DS
   *
   * @return the query string to query the relations between models
   */
  private String getQueryStringRelations() {
    return FileUtil.read(TrisotechWrapper.class.getResourceAsStream("/queryRelations.tt.sparql"))
        .orElseThrow(IllegalStateException::new);
  }

  /**
   * Build the queryString needed to query models with their fileId and assetId. This is specific to
   * Trisotech. This query is for ALL models, published and not, though because none of the
   * selectors are given as optional, if they don't exist on a model, that model will not be
   * returned.
   *
   * @return the query string needed to query the models
   */
  private String getQueryStringModels() {
    return FileUtil.read(TrisotechWrapper.class.getResourceAsStream("/queryModels.tt.sparql"))
        .orElseThrow(IllegalStateException::new);
  }


  static class PlacePathIndex {

    // Cache and index by path within the focus Place
    Map<String, TrisotechFileInfo> modelInfoCache;

    // map of artifact relations (artifact = model)
    Map<String, Set<String>> artifactToArtifactDependencyMap;

    Map<UUID, EnumMap<TTGraphTerms, String>> modelsSolutionsByAssetID;
    Map<String, EnumMap<TTGraphTerms, String>> modelsSolutionsByModelID;

    public PlacePathIndex(String focusPlaceId, String path,
        ResultSet allModels, ResultSet relations,
        TTWebClient webClient) {
      artifactToArtifactDependencyMap = new HashMap<>();
      modelsSolutionsByAssetID = new HashMap<>();
      modelsSolutionsByModelID = new HashMap<>();
      modelInfoCache = new HashMap<>();

      indexModels(allModels);
      indexRelationships(relations);
      webClient.collectRepositoryContent(focusPlaceId, modelInfoCache, path, null);

      modelInfoCache = Collections.unmodifiableMap(modelInfoCache);
      artifactToArtifactDependencyMap = Collections.unmodifiableMap(artifactToArtifactDependencyMap);
    }
    
    public void destroy() {
      modelInfoCache.clear();
      modelsSolutionsByAssetID.clear();
      modelsSolutionsByModelID.clear();
      artifactToArtifactDependencyMap.clear();
    }

    private void indexRelationships(ResultSet relations) {
      while (relations.hasNext()) {
        QuerySolution soln = relations.nextSolution();
        artifactToArtifactDependencyMap
            .computeIfAbsent(soln.getResource("?fromModel").getURI(), s -> new HashSet<>())
            .add(soln.getResource("?toModel").getURI());
      }
    }

    private void indexModels(ResultSet modelSet) {
      while (modelSet.hasNext()) {
        QuerySolution soln = modelSet.nextSolution();
        EnumMap<TTGraphTerms, String> metadata = toMap(soln);

        String modelId = Optional.ofNullable(soln.getResource(MODEL.key))
            .map(Resource::getURI).orElseThrow();
        Optional<UUID> optAssetId = Optional.ofNullable(soln.getLiteral(ASSET_ID.key))
            .map(Literal::getString)
            .map(id -> Util.isUUID(id)
                ? UUID.fromString(id)
                : newVersionId(URI.create(id)).getUuid());

        if (modelsSolutionsByModelID.containsKey(modelId)) {
          logger.error("model ID {} has multiple asset IDs {} and {}",
              modelsSolutionsByModelID.get(modelId).get(ARTIFACT_NAME),
              modelsSolutionsByModelID.get(modelId).get(ASSET_ID),
              soln.get(ASSET_ID.key));
        }
        modelsSolutionsByModelID.put(modelId, metadata);

        if (optAssetId.isPresent()) {
          UUID assetId = optAssetId.get();
          if (modelsSolutionsByAssetID.containsKey(assetId)) {
            logger.error("Asset ID {} appears in {} and {}",
                assetId,
                modelsSolutionsByAssetID.get(assetId).get(ARTIFACT_NAME),
                soln.get(ARTIFACT_NAME.key));
          }
          modelsSolutionsByAssetID.put(assetId, metadata);
        }
      }
    }


    private EnumMap<TTGraphTerms, String> toMap(QuerySolution soln) {
      //?model ?assetId ?version ?state ?updated ?mimeType ?artifactName
      EnumMap<TTGraphTerms, String> map = new EnumMap<>(TTGraphTerms.class);

      map.put(MODEL, soln.getResource(MODEL.key).getURI());
      map.put(ASSET_ID, soln.getLiteral(ASSET_ID.key).getString());
      map.put(ARTIFACT_NAME, soln.getLiteral(ARTIFACT_NAME.key).getString());
      map.put(MIME_TYPE, soln.getLiteral(MIME_TYPE.key).getString());

      Optional.ofNullable(soln.getLiteral(VERSION.key))
          .map(Literal::getString)
          .ifPresent(v -> map.put(VERSION, v));
      Optional.ofNullable(soln.getLiteral(STATE.key))
          .map(Literal::getString)
          .ifPresent(s -> map.put(STATE, s));
      Optional.ofNullable(soln.getLiteral(UPDATED.key))
          .map(Literal::getString)
          .ifPresent(u -> map.put(UPDATED, u));

      return map;
    }
  }

}
