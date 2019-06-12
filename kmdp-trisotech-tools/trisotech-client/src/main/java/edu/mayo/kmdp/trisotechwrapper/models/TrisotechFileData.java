package edu.mayo.kmdp.trisotechwrapper.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * data element holding an array of Trisotech files and folders within a repository
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class TrisotechFileData {
  private List<Datum> data;
}
