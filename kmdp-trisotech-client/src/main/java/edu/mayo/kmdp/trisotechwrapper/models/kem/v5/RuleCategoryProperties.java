
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
    "name",
    "documentation",
    "extensionElements"
})

public class RuleCategoryProperties {

  @JsonProperty("name")
  private String name;
  @JsonProperty("documentation")
  private String documentation = "";
  @JsonProperty("extensionElements")
  private List<Object> extensionElements = new ArrayList<>();
  @JsonIgnore
  private final Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  public RuleCategoryProperties withName(String name) {
    this.name = name;
    return this;
  }

  @JsonProperty("documentation")
  public String getDocumentation() {
    return documentation;
  }

  @JsonProperty("documentation")
  public void setDocumentation(String documentation) {
    this.documentation = documentation;
  }

  public RuleCategoryProperties withDocumentation(String documentation) {
    this.documentation = documentation;
    return this;
  }

  @JsonProperty("extensionElements")
  public List<Object> getExtensionElements() {
    return extensionElements;
  }

  @JsonProperty("extensionElements")
  public void setExtensionElements(List<Object> extensionElements) {
    this.extensionElements = extensionElements;
  }

  public RuleCategoryProperties withExtensionElements(List<Object> extensionElements) {
    this.extensionElements = extensionElements;
    return this;
  }

  @JsonAnyGetter
  public Map<String, Object> getAdditionalRuleCategoryProperties() {
    return this.additionalProperties;
  }

  @JsonAnySetter
  public void setAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
  }

  public RuleCategoryProperties withAdditionalProperty(String name, Object value) {
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
    RuleCategoryProperties that = (RuleCategoryProperties) o;
    return Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }
}

