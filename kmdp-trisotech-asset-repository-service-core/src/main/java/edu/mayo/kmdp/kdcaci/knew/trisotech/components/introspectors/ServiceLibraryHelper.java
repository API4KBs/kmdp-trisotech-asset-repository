package edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors;

import static java.nio.charset.StandardCharsets.UTF_8;

import edu.mayo.kmdp.trisotechwrapper.TrisotechWrapper;
import edu.mayo.kmdp.trisotechwrapper.config.TTWEnvironmentConfiguration;
import edu.mayo.kmdp.trisotechwrapper.config.TTWParams;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechExecutionArtifact;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import java.net.URI;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

/**
 * Helper class that can query the status, and/or builds references (URLs), to the resources exposed
 * via a combination of the Trisotech public API and the ServiceLibrary
 * <p>
 * The former is used for discovery of the deployed artifacts, whose metadata is necessary to build
 * the URLs pointing to the latter, where the runtime environments can be accessed.
 */
@Component
public class ServiceLibraryHelper {

  @Autowired
  TrisotechWrapper client;

  @Autowired
  private TTWEnvironmentConfiguration envConfig;

  /**
   * Determines whether a Service is deployed to an execution environment
   * <p>
   * Note that this implementation targets one specific execution environment, which is configurable
   * but not at runtime. FUTURE?
   *
   * @param serviceName the internal name of the service
   * @param manifest    the artifact metadata of the model exposed as a service
   * @return a descriptor of the deployed artifact, if the service is deployed
   */
  public Optional<TrisotechExecutionArtifact> checkIsDeployed(String serviceName,
      TrisotechFileInfo manifest) {
    var execs = client.listExecutionArtifacts(
        envConfig.getTyped(TTWParams.SERVICE_LIBRARY_ENVIRONMENT));
    // DMN executables - the whole model is mapped to a service
    return Optional.ofNullable(execs.get(manifest.getName()))
        // BPMN executables - each process is mapped to a service
        .or(() -> Optional.ofNullable(execs.get(serviceName)));
  }

  /**
   * @param serviceName the internal name of the service
   * @param manifest    the artifact metadata of the model exposed as a service
   * @return a URL pointing to the Service's Swagger UI, if the service is deployed
   */
  public Optional<URI> tryResolveSwaggerUI(String serviceName, TrisotechFileInfo manifest) {
    return checkIsDeployed(serviceName, manifest)
        .flatMap(x -> buildSwaggerUserInterfaceURL(x, serviceName));
  }

  private Optional<URI> buildSwaggerUserInterfaceURL(
      TrisotechExecutionArtifact x, String serviceName) {
    var publicApi = envConfig.tryGetTyped(TTWParams.API_ENDPOINT);
    if (publicApi.isEmpty()) {
      return Optional.empty();
    }

    var url = publicApi.get()
        + "doc/?url=/"
        + getEndpointUrl(x, serviceName);
    return Optional.of(URI.create(url));
  }


  /**
   * @param serviceName the internal name of the service
   * @param manifest    the artifact metadata of the model exposed as a service
   * @return a URL pointing to the Service's OpenAPI spec, if the service is deployed
   */
  public Optional<URI> tryResolveOpenAPIspec(String serviceName, TrisotechFileInfo manifest) {
    return checkIsDeployed(serviceName, manifest)
        .map(x -> buildOpenAPIspecURL(x, serviceName));
  }

  private URI buildOpenAPIspecURL(TrisotechExecutionArtifact x, String serviceName) {
    var url = envConfig.getTyped(TTWParams.BASE_URL) + getEndpointUrl(x, serviceName);
    return URI.create(url);
  }

  /**
   * Assembles the relative endpoint (path) for a specific version of a specific service deployed in
   * the Service Library.
   *
   * @param x           the execution artifact metadata
   * @param serviceName the name of the service
   * @return a (relative) path to where the execution artifact is exposed
   */
  private String getEndpointUrl(TrisotechExecutionArtifact x, String serviceName) {
    return "execution"
        + "/" + x.getLanguage()
        + "/api/openapi"
        + "/" + envConfig.getTyped(TTWParams.SERVICE_LIBRARY_ENVIRONMENT)
        + "/" + x.getGroupId()
        + "/" + x.getArtifactId()
        + ("dmn".equals(x.getLanguage()) ? "/" + UriUtils.encode(serviceName, UTF_8) : "")
        + "/" + x.getVersion();
  }
}
