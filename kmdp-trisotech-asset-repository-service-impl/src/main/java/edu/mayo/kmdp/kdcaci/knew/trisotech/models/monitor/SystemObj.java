package edu.mayo.kmdp.kdcaci.knew.trisotech.models.monitor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * SystemObj class
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SystemObj {

  private String id;
  private String url;
  private String display;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getDisplay() {
    return display;
  }

  public void setDisplay(String display) {
    this.display = display;
  }

}
