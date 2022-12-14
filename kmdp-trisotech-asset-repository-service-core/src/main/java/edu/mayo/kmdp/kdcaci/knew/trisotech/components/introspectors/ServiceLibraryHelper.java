package edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors;

import edu.mayo.kmdp.trisotechwrapper.TrisotechWrapper;
import edu.mayo.kmdp.trisotechwrapper.config.TTWEnvironmentConfiguration;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechExecutionArtifact;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ServiceLibraryHelper {

  @Autowired
  TrisotechWrapper client;

  @Autowired
  private TTWEnvironmentConfiguration envConfig;

  public Optional<TrisotechExecutionArtifact> checkIsDeployed(TrisotechFileInfo info) {
    var exec = client.listExecutionArtifacts(envConfig.getExecutionEnvironment())
        .get(info.getName());
    return Optional.ofNullable(exec);
  }

  public Optional<URI> tryResolveSwagger(TrisotechFileInfo manifest) {
    return checkIsDeployed(manifest)
        .flatMap(this::buildSwaggerUI);
  }

  private Optional<URI> buildSwaggerUI(TrisotechExecutionArtifact x) {
    var publicApi = envConfig.getApiEndpoint();
    if (publicApi.isEmpty()) {
      return Optional.empty();
    }

    var url = publicApi.get()
        + "doc/?url=/"
        + getOpenAPIUrl(x);
    return Optional.of(URI.create(url));
  }

  public Optional<URI> tryResolveOpenAPI(TrisotechFileInfo manifest) {
    return checkIsDeployed(manifest)
        .map(this::buildOpenAPI);
  }

  private URI buildOpenAPI(TrisotechExecutionArtifact x) {
    var url = envConfig.getBaseURL() + getOpenAPIUrl(x);
    return URI.create(url);
  }

  private String getOpenAPIUrl(TrisotechExecutionArtifact x) {
    return "execution"
        + "/" + x.getLanguage()
        + "/api/openapi"
        + "/" + envConfig.getExecutionEnvironment()
        + "/" + x.getGroupId()
        + "/" + x.getArtifactId()
        + "/" + URLEncoder.encode(x.getName(), StandardCharsets.UTF_8)
        + "/" + x.getVersion();
  }
}
