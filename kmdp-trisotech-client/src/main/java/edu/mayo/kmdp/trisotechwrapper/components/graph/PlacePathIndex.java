package edu.mayo.kmdp.trisotechwrapper.components.graph;

import static edu.mayo.kmdp.trisotechwrapper.components.graph.TTGraphTerms.ARTIFACT_NAME;
import static edu.mayo.kmdp.trisotechwrapper.components.graph.TTGraphTerms.ASSET_ID;
import static edu.mayo.kmdp.trisotechwrapper.components.graph.TTGraphTerms.ASSET_TYPE;
import static edu.mayo.kmdp.trisotechwrapper.components.graph.TTGraphTerms.MIME_TYPE;
import static edu.mayo.kmdp.trisotechwrapper.components.graph.TTGraphTerms.MODEL;
import static edu.mayo.kmdp.trisotechwrapper.components.graph.TTGraphTerms.PATH;
import static edu.mayo.kmdp.trisotechwrapper.components.graph.TTGraphTerms.SERVICE_FRAGMENT;
import static edu.mayo.kmdp.trisotechwrapper.components.graph.TTGraphTerms.SERVICE_ID;
import static edu.mayo.kmdp.trisotechwrapper.components.graph.TTGraphTerms.SERVICE_NAME;
import static edu.mayo.kmdp.trisotechwrapper.components.graph.TTGraphTerms.STATE;
import static edu.mayo.kmdp.trisotechwrapper.components.graph.TTGraphTerms.UPDATED;
import static edu.mayo.kmdp.trisotechwrapper.components.graph.TTGraphTerms.UPDATER;
import static edu.mayo.kmdp.trisotechwrapper.components.graph.TTGraphTerms.VERSION;
import static edu.mayo.kmdp.trisotechwrapper.config.TTApiConstants.toApiEndpoint;
import static edu.mayo.kmdp.util.DateTimeUtil.parseDateTime;
import static edu.mayo.kmdp.util.DateTimeUtil.toLocalDate;
import static java.lang.String.format;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.reducing;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newId;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newVersionId;

import com.github.zafarkhaja.semver.Version;
import edu.mayo.kmdp.trisotechwrapper.components.SemanticModelInfo;
import edu.mayo.kmdp.trisotechwrapper.config.TTWConfigParamsDef;
import edu.mayo.kmdp.trisotechwrapper.config.TTWEnvironmentConfiguration;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechPlace;
import edu.mayo.kmdp.util.DateTimeUtil;
import edu.mayo.kmdp.util.NameUtils;
import edu.mayo.kmdp.util.StreamUtil;
import edu.mayo.kmdp.util.Util;
import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.omg.spec.api4kp._20200801.id.IdentifierConstants;
import org.omg.spec.api4kp._20200801.id.KeyIdentifier;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.id.VersionIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Graph-based shape that indexes the Models within a focus Place, filtering the Models stored in
 * one or more given Paths (folders) in that Place. A PlacePathIndex is derived from queries to the
 * TT DES Knowledge Graph, and is designed for use with a Cache
 *
 * <p>
 * Supports Knowledge Assets and Service Assets, with mappings as follows
 * <ul>
 *   <li>Service/Knowledge Asset Id => Semantic Metadata (1:N)</li>
 *   <li>Model (Knowledge Artifact) Id => Semantic Metadata (1:1)</li>
 * </ul>
 * <p>
 * Note that a Model ID maps to 1..1 Model Metadata Manifest; an Asset ID may map to 1..N Model
 * Metadata Manifests, since the same Asset can be realized by multiple models. A Model Manifest
 * contains the 0..N relations to the Service Assets exposed by that Model, which in turn have their
 * own Manifest. A Model Manifest also contains a reverse link to the 0..1 Asset it carries, and the
 * 0..N dependency relationships to other Models.
 */
public class PlacePathIndex {

  /**
   * Logger
   */
  private static final Logger logger = LoggerFactory.getLogger(PlacePathIndex.class);

  /**
   * Descriptor of the indexed Place
   */
  private final TrisotechPlace place;

  /**
   * Path filters
   */
  private final Set<String> paths;

  /**
   * Map of Asset ID to semantic metadata
   */
  private final Map<KeyIdentifier, SortedSet<SemanticModelInfo>> modelInfoByAssetID;

