package edu.mayo.kmdp.kdcaci.knew.trisotech.components;

import java.util.Properties;
import org.omg.spec.api4kp._20200801.services.repository.artifact.KArtfHrefBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * {@link KArtfHrefBuilder} that uses the Spring context to determine the base URL, so that URI-based
 * links can be mapped to URLs bound to the server deployment
 */
public class TTRepoContextAwareHrefBuilder extends KArtfHrefBuilder {

  private static final Logger logger = LoggerFactory.getLogger(
      TTRepoContextAwareHrefBuilder.class);

  public TTRepoContextAwareHrefBuilder(Properties cfg) {
    super(cfg);
  }

  @Override
  public String getHost() {
    try {
      return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
    } catch (Exception e) {
      logger.info(e.getMessage());
      return "";
    }
  }

  @Override
  public String getCurrentURL() {
    return ServletUriComponentsBuilder.fromCurrentRequest().build().toUriString();
  }
}
