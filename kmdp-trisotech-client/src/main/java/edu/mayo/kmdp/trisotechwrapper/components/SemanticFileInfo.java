package edu.mayo.kmdp.trisotechwrapper.components;

import java.util.ArrayList;
import java.util.List;

/**
 * POJO that holds the values of the semantic annotations extracted from the TT KG, via SPARQL
 *
 * @see TTGraphTerms
 */
public class SemanticFileInfo {

  private String assetId;

  private String serviceId;

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

  public void assertAssetId(String assetId) {
    this.assetId = assetId;
  }
}
