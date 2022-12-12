package edu.mayo.kmdp.kdcaci.knew.trisotech.components;

import java.util.Properties;
import org.omg.spec.api4kp._20200801.services.repository.asset.KARSHrefBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

public class TTServerContextAwareHrefBuilder extends KARSHrefBuilder {

  private static final Logger logger = LoggerFactory.getLogger(
      TTServerContextAwareHrefBuilder.class);

  public TTServerContextAwareHrefBuilder(Properties cfg) {
    super(cfg);
  }

  @Override
  protected String getHost() {
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
