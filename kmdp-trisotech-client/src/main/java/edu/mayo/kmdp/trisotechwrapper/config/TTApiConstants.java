/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package edu.mayo.kmdp.trisotechwrapper.config;

public class TTApiConstants {

  private TTApiConstants() {
    // static only
  }

  // List of repositories
  public static final String REPOSITORY_PATH = "repository";
  // content of specified repository; set up for URIComponentsBuilder; mimetype and path are optional
  public static final String CONTENT_PATH = "repositorycontent?repository={repo}&mimetype={mime}&path={path}";
  public static final String MODEL_PATH = "repositoryfilecontent?repository={repo}&mimetype={mime}&path={path}&sku={file}";
  // path for POST to specified repository; mimetype is required; at this time version and state are not used
  public static final String CONTENT_PATH_POST = "repositorycontent?repository={repo}&name={name}&mimetype={mime}&path={path}";
  public static final String CONTENT_PATH_POST_WITH_VERSION = "repositorycontent?repository={repo}&name={name}&mimetype={mime}&path={path}&version={version}&state={state}";
  // versions of specific file within specified repository
  public static final String VERSIONS_PATH = "repositoryfileversion?repository={repo}&id={fileId}";

  public static final String EXEC_ARTIFACTS_PATH = "executionrepositoryartifact?name={execEnv}";

  public static final String SPARQL_PATH = "/ds/query";



  /**
   * Derives the public API endpoint for a given DES server instance
   * @param baseURL the DES server base URL
   * @return the DES API endpoint
   */
  public static String apiEndpoint(String baseURL) {
    return baseURL
        + (baseURL.endsWith("/") ? "" : "/")
        + "publicapi/";
  }

}
