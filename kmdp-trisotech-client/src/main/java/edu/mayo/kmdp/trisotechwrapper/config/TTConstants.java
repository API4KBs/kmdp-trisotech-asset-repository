package edu.mayo.kmdp.trisotechwrapper.config;

/**
 * Class that exposes a variety of TT-related constants
 */
public class TTConstants {

  private TTConstants() {
    // static constants only
  }

  /**
   * TT's domain name
   */
  public static final String TRISOTECH_COM = "trisotech.com";
  /**
   * Base URI used for model IDs
   */
  public static final String TT_BASE_MODEL_URI = "http://www.trisotech.com/definitions/_";

  /**
   * TT's Graph URI for the 'Accelerator' Graph/Model meta-classifier
   */
  public static final String TT_ACCELERATOR_META_CLASS = "http://www.trisotech.com/graph/1.0/element#Framework";
  /**
   * TT's Graph URI for the 'Entity' class
   */
  public static final String TT_ACCELERATOR_ENTTIY = "http://www.trisotech.com/graph/1.0/element#Entity";

  /**
   * TT's Graph URI for the 'KEM' model Graph/Model meta-classifier
   */
  public static final String TT_KEM_META_CLASS = "http://www.trisotech.com/graph/1.0/element#BusinessEntityModel";
  /**
   * TT's Graph URI for the 'Entity' class
   */
  public static final String TT_KEM_TERM = "http://www.trisotech.com/graph/1.0/element#Term";

  /**
   * TT's base URI for named RDF graphs (mapped to TT Places)
   */
  public static final String TRISOTECH_GRAPH = "http://trisotech.com/graph/1.0/graph#";

  /**
   * Standard prefix associated to W3C's XML Namespaces namespace
   */
  public static final String XMLNS_PREFIX = "xmlns:";

  /**
   * Standard prefix associated to API4KP Assets namespace
   */
  public static final String ASSETS_PREFIX = "assets:";

  /**
   * W3C's XML Namespaces namespace
   */
  public static final String W3C_XMLNS = "http://www.w3.org/2000/xmlns/";
  /**
   * W3C's XML Schema Instance namespace
   */
  public static final String W3C_XSI = "http://www.w3.org/2001/XMLSchema-instance";
  /**
   * OMG's CMMN 1.1 namespace
   */
  public static final String CMMN_11_XMLNS = "http://www.omg.org/spec/CMMN/20151109/MODEL";
  /**
   * OMG's DMN 1.2 namespace
   */
  public static final String DMN_12_XMLNS = "http://www.omg.org/spec/DMN/20180521/MODEL/";
  /**
   * OMG's DMN 1.3 namespace
   */
  public static final String DMN_13_XMLNS = "https://www.omg.org/spec/DMN/20191111/MODEL/";
  /**
   * OMG's DMN 1.4 namespace
   */
  public static final String DMN_14_XMLNS = "https://www.omg.org/spec/DMN/20211108/MODEL/";

  /**
   * TT's namespace associated to metadata elements
   */
  public static final String TT_METADATA_NS = "http://www.trisotech.com/2015/triso/modeling";
  /**
   * TT's cross-version DMN namespace
   */
  public static final String TT_DMN_12_NS = "http://www.trisotech.com/2016/triso/dmn";
  /**
   * TT's cross-version CMMN namepace
   */
  public static final String TT_CMMN_11_NS = "http://www.trisotech.com/2014/triso/cmmn";

  /**
   * TT's namespace for DMN imports of library functions
   */
  public static final String TT_LIBRARIES = "https://www.trisotech.com/libraries";

  /**
   * XML attribute that holds a reference to the XML export algorithm
   */
  public static final String TT_META_EXPORTER = "exporter";
  /**
   * XML attribute that holds a reference to the XML export algorithm version
   */
  public static final String TT_META_EXPORTER_VERSION = "exporterVersion";

  /**
   * TT's XML attribute linking elements to semantic concepts
   */
  public static final String TT_SEMANTICLINK = "semanticLink";
  /**
   * TT's XML attribute cross-linking elements
   */
  public static final String TT_RELATIONSHIP = "interrelationship";
  /**
   * TT's XML attribute referencing model custom attributes
   */
  public static final String TT_CUSTOM_ATTRIBUTE_ATTR = "customAttribute";
  /**
   * TT's XML attribute for cross-model reuse by reference links
   */
  public static final String TT_REUSELINK = "reuseLink";
  /**
   * TT's XML attribute for cross-model reuse by copy links
   */
  public static final String TT_COPYOFLINK = "copyOfLink";
  /**
   * TT's XML attribute for comments
   */
  public static final String TT_COMMENTS = "comments";

  /**
   * CMMN XML extension element for input data bindings
   */
  public static final String TT_INPUT_BINDINGS = "dataInputBindings";
  /**
   * CMMN XML extension element for output data bindings
   */
  public static final String TT_OUTPUT_BINDINGS = "dataOutputBindings";

  /**
   * DMN XML for auto-generated decision services (from diagrams)
   */
  public static final String TT_DYNAMIC_DECISION_SERVICE = "dynamicDecisionService";
  /**
   * TT's XML for attachments
   */
  public static final String TT_ATTACHMENT_ITEM = "attachment";

  /**
   * JBoss Drools namespace
   */
  public static final String DROOLS_NS = "http://www.drools.org/kie/dmn/1.1";

  /**
   * Standard DMN Element "decision"
   */
  public static final String DMN_EL_DECISION = "decision";
  /**
   * Standard DMN Element "decisionService"
   */
  public static final String DMN_EL_DECISION_SERVICE = "decisionService";
  /**
   * Standard DMN Element "extensionElements"
   */
  public static final String DMN_EL_EXTENSIONS = "extensionElements";
  /**
   * Standard DMN Element "import"
   */
  public static final String DMN_IMPORT = "import";
  /**
   * Standard DMN Element "importType"
   */
  public static final String DMN_IMPORTTYPE = "importType";
  /**
   * Standard CMMN Element "extensionElements"
   */
  public static final String CMMN_EL_EXTENSIONS = "extensionElements";

  /**
   * Prefix associated to OMG's API4KP
   */
  public static final String API4KP_PREFIX = "api4kp:";

  /**
   * XML attribute used in key/value pairs, to denote the value
   */
  public static final String VALUE = "value";
  /**
   * XML attribute used in key/value pairs, to denote the key
   */
  public static final String KEY = "key";


}
