
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
    "targetNamespace",
    "defaultLanguage",
    "extensionElements"
})

public class KemModelProperties {

  @JsonProperty("name")
  private String name;
  @JsonProperty("targetNamespace")
  private String targetNamespace;
  @JsonProperty("defaultLanguage")
  private String defaultLanguage;
  @JsonProperty("extensionElements")
  private List<ExtensionElement> extensionElements = new ArrayList<ExtensionElement>();
  @JsonIgnore
  private final Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

  /**
   * No args constructor for use in serialization
   */
  public KemModelProperties() {
  }

  /**
   * @param defaultLanguage
   * @param targetNamespace
   * @param extensionElements
   * @param name
   */
  public KemModelProperties(String name, String targetNamespace, String defaultLanguage,
      List<ExtensionElement> extensionElements) {
    super();
    this.name = name;
    this.targetNamespace = targetNamespace;
    this.defaultLanguage = defaultLanguage;
    this.extensionElements = extensionElements;
  }

  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  public KemModelProperties withName(String name) {
    this.name = name;
    return this;
  }

  @JsonProperty("targetNamespace")
  public String getTargetNamespace() {
    return targetNamespace;
  }

  @JsonProperty("targetNamespace")
  public void setTargetNamespace(String targetNamespace) {
    this.targetNamespace = targetNamespace;
  }

  public KemModelProperties withTargetNamespace(String targetNamespace) {
    this.targetNamespace = targetNamespace;
    return this;
  }

  @JsonProperty("defaultLanguage")
  public String getDefaultLanguage() {
    return defaultLanguage;
  }

  @JsonProperty("defaultLanguage")
  public void setDefaultLanguage(String defaultLanguage) {
    this.defaultLanguage = defaultLanguage;
  }

  public KemModelProperties withDefaultLanguage(String defaultLanguage) {
    this.defaultLanguage = defaultLanguage;
    return this;
  }

  @JsonProperty("extensionElements")
  public List<ExtensionElement> getExtensionElements() {
    return extensionElements;
  }

  @JsonProperty("extensionElements")
  public void setExtensionElements(List<ExtensionElement> extensionElements) {
    this.extensionElements = extensionElements;
  }

  public KemModelProperties withExtensionElements(List<ExtensionElement> extensionElements) {
    this.extensionElements = extensionElements;
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

  public KemModelProperties withAdditionalProperty(String name, Object value) {
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
    KemModelProperties that = (KemModelProperties) o;
    return Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }
}
