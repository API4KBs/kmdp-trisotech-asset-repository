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

/**
 * Exception to handle when the asset version we're looking for doesn't
 * exist for the latest version of the artifact.
 * Provide the model URI for the artifact as the errorMessage.
 */
public class NotLatestVersionException extends Exception {

  private String modelUri;

  public NotLatestVersionException(String modelUri) {
    super(
        "Model " + modelUri + " was resolved using a non-current Asset version");
    this.modelUri = modelUri;
  }

  public String getModelUri() {
    return modelUri;
  }
}
