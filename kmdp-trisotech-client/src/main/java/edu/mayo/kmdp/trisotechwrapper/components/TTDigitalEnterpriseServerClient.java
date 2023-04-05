package edu.mayo.kmdp.trisotechwrapper.components;

import edu.mayo.kmdp.trisotechwrapper.models.TrisotechExecutionArtifactData;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechPlaceData;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.apache.http.HttpException;
import org.apache.jena.query.Query;
import org.apache.jena.query.ResultSet;
import org.w3c.dom.Document;

public interface TTDigitalEnterpriseServerClient {

  List<TrisotechFileInfo> getModelPreviousVersions(
      String repositoryId,
      String fileId);

  Optional<TrisotechPlaceData> getPlaces() throws IOException;

  Optional<Document> downloadXmlModel(TrisotechFileInfo from);

  Optional<TrisotechExecutionArtifactData> getExecutionArtifacts(String execEnv)
      throws IOException;

  void uploadXmlModel(String repositoryId, String path, String name,
      String mimeType, String version, String state,
      byte[] fileContents)
      throws IOException, HttpException;

  ResultSet askQuery(Query query);
}
