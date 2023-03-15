
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
    "businessRule"
})

public class BusinessRuleItemModelElements {

  @JsonProperty("businessRule")
  private List<BusinessRule> businessRule = new ArrayList<BusinessRule>();
  @JsonIgnore
  private final Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

  @JsonProperty("businessRule")
  public List<BusinessRule> getBusinessRule() {
    return businessRule;
  }

  @JsonProperty("businessRule")
  public void setBusinessRule(List<BusinessRule> businessRule) {
    this.businessRule = businessRule;
  }

  public BusinessRuleItemModelElements withBusinessRule(List<BusinessRule> businessRule) {
    this.businessRule = businessRule;
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

  public BusinessRuleItemModelElements withAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
    return this;
  }



}
