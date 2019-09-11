package edu.mayo.kmdp;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChainConverterConfig {

  @Bean
  ChainConverter chainConverter() {
    return new ChainConverter();
  }
}
