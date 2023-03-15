
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
    "semanticType",
    "content"
})

public class Tag {

  @JsonProperty("semanticType")
  private String semanticType;
  @JsonProperty("content")
  private String content;
  @JsonIgnore
  private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

  /**
   * No args constructor for use in serialization
   */
  public Tag() {
  }

  /**
   * @param semanticType
   * @param content
   */
  public Tag(String semanticType, String content) {
    super();
    this.semanticType = semanticType;
    this.content = content;
  }

  @JsonProperty("semanticType")
  public String getSemanticType() {
    return semanticType;
  }

  @JsonProperty("semanticType")
  public void setSemanticType(String semanticType) {
    this.semanticType = semanticType;
  }

  public Tag withSemanticType(String semanticType) {
    this.semanticType = semanticType;
    return this;
  }

  @JsonProperty("content")
  public String getContent() {
    return content;
  }

  @JsonProperty("content")
  public void setContent(String content) {
    this.content = content;
  }

  public Tag withContent(String content) {
    this.content = content;
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

  public Tag withAdditionalProperty(String name, Object value) {
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
    Tag tag = (Tag) o;
    return Objects.equals(semanticType, tag.semanticType) && Objects.equals(
        content, tag.content);
  }

  @Override
  public int hashCode() {
    return Objects.hash(semanticType, content);
  }
}
