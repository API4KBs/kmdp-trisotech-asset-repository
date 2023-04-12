package edu.mayo.kmdp.trisotechwrapper.components.hooks;

import edu.mayo.kmdp.trisotechwrapper.TTAPIAdapter;
import io.cloudevents.CloudEvent;
import java.util.List;
import javax.annotation.Nonnull;

/**
 * Trisotech WebHook handler
 */
public interface TTHooksHandler {

  /**
   * @return the list of supported types, to match {@link CloudEvent#getType()}
   */
  @Nonnull
  List<String> getSupportedEventTypes();

  /**
   * Handles an event, calling the appropriate methods on {@link TTAPIAdapter}
   *
   * @param event the event
   * @param ttw   the delegate
   */
  void handleEvent(
      @Nonnull final CloudEvent event,
      @Nonnull final TTAPIAdapter ttw);
}
