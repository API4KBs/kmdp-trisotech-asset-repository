package edu.mayo.kmdp.kdcaci.knew.trisotech.models.monitor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * StateObj Class
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class StateObj {

  private SchemaObj schemaObj = new SchemaObj("0.0.1", "https://schemas.kmd.mayo.edu/state-endpoint.json");

  private Object[] env;
  private Object[] features;
  private Object[] capabilities;

  public SchemaObj getSchemaObj() {
    return schemaObj;
  }

  public Object[] getEnv() {
    return env;
  }

  public void setEnv(Object[] env) {
    this.env = env;
  }

  public Object[] getFeatures() {
    return features;
  }

  public void setFeatures(Object[] features) {
    this.features = features;
  }

  public Object[] getCapabilities() {
    return capabilities;
  }

  public void setCapabilities(Object[] capabilities) {
    this.capabilities = capabilities;
  }


}