  /**
   * Map of Model ID to semantic metadata
   */
  private final Map<String, SemanticModelInfo> modelInfoByModelID;

  /**
   * Constructor.
   * <p>
   * Builds an empty PlacePathIndex for a given Place
   *
   * @param focusPlace the Place to be indexed
   */
  protected PlacePathIndex(
      @Nonnull final TrisotechPlace focusPlace,
      @Nonnull final Set<String> paths) {
    this.place = focusPlace;
    this.paths = paths;
    this.modelInfoByAssetID = new ConcurrentHashMap<>();
    this.modelInfoByModelID = new ConcurrentHashMap<>();
  }

  /**
   * Factory.
   * <p>
   * Builds a PlacePathIndex graph object for the given Place, using the environment information,
   * and the results of the queries to the TT DES Knowledge Graph
   *
   * @param focusPlace      the Place for which to build this index
   * @param paths           the Paths of the folders used to filter the Models
   * @param allModels       the result of the Query that describes the Models
   * @param relations       the result of the Query that describes the Model/Model relationships
   * @param services        the result of the Query that describes the Models/Service relationships
   * @param historyProvider the mapping between a Model and the descriptors of its previous
   *                        versions
   * @param cfg             the Environment configuration
   * @return a PlacePathIndex for the given Path, based on the query results
   */
  @Nonnull
  public static PlacePathIndex index(
      @Nonnull final TrisotechPlace focusPlace,
      @Nonnull final Set<String> paths,
      @Nonnull final ResultSet allModels,
      @Nonnull final ResultSet relations,
      @Nonnull final ResultSet services,
      @Nonnull final BiFunction<String, String, List<TrisotechFileInfo>> historyProvider,
      @Nonnull final TTWEnvironmentConfiguration cfg) {
    var ppi = new PlacePathIndex(focusPlace, paths);
    ppi.indexModels(allModels, focusPlace, paths, historyProvider, cfg);
    ppi.indexRelationships(relations);
    ppi.indexServices(services, cfg);
    return ppi;
  }

  /**
   * Clears the internal indexes
   */
  public void destroy() {
    modelInfoByAssetID.clear();
    modelInfoByModelID.clear();
  }

  /**
   * @return the Model ID to Model Manifest index, as an Immutable Map
   */
  @Nonnull
  public Map<String, SemanticModelInfo> getModelToManifestMappings() {
    return Collections.unmodifiableMap(modelInfoByModelID);
  }

  /**
   * @return the Asset ID to Model(s) Manifest index, as an Immutable Map
   */
  @Nonnull
  public Map<KeyIdentifier, SortedSet<SemanticModelInfo>> getAssetToManifestMappings() {
    return Collections.unmodifiableMap(modelInfoByAssetID);
  }

  /**
   * @return the descriptor of the indexed place
   */
  public TrisotechPlace getPlace() {
    return place;
  }

  /**
   * @return the path filters, as an immutable Set
   */
  public Set<String> getPaths() {
    return Collections.unmodifiableSet(paths);
  }

  /* ---------------------------------------------------------------------------------------- */

  /**
   * Indexes the BPM+ models.
   * <p>
   * Creates a {@link SemanticModelInfo} manifest for each model, based on the result of the
   * descriptive query, applying several filters
   *  <ul>
   *    <li>Filters based on Place/Path focus, only retaining models stored in the given
   *  paths</li>
   *    <li>Filters on publication status: if PUBLISHED_ONLY_FLAG is set, and the latest version of
   *  a model is not published, indexes the latest published version instead</li>
   * <li>Filters out models that do not have an asset ID, unless ANONYMOUS_ASSETS_FLAG is set, when
   * a system asset ID is generated for models that do not assert on </li>
   * </ul>
   * . .
   *
   * @param modelSet        the model metadata, as queried from the DES KG
   * @param focusPlace      the place the models come from
   * @param paths           the path filters
   * @param historyProvider the mapping between a Model and the descriptors of its previous *
   *                        versions
   * @param cfg             the environment configuration
   * @see TTGraphTerms
   */
  protected void indexModels(
      @Nonnull final ResultSet modelSet,
      @Nonnull final TrisotechPlace focusPlace,
      @Nonnull final Set<String> paths,
      @Nonnull final BiFunction<String, String, List<TrisotechFileInfo>> historyProvider,
      @Nonnull final TTWEnvironmentConfiguration cfg) {
    var publishedOnly = cfg.getTyped(TTWConfigParamsDef.PUBLISHED_ONLY_FLAG, Boolean.class);
    var allowsAnonymous = cfg.getTyped(TTWConfigParamsDef.ANONYMOUS_ASSETS_FLAG, Boolean.class);

    List<SemanticModelInfo> infoList = new LinkedList<>();
    while (modelSet.hasNext()) {
      Optional.of(toModelManifest(modelSet.nextSolution(), focusPlace, allowsAnonymous, cfg))
          .filter(mf -> filterByPath(mf, paths))
          .flatMap(mf -> applyStatus(mf, publishedOnly, focusPlace, historyProvider))
          .ifPresent(infoList::add);
    }

    // more than one solution per
    infoList.stream()
        .collect(groupingBy(SemanticModelInfo::getId, reducing(SemanticModelInfo::merge)))
        .values().stream()
        .flatMap(StreamUtil::trimStream)
        .forEach(this::indexModel);
  }

