package edu.mayo.kmdp.trisotechwrapper.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;


/**
 * Class for the different structures that can be returned from Trisotech repository.
 *  data is made up of "file" OR "folder"
 *  {
 *    "folder": {
 *    ... folder fields ...
 *     }
 *  },
 *  {
 *     "file": {
 *     ... file fields ...
 *     }
 *  },
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class Datum {

  public TrisotechFileInfo file;
  public TrisotechFolderInfo folder;

}
