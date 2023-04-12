package edu.mayo.kmdp.trisotechwrapper.components.hooks;

import java.util.Arrays;
import java.util.Objects;

/**
 * Enumeration of supported TT Event Types
 */
public enum SupportedHooks {
  GRAPH_INDEXED("GraphModelIndexed"),
  REPOSITORY_MODEL_WRITE("RepositoryModelWrite"),
  REPOSITORY_MODEL_DELETE("RepositoryModelDelete"),
  UNSUPPORTED("NA");

  private final String eventCode;

  SupportedHooks(String type) {
    this.eventCode = type;
  }

  public String getEventCode() {
    return eventCode;
  }

  public static SupportedHooks decode(String type) {
    return Arrays.stream(values())
        .filter(ev -> Objects.equals(type, ev.getEventCode()))
        .findFirst()
        .orElse(UNSUPPORTED);
  }

}