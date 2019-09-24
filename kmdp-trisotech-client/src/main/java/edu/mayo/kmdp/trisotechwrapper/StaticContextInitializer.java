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
 * This is generally considered bad practice.
 * TODO: Review how TrisotechWrapper is used and decide if this is OK CAO
 */
@Component
public class StaticContextInitializer {
  private static final Logger logger = LoggerFactory.getLogger(StaticContextInitializer.class);

  @Value("${edu.mayo.kmdp.trisotechwrapper.trisotechToken}")
  private String token;

  @Value("${edu.mayo.kmdp.trisotechwrapper.repositoryName}")
  private String repositoryName;

  @PostConstruct
  public void init() {
    if(logger.isDebugEnabled()) {
      logger.debug("\n\n****token in PostConstruct is " + token);
      logger.debug("repositoryName in PostConstruct is: " + repositoryName + "*****\n\n");
    }
    TrisotechWrapper.setToken(token);
    TrisotechWrapper.setRoot(repositoryName);
  }
}
