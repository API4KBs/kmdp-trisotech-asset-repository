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

import edu.mayo.kmdp.util.Util;
import java.util.Optional;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * This class is used to set the static context in the TrisotechWrapper.
 * @Value cannot be used with static values.
 * @Value is needed to set the token and repository needed in TrisotechWrapper so those values
 * (especially the token) are NOT in the codebase.
 */
@Component
public class TTWEnvironmentConfiguration {

  private static final Logger logger = LoggerFactory.getLogger(TTWEnvironmentConfiguration.class);

  @Value("${edu.mayo.kmdp.trisotechwrapper.baseUrl:}")
  private String baseURL;

  @Value("${edu.mayo.kmdp.trisotechwrapper.trisotechToken:}")
  private String token;

  @Value("${edu.mayo.kmdp.trisotechwrapper.repositoryName:}")
  private String repositoryName;

  @Value("${edu.mayo.kmdp.trisotechwrapper.repositoryPath:/}")
  private String repositoryPath;

  @Value("${edu.mayo.kmdp.trisotechwrapper.repositoryId:}")
  private String repositoryId;

  @Value("${edu.mayo.kmdp.trisotechwrapper.expiration:1440}")
  private String cacheExpiration;

  @Value("${edu.mayo.kmdp.trisotechwrapper.namespace.public:https://clinicalknowledgemanagement.mayo.edu/artifacts}")
  private String publicNamespace;

  private String apiEndpoint;

  @PostConstruct
  public void init() {
    apiEndpoint = Util.isNotEmpty(baseURL)
        ? baseURL + "/publicapi/"
        : null;

    if (logger.isDebugEnabled()) {
      logger.debug("\n\n****token in PostConstruct is {} ", token);
      logger.debug("repositoryName in PostConstruct is: {}", repositoryName);
      logger.debug("baseUrl in PostConstruct is: {}", baseURL);
      logger.debug("apiEndpoint in PostConstruct is {}", apiEndpoint + "*****\n\n");
    }
    if (Util.isEmpty(token)) {
      logger.warn("No bearer token detected - Unable to connect to the TT DES");
    }
    if (Util.isEmpty(repositoryName) || Util.isEmpty(repositoryId)) {
      logger.warn("No target Place/Repository configuration detected "
          + "- Unable to retrieve models");
    }

    if (token.startsWith("edu.mayo") && token.contains("=")) {
      this.token = token.substring(token.indexOf('=') + 1);
    }
  }

  public String getBaseURL() {
    return baseURL;
  }

  public Optional<String> getApiEndpoint() {
    return Optional.ofNullable(apiEndpoint);
  }

  public String getToken() {
    return token;
  }

  public String getRepositoryName() {
    return repositoryName;
  }

  public String getCacheExpiration() {
    return cacheExpiration;
  }

  public String getRepositoryId() {
    return repositoryId;
  }

  public String getPath() {
    return repositoryPath;
  }

  public String getPublicNamespace() {
    return publicNamespace;
  }
}
