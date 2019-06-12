package edu.mayo.kmdp.trisotechwrapper.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 'places' of the Trisotech repository
 * This is information about a specific 'place' (repository) the user has access to.
 *
 * @JsonIgnoreProperties will ignore any property not configured here
 * @Data will generate getters and setters
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class TrisotechPlace {
  private String id;
  private String name;
  private String type;
}
