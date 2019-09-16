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
package edu.mayo.kmdp.trisotechwrapper;

import org.springframework.beans.factory.annotation.Value;

public class TrisotechApiUrls {

  private TrisotechApiUrls() { throw new IllegalStateException("Utility class"); }

  static final String BASE_URL = "https://mc.trisotech.com/publicapi/";

  //    TODO: Needed? Only returns user that is signed in. Needed to confirm API user? CAO
//  TODO: ?  public static String LOGIN_PATH = "login/";
  // List of repositories
  static final String REPOSITORY_PATH = "repository";
  // content of specified repository; set up for URIComponentsBuilder; mimetype and path are optional
  static final String CONTENT_PATH = "repositorycontent?repository={repo}&mimetype={mime}&path={path}";
  // versions of specific file within specified repository
  static final String VERSIONS_PATH = "repositoryfileversion?repository={repo}&id={fileId}&mimetype={mime}";


  // TODO: Do we have a need to ever get the JSON? CAO
  // return DMN files in XML format
  static final String DMN_XML_MIMETYPE = "application/dmn-1-2+xml";
  // return CMMN files in XML format
  static final String CMMN_XML_MIMETYPE = "application/cmmn-1-1+xml";

}
