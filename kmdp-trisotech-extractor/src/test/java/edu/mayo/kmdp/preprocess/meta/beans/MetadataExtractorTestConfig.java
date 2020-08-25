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
package edu.mayo.kmdp.preprocess.meta.beans;

import edu.mayo.kmdp.preprocess.meta.IdentityMapper;
import edu.mayo.kmdp.preprocess.meta.MetadataExtractor;
import edu.mayo.kmdp.preprocess.meta.ModelReader;
import edu.mayo.kmdp.preprocess.meta.ReaderConfig;
import edu.mayo.kmdp.preprocess.meta.TrisotechExtractionStrategy;
import edu.mayo.kmdp.preprocess.meta.Weaver;
import edu.mayo.kmdp.preprocess.meta.beans.ChainConverter;
import edu.mayo.kmdp.trisotechwrapper.StaticContextInitializer;
import edu.mayo.kmdp.trisotechwrapper.TrisotechWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetadataExtractorTestConfig {

  @Bean
  public StaticContextInitializer cfg() {
    return new StaticContextInitializer();
  }

  @Bean
  public ChainConverter chainConverter() {
    return new ChainConverter();
  }

  @Bean
  public IdentityMapper identityMapper() {
    return new IdentityMapper();
  }

  @Bean
  @Autowired
  public TrisotechWrapper trisotechWrapper(StaticContextInitializer cfg) {
    return new TrisotechWrapper(cfg);
  }

  @Bean
  public ModelReader reader(ReaderConfig config) {
    return new ModelReader(config);
  }

  @Bean
  public ReaderConfig config() {
    return new ReaderConfig();
  }

  @Bean
  public Weaver weaver() {
    return new Weaver();
  }

  @Bean
  public TrisotechExtractionStrategy strategy() {
    return new TrisotechExtractionStrategy();
  }

  @Bean
  public MetadataExtractor extractor() {
    return new MetadataExtractor();
  }
}
