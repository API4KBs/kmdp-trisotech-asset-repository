
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
    "itemModelElements"
})

public class BusinessRule {

  @JsonProperty("resourceId")
  private String resourceId;
  @JsonProperty("properties")
  private RuleProperties properties;
  @JsonProperty("stencil")
  private Stencil stencil;
  @JsonProperty("itemModelElements")
  private BusinessRuleItemModelElements itemModelElements;
  @JsonIgnore
  private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

  @JsonProperty("resourceId")
  public String getResourceId() {
    return resourceId;
  }

  @JsonProperty("resourceId")
  public void setResourceId(String resourceId) {
    this.resourceId = resourceId;
  }

  public BusinessRule withResourceId(String resourceId) {
    this.resourceId = resourceId;
    return this;
  }

  @JsonProperty("properties")
  public RuleProperties getProperties() {
    return properties;
  }

  @JsonProperty("properties")
  public void setProperties(RuleProperties properties) {
    this.properties = properties;
  }

  public BusinessRule withProperties(RuleProperties properties) {
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

  public BusinessRule withStencil(Stencil stencil) {
    this.stencil = stencil;
    return this;
  }

  @JsonProperty("itemModelElements")
  public BusinessRuleItemModelElements getItemModelElements() {
    return itemModelElements;
  }

  @JsonProperty("itemModelElements")
  public void setItemModelElements(BusinessRuleItemModelElements itemModelElements) {
    this.itemModelElements = itemModelElements;
  }

  public BusinessRule withItemModelElements(BusinessRuleItemModelElements itemModelElements) {
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

  public BusinessRule withAdditionalProperty(String name, Object value) {
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
    BusinessRule that = (BusinessRule) o;
    return Objects.equals(resourceId, that.resourceId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(resourceId);
  }
}
