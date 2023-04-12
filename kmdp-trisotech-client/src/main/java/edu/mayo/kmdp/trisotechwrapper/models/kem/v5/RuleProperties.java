
package edu.mayo.kmdp.trisotechwrapper.models.kem.v5;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "name",
    "documentation",
    "extensionElements",
    "ruleText",
    "ruleHTML",
    "termRefs",
    "localName"
})

public class RuleProperties {

  @JsonProperty("name")
  private String name;
  @JsonProperty("documentation")
  private String documentation;
  @JsonProperty("extensionElements")
  private List<Object> extensionElements;
  @JsonProperty("ruleText")
  private String ruleText;
  @JsonProperty("ruleHTML")
  private String ruleHTML;
  @JsonProperty("termRefs")
  private List<String> termRefs;
  @JsonProperty("localName")
  private String localName;
  @JsonIgnore
  private final Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  public RuleProperties withName(String name) {
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

  public RuleProperties withDocumentation(String documentation) {
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

  public RuleProperties withExtensionElements(List<Object> extensionElements) {
    this.extensionElements = extensionElements;
    return this;
  }

  @JsonProperty("ruleText")
  public String getRuleText() {
    return ruleText;
  }

  @JsonProperty("ruleText")
  public void setRuleText(String ruleText) {
    this.ruleText = ruleText;
  }

  public RuleProperties withRuleText(String ruleText) {
    this.ruleText = ruleText;
    return this;
  }

  @JsonProperty("ruleHTML")
  public String getRuleHTML() {
    return ruleHTML;
  }

  @JsonProperty("ruleHTML")
  public void setRuleHTML(String ruleHTML) {
    this.ruleHTML = ruleHTML;
  }

  public RuleProperties withRuleHTML(String ruleHTML) {
    this.ruleHTML = ruleHTML;
    return this;
  }

  @JsonProperty("termRefs")
  public List<String> getTermRefs() {
    return termRefs;
  }

  @JsonProperty("termRefs")
  public void setTermRefs(List<String> termRefs) {
    this.termRefs = termRefs;
  }

  public RuleProperties withTermRefs(List<String> termRefs) {
    this.termRefs = termRefs;
    return this;
  }

  @JsonProperty("localName")
  public String getLocalName() {
    return localName;
  }

  @JsonProperty("localName")
  public void setLocalName(String localName) {
    this.localName = localName;
  }

  public RuleProperties withLocalName(String localName) {
    this.localName = localName;
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

  public RuleProperties withAdditionalProperty(String name, Object value) {
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
    RuleProperties that = (RuleProperties) o;
    return Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }
}
