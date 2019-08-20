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
package edu.mayo.kmdp.preprocess.meta;

import java.util.regex.Pattern;

public class ModelReader implements TrisotechReader {

  protected Pattern urlPattern;

  protected ReaderConfig        config;

  public ModelReader(ReaderConfig config) {
    this.config = config;

    this.urlPattern = Pattern.compile( config.getTyped( ReaderOptions.URL_PATTERN_ST ) );
  }

  public Pattern getURLPattern() {
    return urlPattern;
  }


//	protected ConceptIdentifier toConceptIdentifier( String uuid, String label, URI ref, String catUUID, String catName ) {
//		return KnownValuesets.resolve( catName, uuid, label, ref );
//	}

}
