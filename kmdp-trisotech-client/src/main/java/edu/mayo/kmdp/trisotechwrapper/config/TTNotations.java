package edu.mayo.kmdp.trisotechwrapper.config;

import static edu.mayo.kmdp.trisotechwrapper.config.TTLanguages.BPMN;
import static edu.mayo.kmdp.trisotechwrapper.config.TTLanguages.CMMN;
import static edu.mayo.kmdp.trisotechwrapper.config.TTLanguages.DMN;
import static edu.mayo.kmdp.trisotechwrapper.config.TTLanguages.KEM;
import static edu.mayo.kmdp.trisotechwrapper.config.TTLanguages.UNSUPPORTED;
import static edu.mayo.kmdp.util.Util.isEmpty;

import java.util.Optional;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enumeration of Trisotech's natively supported KRR Languages+Notations/Formats
 * <p>
 * Includes utility/helper methods to map Languages to the MIME types used in the DES
 */
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

  /**
   * Logger
   */
  public static final Logger logger = LoggerFactory.getLogger(TTNotations.class);


  /**
   * The canonical MIME type associated to a Language+Format
   */
  private final String mimeType;

  TTNotations(String mime) {
    this.mimeType = mime;
  }

  public String getMimeType() {
    return mimeType;
  }


  /**
   * Decodes a MIME type code, to identify the core Language
   *
   * @param mimetype the MIME type
   * @return the core {@link TTLanguages}
   */
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
   * get the XML-specified mimeType for transferring XML files with Trisotech.
   * <p>
   * Maps across variant formats - usually native JSON vs standard XML - and returns the canonical,
   * XML based standard type
   *
   * @param mimetype the mimetype
   * @return the normalized-to-XML mimetype
   */
  public static Optional<String> getStandardXmlMimeType(String mimetype) {
    if (isEmpty(mimetype)) {
      return Optional.empty();
    }
    var lang = detectTTLanguage(mimetype);
    var xmlMime = getStandardXmlMimeType(lang);
    return Optional.of(xmlMime != null ? xmlMime : mimetype);
  }

  /**
   * get the canonical, standard, version of an XML-based mimeType for a given {@link TTLanguages}
   *
   * @param lang the language
   * @return the canonical XML-based mimetype
   */
  private static String getStandardXmlMimeType(TTLanguages lang) {
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

  /**
   * get the native (TT internal) representation of a supported Language
   *
   * @param mimetype the mimeType of a Model's variant
   * @return the native {@link TTNotations}
   */
  public static TTNotations getNativeNotationMimeType(String mimetype) {
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

  /**
   * Compares two mimeTypes
   * <p>
   * Determines if they share the same language for, regardless of notation (version) and format
   *
   * @param mime1 the first mimeType of a Model's variant
   * @param mime2 the second mimeType of a Model's variant
   * @return true if both mimeTypes denote the same Language, regardless of version and format
   */
  public static boolean mimeMatches(String mime1, String mime2) {
    return mime1 != null && mime2 != null
        && getNativeNotationMimeType(mime1) == getNativeNotationMimeType(mime2);
  }

  /**
   * Maps an Asset Type (tag) used in the KARS API to the default representation (mimeType) for
   * Artifacts carrying knowledge of that Asset Type.
   * <p>
   * The mapping is based on the assumption that BPM+ languages are used consistently for specific
   * asset types.
   *
   * @param assetTypeTag a formal asset type tag
   * @return the (TT) mime type implied by the asset type
   * @see KnowledgeAssetTypeSeries#resolve(String)
   */
  public static String getXmlMimeTypeByAssetType(String assetTypeTag) {
    if (assetTypeTag == null) {
      // no filter
      return null;
    }

    if (assetTypeTag.contains("Decision") || assetTypeTag.contains("Rule")) {
      return getStandardXmlMimeType(DMN);
    } else if (assetTypeTag.contains("Case")) {
      return getStandardXmlMimeType(CMMN);
    } else if (assetTypeTag.contains("Process")
        || assetTypeTag.contains("Pathway") || assetTypeTag.contains("Protocol")) {
      return getStandardXmlMimeType(BPMN);
    } else if (assetTypeTag.contains("Lexicon")) {
      return getStandardXmlMimeType(KEM);
    } else {
      throw new IllegalArgumentException("Unexpected Asset Type " + assetTypeTag);
    }
  }
}
