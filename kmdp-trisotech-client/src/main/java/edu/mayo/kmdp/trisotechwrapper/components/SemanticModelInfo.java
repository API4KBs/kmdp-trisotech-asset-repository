package edu.mayo.kmdp.trisotechwrapper.components;

import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newVersionId;
import static org.springframework.web.util.UriComponentsBuilder.fromHttpUrl;

import edu.mayo.kmdp.trisotechwrapper.components.graph.TTGraphTerms;
import edu.mayo.kmdp.trisotechwrapper.config.TTApiConstants;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechPlace;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
import org.omg.spec.api4kp._20200801.id.KeyIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * POJO that holds the values of the semantic annotations extracted from the TT KG, via SPARQL
 *
 * @see TTGraphTerms
 */
public class SemanticModelInfo extends TrisotechFileInfo {

  private static final Logger logger = LoggerFactory.getLogger(SemanticModelInfo.class);

  protected String assetId;
  protected KeyIdentifier assetKey;
  protected String serviceId;
  protected KeyIdentifier serviceKey;
  protected String serviceFragmentId;
  protected String serviceFragmentName;
  protected String placeId;
  protected String placeName;

  protected final Set<String> assetTypes = new LinkedHashSet<>(3);

  protected final Set<String> modelDependencies = new LinkedHashSet<>(3);
  protected final Set<KeyIdentifier> exposedServices = new LinkedHashSet<>(3);


  public static SemanticModelInfo testNewInfo(TrisotechFileInfo tt) {
    return new SemanticModelInfo(tt);
  }

  public SemanticModelInfo() {
    // empty constructor
  }

  public SemanticModelInfo(TrisotechFileInfo tt) {
    this.mergeInternal(tt);
  }

  public SemanticModelInfo(TrisotechFileInfo tt, SemanticModelInfo seed) {
    mergeInternal(tt);
    mergeSemantic(seed);
  }

  /**
   * Merge method combines the values of 'one' and 'other' into a new {@link SemanticModelInfo}
   * <p>
   * Atomic values are copied over unless already set. Collection values are combined
   *
   * @param one   the first {@link SemanticModelInfo}
   * @param other the other {@link SemanticModelInfo}
   * @return a new {@link SemanticModelInfo} that combines one and other
   */
  public static SemanticModelInfo merge(SemanticModelInfo one, SemanticModelInfo other) {
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

  private void mergeSemantic(SemanticModelInfo other) {
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
  public SemanticModelInfo put(TTGraphTerms key, List<String> values) {
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
  public SemanticModelInfo put(TTGraphTerms key, String value) {
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

  public String getAssetId() {
    return assetId;
  }

  public void setAssetId(String assetId) {
    this.assetId = assetId;
  }

  public boolean hasAssetId() {
    return getAssetId() != null;
  }

  public String getServiceId() {
    return serviceId;
  }

  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }

  public boolean hasServiceId() {
    return getServiceId() != null;
  }

  public boolean hasAssetKey() {
    return assetKey != null;
  }

  public KeyIdentifier getAssetKey() {
    return assetKey;
  }

  public void setAssetKey(KeyIdentifier assetKey) {
    this.assetKey = assetKey;
  }

  public boolean hasServiceKey() {
    return serviceKey != null;
  }

  public KeyIdentifier getServiceKey() {
    return serviceKey;
  }

  public void setServiceKey(KeyIdentifier serviceKey) {
    this.serviceKey = serviceKey;
  }

  public String getServiceFragmentName() {
    return serviceFragmentName;
  }

  public void setServiceFragmentName(String serviceFragmentName) {
    this.serviceFragmentName = serviceFragmentName;
  }

  public boolean hasServiceFragmentName() {
    return serviceFragmentName != null;
  }

  public String getServiceFragmentId() {
    return serviceFragmentId;
  }

  public void setServiceFragmentId(String serviceFragmentId) {
    this.serviceFragmentId = serviceFragmentId;
  }

  public boolean hasServiceFragmentId() {
    return serviceFragmentId != null;
  }

  public String getPlaceId() {
    return placeId;
  }

  public void setPlaceId(String placeId) {
    this.placeId = placeId;
  }

  public boolean hasPlaceId() {
    return getPlaceId() != null;
  }

  public String getPlaceName() {
    return placeName;
  }

  public void setPlaceName(String placeName) {
    this.placeName = placeName;
  }

  public boolean hasPlaceName() {
    return getPlaceName() != null;
  }


  public List<String> getAssetTypes() {
    return Collections.unmodifiableList(new ArrayList<>(assetTypes));
  }

  public void addAssetType(String assetType) {
    this.assetTypes.add(assetType);
  }

  public Set<String> getModelDependencies() {
    return Collections.unmodifiableSet(modelDependencies);
  }

  public void addModelDependency(String dependencyModelId) {
    this.modelDependencies.add(dependencyModelId);
  }

  public List<KeyIdentifier> getExposedServices() {
    return Collections.unmodifiableList(new ArrayList<>(exposedServices));
  }

  public void addExposedService(KeyIdentifier serviceAssetId) {
    this.exposedServices.add(serviceAssetId);
  }


  public void assertAssetId(String assetId) {
    setAssetId(assetId);
    setAssetKey(assetId != null ? newVersionId(URI.create(assetId)).asKey() : null);
  }

  public void assertServiceId(String serviceId) {
    setServiceId(serviceId);
    setServiceKey(serviceId != null ? newVersionId(URI.create(serviceId)).asKey() : null);
  }

  public void initUrl(String baseUrl) {
    this.url = fromHttpUrl(baseUrl + TTApiConstants.MODEL_PATH)
        .build(placeId, mimetype, path, sku).toString();
  }


  public void addResource(
      TTGraphTerms graphTerm, QuerySolution soln) {
    Optional.ofNullable(soln.getResource(graphTerm.getKey()))
        .map(Resource::getURI)
        .ifPresent(t -> put(graphTerm, t));
  }

  public void addLiteral(
      TTGraphTerms graphTerm, QuerySolution soln) {
    Optional.ofNullable(soln.getLiteral(graphTerm.getKey()))
        .map(Literal
            ::getString)
        .ifPresent(t -> put(graphTerm, t));
  }


  public SemanticModelInfo fromPlace(TrisotechPlace focusPlace) {
    setPlaceId(focusPlace.getId());
    setPlaceName(focusPlace.getName());
    return this;
  }


  private String processPath(String value) {
    // the path from the graph contains the file name
    return value.substring(0, value.lastIndexOf('/') + 1);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.id);
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof SemanticModelInfo &&
        Objects.equals(this.id, ((SemanticModelInfo) o).id);
  }


  protected static <S, T> void merge(
      S one,
      S other,
      Predicate<S> hasIt,
      Function<S, T> getter,
      BiConsumer<S, T> setter) {
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
