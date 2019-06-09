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
package edu.mayo.kmdp;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TestConfig.class})
public abstract class IntegrationTestBase {

  private static ConfigurableApplicationContext ctx;

  // private ApiClient apiClient = new ApiClient().setBasePath( "http://localhost:11111" ); // set port in application.test.properties
  // private ServerApi server = ServerApi.newInstance(apiClient);


  @BeforeAll
  public static void setupServer() {
    SpringApplication app = null;
    //TODO: Use your Swagger2SpringBoot class here.
    //SpringApplication app = new SpringApplication(Swagger2SpringBoot.class);
    ctx = app.run();
  }

  @AfterAll
  public static void stopServer() {
    if (ctx != null) {
      ctx.close();
    }
  }
}
