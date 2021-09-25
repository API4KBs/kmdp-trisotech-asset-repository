package edu.mayo.kmdp.kdcaci.knew.trisotech.models.monitor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * SchemaObj class
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SchemaObj {

  private String version;
  private String url;

  public SchemaObj(String version, String url) {
    this.version = version;
    this.url = url;
  }

  public String getVersion() {
    return version;
  }

  public String getUrl() {
    return url;
  }


}
