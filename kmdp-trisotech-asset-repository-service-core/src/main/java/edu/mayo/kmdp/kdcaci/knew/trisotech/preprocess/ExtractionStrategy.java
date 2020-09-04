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
package edu.mayo.kmdp.kdcaci.knew.trisotech.preprocess;

import com.fasterxml.jackson.databind.JsonNode;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

@Component
public interface ExtractionStrategy {

	void setMapper( IdentityMapper mapper );

	KnowledgeAsset extractXML( Document dox, JsonNode meta );

	KnowledgeAsset extractXML( Document dox, TrisotechFileInfo meta );

	KnowledgeAsset extractXML(Document woven, TrisotechFileInfo model, ResourceIdentifier asset);

	Optional<ResourceIdentifier> getAssetID(String fileId);

  Optional<URI> getEnterpriseAssetIdForAsset(UUID assetId);

	Optional<URI> getEnterpriseAssetVersionIdForAsset(UUID assetId, String versionTag, boolean any)
			throws NotLatestVersionException;

	Optional<String> getArtifactID( Document dox, TrisotechFileInfo meta );

	String getArtifactID(ResourceIdentifier id, boolean any) throws NotLatestVersionException;

  Optional<SyntacticRepresentation> getRepLanguage( Document dox, boolean concrete );

  Optional<String> getMimetype(UUID assetId);
	Optional<String> getMimetype(String internalId);

	Optional<String> getArtifactVersion(UUID assetId);

	Optional<String> getFileId(String internalId);

	Optional<String> getFileId(UUID assetId, boolean any);

}
