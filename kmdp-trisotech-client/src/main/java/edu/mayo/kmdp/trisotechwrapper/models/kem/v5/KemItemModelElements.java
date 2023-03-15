
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

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "businessRulesCategory"
})

public class KemItemModelElements {

  @JsonProperty("businessRulesCategory")
  private List<BusinessRulesCategory> businessRulesCategory = new ArrayList<>();
  @JsonIgnore
  private final Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

  @JsonProperty("businessRulesCategory")
  public List<BusinessRulesCategory> getBusinessRulesCategory() {
    return businessRulesCategory;
  }

  @JsonProperty("businessRulesCategory")
  public void setBusinessRulesCategory(List<BusinessRulesCategory> businessRulesCategory) {
    this.businessRulesCategory = businessRulesCategory;
  }

  public KemItemModelElements withBusinessRulesCategory(
      List<BusinessRulesCategory> businessRulesCategory) {
    this.businessRulesCategory = businessRulesCategory;
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

  public KemItemModelElements withAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
    return this;
  }



}
