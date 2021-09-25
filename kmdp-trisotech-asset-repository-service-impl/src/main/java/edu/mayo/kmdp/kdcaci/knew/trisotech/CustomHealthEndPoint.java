package edu.mayo.kmdp.kdcaci.knew.trisotech;

import edu.mayo.kmdp.kdcaci.knew.trisotech.models.monitor.HealthObj;
import edu.mayo.kmdp.kdcaci.knew.trisotech.models.monitor.SystemObj;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.*;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@WebEndpoint(id = "health")
public class CustomHealthEndPoint {

  @Autowired
  private Environment environment;

  @Autowired
  BuildProperties buildProperties;

  @ReadOperation
  public HealthObj health()  {

    HealthObj healthObj = new HealthObj();
    SystemObj systemObj = new SystemObj();
    Map<String, String> components = new HashMap<>();
    var defaultRepo = this.environment.getProperty("edu.mayo.kmdp.project.defaultRepoUrl");
    var env = this.environment.getProperty("env");
    var configItemId = this.environment.getProperty("edu.mayo.kmdp.trisotechwrapper.configItemId");
    var configItemUrl = this.environment.getProperty("edu.mayo.kmdp.trisotechwrapper.configItemUrl");
    var configItemDisplay = this.environment.getProperty("edu.mayo.kmdp.trisotechwrapper.configItemDisplay");

    String status = "";
    if (defaultRepo != null) {
      status = pingService(defaultRepo);
      healthObj.setStatus(status);
    }

    systemObj.setId(configItemId);
    systemObj.setUrl(configItemUrl);
    systemObj.setDisplay(configItemDisplay);

    healthObj.setSystemObj(systemObj);

    healthObj.setVersion(buildProperties.getVersion());

    if (env != null) {
      healthObj.setEnv(env);
    }

    if ("UP".equals(status)) {
      healthObj.setMessage("System is UP");
    } else
      healthObj.setMessage("System is DOWN, please contact support");

    components.put("ComponentOne","Info on ComponentOne");
    healthObj.setComponents(components.entrySet().toArray());

    return healthObj;
  }

  /**
   * A simple service to ping the given repository URL to verify health
   * @param defaultRepo The repository URL to ping from application-{}.properties
   * @return String of status, either UP or DOWN
   */
  public String pingService(String defaultRepo) {
    HttpURLConnection connection = null;

    try {

      if (defaultRepo.isEmpty()) {
        throw new IOException("No default repository found");
      } else {
        URL u = new URL(defaultRepo);

        connection = (HttpURLConnection) u.openConnection();
        connection.setRequestMethod("HEAD");
        int code = connection.getResponseCode();

        if (code == 200) {
          return "UP";
        } else {
          return "DOWN";
        }

      }

    } catch (IOException e) {

      e.printStackTrace();
      return "DOWN";

    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }

}
