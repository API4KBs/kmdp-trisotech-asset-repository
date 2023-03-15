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
    "name",
    "uri",
    "semanticType",
    "id"
})

public class Source {

  @JsonProperty("name")
  private String name;
  @JsonProperty("uri")
  private String uri;
  @JsonProperty("semanticType")
  private String semanticType;
  @JsonProperty("id")
  private String id;
  @JsonIgnore
  private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  public Source withName(String name) {
    this.name = name;
    return this;
  }

  @JsonProperty("uri")
  public String getUri() {
    return uri;
  }

  @JsonProperty("uri")
  public void setUri(String uri) {
    this.uri = uri;
  }

  public Source withUri(String uri) {
    this.uri = uri;
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

  public Source withSemanticType(String semanticType) {
    this.semanticType = semanticType;
    return this;
  }

  @JsonProperty("id")
  public String getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(String id) {
    this.id = id;
  }

  public Source withId(String id) {
    this.id = id;
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

  public Source withAdditionalProperty(String name, Object value) {
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
    Source source = (Source) o;
    return Objects.equals(id, source.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}