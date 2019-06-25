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

import edu.mayo.kmdp.Opt;
import edu.mayo.kmdp.Option;

/**
 * Information about the Trisotech-specific values for parsing the file. In particular, those items that are
 * labeled with trisotech namespace, in order to modify to KMDP namespace.
 */
public enum ReaderOptions implements Option<ReaderOptions> {

	// TODO: reexamine required boolean values; currently set to 'false' until know more of what it does
	URL_PATTERN_ST( Opt.of( "URL_PATTERN_ST","\\\"(.*)\\\"", String.class, false ) ),

	// Dictionary Information
	// TODO: update to DMN 1.2 model -- need KRLanguage support first? https://www.omg.org/spec/DMN/20180505/DMN12.xsd
	// NOTE: OMG has uri as above, but Trisotech model has 20180521 in the path -- cannot find this value on OMG site
	// Trisotech model also does not call out DMN12.xsd file like Signavio model did
	// instead has links like this; http://www.omg.org/spec/DMN/20180521/MODEL/ which gives a 404 if I try to traverse to it
	// Davide explained this: OMG changed it so you have to go to the ref: https://www.omg.org/spec/DMN/1.2 to get the value for the schema.
	// http://www.omg.org/spec/DMN/20180521/MODEL is the schema
	// which value to use here?
	// Is MODEL_NS needed? CAO
	p_MODEL_NS( Opt.of( "MODEL_NS", "http://www.omg.org/spec/DMN/20151101/dmn.xsd", String.class, false ) ),
	p_EL_MODEL_EXT( Opt.of( "EL_MODEL_EXTENSIONS", "extensionElements", String.class, false ) ),

	p_METADATA_NS( Opt.of( "METADATA_NS", "http://www.trisotech.com/2015/triso/modeling", String.class, false ) ),
	p_DIAGRAM_NS( Opt.of( "DIAGRAM_NS", "http://www.omg.org/spec/DMN/20180521/DI/", String.class, false ) ),
	p_METADATA_DIAGRAM_DMN_NS( Opt.of( "METADATA_DIAGRAM_DMN_NS", "http://www.trisotech.com/2016/triso/dmn", String.class, false)),
	p_METADATA_DIAGRAM_CMMN_NS( Opt.of( "METADATA_DIAGRAM_CMMN_NS", "http://www.trisotech.com/2016/triso/cmmn", String.class, false)),
	p_EL_DIAGRAM_EXT( Opt.of( "EL_DIAGRAM_EXTENSION", "extension", String.class, false)),
	p_EL_ANNOTATION( Opt.of( "EL_ANNOTATION", "semanticLink", String.class, false ) ),
	p_EL_RELATIONSHIP( Opt.of( "EL_RELATIONSHIP", "interrelationship", String.class, false ) ),
	p_EL_ANNOTATION_ID( Opt.of( "EL_ANNOTATION_ID", "customAttribute", String.class, false)),
	p_EL_ANNOTATED_ITEM( Opt.of( "EL_ANNOTATED_ITEM", "itemDefinition", String.class, false ) );


	private Opt<ReaderOptions> opt;

	ReaderOptions(Opt<ReaderOptions> opt ) {
		this.opt = opt;
	}

	@Override
	public Opt<ReaderOptions> getOption() {
		return opt;
	}
}
