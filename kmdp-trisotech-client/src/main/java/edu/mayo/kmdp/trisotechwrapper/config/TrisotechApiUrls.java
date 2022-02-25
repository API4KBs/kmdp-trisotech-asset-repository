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

import static edu.mayo.kmdp.util.Util.isEmpty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrisotechApiUrls {

  static Logger logger = LoggerFactory.getLogger(TrisotechApiUrls.class);

  public static final String CMMN_UPPER = "CMMN";
  public static final String CMMN_LOWER = "cmmn";
  public static final String DMN_LOWER = "dmn";
  public static final String DMN_UPPER = "DMN";
  public static final String BPMN_LOWER = "bpmn";
  public static final String BPMN_UPPER = "BPMN";

  private TrisotechApiUrls() {
    throw new IllegalStateException("Utility class");
  }

  // List of repositories
  public static final String REPOSITORY_PATH = "repository";
  // content of specified repository; set up for URIComponentsBuilder; mimetype and path are optional
  public static final String CONTENT_PATH = "repositorycontent?repository={repo}&mimetype={mime}&path={path}";
  // path for POST to specified repository; mimetype is required; at this time version and state are not used
  public static final String CONTENT_PATH_POST = "repositorycontent?repository={repo}&name={name}&mimetype={mime}&path={path}";
  public static final String CONTENT_PATH_POST_WITH_VERSION = "repositorycontent?repository={repo}&name={name}&mimetype={mime}&path={path}&version={version}&state={state}";
  // versions of specific file within specified repository
  public static final String VERSIONS_PATH = "repositoryfileversion?repository={repo}&id={fileId}";

  public static final String SPARQL_PATH = "/ds/query";

  // return DMN files in XML format
  public static final String DMN_XML_MIMETYPE = "application/dmn-1-2+xml";
  // return CMMN files in XML format
  public static final String CMMN_XML_MIMETYPE = "application/cmmn-1-1+xml";
  // return BPMN files in XML format
  public static final String BPMN_XML_MIMETYPE = "application/bpmn-2-1+xml";


  /**
   * get the XML-specified mimeType for transferring XML files with Trisotech
   *
   * @param mimetype the mimetype specified through file information
   * @return the XML mimetype specfication to be used in API calls
   * @throws IllegalArgumentException if a type other than "dmn" and "cmmn" is requested
   */
  public static String getXmlMimeType(String mimetype) {
    if (isEmpty(mimetype)) {
      return null;
    }
    String mime = mimetype.toLowerCase();
    if (mime.contains(DMN_LOWER)) {
      return DMN_XML_MIMETYPE;
    } else if (mime.contains(CMMN_LOWER)) {
      return CMMN_XML_MIMETYPE;
    } else {
      logger.warn("Unexpected MIME type {}", mimetype);
      return mimetype;
    }
  }

  /**
   * Maps the assetTypeTag used in the KARS API to the mime type used by the TT API The mapping is
   * based on the assumption that BPM+ languages are used consistently for specific asset types.
   *
   * @param assetTypeTag a formal asset type tag
   * @return the (TT) mime type implied by the asset type
   */
  public static String getXmlMimeTypeByAssetType(String assetTypeTag) {
    if (assetTypeTag == null) {
      // no filter
      return null;
    }

    if (assetTypeTag.contains("Decision") || assetTypeTag.contains("Rule")) {
      return getXmlMimeType(DMN_LOWER);
    } else if (assetTypeTag.contains("Case")) {
      return getXmlMimeType(CMMN_LOWER);
    } else if (assetTypeTag.contains("Process")
        || assetTypeTag.contains("Pathway") || assetTypeTag.contains("Protocol")) {
      return getXmlMimeType(BPMN_LOWER);
    } else {
      throw new IllegalArgumentException("Unexpected Asset Type " + assetTypeTag);
    }
  }
}
