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
import javax.annotation.Nonnull;
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
 * In addition to the default implementation provided by {@link HealthEndPoint}, monitors the
 * following internal components:
 *
 * <ul>
 *   <li>the {@link TTAPIAdapter}, as a proxy for the TT DES server API</li>
 *   <li>the {@link CachingTTWKnowledgeStore}'s Place Cache, as a proxy for the TT DES Knowledge Graph</li>>
 *   <li>the {@link CachingTTWKnowledgeStore}'s Model Cache, as a proxy for the TT DES Model Repository</li>>
 * </ul>
 */
@ComponentScan(basePackageClasses = {
    TTAPIAdapter.class,
    TTMonitoringConfig.class,
    HealthEndPoint.class,
    StateEndPoint.class,
    VersionEndPoint.class})
@Configuration
@PropertySource(value = {"classpath:application.properties"})
public class TTMonitoringConfig {

  @Value("${edu.mayo.kmdp.trisotechwrapper.repositoryId:nil}")
  String mainPlaceId;

  /**
   * Creates a representation of the TT DES server (APIs) as a health-monitored
   * {@link ApplicationComponent}
   *
   * @param env    the system Environment
   * @param client the DES server client
   * @return the DES Server health status, as an {@link ApplicationComponent}
   */
  @Bean
  Supplier<ApplicationComponent> ttServer(
      @Autowired @Nonnull final ConfigurableEnvironment env,
      @Autowired @Nonnull final TTAPIAdapter client) {
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

  /**
   * Creates a representation of the TTW Place Cache, as a health-monitored
   * {@link ApplicationComponent}
   *
   * @param client the DES server client
   * @return the TTW Place Cache health status, as an {@link ApplicationComponent}
   */
  @Bean
  Supplier<ApplicationComponent> placeCache(
      @Autowired @Nonnull final TTAPIAdapter client) {
    return () -> cacheComponent(client, "Place Cache",
        TTAPIAdapter::getPlaceCache);
  }

  /**
   * Creates a representation of the TTW Models Cache, as a health-monitored
   * {@link ApplicationComponent}
   *
   * @param client the DES server client
   * @return the TTW Models Cache health status, as an {@link ApplicationComponent}
   */
  @Bean
  Supplier<ApplicationComponent> modelsCache(
      @Autowired @Nonnull final TTAPIAdapter client) {
    return () -> cacheComponent(client, "Models Cache",
        TTAPIAdapter::getModelCache);
  }


  /**
   * Determines the health status of the TT DES server, updating its {@link ApplicationComponent}
   *
   * @param client the DES client
   * @param c      the DES client health status descriptor, to be updated
   */
  private void diagnoseTTAdapter(
      @Nonnull final TTAPIAdapter client,
      @Nonnull final ApplicationComponent c) {
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

  /**
   * Determines the health status of a TTW Cache, updating its {@link ApplicationComponent}
   *
   * @param client the DES client
   * @param name   the name of the Cache component
   * @param mapper the selector to choose the desired cache from the client
   */
  private ApplicationComponent cacheComponent(
      TTAPIAdapter client,
      String name,
      Function<TTAPIAdapter, Cache<?, ?>> mapper) {
    ApplicationComponent c = new ApplicationComponent();
    c.setName(name);

    return describeCache(mapper.apply(client), c);
  }

  /**
   * Decorates a {@link Cache}'s diagnostic {@link ApplicationComponent} with descriptors of that
   * Cache's health status
   *
   * @param cache the Cache to be described
   * @param c     the {@link ApplicationComponent} descriptor
   * @return c, with added status and properties
   */
  private ApplicationComponent describeCache(Cache<?, ?> cache, ApplicationComponent c) {
    MiscProperties mp = new MiscProperties();
    c.status(Status.UP);
    c.setStatusMessage("Present");
    mp.put("estimatedSize", Long.toString(cache.estimatedSize()));
    mp.put("stats", cache.stats().toString());
    c.setDetails(mp);
    return c;
  }

}
