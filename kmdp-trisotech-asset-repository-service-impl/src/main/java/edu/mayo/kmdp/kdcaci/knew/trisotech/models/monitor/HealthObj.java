package edu.mayo.kmdp.kdcaci.knew.trisotech.models.monitor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * HealthObj Class
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class HealthObj {

  private SchemaObj schemaObj = new SchemaObj("0.0.1", "https://schemas.kmd.mayo.edu/health-endpoint.json");
  private String status;
  private SystemObj systemObj;
  private String version;
  private String env;
  private String message;
  private Object[] components;

  public SchemaObj getSchemaObj() {
    return schemaObj;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public SystemObj getSystemObj() {
    return systemObj;
  }

  public void setSystemObj(SystemObj systemObj) {
    this.systemObj = systemObj;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getEnv() {
    return env;
  }

  public void setEnv(String env) {
    this.env = env;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public Object[] getComponents() {
    return components;
  }

  public void setComponents(Object[] components) {
    this.components = components;
  }



}
