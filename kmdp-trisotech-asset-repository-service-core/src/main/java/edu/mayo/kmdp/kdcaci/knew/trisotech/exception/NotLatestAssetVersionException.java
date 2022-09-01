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
package edu.mayo.kmdp.kdcaci.knew.trisotech.exception;

import edu.mayo.ontology.taxonomies.ws.responsecodes.ResponseCodeSeries;
import java.net.URI;
import org.omg.spec.api4kp._20200801.Explainer;
import org.omg.spec.api4kp._20200801.ServerSideException;

/**
 * Exception to handle when the asset version we're looking for doesn't
 * exist for the latest version of the artifact.
 * Provide the model URI for the artifact as the errorMessage.
 */
public class NotLatestAssetVersionException extends ServerSideException {

  private String modelUri;

  public NotLatestAssetVersionException(
      String requestedVersion,
      String currentVersion,
      String modelUri) {
    super(
        Explainer.GENERIC_ERROR_TYPE,
        "Outdated Asset Version",
        ResponseCodeSeries.Conflict,
        "Model " + modelUri + " is on asset version " + currentVersion + " while version " + requestedVersion + " was requested",
        URI.create(modelUri));
    this.modelUri = modelUri;
  }

  public String getModelUri() {
    return modelUri;
  }
}
