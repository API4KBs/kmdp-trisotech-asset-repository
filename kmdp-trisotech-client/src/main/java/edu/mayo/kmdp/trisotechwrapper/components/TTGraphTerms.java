package edu.mayo.kmdp.trisotechwrapper.components;

public enum TTGraphTerms {

  ASSET_ID( "?assetId" ),
  ASSET_TYPE( "?assetType" ),
  MODEL( "?model" ),
  STATE( "?state" ),
  MIME_TYPE( "?mimeType" ),
  VERSION( "?version" ),
  UPDATED( "?updated" ),
  ARTIFACT_NAME( "?artifactName" );

  String key;

  TTGraphTerms(String key) {
    this.key = key;
  }

  public String getKey() {
    return key;
  }
}
