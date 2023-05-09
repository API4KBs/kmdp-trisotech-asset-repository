package edu.mayo.kmdp.trisotechwrapper.models;

import java.util.Arrays;

public enum TrisotechPublicationStates {

  PUBLISHED("Published"),
  PENDING_APPROVAL("Pending Approval"),
  DRAFT("Draft"),
  UNPUBLISHED("Unpublished");

  private final String state;

  TrisotechPublicationStates(String state) {
    this.state = state;
  }

  public static TrisotechPublicationStates parse(String status) {
    if (status == null) {
      return UNPUBLISHED;
    }
    return Arrays.stream(TrisotechPublicationStates.values())
        .filter(s -> s.state.equalsIgnoreCase(status.trim()))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unexpected publication status " + status));
  }
}
