package edu.mayo.kmdp.trisotechwrapper.config;

import static edu.mayo.kmdp.trisotechwrapper.config.TTLanguages.BPMN;
import static edu.mayo.kmdp.trisotechwrapper.config.TTLanguages.CMMN;
import static edu.mayo.kmdp.trisotechwrapper.config.TTLanguages.DMN;
import static edu.mayo.kmdp.trisotechwrapper.config.TTLanguages.KEM;
import static edu.mayo.kmdp.trisotechwrapper.config.TTLanguages.UNSUPPORTED;
import static edu.mayo.kmdp.util.Util.isEmpty;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum TTNotations {
  // return DMN files in XML format
  DMN_12_XML("application/dmn-1-2+xml"),
  DMN_13_XML("application/dmn-1-3+xml"),
  DMN_14_XML("application/dmn-1-4+xml"),
  DMN_15_XML("application/dmn-1-5+xml"),
  DMN_JSON("application/vnd.triso-dmn+json"),
  // return CMMN files in XML format
  CMMN_11_XML("application/cmmn-1-1+xml"),
  CMMN_JSON("application/vnd.triso-cmmn+json"),
  // return BPMN files in XML format
  BPMN2_XML("application/bpmn-2-0+xml"),
  KEM_JSON("application/vnd.triso-businessentity+json"),
  CAP_JSON("application/vnd.triso-capability+json"),
  LAND_JSON("application/vnd.triso-landscaping+json"),
  ACCEL_JSON("application/vnd.triso-discovery+json");

  public static final Logger logger = LoggerFactory.getLogger(TTNotations.class);


  private final String mimeType;

  TTNotations(String mime) {
    this.mimeType = mime;
  }

  public String getMimeType() {
    return mimeType;
  }


  public static TTLanguages detectTTLanguage(String mimetype) {
    if (isEmpty(mimetype)) {
      return UNSUPPORTED;
    }
    String mime = mimetype.toLowerCase();
    if (mime.contains(DMN.getTag())) {
      return DMN;
    } else if (mime.contains(CMMN.getTag())) {
      return CMMN;
    } else if (mime.contains(KEM.getTag())) {
      return KEM;
    } else if (mime.contains(BPMN.getTag())) {
      return BPMN;
    } else {
      logger.warn("Unexpected MIME type {}", mimetype);
      return UNSUPPORTED;
    }
  }

  /**
   * get the XML-specified mimeType for transferring XML files with Trisotech
   *
   * @param mimetype the mimetype specified through file information
   * @return the XML mimetype specfication to be used in API calls
   * @throws IllegalArgumentException if a type other than "dmn" and "cmmn" is requested
   */
  public static Optional<String> getXmlMimeType(String mimetype) {
    if (isEmpty(mimetype)) {
      return Optional.empty();
    }
    var lang = detectTTLanguage(mimetype);
    var xmlMime = getXmlMimeType(lang);
    return Optional.of(xmlMime != null ? xmlMime : mimetype);
  }

  private static String getXmlMimeType(TTLanguages lang) {
    switch (lang) {
      case CMMN:
        return CMMN_11_XML.getMimeType();
      case DMN:
        return DMN_12_XML.getMimeType();
      case KEM:
        return KEM_JSON.getMimeType();
      case BPMN:
        return BPMN2_XML.getMimeType();
      case UNSUPPORTED:
      default:
        return null;
    }
  }

  public static TTNotations getCanonicalMimeType(String mimetype) {
    if (isEmpty(mimetype)) {
      return null;
    }
    var lang = detectTTLanguage(mimetype);
    switch (lang) {
      case CMMN:
        return CMMN_JSON;
      case DMN:
        return DMN_JSON;
      case KEM:
        return KEM_JSON;
      case BPMN:
        return BPMN2_XML;
      case UNSUPPORTED:
      default:
        return null;
    }
  }

  public static boolean mimeMatches(String mime1, String mime2) {
    return mime1 != null && mime2 != null
        && getCanonicalMimeType(mime1) == getCanonicalMimeType(mime2);
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
      return getXmlMimeType(DMN);
    } else if (assetTypeTag.contains("Case")) {
      return getXmlMimeType(CMMN);
    } else if (assetTypeTag.contains("Process")
        || assetTypeTag.contains("Pathway") || assetTypeTag.contains("Protocol")) {
      return getXmlMimeType(BPMN);
    } else if (assetTypeTag.contains("Lexicon")) {
      return getXmlMimeType(KEM);
    } else {
      throw new IllegalArgumentException("Unexpected Asset Type " + assetTypeTag);
    }
  }
}
