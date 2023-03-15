
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
    "extensionElements",
    "alternatives",
    "deprecatedAlternatives"
})

public class RelationshipProperties {

  @JsonProperty("name")
  private String name;
  @JsonProperty("documentation")
  private String documentation;
  @JsonProperty("extensionElements")
  private List<Object> extensionElements = new ArrayList<Object>();
  @JsonProperty("alternatives")
  private List<Object> alternatives = new ArrayList<Object>();
  @JsonProperty("deprecatedAlternatives")
  private List<Object> deprecatedAlternatives = new ArrayList<Object>();
  @JsonIgnore
  private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

  /**
   * No args constructor for use in serialization
   */
  public RelationshipProperties() {
  }

  /**
   * @param deprecatedAlternatives
   * @param extensionElements
   * @param documentation
   * @param name
   * @param alternatives
   */
  public RelationshipProperties(String name, String documentation, List<Object> extensionElements,
      List<Object> alternatives, List<Object> deprecatedAlternatives) {
    super();
    this.name = name;
    this.documentation = documentation;
    this.extensionElements = extensionElements;
    this.alternatives = alternatives;
    this.deprecatedAlternatives = deprecatedAlternatives;
  }

  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  public RelationshipProperties withName(String name) {
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

  public RelationshipProperties withDocumentation(String documentation) {
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

  public RelationshipProperties withExtensionElements(List<Object> extensionElements) {
    this.extensionElements = extensionElements;
    return this;
  }

  @JsonProperty("alternatives")
  public List<Object> getAlternatives() {
    return alternatives;
  }

  @JsonProperty("alternatives")
  public void setAlternatives(List<Object> alternatives) {
    this.alternatives = alternatives;
  }

  public RelationshipProperties withAlternatives(List<Object> alternatives) {
    this.alternatives = alternatives;
    return this;
  }

  @JsonProperty("deprecatedAlternatives")
  public List<Object> getDeprecatedAlternatives() {
    return deprecatedAlternatives;
  }

  @JsonProperty("deprecatedAlternatives")
  public void setDeprecatedAlternatives(List<Object> deprecatedAlternatives) {
    this.deprecatedAlternatives = deprecatedAlternatives;
  }

  public RelationshipProperties withDeprecatedAlternatives(List<Object> deprecatedAlternatives) {
    this.deprecatedAlternatives = deprecatedAlternatives;
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

  public RelationshipProperties withAdditionalProperty(String name, Object value) {
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
    RelationshipProperties that = (RelationshipProperties) o;
    return Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }
}
