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

import com.fasterxml.jackson.databind.JsonNode;
import edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.metadata.surrogate.Representation;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import org.omg.spec.api4kp._1_0.identifiers.URIIdentifier;
import org.w3c.dom.Document;

import java.util.Optional;

public interface ExtractionStrategy {

	IdentityMapper getMapper();

	void setMapper( IdentityMapper mapper );

	KnowledgeAsset extractXML( Document dox, JsonNode meta );

	KnowledgeAsset extractXML( Document dox, TrisotechFileInfo meta );

	Optional<URIIdentifier> getResourceID( Document dox, String artifactId, String versionTag );

	Optional<String> getVersionTag( Document dox, TrisotechFileInfo meta );

	Optional<String> getArtifactID( Document dox );

	Optional<Representation> getRepLanguage( Document dox, boolean concrete );

	String getMetadataEntryNameForID( String id );

	default KnowledgeAsset newSurrogate() {
		KnowledgeAsset surr = new edu.mayo.kmdp.metadata.surrogate.resources.KnowledgeAsset();
		return surr;
	}

	URIIdentifier extractAssetID( Document dox, TrisotechFileInfo meta );

}
