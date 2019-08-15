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

public class TrisotechApiUrls {

  private TrisotechApiUrls() { throw new IllegalStateException("Utility class"); }

  public static final String BASE_URL = "https://mc.trisotech.com/publicapi/";

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

  // TODO: better way? CAO
  public static final String TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhcHAiOiIxNGY4YmVhMy1jMTNhLTQ1NDYtOWQ5Ni03ZjgwODhlYzEwMzMiLCJzdWIiOiJkbHJzdG1lYTMwQGV4Y2hhbmdlLm1heW8uZWR1IiwiaXNzIjoibWMudHJpc290ZWNoLmNvbSIsImlhdCI6MTU1OTEzODMyNH0.zZoqSOc6tjOi6ZFoHOmTlq_8zJU0KQz2R58z6ygrULs";

  // Test files

  // id to MEA-Test repository
  // TODO: Need to do same for MEA? Or just 'find' each time? CAO -- this might be better in the environment so can set this one for development/Test? and set the other one for prod/int?
  static final String MEA_TEST = "MEA-Test";
  public static final String MEA_TEST_ID = "d4aca01b-d446-4bc8-a6f0-85d84f4c1aaf";

  // DMN Published
  static final String WEAVER_TEST_1_ID = "123720a6-9758-45a3-8c5c-5fffab12c494";
  // DMN unpublished
  static final String WEAVER_TEST_2_ID = "ffa53262-4d36-4656-890b-4e48ed1cb9c3";
  // CMMN Published
  static final String WEAVE_TEST_1_ID = "93e58aa9-c258-46fd-909d-1cb096e19e64";
  // CMMN unpublished
  static final String WEAVE_TEST_2_ID = "84da9f52-44f5-46d1-ae3f-c5599f78ad1f";
}
