package edu.mayo.kmdp.kdcaci.knew.trisotech;

import edu.mayo.kmdp.kdcaci.knew.trisotech.models.monitor.StateObj;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.*;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@WebEndpoint(id = "state")
public class CustomStateEndPoint {

  @Autowired
  private Environment environment;

  @ReadOperation
  public StateObj state() {

    StateObj stateObj = new StateObj();
    stateObj.setEnv(setEnvMap().entrySet().toArray());
    stateObj.setFeatures(setFeaturesMap().entrySet().toArray());
    stateObj.setCapabilities(setCapabilitiesMap().entrySet().toArray());

    return stateObj;
  }

  public Map<String, String> setEnvMap() {
    Map<String, String> envMap = new HashMap<>();

    // Properties to retrieve from environment
    var env = this.environment.getProperty("env");
    var splunkUrl = this.environment.getProperty("splunk.url");
    var sourceType = this.environment.getProperty("splunk.source.type");
    var indexName = this.environment.getProperty("splunk.index.name");
    var trisoBaseUrl = this.environment.getProperty("edu.mayo.kmdp.trisotechwrapper.baseUrl");
    var trisoRepoName = this.environment.getProperty("edu.mayo.kmdp.trisotechwrapper.repositoryName");
    var trisoRepoId = this.environment.getProperty("edu.mayo.kmdp.trisotechwrapper.repositoryId");
    var trisoCacheExpiration = this.environment.getProperty("edu.mayo.kmdp.trisotechwrapper.expiration");

    envMap.put("env", env);
    envMap.put("SplunkToken", "********************************9aa1");
    envMap.put("SplunkUrl", splunkUrl);
    envMap.put("SplunkSourceType", sourceType);
    envMap.put("SplunkIndexName", indexName);
    envMap.put("trisoBaseUrl", trisoBaseUrl);
    envMap.put("trisoRepoName", trisoRepoName);
    envMap.put("trisoRepoId", trisoRepoId);
    envMap.put("trisoCacheExpiration", trisoCacheExpiration);

    return envMap;
  }

  public Map<String, Boolean> setFeaturesMap() {
    Map<String, Boolean> featuresMap = new HashMap<>();

    featuresMap.put("featureOne", true);

    return featuresMap;
  }

  public Map<String, String> setCapabilitiesMap() {
    Map<String, String> capabilitiesMap = new HashMap<>();

    capabilitiesMap.put("capabilityOne", "this is a capability");

    return capabilitiesMap;
  }

}