  /**
   * Mapper.
   * <p>
   * Ensures that a Model to be indexed matches the publication status criteria. If models are
   * allowed not to have a publication status, or the model has a publication status, will return
   * the original manifest. Otherwise, if a model does not have the required publication status,
   * tries to find the most recent past version of that model that does have a publication status,
   * and builds a manifest for that version
   *
   * @param metadata        the Model manifest
   * @param publishedOnly   if true, requires Models to have a publication status
   * @param focusPlace      the place the models come from
   * @param historyProvider the mapping between a Model and the descriptors of its previous *
   *                        versions
   * @return the version of the Model manifest that meets the status criteria, if any
   */
  @Nonnull
  protected Optional<SemanticModelInfo> applyStatus(
      @Nonnull final SemanticModelInfo metadata,
      final boolean publishedOnly,
      @Nonnull final TrisotechPlace focusPlace,
      @Nonnull final BiFunction<String, String, List<TrisotechFileInfo>> historyProvider) {
    if (Boolean.TRUE.equals(publishedOnly) && !metadata.hasState()) {
      logger.info("Model {} is not published, looking for first published version",
          metadata.getName());
      return historyProvider.apply(focusPlace.getId(), metadata.getId()).stream()
          .filter(info -> info.getState() != null)
          .findFirst()
          .map(file -> new SemanticModelInfo(file, metadata).fromPlace(focusPlace));
    } else {
      return Optional.of(metadata);
    }
  }

  /**
   * Predicate.
   * <p>
   * Determines if a model originates in one of the focus paths.
   *
   * @param metadata the Model manifest
   * @param paths    the focus paths
   * @return true if and only if the model is stored in one of the focus paths
   */
  protected boolean filterByPath(
      @Nonnull final SemanticModelInfo metadata,
      @Nonnull final Set<String> paths) {
    if (paths.stream().anyMatch(path -> metadata.getPath().startsWith(path))) {
      return true;
    } else {
      if (logger.isDebugEnabled()) {
        logger.debug("Skipping model {} in {}, not under any of {}",
            metadata.getName(), metadata.getPath(), String.join(",", paths));
      }
      return false;
    }
  }

  /**
   * Adds a Model to the Model and, if applicable, Asset Indexes
   *
   * @param metadata the Model manifest
   */
  protected void indexModel(
      @Nonnull final SemanticModelInfo metadata) {
    indexByModel(metadata);

    if (metadata.getAssetId() != null) {
      indexByAsset(parseAssetKey(metadata.getAssetId()), metadata);
    } else {
      if (logger.isInfoEnabled()) {
        logger.info("Skipping model {} - {} with no Asset ID",
            metadata.getName(), metadata.getId());
      }
    }
  }

  /**
   * Adds a Model to the Model index
   *
   * @param metadata the Model manifest
   */
  protected void indexByModel(
      @Nonnull final SemanticModelInfo metadata) {
    var modelId = metadata.getId();
    if (modelInfoByModelID.containsKey(modelId)) {
      logger.error("model ID {} - {} has been indexed multiple times",
          metadata.getId(),
          metadata.getName());
    }
    modelInfoByModelID.put(modelId, metadata);
  }

