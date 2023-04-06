package edu.mayo.kmdp.trisotechwrapper.components.graph;


import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetType;

/**
 * Standard variables used in DES SPARQL Queries
 */
public enum TTGraphTerms {

  /**
   * ID of an Asset associated to a Model (URI)
   */
  ASSET_ID( "?assetId" ),
  /**
   * IDd of an Asset associated to a Service (URI)
   */
  SERVICE_ID( "?serviceId" ),
  /**
   * {@link KnowledgeAssetType} associated to a Model (URI)
   */
  ASSET_TYPE( "?assetType" ),
  /**
   * Model/Artifact ID (URI)
   */
  MODEL( "?model" ),
  /**
   * Publication State (String - enumerated)
   */
  STATE( "?state" ),
  /**
   * MIME Type (String - enumerated)
   */
  MIME_TYPE( "?mimeType" ),
  /**
   * Path within a Place (String)
   */
  PATH( "?path" ),
  /**
   * Model Version Tag (String)
   */
  VERSION( "?version" ),
  /**
   * Model Last Update (DateTime)
   */
  UPDATED( "?updated" ),
  /**
   * Model Last Updater (String)
   */
  UPDATER( "?updater" ),
  /**
   * Model Name (String)
   */
  ARTIFACT_NAME( "?artifactName" ),
  /**
   * Service Fragment ID (URI)
   */
  SERVICE_FRAGMENT( "?serviceNode" ),
  /**
   * Service Fragment Name (String)
   */
  SERVICE_NAME( "?serviceName" );

  final String key;

  TTGraphTerms(String key) {
    this.key = key;
  }

  public String getKey() {
    return key;
  }
}
