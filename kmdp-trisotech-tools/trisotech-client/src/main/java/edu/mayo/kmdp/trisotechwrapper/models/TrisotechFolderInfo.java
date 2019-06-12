package edu.mayo.kmdp.trisotechwrapper.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 'folder' of the Trisotech repository data entry
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class TrisotechFolderInfo {

  public String id;
  public String sku;
  public String name;
  public String path;
}