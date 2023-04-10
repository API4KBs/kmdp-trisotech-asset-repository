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
package edu.mayo.kmdp.kdcaci.knew.trisotech;

import edu.mayo.kmdp.kdcaci.knew.trisotech.components.HTMLAdapter;
import edu.mayo.kmdp.kdcaci.knew.trisotech.components.TTRepoContextAwareHrefBuilder;
import edu.mayo.kmdp.kdcaci.knew.trisotech.components.TTServerContextAwareHrefBuilder;
import edu.mayo.kmdp.trisotechwrapper.TTAPIAdapter;
import edu.mayo.kmdp.trisotechwrapper.TTWrapper;
import edu.mayo.kmdp.trisotechwrapper.components.redactors.TTRedactor;
import edu.mayo.kmdp.trisotechwrapper.components.weavers.DomainSemanticsWeaver;
import edu.mayo.kmdp.trisotechwrapper.config.TTWEnvironmentConfiguration;
import edu.mayo.kmdp.util.ws.ContentNegotiationFilter;
import edu.mayo.kmdp.util.ws.PointerHTMLAdapter;
import java.util.List;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.HttpMessageConverter;

/**
 * Primary Spring Configuration that ensures all necessary Components are loaded in the TTW.
 * <p>
 * Adapts any class that is not natively configured as Bean/Component
 */
@Configuration
@ComponentScan(basePackageClasses = {TrisotechAssetRepository.class, TTAPIAdapter.class})
@PropertySource(value = {"classpath:application.properties"})
public class TTServerConfig {

  /**
   * The core provider of business logic for the Asset and Artifact repository servers
   *
   * @param cfg the environment configuration
   * @return a servlet context-aware {@link TTServerContextAwareHrefBuilder}
   */
  @Bean
  public TTAPIAdapter ttAdapter(@Autowired TTWEnvironmentConfiguration cfg) {
    return new TTWrapper(cfg, new DomainSemanticsWeaver(cfg), new TTRedactor());
  }

  /**
   * Negotation filter consolidates Accept with X-Accept headers
   *
   * @return a {@link ContentNegotiationFilter}
   */
  @Bean
  @Order(1)
  public ContentNegotiationFilter negotiationFilter() {
    return new ContentNegotiationFilter();
  }

  /**
   * HrefBuilder use to map Asset Repository API endpoints
   *
   * @param cfg the environment configuration
   * @return a servlet context-aware {@link TTServerContextAwareHrefBuilder}
   */
  @Bean
  public TTServerContextAwareHrefBuilder hrefBuilder(@Autowired TTWEnvironmentConfiguration cfg) {
    return new TTServerContextAwareHrefBuilder(cfg);
  }

  /**
   * HrefBuilder use to map Artifact Repository API endpoints
   *
   * @param cfg the environment configuration
   * @return a servlet context-aware {@link TTRepoContextAwareHrefBuilder}
   */
  @Bean
  public TTRepoContextAwareHrefBuilder artfHrefBuilder(@Autowired TTWEnvironmentConfiguration cfg) {
    return new TTRepoContextAwareHrefBuilder(cfg);
  }

  /**
   * Adapter to unwrap Artifacts/Surrogates in HTML format
   *
   * @return a {@link HTMLAdapter}
   */
  @Bean
  HttpMessageConverter<KnowledgeCarrier> knowledgeCarrierToHTMLAdapter() {
    return new HTMLAdapter();
  }

  /**
   * Adapter to display 'catalog' pages (Lists of Pointers) as HTML, when the client is a user agent
   * that accepts text/html, e.g. a browser
   *
   * @return a {@link PointerHTMLAdapter}
   */
  @Bean
  HttpMessageConverter<List<Pointer>> pointerToHTMLAdapter() {
    return new PointerHTMLAdapter();
  }

}
