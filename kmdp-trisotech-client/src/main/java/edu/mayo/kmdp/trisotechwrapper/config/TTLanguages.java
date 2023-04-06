package edu.mayo.kmdp.trisotechwrapper.config;

/**
 * Enumeration of Trisotech's native KRR Languages that are also supported by the TTW
 */
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
