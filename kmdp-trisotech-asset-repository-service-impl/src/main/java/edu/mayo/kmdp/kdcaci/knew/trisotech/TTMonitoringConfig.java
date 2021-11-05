package edu.mayo.kmdp.kdcaci.knew.trisotech;

import edu.mayo.kmdp.health.datatype.ApplicationComponent;
import edu.mayo.kmdp.health.datatype.MiscProperties;
import edu.mayo.kmdp.health.datatype.Status;
import edu.mayo.kmdp.health.utils.MonitorUtil;
import edu.mayo.kmdp.trisotechwrapper.TrisotechWrapper;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.terminology.v4.server.TermsApiInternal;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.KPComponent;
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

  @Value("${edu.mayo.kmdp.kasrs.repository.defaultRepoUrl:n/a}")
  String mainKarsUrl;

  @Bean
  Supplier<ApplicationComponent> terms(@Autowired @KPComponent(implementation = "broker")
      TermsApiInternal terms) {
    MiscProperties mp = new MiscProperties();
    mp.put("edu.mayo.kmdp.kasrs.repository.defaultRepoUrl", mainKarsUrl);
    return () -> {
      ApplicationComponent c = new ApplicationComponent();
      c.setName("Terminology Provideer");
      if (terms == null) {
        c.setStatusMessage("No Terminology Provider Available");
        c.setStatus(Status.DOWN);
      } else if (mainKarsUrl == null) {
        c.setStatusMessage("No KARS source - falling back to classpath resources");
        c.setStatus(Status.IMPAIRED);
      } else {
        Answer<List<Pointer>> terminologies = terms.listTerminologies();
        if (terminologies.isFailure()) {
          c.setStatusMessage("Unable to access vocabularies : " + terminologies.printExplanation());
          c.setStatus(Status.IMPAIRED);
        } else if (terminologies.get().isEmpty()) {
          c.setStatusMessage("No Terminologies available");
          c.setStatus(Status.IMPAIRED);
        } else {
          c.setStatusMessage("Active vocabularies : " + terminologies.get().stream()
              .map(ResourceIdentifier::getTag).collect(Collectors.joining(","))
          );
          c.setStatus(Status.UP);
        }
      }
      c.setDetails(mp);
      return c;
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
