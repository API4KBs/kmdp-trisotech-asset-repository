/**
 * Copyright © 2019 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
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

// TODO: Needed?
//import edu.mayo.kmdp.trisotechwrapper.dictionary.TrisotechDictionaryEntry;
// Formerly PCO, but not finding use of PCO here anyway
//import edu.mayo.ontology.taxonomies.clinicalsituations.ClinicalSituation;

import java.util.regex.Pattern;

public class ModelReader implements TrisotechReader {

  protected Pattern URL_PATTERN;

  protected ReaderConfig        config;

  public ModelReader(ReaderConfig config) {
    this.config = config;

    this.URL_PATTERN = Pattern.compile( config.getTyped( ReaderOptions.URL_PATTERN_ST ) );
  }

  public Pattern getURLPattern() {
    return URL_PATTERN;
  }


//	protected ConceptIdentifier toConceptIdentifier( String uuid, String label, URI ref, String catUUID, String catName ) {
//		return KnownValuesets.resolve( catName, uuid, label, ref );
//	}

}
