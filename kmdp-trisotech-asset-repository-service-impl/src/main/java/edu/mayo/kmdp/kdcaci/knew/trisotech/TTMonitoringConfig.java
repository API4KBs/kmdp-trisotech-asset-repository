package edu.mayo.kmdp.kdcaci.knew.trisotech;

import edu.mayo.kmdp.health.datatype.ApplicationComponent;
import edu.mayo.kmdp.health.datatype.MiscProperties;
import edu.mayo.kmdp.health.datatype.Status;
import edu.mayo.kmdp.health.utils.MonitorUtil;
import edu.mayo.kmdp.terms.health.TermsHealthService;
import edu.mayo.kmdp.trisotechwrapper.TrisotechWrapper;
import java.util.Map;
import java.util.function.Supplier;
import org.omg.spec.api4kp._20200801.api.terminology.v4.server.TermsApiInternal;
import org.omg.spec.api4kp._20200801.services.KPComponent;
import org.omg.spec.api4kp._20200801.services.KPServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.ConfigurableEnvironment;


@Configuration
@PropertySource(value = {"classpath:application.properties"})
public class TTMonitoringConfig {

  @Autowired
  ConfigurableEnvironment env;

  @Value("${edu.mayo.kmdp.trisotechwrapper.repositoryId:nil}")
  String mainPlaceId;

  @Bean
  public Supplier<ApplicationComponent> termsHealth(
      @Autowired @KPServer @KPComponent(implementation = "broker") TermsApiInternal termsApiInternal) {

    return () -> {

      TermsHealthService termsHealthService = new TermsHealthService(termsApiInternal);
      return termsHealthService.assessHealth();

    };

  }

  @Bean
  Supplier<ApplicationComponent> ttServer(@Autowired TrisotechWrapper client) {
    MiscProperties mp = new MiscProperties();
    mp.putAll(MonitorUtil.getAppProperties(env));

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
