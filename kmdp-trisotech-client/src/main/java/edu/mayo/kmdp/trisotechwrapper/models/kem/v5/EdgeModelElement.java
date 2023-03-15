
package edu.mayo.kmdp.trisotechwrapper.models.kem.v5;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "resourceId",
    "properties",
    "stencil",
    "sourceRef",
    "targetRef"
})

public class EdgeModelElement {

  @JsonProperty("resourceId")
  private String resourceId;
  @JsonProperty("properties")
  private RelationshipProperties properties;
  @JsonProperty("stencil")
  private Stencil stencil;
  @JsonProperty("sourceRef")
  private String sourceRef;
  @JsonProperty("targetRef")
  private String targetRef;
  @JsonIgnore
  private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

  /**
   * No args constructor for use in serialization
   */
  public EdgeModelElement() {
  }

  /**
   * @param targetRef
   * @param resourceId
   * @param sourceRef
   * @param stencil
   * @param properties
   */
  public EdgeModelElement(String resourceId, RelationshipProperties properties, Stencil stencil,
      String sourceRef, String targetRef) {
    super();
    this.resourceId = resourceId;
    this.properties = properties;
    this.stencil = stencil;
    this.sourceRef = sourceRef;
    this.targetRef = targetRef;
  }

  @JsonProperty("resourceId")
  public String getResourceId() {
    return resourceId;
  }

  @JsonProperty("resourceId")
  public void setResourceId(String resourceId) {
    this.resourceId = resourceId;
  }

  public EdgeModelElement withResourceId(String resourceId) {
    this.resourceId = resourceId;
    return this;
  }

  @JsonProperty("properties")
  public RelationshipProperties getProperties() {
    return properties;
  }

  @JsonProperty("properties")
  public void setProperties(RelationshipProperties properties) {
    this.properties = properties;
  }

  public EdgeModelElement withProperties(RelationshipProperties properties) {
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

  public EdgeModelElement withStencil(Stencil stencil) {
    this.stencil = stencil;
    return this;
  }

  @JsonProperty("sourceRef")
  public String getSourceRef() {
    return sourceRef;
  }

  @JsonProperty("sourceRef")
  public void setSourceRef(String sourceRef) {
    this.sourceRef = sourceRef;
  }

  public EdgeModelElement withSourceRef(String sourceRef) {
    this.sourceRef = sourceRef;
    return this;
  }

  @JsonProperty("targetRef")
  public String getTargetRef() {
    return targetRef;
  }

  @JsonProperty("targetRef")
  public void setTargetRef(String targetRef) {
    this.targetRef = targetRef;
  }

  public EdgeModelElement withTargetRef(String targetRef) {
    this.targetRef = targetRef;
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

  public EdgeModelElement withAdditionalProperty(String name, Object value) {
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
    EdgeModelElement that = (EdgeModelElement) o;
    return Objects.equals(resourceId, that.resourceId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(resourceId);
  }
}
