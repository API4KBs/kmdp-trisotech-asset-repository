package edu.mayo.kmdp.kdcaci.knew.trisotech.exception;

import edu.mayo.ontology.taxonomies.ws.responsecodes.ResponseCodeSeries;
import java.net.URI;
import org.omg.spec.api4kp._20200801.ServerSideException;

public class NotFoundException extends ServerSideException {

  public NotFoundException(
      String title,
      String detail,
      URI instance) {
    super(URI.create("https://www.omg.org/spec/API4KP/api4kp-kp/UnresolvedKnowledgeBase"),
        title, ResponseCodeSeries.NotFound, detail, instance);
  }
}