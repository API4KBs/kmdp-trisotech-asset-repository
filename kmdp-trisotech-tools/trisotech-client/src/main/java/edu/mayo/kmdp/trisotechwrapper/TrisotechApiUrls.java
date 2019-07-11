/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.mayo.kmdp.trisotechwrapper;

public class TrisotechApiUrls {
  public static String BASE_URL = "https://mc.trisotech.com/publicapi/";

  //    TODO: Needed? Only returns user that is signed in. Needed to confirm API user? CAO
//    public static String LOGIN_PATH = "login/";
  // List of repositories
  public static String REPOSITORY_PATH = "repository";
  // content of specified repository; set up for URIComponentsBuilder; mimetype and path are optional
  public static String CONTENT_PATH = "repositorycontent?repository={repo}&mimetype={mime}&path={path}";
  // versions of specific file within specified repository
  public static String VERSIONS_PATH = "repositoryfileversion?repository=%s&id=%s";


  // TODO: Do we have a need to ever get the JSON? CAO
  // return DMN files in XML format
  public static String DMN_XML_MIMETYPE = "application/dmn-1-2+xml";
  // return CMMN files in XML format
  public static String CMMN_XML_MIMETYPE = "application/cmmn-1-1+xml";

  // TODO: better way? CAO
  public static String TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhcHAiOiIxNGY4YmVhMy1jMTNhLTQ1NDYtOWQ5Ni03ZjgwODhlYzEwMzMiLCJzdWIiOiJkbHJzdG1lYTMwQGV4Y2hhbmdlLm1heW8uZWR1IiwiaXNzIjoibWMudHJpc290ZWNoLmNvbSIsImlhdCI6MTU1OTEzODMyNH0.zZoqSOc6tjOi6ZFoHOmTlq_8zJU0KQz2R58z6ygrULs";

  // Test files

  // id to MEA-Test repository
  // TODO: Need to do same for MEA? Or just 'find' each time? CAO -- this might be better in the environment so can set this one for development/Test? and set the other one for prod/int?
  public static String MEA_TEST = "d4aca01b-d446-4bc8-a6f0-85d84f4c1aaf";

  // DMN Published
  public static final String WEAVER_TEST_1_ID = "123720a6-9758-45a3-8c5c-5fffab12c494";
  // DMN unpublished
  public static final String WEAVER_TEST_2_ID = "ffa53262-4d36-4656-890b-4e48ed1cb9c3";
  // CMMN Published
  public static final String WEAVE_TEST_1_ID = "93e58aa9-c258-46fd-909d-1cb096e19e64";
  // CMMN unpublished
  public static final String WEAVE_TEST_2_ID = "84da9f52-44f5-46d1-ae3f-c5599f78ad1f";
}
