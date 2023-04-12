package edu.mayo.kmdp.trisotechwrapper.components.hooks;

import edu.mayo.kmdp.trisotechwrapper.TTAPIAdapter;
import edu.mayo.kmdp.trisotechwrapper.components.SemanticModelInfo;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfoEvent;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechPlace;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechPlaceEvent;
import edu.mayo.kmdp.util.JSonUtil;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.data.BytesCloudEventData;
import io.cloudevents.core.data.PojoCloudEventData;
import io.cloudevents.jackson.JsonCloudEventData;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link TTHooksHandler}
 */
public class DefaultTTHooksHandler implements TTHooksHandler {

  /**
   * Logger
   */
  private static final Logger logger = LoggerFactory.getLogger(DefaultTTHooksHandler.class);

  @Override
  @Nonnull
  public List<String> getSupportedEventTypes() {
    return Arrays.stream(SupportedHooks.values())
        .filter(ev -> ev != SupportedHooks.UNSUPPORTED)
        .map(SupportedHooks::getEventCode)
        .collect(Collectors.toList());
  }

  @Override
  public void handleEvent(
      @Nonnull final CloudEvent event,
      @Nonnull final TTAPIAdapter ttw) {
    switch (SupportedHooks.decode(event.getType())) {
      case GRAPH_INDEXED:
        handleGraphIndexed(event, ttw);
        return;
      case REPOSITORY_MODEL_WRITE:
      case REPOSITORY_MODEL_DELETE:
        handleModelUpdate(event, ttw);
        return;
      case UNSUPPORTED:
        logger.warn("Unable to handle Hook Event: {}", event);
    }
  }


  /**
   * Handles a model update event, invalidating the TTW Model Cache for that Model
   *
   * @param event the event
   * @param ttw   the {@link TTAPIAdapter} to delegate to
   */
  private void handleModelUpdate(
      @Nonnull final CloudEvent event,
      @Nonnull final TTAPIAdapter ttw) {
    logger.info("Model update : {}", event);

    var info = getData(event, TrisotechFileInfoEvent.class);
    info.filter(mf -> mf.getModel() != null)
        .map(mf -> new SemanticModelInfo(mf.getModel()))
        .ifPresent(mf -> ttw.getModelCache().invalidate(mf));
  }


  /**
   * Handles a graph update event, invalidating the TTW Place Cache for that Graph.
   * <p>
   * If the change is specific to a Model, the Model Cache for that model will be invalidated as
   * well
   *
   * @param event the event
   * @param ttw   the {@link TTAPIAdapter} to delegate to
   */
  private void handleGraphIndexed(
      @Nonnull final CloudEvent event,
      @Nonnull final TTAPIAdapter ttw) {
    logger.info("Graph update : {}", event);

    var info = getData(event, TrisotechPlaceEvent.class);
    info.filter(mf -> mf.getGraph() != null)
        .map(mf -> TrisotechPlace.key(mf.getGraph()))
        .ifPresent(place -> ttw.getPlaceCache().refresh(place));
    info.filter(mf -> mf.getModel() != null)
        .map(mf -> new SemanticModelInfo(mf.getModel()))
        .ifPresent(mf -> ttw.getModelCache().invalidate(mf));
  }


  /**
   * Parses a cloud event payload, across parsing levels
   *
   * @param event the event
   * @param klass the payload class to parse into
   * @param <T>   the payload class type
   * @return the payload of the event, as an instance of T, if able
   */
  @Nonnull
  private <T> Optional<T> getData(
      @Nonnull final CloudEvent event,
      @Nonnull final Class<T> klass) {
    var data = event.getData();
    if (data instanceof JsonCloudEventData) {
      return JSonUtil.parseJson(((JsonCloudEventData) data).getNode(), klass);
    } else if (data instanceof BytesCloudEventData) {
      return JSonUtil.readJson(data.toBytes(), klass);
    } else if (data instanceof PojoCloudEventData) {
      var paylooad = ((PojoCloudEventData<?>) data).getValue();
      return klass.isInstance(paylooad)
          ? Optional.of(klass.cast(paylooad))
          : Optional.empty();
    } else {
      return Optional.empty();
    }
  }
}
