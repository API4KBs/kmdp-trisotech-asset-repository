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
    "namespace",
    "id",
    "semanticType",
    "name",
    "url",
    "type",
    "embeddable",
    "displayHeight",
    "displayWidth",
    "play"
})

public class Attachment {

  @JsonProperty("namespace")
  private String namespace;
  @JsonProperty("id")
  private String id;
  @JsonProperty("semanticType")
  private String semanticType;
  @JsonProperty("name")
  private String name;
  @JsonProperty("url")
  private String url;
  @JsonProperty("type")
  private String type;
  @JsonProperty("embeddable")
  private String embeddable;
  @JsonProperty("displayHeight")
  private String displayHeight;
  @JsonProperty("displayWidth")
  private String displayWidth;
  @JsonProperty("play")
  private boolean play;
  @JsonIgnore
  private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

  @JsonProperty("namespace")
  public String getNamespace() {
    return namespace;
  }

  @JsonProperty("namespace")
  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public Attachment withNamespace(String namespace) {
    this.namespace = namespace;
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

  public Attachment withId(String id) {
    this.id = id;
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

  public Attachment withSemanticType(String semanticType) {
    this.semanticType = semanticType;
    return this;
  }

  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  public Attachment withName(String name) {
    this.name = name;
    return this;
  }

  @JsonProperty("url")
  public String getUrl() {
    return url;
  }

  @JsonProperty("url")
  public void setUrl(String url) {
    this.url = url;
  }

  public Attachment withUrl(String url) {
    this.url = url;
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

  public Attachment withType(String type) {
    this.type = type;
    return this;
  }

  @JsonProperty("embeddable")
  public String getEmbeddable() {
    return embeddable;
  }

  @JsonProperty("embeddable")
  public void setEmbeddable(String embeddable) {
    this.embeddable = embeddable;
  }

  public Attachment withEmbeddable(String embeddable) {
    this.embeddable = embeddable;
    return this;
  }

  @JsonProperty("displayHeight")
  public String getDisplayHeight() {
    return displayHeight;
  }

  @JsonProperty("displayHeight")
  public void setDisplayHeight(String displayHeight) {
    this.displayHeight = displayHeight;
  }

  public Attachment withDisplayHeight(String displayHeight) {
    this.displayHeight = displayHeight;
    return this;
  }

  @JsonProperty("displayWidth")
  public String getDisplayWidth() {
    return displayWidth;
  }

  @JsonProperty("displayWidth")
  public void setDisplayWidth(String displayWidth) {
    this.displayWidth = displayWidth;
  }

  public Attachment withDisplayWidth(String displayWidth) {
    this.displayWidth = displayWidth;
    return this;
  }

  @JsonProperty("play")
  public boolean isPlay() {
    return play;
  }

  @JsonProperty("play")
  public void setPlay(boolean play) {
    this.play = play;
  }

  public Attachment withPlay(boolean play) {
    this.play = play;
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

  public Attachment withAdditionalProperty(String name, Object value) {
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
    Attachment that = (Attachment) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}