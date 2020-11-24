/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.mayo.kmdp.trisotechwrapper;

import edu.mayo.kmdp.util.Util;
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
public class StaticContextInitializer {
  private static final Logger logger = LoggerFactory.getLogger(StaticContextInitializer.class);

  @Value("${edu.mayo.kmdp.trisotechwrapper.baseUrl:https://test-mc.trisotech.com/publicapi/}")
  private String baseURL;

  @Value("${edu.mayo.kmdp.trisotechwrapper.test.trisotechToken}")
  private String token;

  @Value("${edu.mayo.kmdp.trisotechwrapper.repositoryName:}")
  private String repositoryName;

  @Value("${edu.mayo.kmdp.trisotechwrapper.repositoryPath:/}")
  private String repositoryPath;

  @Value("${edu.mayo.kmdp.trisotechwrapper.repositoryId:}")
  private String repositoryId;

  @PostConstruct
  public void init() {
    if(logger.isDebugEnabled()) {
      logger.debug("\n\n****token in PostConstruct is {} ", token);
      logger.debug("repositoryName in PostConstruct is: {}", repositoryName);
      logger.debug("baseUrl in PostConstruct is: {}", baseURL + "*****\n\n");
    }

    if (Util.isEmpty(token)) {
      throw new IllegalStateException("No bearer token detected - Unable to connect to the TT DES");
    }
    if (Util.isEmpty(repositoryName) || Util.isEmpty(repositoryId)) {
      throw new IllegalStateException("No target Place/Repository configuration detected "
          + "- Unable to retrieve models");
    }

    if (token.startsWith("edu.mayo") && token.contains("=")) {
      this.token = token.substring(token.indexOf('=') + 1);
    }
  }

  public String getBaseURL() {
    return baseURL;
  }

  public String getToken() {
    return token;
  }

  public String getRepositoryName() {
    return repositoryName;
  }

  public String getRepositoryId() {
    return repositoryId;
  }

  public String getPath() {
    return repositoryPath;
  }
}
