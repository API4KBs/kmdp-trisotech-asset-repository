
package edu.mayo.kmdp.trisotechwrapper.models.kem.v5;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "diagramId",
    "modelVersion",
    "properties",
    "itemModelElements",
    "nodeModelElements",
    "edgeModelElements"
})

public class KemModel {

  @JsonProperty("diagramId")
  private String diagramId;
  @JsonProperty("modelVersion")
  private double modelVersion;
  @JsonProperty("properties")
  private KemModelProperties properties;


  @JsonProperty("itemModelElements")
  private KemItemModelElements itemModelElements;
  @JsonProperty("nodeModelElements")
  private List<KemConcept> nodeModelElements = new ArrayList<KemConcept>();

  @JsonProperty("edgeModelElements")
  private List<EdgeModelElement> edgeModelElements = new ArrayList<EdgeModelElement>();
  @JsonIgnore
  private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

  /**
   * No args constructor for use in serialization
   */
  public KemModel() {
  }

  /**
   * @param edgeModelElements
   * @param diagramId
   * @param nodeModelElements
   * @param modelVersion
   * @param properties
   */
  public KemModel(String diagramId,
      KemModelProperties properties,
      KemItemModelElements itemModelElements,
      List<KemConcept> nodeModelElements,
      List<EdgeModelElement> edgeModelElements) {
    super();
    this.diagramId = diagramId;
    this.modelVersion = 5.07;
    this.properties = properties;
    this.itemModelElements = itemModelElements;
    this.nodeModelElements = nodeModelElements;
    this.edgeModelElements = edgeModelElements;
  }

  @JsonProperty("diagramId")
  public String getDiagramId() {
    return diagramId;
  }

  @JsonProperty("diagramId")
  public void setDiagramId(String diagramId) {
    this.diagramId = diagramId;
  }

  public KemModel withDiagramId(String diagramId) {
    this.diagramId = diagramId;
    return this;
  }

  @JsonProperty("modelVersion")
  public double getModelVersion() {
    return modelVersion;
  }

  @JsonProperty("modelVersion")
  public void setModelVersion(double modelVersion) {
    this.modelVersion = modelVersion;
  }

  public KemModel withModelVersion(double modelVersion) {
    this.modelVersion = modelVersion;
    return this;
  }

  @JsonProperty("properties")
  public KemModelProperties getProperties() {
    return properties;
  }

  @JsonProperty("properties")
  public void setProperties(KemModelProperties properties) {
    this.properties = properties;
  }

  public KemModel withProperties(KemModelProperties properties) {
    this.properties = properties;
    return this;
  }

  @JsonProperty("nodeModelElements")
  public List<KemConcept> getNodeModelElements() {
    return nodeModelElements;
  }

  @JsonProperty("nodeModelElements")
  public void setNodeModelElements(List<KemConcept> nodeModelElements) {
    this.nodeModelElements = nodeModelElements;
  }

  public KemModel withNodeModelElements(List<KemConcept> nodeModelElements) {
    this.nodeModelElements = nodeModelElements;
    return this;
  }

  @JsonProperty("edgeModelElements")
  public List<EdgeModelElement> getEdgeModelElements() {
    return edgeModelElements;
  }

  @JsonProperty("edgeModelElements")
  public void setEdgeModelElements(List<EdgeModelElement> edgeModelElements) {
    this.edgeModelElements = edgeModelElements;
  }

  public KemModel withEdgeModelElements(List<EdgeModelElement> edgeModelElements) {
    this.edgeModelElements = edgeModelElements;
    return this;
  }

  @JsonProperty("itemModelElements")
  public KemItemModelElements getItemModelElements() {
    return itemModelElements;
  }

  @JsonProperty("itemModelElements")
  public void setItemModelElements(
      KemItemModelElements itemModelElements) {
    this.itemModelElements = itemModelElements;
  }

  public KemModel withKemItemModelElements(KemItemModelElements itemModelElements) {
    this.itemModelElements = itemModelElements;
    return this;
  }

  @JsonAnyGetter
  public Map<String, Object> getAdditionalProperties() {
    return this.additionalProperties;
  }

  @JsonAnySetter
  public void setAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
  }

  public KemModel withAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    KemModel kemModel = (KemModel) o;
    return Objects.equals(diagramId, kemModel.diagramId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(diagramId);
  }
}
