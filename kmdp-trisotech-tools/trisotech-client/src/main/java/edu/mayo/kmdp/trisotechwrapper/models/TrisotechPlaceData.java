package edu.mayo.kmdp.trisotechwrapper.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * data element that holds an array of Places (repositories) from the Trisotech server
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class TrisotechPlaceData {
  private List<TrisotechPlace> data;
}