  /**
   * Adds a Model to the Asset index
   *
   * @param assetKey the Asset ID, as a {@link KeyIdentifier}
   * @param metadata the Model manifest
   */
  protected void indexByAsset(
      @Nonnull final KeyIdentifier assetKey,
      @Nonnull final SemanticModelInfo metadata) {
    if (logger.isTraceEnabled()) {
      logger.trace(
          "Cache indexing {} - {} as asset {}",
          metadata.getName(),
          metadata.getId(),
          metadata.getAssetId());
    }
    modelInfoByAssetID.computeIfAbsent(assetKey, k -> newSortedManifestSet()).add(metadata);
  }

  /* ---------------------------------------------------------------------------------------- */


  /**
   * Indexes Model/Model dependencies between BPM+ models, from the source model that 'depends on' a
   * target model. Both models must be indexed before the relationship between the two is. Adds a
   * reverese relationship link if the direct one can be established.
   *
   * @param relations the dependencies, as queried from the DES KG
   */
  protected void indexRelationships(ResultSet relations) {
    while (relations.hasNext()) {
      QuerySolution sol = relations.nextSolution();
      var srcModel = sol.getResource("?fromModel").getURI();
      var tgtModel = sol.getResource("?toModel").getURI();
      modelInfoByModelID.computeIfPresent(srcModel, (k, info) -> {
        // apply only if srcAsset has not been filtered out
        if (modelInfoByModelID.containsKey(tgtModel)) {
          info.addModelDependency(tgtModel);
          modelInfoByModelID.get(tgtModel)
              .addReverseModelDependency(srcModel);
        }
        return info;
      });
    }
  }

  /* ---------------------------------------------------------------------------------------- */


  /**
   * Indexes relationships between (computable) BPM+ models, and the Decision/Process Services that
   * expose them.
   * <p>
   * Assuming the models are annotated with Service Asset Ids, and that one model can expose
   * multiple services, maps:
   * <ul>
   *   <li>each Model ID to the Service Asset Ids exposed by that model</li>
   *   <li>each Service Asset ID to its semantic metadata descriptor</li>
   * </ul>
   * <p>
   * The semantic metadata descriptor of a service asset is partially inferred from the semantic
   * metadata of the underlying knowledge asset
   *
   * @param services the semantic metadata for all services, as queried from the DES KG
   * @param cfg      the environment configuration
   */
  protected void indexServices(
      @Nonnull final ResultSet services,
      @Nonnull final TTWEnvironmentConfiguration cfg) {
    var allowsAnonymous = cfg.getTyped(TTWConfigParamsDef.ANONYMOUS_ASSETS_FLAG, Boolean.class);

    while (services.hasNext()) {
      var sol = services.nextSolution();
      indexService(sol, allowsAnonymous, cfg);
    }
  }

  /**
   * Indexes a (Decision) Service exposed by a Decision or Process Model.
   * <p>
   * Links the Service Manifest to the Model Manifest, and indexes the Service Manifest as an Asset.
   * Note that Service Assets are not indexed as Models themselves because they do not exist as
   * such. Services originate as Model fragments, and get manifested via their API spec in the
   * Service Library, which is not indexable from the TT DES graph
   *
   * @param sol             the service asset semantic metadata, as queried from the DES KG
   * @param allowsAnonymous if true, will mint an asset ID for services that do not have one
   * @param cfg             the environment configuration
   */
  protected void indexService(
      @Nonnull final QuerySolution sol,
      final boolean allowsAnonymous,
      @Nonnull final TTWEnvironmentConfiguration cfg) {
    // asset + service joint metadata
    SemanticModelInfo serviceManifest = toServiceManifest(sol, allowsAnonymous, cfg);

    if (!allowsAnonymous && !serviceManifest.hasAssetId()) {
      // skip - anonymous asset not allowed
      return;
    }
    // ensure declaring model has not been filtered
    Optional.ofNullable(modelInfoByModelID.get(serviceManifest.getId()))
        .ifPresent(manifest -> {
          // index service - as asset only
          mergeManifests(serviceManifest, manifest);
          indexByAsset(serviceManifest.getServiceKey(), serviceManifest);
        });
  }

