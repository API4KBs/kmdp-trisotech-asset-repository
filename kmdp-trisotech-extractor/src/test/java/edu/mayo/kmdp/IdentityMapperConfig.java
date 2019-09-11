package edu.mayo.kmdp;

import edu.mayo.kmdp.preprocess.meta.IdentityMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IdentityMapperConfig {
  @Bean
  IdentityMapper identityMapper() {
    System.out.println("in identityMapper for IdentityMapperConfig...");
    return new IdentityMapper();
  }
}
