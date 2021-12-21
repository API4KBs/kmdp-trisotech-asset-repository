package edu.mayo.kmdp.trisotechwrapper.components;

import static edu.mayo.kmdp.trisotechwrapper.components.TTGraphTerms.ARTIFACT_NAME;
import static edu.mayo.kmdp.trisotechwrapper.components.TTGraphTerms.ASSET_ID;
import static edu.mayo.kmdp.trisotechwrapper.components.TTGraphTerms.ASSET_TYPE;
import static edu.mayo.kmdp.trisotechwrapper.components.TTGraphTerms.MIME_TYPE;
import static edu.mayo.kmdp.trisotechwrapper.components.TTGraphTerms.MODEL;
import static edu.mayo.kmdp.trisotechwrapper.components.TTGraphTerms.STATE;
import static edu.mayo.kmdp.trisotechwrapper.components.TTGraphTerms.UPDATED;
import static edu.mayo.kmdp.trisotechwrapper.components.TTGraphTerms.VERSION;
import static edu.mayo.kmdp.trisotechwrapper.components.TTWebClient.TRISOTECH_GRAPH;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newId;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newKey;
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
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
import org.omg.spec.api4kp._20200801.id.IdentifierConstants;
import org.omg.spec.api4kp._20200801.id.KeyIdentifier;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
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
    return pathIndexes.getUnchecked(indexKey(repoId, path))
        .modelInfoCache;
  }

  public TrisotechFileInfo getModelInfo(String repoId, String path, String modelUri) {
    return pathIndexes.getUnchecked(indexKey(repoId, path))
        .modelInfoCache.get(modelUri);
  }

  public Map<String, Set<String>> getDependencyMap(String repoId, String path) {
    return pathIndexes.getUnchecked(indexKey(repoId, path))
        .artifactToArtifactDependencyMap;
  }

  public Map<TTGraphTerms, String> getMetadataByModel(String repoId, String path, String modelUri) {
    return pathIndexes.getUnchecked(indexKey(repoId, path))
        .modelsSolutionsByModelID.get(modelUri);
  }

  /**
   * Resolves an Asset (ID + version) to the salient metadata, as per the TT Knowledge Graph, of the
   * most recent version of the model that carries that asset (version*).
   * <p>
   * * Since only the most recent version of a model is indexed, IF an asset version is carried by a
   * model version that is NOT the most recent, this method may return a model version that is
   * mismatching the asset version. This mismatch will be compensated upstream, where older versions
   * of the model may be processed to match the older version of the asset
   *
   * @param repoId          The Place that this search is scoped to
   * @param path            The Path within the Place that this search is scoped to
   * @param assetId         The ID of the asset for which a carrier model is being looked up
   * @param assetVersionTag The version of the asset for which a carrier model is being looked up
   * @return The core metadata for the candidate model that carries the requested asset, if any,
   * with caveats on the asset version.
   */
  public Optional<Map<TTGraphTerms, String>> getMetadataByAsset(
      String repoId, String path, UUID assetId, String assetVersionTag) {
    KeyIdentifier searchKey = newKey(assetId, assetVersionTag);
    Map<KeyIdentifier, EnumMap<TTGraphTerms, String>> index =
        pathIndexes.getUnchecked(indexKey(repoId, path)).modelsSolutionsByAssetID;

    if (index.containsKey(searchKey)) {
      // lookup is successful - return
      return Optional.ofNullable(index.get(searchKey));
    } else {
      // all known versions of the asset being looked up
      // for each asset, expect a small number of versions
      List<KeyIdentifier> assetCarriers = index.keySet().stream()
          .filter(k -> k.getUuid().equals(assetId))
          // sort so that greater versions come first
          .sorted(Comparator.reverseOrder())
          .collect(Collectors.toList());
      if (assetCarriers.isEmpty()) {
        // no version for an asset id -> will eventually cause not found
        return Optional.empty();
      }
      if (IdentifierConstants.VERSION_LATEST.equalsIgnoreCase(assetVersionTag)) {
        // assumes latest = greatest. asset version IDs should be asserted sequentially by modelers
        return Optional.ofNullable(index.get(assetCarriers.get(0)));
      } else {
        return guessCarrierOfOlderAssetVersion(searchKey, assetCarriers)
            .map(index::get);
      }
    }
  }

  /**
   * Before #181629, only one model/artifact was allowed to carry the same asset ID: as a
   * consequence, different versions of an asset had to be carried by the different versions of a
   * model. Since TT only indexes the asset id on the most recent version of a model, a previous
   * version of an asset has to be looked for in a previous version of the artifact carrying the
   * asset series.
   * <p>
   * Since #181629, this operation becomes even more complicated because a previous version of an
   * asset may be carried in a previous version of one of several models.
   * <p>
   * Given that inspecting older versions of models is a very expensive operation anyway - models
   * have to be downloaded to be inspected - UNLESS/UNTIL TT exposes the asset id (a custom Mayo
   * annotation) more directly - it is just easier and efficient to use different models for
   * different versions of different assets
   * <p>
   * This heuristic method is only maintained for backward compatibility - mostly with test models.
   * If only one model is found, that is returned, and versions of that model will be inspected. If
   * more than one model is found, picks the first whose asset version PRECEDES the search key.
   * <p>
   * Example: assume that model A carries version 3.0, model B carries version 2.0 and model C
   * carries version 1.0. The history of model B should carry asset versions between (1.0, 2.0],
   * while the history of model A should carry versions later than 2.0 but preceding up to 3.0.
   * Likewise, the history of model C should carry asset versions from none to up to 1.0. If
   * searching for asset version, say 1.6, the most likely model to carry it is B, and B's history
   * will be inspected
   *
   * @param assetSearchKey the version identifier of the asset being looked up
   * @param assetCarriers  the asset versions that correspond to actual (latest versions of) models,
   *                       sorted from most to least recent
   * @return the (identifier of the) model that is most likely to carry the given version of an
   * asset in its history
   */
  @Deprecated
  private Optional<KeyIdentifier> guessCarrierOfOlderAssetVersion(
      KeyIdentifier assetSearchKey,
      List<KeyIdentifier> assetCarriers) {
    if (assetCarriers.isEmpty()) {
      return Optional.empty();
    }
    Iterator<KeyIdentifier> iter = assetCarriers.iterator();
    KeyIdentifier mostLikelyCarrier = iter.next();
    // contin
    while (iter.hasNext() && mostLikelyCarrier.compareTo(assetSearchKey) > 0) {
      mostLikelyCarrier = iter.next();
    }
    return Optional.ofNullable(mostLikelyCarrier);
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

    Map<KeyIdentifier, EnumMap<TTGraphTerms, String>> modelsSolutionsByAssetID;
    Map<String, EnumMap<TTGraphTerms, String>> modelsSolutionsByModelID;

    public PlacePathIndex(String focusPlaceId, String path,
        ResultSet allModels, ResultSet relations,
        TTWebClient webClient) {
      artifactToArtifactDependencyMap = new ConcurrentHashMap<>();
      modelsSolutionsByAssetID = new ConcurrentHashMap<>();
      modelsSolutionsByModelID = new ConcurrentHashMap<>();
      modelInfoCache = new ConcurrentHashMap<>();

      indexModels(allModels);
      indexRelationships(relations);
      webClient.collectRepositoryContent(focusPlaceId, modelInfoCache, path, null);

      modelInfoCache = Collections.unmodifiableMap(modelInfoCache);
      artifactToArtifactDependencyMap = Collections.unmodifiableMap(
          artifactToArtifactDependencyMap);
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
        Optional<ResourceIdentifier> optAssetId = Optional.ofNullable(soln.getLiteral(ASSET_ID.key))
            .map(Literal::getString)
            .map(id -> Util.isUUID(id)
                ? newId(id)
                : newVersionId(URI.create(id)));

        // A given model/artifact can only carry one asset
        if (modelsSolutionsByModelID.containsKey(modelId)) {
          logger.error("model ID {} has multiple asset IDs {} and {}",
              modelsSolutionsByModelID.get(modelId).get(ARTIFACT_NAME),
              modelsSolutionsByModelID.get(modelId).get(ASSET_ID),
              soln.get(ASSET_ID.key));
        } else {
          modelsSolutionsByModelID.put(modelId, metadata);
        }

        if (optAssetId.isPresent()) {
          ResourceIdentifier assetId = optAssetId.get();
          KeyIdentifier key = assetId.asKey();
          if (modelsSolutionsByAssetID.containsKey(key)) {
            // A given asset VERSION can only be carried by one model
            // This limitation may be relaxed in the future, to allow for VARIANT models
            // alternative carriers of the same asset
            logger.error("Asset VERSION ID {} appears in {} and {}",
                assetId,
                modelsSolutionsByAssetID.get(key).get(ARTIFACT_NAME),
                soln.get(ARTIFACT_NAME.key));
          } else {
            if (logger.isInfoEnabled()) {
              logger.info(
                  "Cache indexing {} - {} as asset {}",
                  metadata.get(ARTIFACT_NAME),
                  metadata.get(MODEL),
                  metadata.get(ASSET_ID));
            }
            modelsSolutionsByAssetID.put(key, metadata);
          }
        } else {
          // ignore models with no AssetID
          if (logger.isInfoEnabled()) {
            logger.info("Skipping model {} - {} with no Asset ID",
                metadata.get(ARTIFACT_NAME),
                metadata.get(MODEL));
          }
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

      Optional.ofNullable(soln.getResource(ASSET_TYPE.key))
          .map(Resource::getURI)
          .ifPresent(t -> map.put(ASSET_TYPE, t));

      return map;
    }
  }

}
