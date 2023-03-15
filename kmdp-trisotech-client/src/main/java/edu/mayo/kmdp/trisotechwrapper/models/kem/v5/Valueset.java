
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
    "id",
    "value",
    "codingSystem",
    "display",
    "codes",
    "semanticType"
})

public class Valueset {

  @JsonProperty("id")
  private String id;
  @JsonProperty("value")
  private String value;
  @JsonProperty("codingSystem")
  private String codingSystem;
  @JsonProperty("display")
  private String display;
  @JsonProperty("codes")
  private List<Code> codes = new ArrayList<Code>();
  @JsonProperty("semanticType")
  private String semanticType;
  @JsonIgnore
  private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

  /**
   * No args constructor for use in serialization
   */
  public Valueset() {
  }

  /**
   * @param codes
   * @param display
   * @param semanticType
   * @param id
   * @param codingSystem
   * @param value
   */
  public Valueset(String id, String value, String codingSystem, String display, List<Code> codes,
      String semanticType) {
    super();
    this.id = id;
    this.value = value;
    this.codingSystem = codingSystem;
    this.display = display;
    this.codes = codes;
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

  public Valueset withId(String id) {
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

  public Valueset withValue(String value) {
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

  public Valueset withCodingSystem(String codingSystem) {
    this.codingSystem = codingSystem;
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

  public Valueset withDisplay(String display) {
    this.display = display;
    return this;
  }

  @JsonProperty("codes")
  public List<Code> getCodes() {
    return codes;
  }

  @JsonProperty("codes")
  public void setCodes(List<Code> codes) {
    this.codes = codes;
  }

  public Valueset withCodes(List<Code> codes) {
    this.codes = codes;
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

  public Valueset withSemanticType(String semanticType) {
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

  public Valueset withAdditionalProperty(String name, Object value) {
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
    Valueset valueset = (Valueset) o;
    return id.equals(valueset.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
