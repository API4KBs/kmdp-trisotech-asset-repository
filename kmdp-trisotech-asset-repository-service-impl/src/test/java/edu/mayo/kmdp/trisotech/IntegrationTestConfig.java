package edu.mayo.kmdp.trisotech;


import edu.mayo.kmdp.kdcaci.knew.trisotech.TrisotechAssetRepository;
import edu.mayo.kmdp.trisotechwrapper.TrisotechWrapper;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = {TrisotechWrapper.class, TrisotechAssetRepository.class})
@EnableAutoConfiguration
public class IntegrationTestConfig {

  TrisotechWrapper tw;

}
