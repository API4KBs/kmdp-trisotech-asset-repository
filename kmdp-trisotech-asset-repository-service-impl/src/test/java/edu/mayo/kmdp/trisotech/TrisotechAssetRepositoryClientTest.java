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
package edu.mayo.kmdp.trisotech;


import edu.mayo.kmdp.kdcaci.knew.trisotech.TTServerConfig;
import edu.mayo.kmdp.util.ws.JsonRestWSUtils.WithFHIR;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.KnowledgeAssetCatalogApi;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.client.ApiClientFactory;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.server.Swagger2SpringBoot;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    classes = Swagger2SpringBoot.class)
@ContextConfiguration(classes = TTServerConfig.class)
@TestPropertySource(properties = {
    "edu.mayo.kmdp.trisotechwrapper.repositoryName=EU-Rent",
    "edu.mayo.kmdp.trisotechwrapper.repositoryId=9b6b13d5-00e5-42fe-a844-51a1a4c78106"})
class TrisotechAssetRepositoryClientTest {

  @LocalServerPort
  int port;

  // confirm the client starts
  @Test
  void contextLoad() {
    ApiClientFactory client = new ApiClientFactory("http://localhost:" + port, WithFHIR.NONE);
    KnowledgeAssetCatalogApi tcat = KnowledgeAssetCatalogApi.newInstance(client);

    tcat.listKnowledgeAssets()
        .map(List::size)
        .orElseGet(Assertions::fail);

  }

}