  /**
   * Links a Service Manifest to the Manifest of the Model that declares the service, then populates
   * the service manifest with the information inherited from the declaring model
   *
   * @param serviceInfo the service manifest
   * @param modelInfo   the model manifest
   */
  protected void mergeManifests(
      @Nonnull final SemanticModelInfo serviceInfo,
      @Nonnull final SemanticModelInfo modelInfo) {
    if (serviceInfo.getServiceId() != null) {
      var serviceId = parseAssetKey(serviceInfo.getServiceId());
      modelInfo.addExposedService(serviceId);
    }

    serviceInfo.assertServiceName(modelInfo.getName());
    serviceInfo.assertAssetId(modelInfo.getAssetId());

    serviceInfo.setPlaceId(modelInfo.getPlaceId());
    serviceInfo.setPlaceName(modelInfo.getPlaceName());
    serviceInfo.setUpdated(modelInfo.getUpdated());
    serviceInfo.setUpdater(modelInfo.getUpdater());
    serviceInfo.setVersion(modelInfo.getVersion());

    serviceInfo.setMimetype(modelInfo.getMimetype());
    serviceInfo.setUrl(modelInfo.getUrl());
  }

  /* ---------------------------------------------------------------------------------------- */

  /**
   * Transforms the result of a model query to a model manifest
   * <p>
   * Uses the query bound variables to initialize the fields of the manifest object. Generates an
   * asset ID if none is specified, and ANONYMOUS_ASSETS_FLAG is set.
   *
   * @param sol             the query result, as a set of Bindings
   * @param focusPlace      the place where the model originated
   * @param allowsAnonymous if true, will mint an asset ID for models that do not have one
   * @param cfg             the environment configuration
   * @return a manifest for the model described by the query results
   */
  @Nonnull
  protected SemanticModelInfo toModelManifest(
      @Nonnull final QuerySolution sol,
      @Nonnull final TrisotechPlace focusPlace,
      boolean allowsAnonymous,
      @Nonnull final TTWEnvironmentConfiguration cfg) {

    //?model ?assetId ?version ?state ?updated ?mimeType ?artifactName
    SemanticModelInfo manifest = new SemanticModelInfo();
    manifest.addResource(MODEL, sol);
    manifest.addLiteral(ASSET_ID, sol);

    manifest.addLiteral(ARTIFACT_NAME, sol);
    manifest.addLiteral(MIME_TYPE, sol);
    manifest.addLiteral(PATH, sol);

    manifest.addLiteral(VERSION, sol);
    manifest.addLiteral(STATE, sol);
    manifest.addLiteral(UPDATED, sol);
    manifest.addLiteral(UPDATER, sol);

    manifest.addResource(ASSET_TYPE, sol);

    manifest.setPlaceId(focusPlace.getId());
    manifest.setPlaceName(focusPlace.getName());

    manifest.initUrl(toApiEndpoint(cfg.getTyped(TTWConfigParamsDef.BASE_URL)));

    if (!manifest.hasAssetId() && allowsAnonymous) {
      var assetId =
          mintAssetIdForAnonymous(
              cfg.getTyped(TTWConfigParamsDef.ASSET_NAMESPACE),
              manifest.getId(),
              manifest.getState(),
              manifest.getUpdated());
      logger.trace("Assigned asset ID {} to model {}", assetId, manifest.getId());
      manifest.assertAssetId(assetId.getVersionId().toString());
    }
    return manifest;
  }


  /**
   * Transforms the result of a model/service query to a service manifest
   * <p>
   * Uses the query bound variables to initialize the fields of the manifest object. Generates a
   * service asset ID if none is specified, and ANONYMOUS_ASSETS_FLAG is set.
   *
   * @param sol             the query result, as a set of Bindings
   * @param allowsAnonymous if true, will mint an asset ID for models that do not have one
   * @param cfg             the environment configuration
   * @return a manifest for the model described by the query results
   */
  @Nonnull
  protected SemanticModelInfo toServiceManifest(
      @Nonnull final QuerySolution sol,
      boolean allowsAnonymous,
      @Nonnull final TTWEnvironmentConfiguration cfg) {

    SemanticModelInfo manifest = new SemanticModelInfo();
    manifest.addResource(MODEL, sol);
    manifest.addLiteral(VERSION, sol);
    manifest.addLiteral(SERVICE_ID, sol);
    manifest.addLiteral(SERVICE_NAME, sol);
    manifest.addResource(SERVICE_FRAGMENT, sol);

    manifest.addLiteral(ARTIFACT_NAME, sol);
    manifest.addResource(ASSET_TYPE, sol);

    if (manifest.getServiceFragmentId() != null && !manifest.hasAssetId() && allowsAnonymous) {
      var serviceAssetId =
          mintAssetIdForAnonymous(
              cfg.getTyped(TTWConfigParamsDef.ASSET_NAMESPACE),
              manifest.getServiceFragmentId(),
              manifest.getState(),
              manifest.getUpdated());
      logger.trace("Assigned Service asset ID {} to service {} in model {}",
          serviceAssetId, manifest.getServiceFragmentId(), manifest.getId());
      manifest.assertServiceId(serviceAssetId.getVersionId().toString());
    }

    return manifest;
  }


