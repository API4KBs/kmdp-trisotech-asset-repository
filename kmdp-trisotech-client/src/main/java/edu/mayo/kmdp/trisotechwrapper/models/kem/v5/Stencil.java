
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
    "id"
})

public class Stencil {

  @JsonProperty("id")
  private String id;
  @JsonIgnore
  private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

  /**
   * No args constructor for use in serialization
   */
  public Stencil() {
  }

  /**
   * @param id
   */
  public Stencil(String id) {
    super();
    this.id = id;
  }

  @JsonProperty("id")
  public String getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(String id) {
    this.id = id;
  }

  public Stencil withId(String id) {
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

  public Stencil withAdditionalProperty(String name, Object value) {
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
    Stencil stencil = (Stencil) o;
    return id.equals(stencil.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
