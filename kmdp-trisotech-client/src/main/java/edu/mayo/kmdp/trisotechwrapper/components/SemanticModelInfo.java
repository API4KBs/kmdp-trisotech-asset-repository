package edu.mayo.kmdp.trisotechwrapper.components;

import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newVersionId;
import static org.springframework.web.util.UriComponentsBuilder.fromHttpUrl;

import com.sun.istack.NotNull;
import edu.mayo.kmdp.trisotechwrapper.components.graph.TTGraphTerms;
import edu.mayo.kmdp.trisotechwrapper.config.TTApiConstants;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechPlace;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
import org.omg.spec.api4kp._20200801.id.KeyIdentifier;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link TrisotechFileInfo} is a Shape POJO that carries the technical "Artifact" metadata about a
 * Model as indexed in the TT DES Knowledge Graph. This {@link SemanticModelInfo} Shape extends
 * {@link TrisotechFileInfo}, adding "Asset" related metadata.
 * <p>
 * Since this class represents Assets, there is generally an N..M relationship between Models
 * (designated by {@link TrisotechFileInfo#id}), and Assets (#assetId) / Service Assets
 * ({@link #serviceId}). The presence of an assetId alone indicates that this class is used to
 * represent an Asset as carried by a specific Model; the serviceId denotes a Service Asset exposed
 * by a Decision or Process Model, regardless of whether the Model carries a named Asset.
 * <p>
 * Like its parent {@link TrisotechFileInfo}, this class can be instantiated using values queried
 * directly from the TT DES Graph.
 *
 * @see TTGraphTerms
 */
public class SemanticModelInfo extends TrisotechFileInfo {

  /**
   * Logger
   */
  private static final Logger logger = LoggerFactory.getLogger(SemanticModelInfo.class);

  /**
   * ID of the Asset carried by the Model, if any, as a String
   */
  @Nullable
  protected String assetId;
  /**
   * {@link KeyIdentifier} representation of the assetId, if any
   */
  @Nullable
  protected KeyIdentifier assetKey;
  /**
   * ID of a Service Asset exposed by the Model
   */
  @Nullable
  protected String serviceId;
  /**
   * {@link KeyIdentifier} representation of the serviceId, if any
   */
  protected KeyIdentifier serviceKey;
  /**
   * ID of the Model element (fragment) that specifically defines the exposed service This element
   * is set if and only if serviceId is set
   */
  @Nullable
  protected String serviceFragmentId;
  /**
   * Name/Label of the Model element (fragment) that specifically defines the exposed service This
   * element is set if and only if serviceId is set
   */
  @Nullable
  protected String serviceFragmentName;
  /**
   * The ID of the Place (repository) that this model is stored in
   * <p>
   * Note: this field may eventually belong in {@link TrisotechFileInfo}, but the native APIs do not
   * include it
   */
  @Nullable
  protected String placeId;
  /**
   * The Name of the Place (repository) that this model is stored in
   * <p>
   * Note: this field may eventually belong in {@link TrisotechFileInfo}, but the native APIs do not
   * include it
   */
  @Nullable
  protected String placeName;

  /**
   * URIs, as Strings, that evoke {@link KnowledgeAssetType} concepts used to categorize the
   * Knowledge (Service) Asset
   * <p>
   * Only includes the asserted types on the original Model, but no default nor inferrable types
   */
  protected final Set<String> assetTypes = new LinkedHashSet<>(3);

  /**
   * List of Model IDs ({@link TrisotechFileInfo#id}) of Models that this Model depends on
   */
  protected final Set<String> modelDependencies = new LinkedHashSet<>(3);
  /**
   * List of IDs ({@link #serviceKey}) of Service Assets that this Model exposes.
   * <p>
   * This field is generally set when a Model carries and Asset, an exposes at least one Service
   * Asset. This field will hold the to-many relationship on the side of the Model Asset Info
   * object. The inverse to-one key is given by the {@link #assetKey} on the
   * {@link SemanticModelInfo} where #serviceKey is the 'primary' key
   */
  protected final Set<KeyIdentifier> exposedServices = new LinkedHashSet<>(3);


  /**
   * Test factory constructor.
   * <p>
   * Adapts a {@link TrisotechFileInfo} into a {@link SemanticModelInfo}, used to test API
   * integration
   *
   * @param tt the source {@link TrisotechFileInfo}
   * @return a {@link SemanticModelInfo} with the information in the input tt
   */
  public static @Nonnull SemanticModelInfo testNewInfo(@Nonnull TrisotechFileInfo tt) {
    return new SemanticModelInfo(tt);
  }

  /**
   * Empty Constructor
   */
  public SemanticModelInfo() {
    // empty constructor
  }

  /**
   * Key Constructor
   *
   * @param modelUri the Model ID
   */
  public SemanticModelInfo(
      @Nonnull final String modelUri) {
    this.id = modelUri;
  }

  /**
   * Copy/Cast Constructor
   * <p>
   * Adapts a {@link TrisotechFileInfo} into a {@link SemanticModelInfo}
   *
   * @param tt the source {@link TrisotechFileInfo}
   */
  public SemanticModelInfo(@NotNull TrisotechFileInfo tt) {
    this.mergeInternal(tt);
  }

  /**
   * Copy/Merge Constructor
   * <p>
   * Adapts a {@link TrisotechFileInfo} into a {@link SemanticModelInfo}, then adds the additional
   * values found in a {@link SemanticModelInfo}
   *
   * @param tt   the source {@link TrisotechFileInfo}
   * @param seed the {@link SemanticModelInfo} used to complement the tt {@link TrisotechFileInfo}
   */
  public SemanticModelInfo(
      @Nonnull final TrisotechFileInfo tt,
      @Nonnull final SemanticModelInfo seed) {
    mergeInternal(tt);
    mergeSemantic(seed);
  }

  /**
   * Merger
   * <p>
   * Combines the values of 'one' and 'other' into a new {@link SemanticModelInfo}
   * <p>
   * Atomic values are copied over unless already set. Collection values are combined
   *
   * @param one   the first {@link SemanticModelInfo}
   * @param other the other {@link SemanticModelInfo}
   * @return a new {@link SemanticModelInfo} that combines one and other
   */
  public static SemanticModelInfo merge(
      @Nonnull final SemanticModelInfo one,
      @Nonnull final SemanticModelInfo other) {
    if (other == one) {
      return one;
    }
    var newInfo = new SemanticModelInfo();
    newInfo.mergeInternal(one);
    newInfo.mergeInternal(other);
    newInfo.mergeSemantic(one);
    newInfo.mergeSemantic(other);
    return newInfo;
  }

  /**
   * Merger
   * <p>
   * Combines the values of the 'other' {@link SemanticModelInfo} into this, scoped to all and only
   * the fields defined in the {@link SemanticModelInfo} class itself - excluding the fields defined
   * and inherited from {@link TrisotechFileInfo}
   * <p>
   * Atomic values are copied over unless already set. Collection values are combined
   *
   * @param other the other {@link SemanticModelInfo}
   * @see #mergeInternal(TrisotechFileInfo)
   */
  private void mergeSemantic(
      @Nonnull final SemanticModelInfo other) {
    if (other == this) {
      return;
    }
    merge(this, other, SemanticModelInfo::hasAssetId, SemanticModelInfo::getAssetId,
        SemanticModelInfo::setAssetId);
    merge(this, other, SemanticModelInfo::hasAssetKey, SemanticModelInfo::getAssetKey,
        SemanticModelInfo::setAssetKey);
    merge(this, other, SemanticModelInfo::hasServiceId, SemanticModelInfo::getServiceId,
        SemanticModelInfo::setServiceId);
    merge(this, other, SemanticModelInfo::hasServiceKey, SemanticModelInfo::getServiceKey,
        SemanticModelInfo::setServiceKey);
    merge(this, other, SemanticModelInfo::hasServiceFragmentId,
        SemanticModelInfo::getServiceFragmentId, SemanticModelInfo::setServiceFragmentId);
    merge(this, other, SemanticModelInfo::hasServiceFragmentName,
        SemanticModelInfo::getServiceFragmentName, SemanticModelInfo::setServiceFragmentName);
    merge(this, other, SemanticModelInfo::hasPlaceId, SemanticModelInfo::getPlaceId,
        SemanticModelInfo::setPlaceId);
    merge(this, other, SemanticModelInfo::hasPlaceName, SemanticModelInfo::getPlaceName,
        SemanticModelInfo::setPlaceName);

    this.assetTypes.addAll(other.getAssetTypes());
    this.modelDependencies.addAll(other.getModelDependencies());
    this.exposedServices.addAll(other.getExposedServices());
  }


  /**
   * Merger
   * <p>
   * Combines the values of the 'other' {@link TrisotechFileInfo} into this, scoped to all and only
   * the fields defined and inherited from {@link TrisotechFileInfo}
   *
   * @param other the other {@link TrisotechFileInfo}
   * @see #mergeSemantic(SemanticModelInfo)
   */
  protected void mergeInternal(TrisotechFileInfo other) {
    if (other == this) {
      return;
    }
    merge(this, other, TrisotechFileInfo::hasId, TrisotechFileInfo::getId,
        TrisotechFileInfo::setId);
    merge(this, other, TrisotechFileInfo::hasSku, TrisotechFileInfo::getSku,
        TrisotechFileInfo::setSku);
    merge(this, other, TrisotechFileInfo::hasName, TrisotechFileInfo::getName,
        TrisotechFileInfo::setName);
    merge(this, other, TrisotechFileInfo::hasPath, TrisotechFileInfo::getPath,
        TrisotechFileInfo::setPath);
    merge(this, other, TrisotechFileInfo::hasMimeType, TrisotechFileInfo::getMimetype,
        TrisotechFileInfo::setMimetype);
    merge(this, other, TrisotechFileInfo::hasUpdated, TrisotechFileInfo::getUpdated,
        TrisotechFileInfo::setUpdated);
    merge(this, other, TrisotechFileInfo::hasUpdater, TrisotechFileInfo::getUpdater,
        TrisotechFileInfo::setUpdater);
    merge(this, other, TrisotechFileInfo::hasUrl, TrisotechFileInfo::getUrl,
        TrisotechFileInfo::setUrl);
    merge(this, other, TrisotechFileInfo::hasVersion, TrisotechFileInfo::getVersion,
        TrisotechFileInfo::setVersion);
    merge(this, other, TrisotechFileInfo::hasState, TrisotechFileInfo::getState,
        TrisotechFileInfo::setState);
  }


  /**
   * Map-like Setter for collection fields
   *
   * @param key    the key that denotes the field to set
   * @param values the values to set
   * @return this {@link SemanticModelInfo} with the updated value
   */
  public SemanticModelInfo put(
      @Nonnull final TTGraphTerms key,
      @Nonnull final List<String> values) {
    if (key == TTGraphTerms.ASSET_TYPE) {
      this.assetTypes.clear();
      this.assetTypes.addAll(values);
    } else {
      throw new UnsupportedOperationException(
          "Unable to handle " + key + " with multiple values");
    }
    return this;
  }

  /**
   * Map-like Setter
   *
   * @param key   the key that denotes the field to set
   * @param value the value to set
   * @return this {@link SemanticModelInfo} with the updated value
   */
  public SemanticModelInfo put(
      @Nonnull final TTGraphTerms key,
      @Nullable String value) {
    switch (key) {
      case MIME_TYPE:
        this.mimetype = value;
        break;
      case MODEL:
        this.id = value;
        this.sku = value;
        break;
      case STATE:
        this.state = value;
        break;
      case UPDATED:
        this.updated = value;
        break;
      case UPDATER:
        this.updater = value;
        break;
      case VERSION:
        this.version = value;
        break;
      case ASSET_ID:
        assertAssetId(value);
        break;
      case ASSET_TYPE:
        this.assetTypes.add(value);
        break;
      case SERVICE_ID:
        assertServiceId(value);
        break;
      case ARTIFACT_NAME:
        this.name = value;
        break;
      case SERVICE_FRAGMENT:
        this.serviceFragmentId = value;
        break;
      case SERVICE_NAME:
        this.serviceFragmentName = value;
        break;
      case PATH:
        this.path = processPath(value);
        break;
      default:
        throw new UnsupportedOperationException("Unable to handle " + key);
    }
    return this;
  }

  @Nullable
  public String getAssetId() {
    return assetId;
  }

  public void setAssetId(
      @Nullable final String assetId) {
    this.assetId = assetId;
  }

  public boolean hasAssetId() {
    return getAssetId() != null;
  }

  @Nullable
  public String getServiceId() {
    return serviceId;
  }

  public void setServiceId(
      @Nullable final String serviceId) {
    this.serviceId = serviceId;
  }

  public boolean hasServiceId() {
    return getServiceId() != null;
  }

  public boolean hasAssetKey() {
    return assetKey != null;
  }

  @Nullable
  public KeyIdentifier getAssetKey() {
    return assetKey;
  }

  public void setAssetKey(
      @Nullable final KeyIdentifier assetKey) {
    this.assetKey = assetKey;
  }

  public boolean hasServiceKey() {
    return serviceKey != null;
  }

  public KeyIdentifier getServiceKey() {
    return serviceKey;
  }

  public void setServiceKey(
      @Nullable final KeyIdentifier serviceKey) {
    this.serviceKey = serviceKey;
  }

  @Nullable
  public String getServiceFragmentName() {
    return serviceFragmentName;
  }

  public void setServiceFragmentName(
      @Nullable final String serviceFragmentName) {
    this.serviceFragmentName = serviceFragmentName;
  }

  public boolean hasServiceFragmentName() {
    return serviceFragmentName != null;
  }

  public @Nullable String getServiceFragmentId() {
    return serviceFragmentId;
  }

  public void setServiceFragmentId(
      @Nullable final String serviceFragmentId) {
    this.serviceFragmentId = serviceFragmentId;
  }

  public boolean hasServiceFragmentId() {
    return serviceFragmentId != null;
  }

  public @Nullable String getPlaceId() {
    return placeId;
  }

  public void setPlaceId(
      @Nullable final String placeId) {
    this.placeId = placeId;
  }

  public boolean hasPlaceId() {
    return getPlaceId() != null;
  }

  public @Nullable String getPlaceName() {
    return placeName;
  }

  public void setPlaceName(
      @Nullable final String placeName) {
    this.placeName = placeName;
  }

  public boolean hasPlaceName() {
    return getPlaceName() != null;
  }


  @Nonnull
  public List<String> getAssetTypes() {
    return List.copyOf(assetTypes);
  }

  public void addAssetType(
      @Nonnull final String assetType) {
    this.assetTypes.add(assetType);
  }

  @Nonnull
  public Set<String> getModelDependencies() {
    return Collections.unmodifiableSet(modelDependencies);
  }

  public void addModelDependency(
      @Nonnull final String dependencyModelId) {
    this.modelDependencies.add(dependencyModelId);
  }

  @Nonnull
  public List<KeyIdentifier> getExposedServices() {
    return List.copyOf(exposedServices);
  }

  public void addExposedService(
      @Nonnull final KeyIdentifier serviceAssetId) {
    this.exposedServices.add(serviceAssetId);
  }


  public void assertAssetId(
      @Nullable final String assetId) {
    setAssetId(assetId);
    setAssetKey(assetId != null ? newVersionId(URI.create(assetId)).asKey() : null);
  }

  public void assertServiceId(
      @Nullable final String serviceId) {
    setServiceId(serviceId);
    setServiceKey(serviceId != null ? newVersionId(URI.create(serviceId)).asKey() : null);
  }

  public void initUrl(
      @Nonnull final String baseUrl) {
    this.url = fromHttpUrl(baseUrl + TTApiConstants.MODEL_PATH)
        .build(placeId, mimetype, path, sku).toString();
  }


  /**
   * Binder
   * <p>
   * Sets a Resource (URI) value based on a SPARQL Query Result bound variable
   *
   * @param graphTerm the Query Variable
   * @param sol       the bound valuables, which should include the graphTerm
   */
  public void addResource(
      @Nonnull final TTGraphTerms graphTerm,
      @Nonnull final QuerySolution sol) {
    Optional.ofNullable(sol.getResource(graphTerm.getKey()))
        .map(Resource::getURI)
        .ifPresent(t -> put(graphTerm, t));
  }

  /**
   * Binder
   * <p>
   * Sets a Literal value based on a SPARQL Query Result bound variable
   *
   * @param graphTerm the Query Variable
   * @param sol       the bound valuables, which should include the graphTerm
   */
  public void addLiteral(
      @Nonnull final TTGraphTerms graphTerm,
      @Nonnull final QuerySolution sol) {
    Optional.ofNullable(sol.getLiteral(graphTerm.getKey()))
        .map(Literal
            ::getString)
        .ifPresent(t -> put(graphTerm, t));
  }

  /**
   * Combined Setter
   * <p>
   * Sets the {@link #placeId} and {@link #placeName} given a Place descriptor
   *
   * @param focusPlace the Place
   * @return this, with Place ID and name set
   */
  @Nonnull
  public SemanticModelInfo fromPlace(
      @Nonnull final TrisotechPlace focusPlace) {
    setPlaceId(focusPlace.getId());
    setPlaceName(focusPlace.getName());
    return this;
  }

  /**
   * Adapter
   * <p>
   * Removes the file name from a file path, to yield a path from a Place root ("/") to the folder
   * that contains the Model file
   *
   * @param value the file path
   * @return the path to the folder that contains the file
   */
  @Nullable
  private String processPath(
      @Nullable final String value) {
    if (value == null) {
      return null;
    }
    // the path from the graph contains the file name
    return value.substring(0, value.lastIndexOf('/') + 1);
  }

  /**
   * Equals
   * <p>
   * Uses {@link #id}
   *
   * @param o the other object to be compared
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SemanticModelInfo that = (SemanticModelInfo) o;
    return Objects.equals(id, that.id);
  }

  /**
   * Hashcode
   * <p>
   * Uses {@link #id}
   *
   * @return the hashCode of this object
   */
  @Override
  public int hashCode() {
    return Objects.hash(id);
  }


  /**
   * Generic merge utility
   * <p>
   * Implements a generics coalesce function:
   * <p>
   * {@code if one.X == null then one.X = other.X}
   */
  protected static <S, T> void merge(
      @Nonnull final S one,
      @Nonnull final S other,
      @Nonnull final Predicate<S> hasIt,
      @Nonnull final Function<S, T> getter,
      @Nonnull final BiConsumer<S, T> setter) {
    var x1 = getter.apply(one);
    var x2 = getter.apply(other);
    if (Objects.equals(x1, x2)) {
      return;
    }
    if (!hasIt.test(one)) {
      setter.accept(one, getter.apply(other));
    } else {
      logger.error("Unable to merge conflicting values : {} and {}", x1, x2);
      throw new UnsupportedOperationException(
          "Unable to merge conflicting values : " + x1 + " and " + x2);
    }
  }

}
