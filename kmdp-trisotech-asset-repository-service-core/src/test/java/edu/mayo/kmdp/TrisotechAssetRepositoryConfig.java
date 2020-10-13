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

import edu.mayo.kmdp.kdcaci.knew.trisotech.TrisotechAssetRepository;
import edu.mayo.kmdp.kdcaci.knew.trisotech.preprocess.IdentityMapper;
import edu.mayo.kmdp.kdcaci.knew.trisotech.preprocess.MetadataExtractor;
import edu.mayo.kmdp.kdcaci.knew.trisotech.preprocess.ModelReader;
import edu.mayo.kmdp.kdcaci.knew.trisotech.preprocess.ReaderConfig;
import edu.mayo.kmdp.kdcaci.knew.trisotech.preprocess.TrisotechExtractionStrategy;
import edu.mayo.kmdp.kdcaci.knew.trisotech.preprocess.Weaver;
import edu.mayo.kmdp.terms.TermsProvider;
import edu.mayo.kmdp.trisotechwrapper.StaticContextInitializer;
import edu.mayo.kmdp.trisotechwrapper.TrisotechWrapper;
import org.omg.spec.api4kp._20200801.api.terminology.v4.server.TermsApiInternal;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TrisotechAssetRepositoryConfig {

  @Bean StaticContextInitializer cfg() {
    return new StaticContextInitializer();
  }

  @Bean
  TrisotechWrapper trisotechWrapper(StaticContextInitializer cfg) {
    return new TrisotechWrapper(cfg);
  }

  @Bean
  TrisotechAssetRepository trisotechAssetRepository() {
    return new TrisotechAssetRepository();
  }

  @Bean
  StaticContextInitializer staticContextInitializer() {
    return new StaticContextInitializer();
  }

  @Bean
  IdentityMapper identityMapper() {
    return new IdentityMapper();
  }

  @Bean
  ModelReader modelReader(ReaderConfig readerConfig) {
    return new ModelReader(readerConfig);
  }

  @Bean
  ReaderConfig readerConfig() {
    return new ReaderConfig();
  }

  @Bean
  Weaver weaver() {
    return new Weaver();
  }

  @Bean
  TermsApiInternal terms() {
    return new TermsProvider();
  }

  @Bean
  TrisotechExtractionStrategy trisotechExtractionStrategy() {
    return new TrisotechExtractionStrategy();
  }

  @Bean
  MetadataExtractor extractor() {
    return new MetadataExtractor();
  }

}