  /**
   * Generates a predictable asset ID for the knowledge Asset implicitly carried by a model, as a
   * knowledge artifact.
   * <p>
   * Anonymous Model Assets are versioned as follows: re-hashes the UUID of the model to obtain an
   * asset UUID Uses a CalVer version tag, based on the model last update date, approximated to the
   * Year/Month - assuming models are up-to-date, and updated around the time the knowledge is
   * revised. Adds a SNAPSHOT tag if the model has never been published, even as a draft.
   *
   * @param assetNamespace the Asset Namespace
   * @param elementId      A model ID, or a Decision Service ID, or a BPMN TODO?
   * @param state          the publication state
   * @param lastUpdated    the date when the element's owner model was last updated
   * @return a candidate Asset ID
   */
  @Nonnull
  public static ResourceIdentifier mintAssetIdForAnonymous(
      @Nonnull final URI assetNamespace,
      @Nonnull final String elementId,
      @Nullable final String state,
      @Nullable final String lastUpdated) {
    var localId = NameUtils.getTrailingPart(elementId);
    if (Util.isEmpty(localId)) {
      throw new IllegalStateException(
          "Defensive! Unable to determine anonymous asset ID for element " + elementId);
    }
    var guid = Util.uuid(localId);

    String versionTag;
    if (lastUpdated != null) {
      var lastUpdate = toLocalDate(parseDateTime(lastUpdated)).atStartOfDay();
      versionTag = format("%d.%d.0",
          lastUpdate.getYear(),
          lastUpdate.getMonthValue());
      if (state == null) {
        versionTag = versionTag + "-SNAPSHOT";
      }
    } else {
      versionTag = IdentifierConstants.VERSION_ZERO_SNAPSHOT;
    }

    return SemanticIdentifier.newId(assetNamespace, guid, versionTag);
  }

  /* ---------------------------------------------------------------------------------------- */

  /**
   * Utility.
   * <p>
   * Parses a (versioned) asset ID into a Resource Identifier
   * <p>
   * Supports pure UUIDs, or versioned URIs, in String form
   *
   * @param id the asset ID, in String form
   * @return the asset ID, as a KeyIdentifier
   */
  @Nonnull
  private KeyIdentifier parseAssetKey(@Nonnull final String id) {
    return Util.isUUID(id)
        ? newId(id).asKey()
        : newVersionId(URI.create(id)).asKey();
  }


  /**
   * Utility
   *
   * @return a Manifest Set that sorts by 'latest', and then by 'greatest' versions
   */
  @Nonnull
  private SortedSet<SemanticModelInfo> newSortedManifestSet() {
    return new TreeSet<>(comparing(this::getLastUpdateDateTime).thenComparing(this::getVersion));
  }

  /**
   * Utility
   *
   * @param info a Model manifest
   * @return the last time the Model was updated, as a Date with time granularity
   */
  @Nonnull
  private Date getLastUpdateDateTime(@Nonnull final SemanticModelInfo info) {
    return Optional.ofNullable(info.getUpdated())
        .map(DateTimeUtil::parseDateTime)
        .orElseGet(Date::new);
  }

  /**
   * Utility
   *
   * @param info a Model manifest
   * @return the Model (artifact) version, as a SemVer tag
   */
  @Nonnull
  private Version getVersion(@Nonnull final SemanticModelInfo info) {
    return VersionIdentifier.semVerOf(
        Optional.ofNullable(info.getVersion())
            .orElse(IdentifierConstants.VERSION_ZERO_SNAPSHOT));
  }

}
