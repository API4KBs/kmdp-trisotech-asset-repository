package edu.mayo.kmdp.kdcaci.knew.trisotech;

import static java.util.stream.Collectors.toSet;

import com.github.benmanes.caffeine.cache.Cache;
import edu.mayo.kmdp.health.HealthEndPoint;
import edu.mayo.kmdp.health.StateEndPoint;
import edu.mayo.kmdp.health.VersionEndPoint;
import edu.mayo.kmdp.health.datatype.ApplicationComponent;
import edu.mayo.kmdp.health.datatype.MiscProperties;
import edu.mayo.kmdp.health.datatype.Status;
import edu.mayo.kmdp.health.utils.MonitorUtil;
import edu.mayo.kmdp.trisotechwrapper.TTAPIAdapter;
import edu.mayo.kmdp.trisotechwrapper.components.cache.CachingTTWKnowledgeStore;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.ConfigurableEnvironment;


/**
 * Spring Configuration class for the TTW monitorable components.
 * <p>
 * Health-monitorable components include the DES server, as well as the internal caches
 */
@ComponentScan(basePackageClasses = {
    TTAPIAdapter.class,
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
      @Autowired TTAPIAdapter client) {
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

      diagnoseTTAdapter(client, c);
      c.setDetails(mp);
      return c;
    };
  }

  @Bean
  Supplier<ApplicationComponent> placeCache(
      @Autowired TTAPIAdapter client) {
    return () -> cacheComponent(client, "Place Cache", CachingTTWKnowledgeStore::getPlaceCache);
  }

  @Bean
  Supplier<ApplicationComponent> modelsCache(
      @Autowired TTAPIAdapter client) {
    return () -> cacheComponent(client, "Models Cache", CachingTTWKnowledgeStore::getModelCache);
  }


  private void diagnoseTTAdapter(TTAPIAdapter client, ApplicationComponent c) {
    var accessible = client.listAccessiblePlaces().values();
    var configured = client.getCacheablePlaces().values();
    var usable = configured.stream()
        .filter(accessible::contains)
        .collect(toSet());

    if (usable.isEmpty()) {
      c.setStatusMessage("Unable to access configured Place(s)");
      c.setStatus(Status.DOWN);
    } else if (usable.size() < configured.size()) {
      c.setStatusMessage("Limited Place access: " +
          usable.stream().map(Object::toString).collect(Collectors.joining()));
      c.setStatus(Status.IMPAIRED);
    } else {
      c.setStatusMessage("Pulling from Places: " +
          usable.stream().map(Object::toString).collect(Collectors.joining()));
      c.status(Status.UP);
    }
  }

  private ApplicationComponent cacheComponent(
      TTAPIAdapter client,
      String name,
      Function<CachingTTWKnowledgeStore, Cache<?, ?>> mapper) {
    ApplicationComponent c = new ApplicationComponent();
    c.setName(name);
    c.status(Status.UP);
    var cacheMgr = client.getCacheManager();
    c.setStatusMessage(cacheMgr.isPresent() ? "Present" : "Absent");

    MiscProperties mp = new MiscProperties();
    if (cacheMgr.isPresent()) {
      var cache = mapper.apply(cacheMgr.get());
      mp.put("estimatedSize", Long.toString(cache.estimatedSize()));
      mp.put("stats", cache.stats().toString());
    }
    c.setDetails(mp);
    return c;
  }

}
