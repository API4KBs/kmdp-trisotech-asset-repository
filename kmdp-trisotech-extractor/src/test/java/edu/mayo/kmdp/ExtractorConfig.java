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

import edu.mayo.kmdp.preprocess.meta.MetadataExtractor;
import edu.mayo.kmdp.preprocess.meta.ModelReader;
import edu.mayo.kmdp.preprocess.meta.ReaderConfig;
import edu.mayo.kmdp.preprocess.meta.TrisotechExtractionStrategy;
import edu.mayo.kmdp.preprocess.meta.Weaver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExtractorConfig {

  @Bean
  ModelReader reader(ReaderConfig config) {
    System.out.println("reader in extractorConfig");
    return new ModelReader(config);
  }

  @Bean
  ReaderConfig config() {
    System.out.println("config in extractorConfig");
    return new ReaderConfig();
  }

  @Bean
  Weaver weaver() {
    System.out.println("weaver in extractorConfig");
    return new Weaver();
  }

  @Bean
  TrisotechExtractionStrategy strategy() {
    System.out.println("strategy in extractorConfig");
    return new TrisotechExtractionStrategy();
  }

  @Bean
  MetadataExtractor extractor() {
    System.out.println("in extractorConfig");
    return new MetadataExtractor();
  }
}
