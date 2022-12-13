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
package edu.mayo.kmdp.kdcaci.knew.trisotech;

import edu.mayo.kmdp.kdcaci.knew.trisotech.TTAssetRepositoryConfig.TTWParams;
import edu.mayo.kmdp.kdcaci.knew.trisotech.components.HTMLAdapter;
import edu.mayo.kmdp.kdcaci.knew.trisotech.components.TTServerContextAwareHrefBuilder;
import edu.mayo.kmdp.terms.TermsProvider;
import edu.mayo.kmdp.trisotechwrapper.TrisotechWrapper;
import edu.mayo.kmdp.util.ws.ContentNegotiationFilter;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.HttpMessageConverter;

@Configuration
@ComponentScan(basePackageClasses = {TrisotechAssetRepository.class, TermsProvider.class, TrisotechWrapper.class})
@PropertySource(value={"classpath:application.properties"})
public class TTServerConfig {

  @Value("${edu.mayo.kmdp.application.flag.publishedOnly:false}")
  private String publishedOnly;

  @Value("${edu.mayo.kmdp.application.flag.assetsOnly:true}")
  private String assetsOnly;

  @Bean
  public TTAssetRepositoryConfig config() {
    return new TTAssetRepositoryConfig()
        .with(TTWParams.PUBLISHED_ONLY, Boolean.valueOf(publishedOnly))
        .with(TTWParams.ASSETS_ONLY, Boolean.valueOf(assetsOnly));
  }

  @Bean
  public TTServerContextAwareHrefBuilder hrefBuilder(@Autowired TTAssetRepositoryConfig cfg) {
    return new TTServerContextAwareHrefBuilder(cfg);
  }

  @Bean
  @Order(1)
  public ContentNegotiationFilter negotiationFilter() {
    return new ContentNegotiationFilter();
  }

  @Bean
  HttpMessageConverter<KnowledgeCarrier> knowledgeCarrierToHTMLAdapter() {
    return new HTMLAdapter();
  }

}