package edu.mayo.kmdp.trisotechwrapper.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties( ignoreUnknown = true )
public class TrisotechExecutionArtifactData {
  private List<TrisotechExecutionArtifact> data;

  public List<TrisotechExecutionArtifact> getData() {
    return data;
  }

  public void setData(List<TrisotechExecutionArtifact> data) {
    this.data = data;
  }
}
