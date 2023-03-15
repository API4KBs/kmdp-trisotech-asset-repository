
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
    "resourceId",
    "properties",
    "stencil",
    "nodeModelElements",
    "attachedNodeRefs"
})

public class KemConcept {

  @JsonProperty("resourceId")
  private String resourceId;
  @JsonProperty("properties")
  private KemConceptProperties properties;
  @JsonProperty("stencil")
  private Stencil stencil;
  @JsonProperty("nodeModelElements")
  private List<Object> nodeModelElements = new ArrayList<Object>();
  @JsonProperty("attachedNodeRefs")
  private List<Object> attachedNodeRefs = new ArrayList<Object>();
  @JsonIgnore
  private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

  /**
   * No args constructor for use in serialization
   */
  public KemConcept() {
  }

  /**
   * @param resourceId
   * @param nodeModelElements
   * @param attachedNodeRefs
   * @param stencil
   * @param properties
   */
  public KemConcept(String resourceId, KemConceptProperties properties, Stencil stencil,
      List<Object> nodeModelElements, List<Object> attachedNodeRefs) {
    super();
    this.resourceId = resourceId;
    this.properties = properties;
    this.stencil = stencil;
    this.nodeModelElements = nodeModelElements;
    this.attachedNodeRefs = attachedNodeRefs;
  }

  @JsonProperty("resourceId")
  public String getResourceId() {
    return resourceId;
  }

  @JsonProperty("resourceId")
  public void setResourceId(String resourceId) {
    this.resourceId = resourceId;
  }

  public KemConcept withResourceId(String resourceId) {
    this.resourceId = resourceId;
    return this;
  }

  @JsonProperty("properties")
  public KemConceptProperties getProperties() {
    return properties;
  }

  @JsonProperty("properties")
  public void setProperties(KemConceptProperties properties) {
    this.properties = properties;
  }

  public KemConcept withProperties(KemConceptProperties properties) {
    this.properties = properties;
    return this;
  }

  @JsonProperty("stencil")
  public Stencil getStencil() {
    return stencil;
  }

  @JsonProperty("stencil")
  public void setStencil(Stencil stencil) {
    this.stencil = stencil;
  }

  public KemConcept withStencil(Stencil stencil) {
    this.stencil = stencil;
    return this;
  }

  @JsonProperty("nodeModelElements")
  public List<Object> getNodeModelElements() {
    return nodeModelElements;
  }

  @JsonProperty("nodeModelElements")
  public void setNodeModelElements(List<Object> nodeModelElements) {
    this.nodeModelElements = nodeModelElements;
  }

  public KemConcept withNodeModelElements(List<Object> nodeModelElements) {
    this.nodeModelElements = nodeModelElements;
    return this;
  }

  @JsonProperty("attachedNodeRefs")
  public List<Object> getAttachedNodeRefs() {
    return attachedNodeRefs;
  }

  @JsonProperty("attachedNodeRefs")
  public void setAttachedNodeRefs(List<Object> attachedNodeRefs) {
    this.attachedNodeRefs = attachedNodeRefs;
  }

  public KemConcept withAttachedNodeRefs(List<Object> attachedNodeRefs) {
    this.attachedNodeRefs = attachedNodeRefs;
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

  public KemConcept withAdditionalProperty(String name, Object value) {
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
    KemConcept that = (KemConcept) o;
    return resourceId.equals(that.resourceId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(resourceId);
  }
}
