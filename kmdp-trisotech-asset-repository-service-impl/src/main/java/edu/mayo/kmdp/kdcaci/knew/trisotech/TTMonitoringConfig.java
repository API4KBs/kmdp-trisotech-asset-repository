package edu.mayo.kmdp.kdcaci.knew.trisotech;

import edu.mayo.kmdp.health.HealthEndPoint;
import edu.mayo.kmdp.health.StateEndPoint;
import edu.mayo.kmdp.health.VersionEndPoint;
import edu.mayo.kmdp.health.datatype.ApplicationComponent;
import edu.mayo.kmdp.health.datatype.MiscProperties;
import edu.mayo.kmdp.health.datatype.Status;
import edu.mayo.kmdp.health.utils.MonitorUtil;
import edu.mayo.kmdp.trisotechwrapper.TrisotechWrapper;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.ConfigurableEnvironment;


@ComponentScan(basePackageClasses = {
    TrisotechWrapper.class,
    HealthEndPoint.class,
    StateEndPoint.class,
    VersionEndPoint.class})
@Configuration
@PropertySource(value = {"classpath:application.properties"})
public class TTMonitoringConfig {

  @Value("${edu.mayo.kmdp.trisotechwrapper.repositoryId:nil}")
  String mainPlaceId;

  @Bean
  Supplier<ApplicationComponent> ttServer(
      @Autowired ConfigurableEnvironment env,
      @Autowired TrisotechWrapper client) {
    MiscProperties mp = new MiscProperties();
    MonitorUtil.getAppProperties(env).forEach((prop, value) -> {
      var safeVal = MonitorUtil.defaultIsSecret(prop)
          ? MonitorUtil.obfuscate(value, 4)
          : value;
      mp.put(prop, safeVal);
    });

    return () -> {
      ApplicationComponent c = new ApplicationComponent();
      c.setName("TT DES Server");
      Map<String, String> placeMap = client.listPlaces();
      if (placeMap.isEmpty()) {
        c.setStatusMessage("Unable to retrieve information from the Server");
        c.setStatus(Status.DOWN);
      } else if (!placeMap.containsKey(mainPlaceId)) {
        c.setStatusMessage("Unable to find primary Model Place");
        c.setStatus(Status.IMPAIRED);
      } else {
        c.setStatus(Status.UP);
      }
      c.setDetails(mp);
      return c;
    };
  }

}
