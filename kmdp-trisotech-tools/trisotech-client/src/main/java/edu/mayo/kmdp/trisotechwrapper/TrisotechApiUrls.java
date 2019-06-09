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
    public static String BASE_URL = "https://mc.trisotech.com/publicapi/";

//    TODO: Needed? Only returns user that is signed in. Needed to confirm API user?
//    public static String LOGIN_PATH = "login/";
  // List of repositories
    public static String REPOSITORY_PATH = "repository";
    // content of specified repository; set up for URIComponentsBuilder; mimetype and path are optional
    public static String CONTENT_PATH = "repositorycontent?repository={repo}&mimetype={mime}&path={path}";
    // versions of specific file within specified repository
    public static String VERSIONS_PATH = "repositoryfileversion?repository=%s&id=%s";

    // id to MEA-Test repository
    // TODO: Need to do same for MEA? Or just 'find' each time?
    public static String MEA_TEST = "d4aca01b-d446-4bc8-a6f0-85d84f4c1aaf";

  // TODO: Do we have a need to ever get the JSON?
    // return DMN files in XML format
    public static String DMN_XML_MIMETYPE = "application/dmn-1-2+xml";
    // return CMMN files in XML format
    public static String CMMN_XML_MIMETYPE = "application/cmmn-1-1+xml";

    // TODO: better way?
    public static String TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhcHAiOiIxNGY4YmVhMy1jMTNhLTQ1NDYtOWQ5Ni03ZjgwODhlYzEwMzMiLCJzdWIiOiJkbHJzdG1lYTMwQGV4Y2hhbmdlLm1heW8uZWR1IiwiaXNzIjoibWMudHJpc290ZWNoLmNvbSIsImlhdCI6MTU1OTEzODMyNH0.zZoqSOc6tjOi6ZFoHOmTlq_8zJU0KQz2R58z6ygrULs";
}
