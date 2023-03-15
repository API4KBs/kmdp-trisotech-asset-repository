
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
    "documentation",
    "extensionElements",
    "sources",
    "code",
    "valueset",
    "resource",
    "typeRef",
    "hasDataType",
    "examples",
    "alternatives",
    "deprecatedAlternatives",
    "triso:linkedTermsId"
})

public class KemConceptProperties {
  @JsonProperty("name")
  private String name;
  @JsonProperty("documentation")
  private String documentation;
  @JsonProperty("extensionElements")
  private List<ExtensionElement> extensionElements = new ArrayList<ExtensionElement>();
  @JsonProperty("sources")
  private List<Source> sources = new ArrayList<Source>();
  @JsonProperty("code")
  private List<Code> code = new ArrayList<Code>();
  @JsonProperty("valueset")
  private List<Valueset> valueset = new ArrayList<Valueset>();
  @JsonProperty("resource")
  private String resource;
  @JsonProperty("typeRef")
  private String typeRef;
  @JsonProperty("hasDataType")
  private String hasDataType;
  @JsonProperty("examples")
  private String examples;
  @JsonProperty("alternatives")
  private List<String> alternatives = new ArrayList<String>();
  @JsonProperty("deprecatedAlternatives")
  private List<Object> deprecatedAlternatives = new ArrayList<Object>();
  @JsonProperty("triso:linkedTermsId")
  private List<String> trisoLinkedTermsId = new ArrayList<String>();
  @JsonIgnore
  private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

  /**
   * No args constructor for use in serialization
   */
  public KemConceptProperties() {
  }

  /**
   * @param code
   * @param valueset
   * @param sources
   * @param extensionElements
   * @param resource
   * @param documentation
   * @param hasDataType
   * @param examples
   * @param deprecatedAlternatives
   * @param name
   * @param alternatives
   * @param trisoLinkedTermsId
   * @param typeRef
   */
  public KemConceptProperties(String name, String documentation,
      List<ExtensionElement> extensionElements, List<Source> sources, List<Code> code,
      List<Valueset> valueset, String resource, String typeRef, String hasDataType, String examples,
      List<String> alternatives, List<Object> deprecatedAlternatives,
      List<String> trisoLinkedTermsId) {
    super();
    this.name = name;
    this.documentation = documentation;
    this.extensionElements = extensionElements;
    this.sources = sources;
    this.code = code;
    this.valueset = valueset;
    this.resource = resource;
    this.typeRef = typeRef;
    this.hasDataType = hasDataType;
    this.examples = examples;
    this.alternatives = alternatives;
    this.deprecatedAlternatives = deprecatedAlternatives;
    this.trisoLinkedTermsId = trisoLinkedTermsId;
  }

  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  public KemConceptProperties withName(String name) {
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

  public KemConceptProperties withDocumentation(String documentation) {
    this.documentation = documentation;
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

  public KemConceptProperties withExtensionElements(List<ExtensionElement> extensionElements) {
    this.extensionElements = extensionElements;
    return this;
  }

  @JsonProperty("sources")
  public List<Source> getSources() {
    return sources;
  }

  @JsonProperty("sources")
  public void setSources(List<Source> sources) {
    this.sources = sources;
  }

  public KemConceptProperties withSources(List<Source> sources) {
    this.sources = sources;
    return this;
  }

  @JsonProperty("code")
  public List<Code> getCode() {
    return code;
  }

  @JsonProperty("code")
  public void setCode(List<Code> code) {
    this.code = code;
  }

  public KemConceptProperties withCode(List<Code> code) {
    this.code = code;
    return this;
  }

  @JsonProperty("valueset")
  public List<Valueset> getValueset() {
    return valueset;
  }

  @JsonProperty("valueset")
  public void setValueset(List<Valueset> valueset) {
    this.valueset = valueset;
  }

  public KemConceptProperties withValueset(List<Valueset> valueset) {
    this.valueset = valueset;
    return this;
  }

  @JsonProperty("resource")
  public String getResource() {
    return resource;
  }

  @JsonProperty("resource")
  public void setResource(String resource) {
    this.resource = resource;
  }

  public KemConceptProperties withResource(String resource) {
    this.resource = resource;
    return this;
  }

  @JsonProperty("typeRef")
  public String getTypeRef() {
    return typeRef;
  }

  @JsonProperty("typeRef")
  public void setTypeRef(String typeRef) {
    this.typeRef = typeRef;
  }

  public KemConceptProperties withTypeRef(String typeRef) {
    this.typeRef = typeRef;
    return this;
  }

  @JsonProperty("hasDataType")
  public String getHasDataType() {
    return hasDataType;
  }

  @JsonProperty("hasDataType")
  public void setHasDataType(String hasDataType) {
    this.hasDataType = hasDataType;
  }

  public KemConceptProperties withHasDataType(String hasDataType) {
    this.hasDataType = hasDataType;
    return this;
  }

  @JsonProperty("examples")
  public String getExamples() {
    return examples;
  }

  @JsonProperty("examples")
  public void setExamples(String examples) {
    this.examples = examples;
  }

  public KemConceptProperties withExamples(String examples) {
    this.examples = examples;
    return this;
  }

  @JsonProperty("alternatives")
  public List<String> getAlternatives() {
    return alternatives;
  }

  @JsonProperty("alternatives")
  public void setAlternatives(List<String> alternatives) {
    this.alternatives = alternatives;
  }

  public KemConceptProperties withAlternatives(List<String> alternatives) {
    this.alternatives = alternatives;
    return this;
  }

  @JsonProperty("deprecatedAlternatives")
  public List<Object> getDeprecatedAlternatives() {
    return deprecatedAlternatives;
  }

  @JsonProperty("deprecatedAlternatives")
  public void setDeprecatedAlternatives(List<Object> deprecatedAlternatives) {
    this.deprecatedAlternatives = deprecatedAlternatives;
  }

  public KemConceptProperties withDeprecatedAlternatives(List<Object> deprecatedAlternatives) {
    this.deprecatedAlternatives = deprecatedAlternatives;
    return this;
  }

  @JsonProperty("triso:linkedTermsId")
  public List<String> getTrisoLinkedTermsId() {
    return trisoLinkedTermsId;
  }

  @JsonProperty("triso:linkedTermsId")
  public void setTrisoLinkedTermsId(List<String> trisoLinkedTermsId) {
    this.trisoLinkedTermsId = trisoLinkedTermsId;
  }

  public KemConceptProperties withTrisoLinkedTermsId(List<String> trisoLinkedTermsId) {
    this.trisoLinkedTermsId = trisoLinkedTermsId;
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

  public KemConceptProperties withAdditionalProperty(String name, Object value) {
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
    KemConceptProperties that = (KemConceptProperties) o;
    return Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }
}
