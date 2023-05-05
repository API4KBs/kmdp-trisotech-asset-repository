package edu.mayo.kmdp.trisotechwrapper.models.kem.v5;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Generated;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "resourceId",
    "properties",
    "stencil",
    "itemModelElements"
})
@Generated("jsonschema2pojo")
public class GraphTerm {

  @JsonProperty("resourceId")
  private String resourceId;
  @JsonProperty("properties")
  private KemConceptProperties properties;
  @JsonProperty("stencil")
  private Stencil stencil;

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

  @JsonProperty("properties")
  public KemConceptProperties getProperties() {
    return properties;
  }

  @JsonProperty("properties")
  public void setProperties(KemConceptProperties properties) {
    this.properties = properties;
  }

  @JsonProperty("stencil")
  public Stencil getStencil() {
    return stencil;
  }

  @JsonProperty("stencil")
  public void setStencil(Stencil stencil) {
    this.stencil = stencil;
  }


  @JsonAnyGetter
  public Map<String, Object> getAdditionalProperties() {
    return this.additionalProperties;
  }

  @JsonAnySetter
  public void setAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
  }

}