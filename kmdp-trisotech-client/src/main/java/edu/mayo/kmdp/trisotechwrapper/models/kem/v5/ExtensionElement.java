
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
    "namespace",
    "semanticType",
    "content",
    "tag",
    "id",
    "uri",
    "itemName",
    "modelName",
    "modelURI",
    "graphURI",
    "graphType",
    "modelType",
    "type"
})

public class ExtensionElement {

  @JsonProperty("namespace")
  private String namespace;
  @JsonProperty("semanticType")
  private String semanticType;
  @JsonProperty("content")
  private String content;
  @JsonProperty("tag")
  private List<Tag> tag = new ArrayList<Tag>();
  @JsonProperty("id")
  private String id;
  @JsonProperty("uri")
  private String uri;
  @JsonProperty("itemName")
  private String itemName;
  @JsonProperty("modelName")
  private String modelName;
  @JsonProperty("modelURI")
  private String modelURI;
  @JsonProperty("graphURI")
  private String graphURI;
  @JsonProperty("graphType")
  private String graphType;
  @JsonProperty("modelType")
  private String modelType;
  @JsonProperty("type")
  private String type;
  @JsonIgnore
  private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

  /**
   * No args constructor for use in serialization
   */
  public ExtensionElement() {
  }

  /**
   * @param graphType
   * @param modelType
   * @param type
   * @param uri
   * @param content
   * @param modelName
   * @param itemName
   * @param semanticType
   * @param namespace
   * @param modelURI
   * @param tag
   * @param id
   * @param graphURI
   */
  public ExtensionElement(String namespace, String semanticType, String content, List<Tag> tag,
      String id, String uri, String itemName, String modelName, String modelURI, String graphURI,
      String graphType, String modelType, String type) {
    super();
    this.namespace = namespace;
    this.semanticType = semanticType;
    this.content = content;
    this.tag = tag;
    this.id = id;
    this.uri = uri;
    this.itemName = itemName;
    this.modelName = modelName;
    this.modelURI = modelURI;
    this.graphURI = graphURI;
    this.graphType = graphType;
    this.modelType = modelType;
    this.type = type;
  }

  @JsonProperty("namespace")
  public String getNamespace() {
    return namespace;
  }

  @JsonProperty("namespace")
  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public ExtensionElement withNamespace(String namespace) {
    this.namespace = namespace;
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

  public ExtensionElement withSemanticType(String semanticType) {
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

  public ExtensionElement withContent(String content) {
    this.content = content;
    return this;
  }

  @JsonProperty("tag")
  public List<Tag> getTag() {
    return tag;
  }

  @JsonProperty("tag")
  public void setTag(List<Tag> tag) {
    this.tag = tag;
  }

  public ExtensionElement withTag(List<Tag> tag) {
    this.tag = tag;
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

  public ExtensionElement withId(String id) {
    this.id = id;
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

  public ExtensionElement withUri(String uri) {
    this.uri = uri;
    return this;
  }

  @JsonProperty("itemName")
  public String getItemName() {
    return itemName;
  }

  @JsonProperty("itemName")
  public void setItemName(String itemName) {
    this.itemName = itemName;
  }

  public ExtensionElement withItemName(String itemName) {
    this.itemName = itemName;
    return this;
  }

  @JsonProperty("modelName")
  public String getModelName() {
    return modelName;
  }

  @JsonProperty("modelName")
  public void setModelName(String modelName) {
    this.modelName = modelName;
  }

  public ExtensionElement withModelName(String modelName) {
    this.modelName = modelName;
    return this;
  }

  @JsonProperty("modelURI")
  public String getModelURI() {
    return modelURI;
  }

  @JsonProperty("modelURI")
  public void setModelURI(String modelURI) {
    this.modelURI = modelURI;
  }

  public ExtensionElement withModelURI(String modelURI) {
    this.modelURI = modelURI;
    return this;
  }

  @JsonProperty("graphURI")
  public String getGraphURI() {
    return graphURI;
  }

  @JsonProperty("graphURI")
  public void setGraphURI(String graphURI) {
    this.graphURI = graphURI;
  }

  public ExtensionElement withGraphURI(String graphURI) {
    this.graphURI = graphURI;
    return this;
  }

  @JsonProperty("graphType")
  public String getGraphType() {
    return graphType;
  }

  @JsonProperty("graphType")
  public void setGraphType(String graphType) {
    this.graphType = graphType;
  }

  public ExtensionElement withGraphType(String graphType) {
    this.graphType = graphType;
    return this;
  }

  @JsonProperty("modelType")
  public String getModelType() {
    return modelType;
  }

  @JsonProperty("modelType")
  public void setModelType(String modelType) {
    this.modelType = modelType;
  }

  public ExtensionElement withModelType(String modelType) {
    this.modelType = modelType;
    return this;
  }

  @JsonProperty("type")
  public String getType() {
    return type;
  }

  @JsonProperty("type")
  public void setType(String type) {
    this.type = type;
  }

  public ExtensionElement withType(String type) {
    this.type = type;
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

  public ExtensionElement withAdditionalProperty(String name, Object value) {
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
    ExtensionElement that = (ExtensionElement) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
