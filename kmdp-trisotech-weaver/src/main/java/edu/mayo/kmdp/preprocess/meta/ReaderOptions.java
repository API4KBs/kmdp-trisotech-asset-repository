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

	URL_PATTERN_ST( Opt.of( "URL_PATTERN_ST","\\\"(.*)\\\"", String.class, false ) ),

	P_MODEL_NS( Opt.of( "MODEL_NS", "http://www.omg.org/spec/DMN/20151101/dmn.xsd", String.class, false ) ),
	P_EL_MODEL_EXT( Opt.of( "EL_MODEL_EXTENSIONS", "extensionElements", String.class, false ) ),

	P_METADATA_NS( Opt.of( "METADATA_NS", "http://www.trisotech.com/2015/triso/modeling", String.class, false ) ),
	P_DIAGRAM_NS( Opt.of( "DIAGRAM_NS", "http://www.omg.org/spec/DMN/20180521/DI/", String.class, false ) ),
	P_METADATA_DIAGRAM_DMN_NS( Opt.of( "METADATA_DIAGRAM_DMN_NS", "http://www.trisotech.com/2016/triso/dmn", String.class, false)),
	P_METADATA_DIAGRAM_CMMN_NS( Opt.of( "METADATA_DIAGRAM_CMMN_NS", "http://www.trisotech.com/2014/triso/cmmn", String.class, false)),
	P_DROOLS_NS( Opt.of( "DROOLS_NS", "http://www.drools.org/kie/dmn/1.1", String.class,  false)),
	P_EL_DIAGRAM_EXT( Opt.of( "EL_DIAGRAM_EXTENSION", "extension", String.class, false)),
	P_EL_EXPORTER( Opt.of( "EL_EXPORTER", "exporter", String.class, false ) ),
	P_EL_EXPORTER_VERSION( Opt.of( "EL_EXPORTER_VERSION", "exporterVersion", String.class, false ) ),
	P_EL_ANNOTATION( Opt.of( "EL_ANNOTATION", "semanticLink", String.class, false ) ),
	P_EL_RELATIONSHIP( Opt.of( "EL_RELATIONSHIP", "interrelationship", String.class, false ) ),
	P_EL_ANNOTATION_ID( Opt.of( "EL_ANNOTATION_ID", "customAttribute", String.class, false)),
	P_EL_DECISION( Opt.of( "EL_DECISION", "decision", String.class, false)),
	P_METADATA_ITEM_DEFINITION( Opt.of( "METADATA_ITEM_DEFINITION", "itemDefinitions", String.class, false ) ),
	P_METADATA_ATTACHMENT_ITEM( Opt.of( "METADATA_ATTACHMENT_ITEM", "attachment", String.class, false ) );


	private Opt<ReaderOptions> opt;

	ReaderOptions(Opt<ReaderOptions> opt ) {
		this.opt = opt;
	}

	@Override
	public Opt<ReaderOptions> getOption() {
		return opt;
	}
}
