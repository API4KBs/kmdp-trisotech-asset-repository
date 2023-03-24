/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package edu.mayo.kmdp.trisotechwrapper.config;

import static edu.mayo.kmdp.trisotechwrapper.config.TrisotechApiUrls.apiEndpoint;

import edu.mayo.kmdp.ConfigProperties;
import edu.mayo.kmdp.util.PropertiesUtil;
import java.util.Optional;
import java.util.Properties;
import javax.annotation.PostConstruct;
import org.owasp.encoder.Encode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * This class is used to set the static context in the TrisotechWrapper.
 */
@Component
public class TTWEnvironmentConfiguration extends
    ConfigProperties<TTWEnvironmentConfiguration, TTWParams> {

  private static final Logger logger = LoggerFactory.getLogger(TTWEnvironmentConfiguration.class);

  private static final Properties DEFAULTS = defaulted(TTWParams.class);

  @Autowired
  private Environment env;

  public TTWEnvironmentConfiguration() {
    super(DEFAULTS);
  }

  public TTWEnvironmentConfiguration(Properties defaults) {
    super(defaults);
  }

  @Override
  public TTWParams[] properties() {
    return TTWParams.values();
  }

  @Override
  public String encode() {
    return PropertiesUtil.serializeProps(this);
  }


  @PostConstruct
  public TTWEnvironmentConfiguration init() {
    scanEnvironment();

    ensureVariablesSet();

    if (get(TTWParams.API_TOKEN).isEmpty()) {
      logger.warn("No bearer token detected - Unable to connect to the TT DES");
    }
    if (get(TTWParams.REPOSITORY_ID).isEmpty() || get(TTWParams.REPOSITORY_NAME).isEmpty()) {
      logger.warn("No target Place/Repository configuration detected "
          + "- Unable to retrieve models");
    }

    return this;
  }

  /**
   * Infers any derived configuration variable value
   *
   * Sets the TT DES public API endpoint, given the base URL
   */
  private void ensureVariablesSet() {
    Optional<String> baseURL = tryGetTyped(TTWParams.BASE_URL);
    Optional<String> apiEndpoint = tryGetTyped(TTWParams.API_ENDPOINT);
    if (apiEndpoint.isEmpty() && baseURL.isPresent()) {
      setTyped(TTWParams.API_ENDPOINT, apiEndpoint(baseURL.get()));
    }
  }

  /**
   * Acquires the configuration values set in the environment
   */
  private void scanEnvironment() {
    for (var param : TTWParams.values()) {
      var sysValue = sanitize(env.getProperty(param.getName()));
      if (sysValue != null) {
        this.setTyped(param, sysValue);
        if (logger.isInfoEnabled()) {
          logger.info("Configuration param {} detected - using value {}",
              param,
              print(param, sysValue));
        }
      }
    }
  }

  /**
   * Ensures the configuration values do not carry attack vectors
   *
   * @param varValue the candiadte config value, as provided by the environment
   * @return a sanitized value
   */
  private String sanitize(String varValue) {
    return varValue != null
        ? Encode.forJava(varValue)
        : null;
  }

  /**
   * Formats a configuration variable value for printing/logging
   * <p>
   * Obfuscates the secrets
   *
   * @param param the config variable
   * @param sysValue the config variable value
   * @return the value, in a form suitable for printing
   */
  private String print(TTWParams param, String sysValue) {
    if (param == TTWParams.API_TOKEN) {
      return sysValue.substring(0, 10);
    }
    return sysValue;
  }

}
