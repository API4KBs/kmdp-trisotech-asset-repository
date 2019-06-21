/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
@JsonIgnoreProperties( ignoreUnknown = true )
@Data
public class Datum {

  public TrisotechFileInfo file;
  public TrisotechFolderInfo folder;

}
