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
 * 'file' of the trisotech repository data entry
 *
 * @JsonIgnoreProperties will ignore any property not configured here
 * @Data will generate getters and setters
 */
@JsonIgnoreProperties( ignoreUnknown = true )
@Data
public class TrisotechFileInfo {
    private String id;
    private String sku;
    private String name;
    private String path;
    private String mimetype;
    private String updated; // Date format: "yyyy-MM-dd'T'HH:mm:ssZ"	2019-05-02T20:03:29Z
    private String updater;
    private String url;
    private String version;
    private String state;

	public String toString() {
		return name;
	}
}
