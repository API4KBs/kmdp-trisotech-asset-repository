package edu.mayo.kmdp.kdcaci.knew.trisotech.components;

import java.util.Properties;
import org.omg.spec.api4kp._20200801.services.repository.artifact.KArtfHrefBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * {@link KArtfHrefBuilder} that uses the Spring context to determine the base URL, so that
 * URI-based links can be mapped to URLs bound to the server deployment.
 * <p>
 * This implementation is best-effort: it relies on a {@link ServletUriComponentsBuilder}
 *
 * @see ServletUriComponentsBuilder
 */
public class TTRepoContextAwareHrefBuilder extends KArtfHrefBuilder {

  /**
   * Logger
   */
  private static final Logger logger = LoggerFactory.getLogger(
      TTRepoContextAwareHrefBuilder.class);

  /**
   * Constructor
   *
   * @param cfg the environment configuration
   */
  public TTRepoContextAwareHrefBuilder(Properties cfg) {
    super(cfg);
  }

  /**
   * Given an API call being processed by a server's handlers, the handling code can invoke this
   * method to determine the base URL of the server.
   *
   * @return the base URL of the Host (server) processing a request
   */
  @Override
  public String getHost() {
    try {
      return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
    } catch (Exception e) {
      logger.info(e.getMessage());
      return "";
    }
  }

  /**
   * Given an API call being processed by a server's handlers, the handling code can invoke this
   * method to determine the URL of the endpoint where the request was submitted
   *
   * @return the base URL of the Host (server) processing a request
   */
  @Override
  public String getCurrentURL() {
    return ServletUriComponentsBuilder.fromCurrentRequest().build().toUriString();
  }
}
