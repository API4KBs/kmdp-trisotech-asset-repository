package edu.mayo.kmdp.trisotechwrapper.components;

import java.util.ArrayList;
import java.util.List;

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

  public void putAll(SemanticFileInfo other) {
    this.assetId = other.assetId;

    this.serviceId = other.serviceId;

    this.assetTypes.clear();
    this.assetTypes.addAll(other.assetTypes);

    this.modelId = other.modelId;

    this.modelVersion = other.modelVersion;

    this.publicationState = other.publicationState;

    this.mimeType = other.mimeType;

    this.lastUpdated = other.mimeType;

    this.modelName = other.modelName;
  }

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


}
