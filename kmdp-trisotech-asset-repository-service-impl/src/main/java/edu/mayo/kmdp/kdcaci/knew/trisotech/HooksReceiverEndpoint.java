package edu.mayo.kmdp.kdcaci.knew.trisotech;

import edu.mayo.kmdp.trisotechwrapper.TTAPIAdapter;
import edu.mayo.kmdp.trisotechwrapper.components.hooks.DefaultTTHooksHandler;
import edu.mayo.kmdp.trisotechwrapper.components.hooks.TTHooksHandler;
import io.cloudevents.CloudEvent;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller
 * <p>
 * Exposes an endpoint where WebHooks in the shape of a Cloud Event can be POSTed
 */
@RestController
public class HooksReceiverEndpoint {

  /**
   * The TTW internal, to relay the events to
   */
  @Nonnull
  private final TTAPIAdapter ttw;
  /**
   * The Hooks handler, optionally
   */
  @Nullable
  private final TTHooksHandler handler;

  /**
   * Constructor
   *
   * @param ttw     the {@link TTAPIAdapter}
   * @param handler the optional {@link DefaultTTHooksHandler}
   */
  public HooksReceiverEndpoint(
      @Autowired @Nonnull final TTAPIAdapter ttw,
      @Autowired(required = false) @Nullable final TTHooksHandler handler) {
    this.handler = handler;
    this.ttw = ttw;
  }

  /**
   * Handles an event, letting the handler provide the desired callbacks to the
   * {@link TTAPIAdapter}
   *
   * @param event the Cloud Event delivered via WebHook
   */
  @PostMapping(value = "/hookListener", consumes = "application/cloudevents+json")
  public void handleEvent(@RequestBody CloudEvent event) {
    if (handler != null) {
      handler.handleEvent(event, ttw);
    }
  }


}
