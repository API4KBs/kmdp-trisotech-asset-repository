
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
    "id",
    "value",
    "codingSystem",
    "codingSystemDisplay",
    "display",
    "semanticType"
})

public class Code {

  @JsonProperty("id")
  private String id;
  @JsonProperty("value")
  private String value;
  @JsonProperty("codingSystem")
  private String codingSystem;
  @JsonProperty("codingSystemDisplay")
  private String codingSystemDisplay;
  @JsonProperty("display")
  private String display;
  @JsonProperty("semanticType")
  private String semanticType;
  @JsonIgnore
  private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

  /**
   * No args constructor for use in serialization
   */
  public Code() {
  }

  /**
   * @param codingSystemDisplay
   * @param display
   * @param semanticType
   * @param id
   * @param codingSystem
   * @param value
   */
  public Code(String id, String value, String codingSystem, String codingSystemDisplay,
      String display, String semanticType) {
    super();
    this.id = id;
    this.value = value;
    this.codingSystem = codingSystem;
    this.codingSystemDisplay = codingSystemDisplay;
    this.display = display;
    this.semanticType = semanticType;
  }

  @JsonProperty("id")
  public String getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(String id) {
    this.id = id;
  }

  public Code withId(String id) {
    this.id = id;
    return this;
  }

  @JsonProperty("value")
  public String getValue() {
    return value;
  }

  @JsonProperty("value")
  public void setValue(String value) {
    this.value = value;
  }

  public Code withValue(String value) {
    this.value = value;
    return this;
  }

  @JsonProperty("codingSystem")
  public String getCodingSystem() {
    return codingSystem;
  }

  @JsonProperty("codingSystem")
  public void setCodingSystem(String codingSystem) {
    this.codingSystem = codingSystem;
  }

  public Code withCodingSystem(String codingSystem) {
    this.codingSystem = codingSystem;
    return this;
  }

  @JsonProperty("codingSystemDisplay")
  public String getCodingSystemDisplay() {
    return codingSystemDisplay;
  }

  @JsonProperty("codingSystemDisplay")
  public void setCodingSystemDisplay(String codingSystemDisplay) {
    this.codingSystemDisplay = codingSystemDisplay;
  }

  public Code withCodingSystemDisplay(String codingSystemDisplay) {
    this.codingSystemDisplay = codingSystemDisplay;
    return this;
  }

  @JsonProperty("display")
  public String getDisplay() {
    return display;
  }

  @JsonProperty("display")
  public void setDisplay(String display) {
    this.display = display;
  }

  public Code withDisplay(String display) {
    this.display = display;
    return this;
  }

  @JsonProperty("semanticType")
  public String getSemanticType() {
    return semanticType;
  }

  @JsonProperty("semanticType")
  public void setSemanticType(String semanticType) {
    this.semanticType = semanticType;
  }

  public Code withSemanticType(String semanticType) {
    this.semanticType = semanticType;
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

  public Code withAdditionalProperty(String name, Object value) {
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
    Code code = (Code) o;
    return Objects.equals(id, code.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
