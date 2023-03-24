package edu.mayo.kmdp.trisotechwrapper.components;

import static edu.mayo.kmdp.util.DateTimeUtil.parseDateTime;
import static edu.mayo.kmdp.util.DateTimeUtil.toLocalDate;
import static java.lang.String.format;

import edu.mayo.kmdp.util.NameUtils;
import edu.mayo.kmdp.util.Util;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.omg.spec.api4kp._20200801.id.IdentifierConstants;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;

/**
 * POJO that holds the values of the semantic annotations extracted from the TT KG, via SPARQL
 *
 * @see TTGraphTerms
 */
public class SemanticFileInfo {

  private String assetId;

  private String serviceId;

  private String serviceFragmentId;

  private final List<String> assetTypes = new ArrayList<>(3);

  private String modelId;

  private String modelVersion;

  private String publicationState;

  private String mimeType;

  private String lastUpdated;

  private String modelName;

  /**
   * Copy method that sets the values of 'this' to the values of 'other'
   *
   * @param other the other {@link SemanticFileInfo}
   */
  public void putAll(SemanticFileInfo other) {
    this.assetId = other.assetId;

    this.serviceId = other.serviceId;

    this.assetTypes.clear();
    this.assetTypes.addAll(other.assetTypes);

    this.modelId = other.modelId;

    this.modelVersion = other.modelVersion;

    this.publicationState = other.publicationState;

    this.mimeType = other.mimeType;

    this.lastUpdated = other.lastUpdated;

    this.modelName = other.modelName;

    this.serviceFragmentId = other.serviceFragmentId;
  }

  /**
   * Merge method combines the values of 'other' into 'this'.
   * <p>
   * Atomic values are copied over unless already set. Collection values are combined
   *
   * @param other the other {@link SemanticFileInfo}
   */
  public SemanticFileInfo merge(SemanticFileInfo other) {
    if (other == this) {
      return this;
    }

    if (!hasAssetId()) {
      this.assetId = other.assetId;
    }

    if (!hasServiceId()) {
      this.serviceId = other.serviceId;
    }

    this.assetTypes.addAll(other.assetTypes);

    if (!hasModelId()) {
      this.modelId = other.modelId;
    }

    if (!hasModelVersion()) {
      this.modelVersion = other.modelVersion;
    }

    if (!hasPublicationState()) {
      this.publicationState = other.publicationState;
    }

    if (!hasMimeType()) {
      this.mimeType = other.mimeType;
    }

    if (!hasLastUpdated()) {
      this.lastUpdated = other.lastUpdated;
    }

    if (!hasModelName()) {
      this.modelName = other.modelName;
    }

    return this;
  }

  /**
   * Map-like Setter for collection fields
   *
   * @param key the key that denotes the field to set
   * @param values the values to set
   * @return this {@link SemanticFileInfo} with the updated value
   */
  public SemanticFileInfo put(TTGraphTerms key, List<String> values) {
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
   * @param key the key that denotes the field to set
   * @param value the value to set
   * @return this {@link SemanticFileInfo} with the updated value
   */
  public SemanticFileInfo put(TTGraphTerms key, String value) {
    switch (key) {
      case MIME_TYPE:
        this.mimeType = value;
        break;
      case MODEL:
        this.modelId = value;
        break;
      case STATE:
        this.publicationState = value;
        break;
      case UPDATED:
        this.lastUpdated = value;
        break;
      case VERSION:
        this.modelVersion = value;
        break;
      case ASSET_ID:
        this.assetId = value;
        break;
      case ASSET_TYPE:
        this.assetTypes.add(value);
        break;
      case SERVICE_ID:
        this.serviceId = value;
        break;
      case ARTIFACT_NAME:
        this.modelName = value;
        break;
      case SERVICE_FRAGMENT:
        this.serviceFragmentId = value;
        break;
      default:
        throw new UnsupportedOperationException("Unable to handle " + key);
    }
    return this;
  }

  public String getAssetId() {
    return assetId;
  }

  public String getServiceId() {
    return serviceId;
  }

  public List<String> getAssetTypes() {
    return assetTypes;
  }

  public String getModelId() {
    return modelId;
  }

  public String getModelVersion() {
    return modelVersion;
  }

  public String getPublicationState() {
    return publicationState;
  }

  public String getMimeType() {
    return mimeType;
  }

  public String getLastUpdated() {
    return lastUpdated;
  }

  public String getModelName() {
    return modelName;
  }

  public String getServiceFragmentId() {
    return serviceFragmentId;
  }

  public boolean hasAssetId() {
    return assetId != null;
  }

  public boolean hasServiceId() {
    return serviceId != null;
  }

  public boolean hasAssetTypes() {
    return !assetTypes.isEmpty();
  }

  public boolean hasModelId() {
    return modelId != null;
  }

  public boolean hasModelVersion() {
    return modelVersion != null;
  }

  public boolean hasPublicationState() {
    return publicationState != null;
  }

  public boolean hasMimeType() {
    return mimeType != null;
  }

  public boolean hasLastUpdated() {
    return lastUpdated != null;
  }

  public boolean hasModelName() {
    return modelName != null;
  }

  public boolean hasServiceFragmentId() {
    return serviceFragmentId != null;
  }

  public void assertAssetId(String assetId) {
    this.assetId = assetId;
  }

  public void assertServiceAssetId(String serviceId) {
    this.serviceId = serviceId;
  }


  /**
   * Generates a predictable asset ID for the knowledge Asset implicitly carried by a model, as a
   * knowledge artifact.
   * <p>
   * Re-hashes the UUID of the model to obtain an asset UUID Uses a CalVer SNAPSHOT version tag,
   * based on the model last update date, approximated to the Year/Month - assuming models are
   * up-to-date, and updated around the time the knowledge is revised
   *
   * @param elementId A model Id, or a Decision Service ID, or a BPMN TODO?
   * @param lastUpdated the date when the element's owner model was last updated
   * @return a candidate Asset ID
   */
  public static ResourceIdentifier mintAssetIdForAnonymous(
      URI baseUri, String elementId, String lastUpdated) {
    var localId = NameUtils.getTrailingPart(elementId);
    if (Util.isEmpty(localId)) {
      throw new IllegalStateException(
          "Defensive! Unable to determine anonymous asset ID for element " + elementId);
    }
    var guid = Util.uuid(localId);

    String versionTag;
    if (lastUpdated != null) {
      var lastUpdate = toLocalDate(parseDateTime(lastUpdated)).atStartOfDay();
      versionTag = format("%04d.%02d.0-SNAPSHOT", lastUpdate.getYear(),
          lastUpdate.getMonthValue());
    } else {
      versionTag = IdentifierConstants.VERSION_ZERO_SNAPSHOT;
    }

    return SemanticIdentifier.newId(baseUri, guid, versionTag);
  }
}
