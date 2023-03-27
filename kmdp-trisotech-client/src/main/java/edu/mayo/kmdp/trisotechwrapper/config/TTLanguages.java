package edu.mayo.kmdp.trisotechwrapper.config;

public enum TTLanguages {
  CMMN("cmmn"),
  DMN("dmn"),
  BPMN("bpmn"),
  KEM("businessentity"),
  UNSUPPORTED(null);

  private final String tag;

  TTLanguages(String tag) {
    this.tag = tag;
  }

  public String getTag() {
    return tag;
  }
}
