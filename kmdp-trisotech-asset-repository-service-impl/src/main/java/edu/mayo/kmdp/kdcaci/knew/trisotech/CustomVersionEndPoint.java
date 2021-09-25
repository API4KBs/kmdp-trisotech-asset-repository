package edu.mayo.kmdp.kdcaci.knew.trisotech;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.*;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;

@Component
@WebEndpoint(id = "version")
public class CustomVersionEndPoint {

  @Autowired
  BuildProperties buildProperties;

  @ReadOperation
  public String version()  {

    // Artifact version
    return buildProperties.getVersion();
  }

}
