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

import com.fasterxml.jackson.databind.JsonNode;
import edu.mayo.kmdp.metadata.surrogate.KnowledgeAsset;
import edu.mayo.kmdp.metadata.surrogate.Representation;
import edu.mayo.kmdp.preprocess.NotLatestVersionException;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import org.omg.spec.api4kp._1_0.identifiers.URIIdentifier;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

@Component
public interface ExtractionStrategy {

	IdentityMapper getMapper();

	void setMapper( IdentityMapper mapper );

	KnowledgeAsset extractXML( Document dox, JsonNode meta );

	KnowledgeAsset extractXML( Document dox, TrisotechFileInfo meta );

	Optional<URIIdentifier> getAssetID( Document dox );
	Optional<URIIdentifier> getAssetID(String fileId);

	Optional<URI> getEnterpriseAssetIdForAsset(UUID assetId);
	URI getEnterpriseAssetIdForAssetVersionId(URI enterpriseAssetVersionId);

	Optional<URI> getEnterpriseAssetVersionIdForAsset(UUID assetId, String versionTag)
			throws NotLatestVersionException;

	Optional<String> getArtifactID( Document dox, TrisotechFileInfo meta );

	String getArtifactID(URIIdentifier id) throws NotLatestVersionException;

  Optional<Representation> getRepLanguage( Document dox, boolean concrete );
	Optional<String> getMimetype(UUID assetId);

	Optional<String> getMimetype(String internalId);

	Optional<String> getArtifactVersion(UUID assetId);

	URIIdentifier extractAssetID( Document dox );

	Optional<String> getFileId(String internalId);

	Optional<String> getFileId(UUID assetId);

}
