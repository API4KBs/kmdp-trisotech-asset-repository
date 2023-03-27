package edu.mayo.kmdp.trisotechwrapper.components.execution;

import static java.nio.charset.StandardCharsets.UTF_8;

import edu.mayo.kmdp.trisotechwrapper.config.TTWConfigParamsDef;
import edu.mayo.kmdp.trisotechwrapper.config.TTWEnvironmentConfiguration;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechExecutionArtifact;
import java.net.URI;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.springframework.web.util.UriUtils;

/**
 * Helper class that builds references (URLs) to the Services exposed via a TT ServiceLibrary,
 * either as OpenAPI specs or interactive API interfaces
 */
public final class ServiceLibraryHelper {

  /**
   * No-op Constructor. This class only contains static functions and should not be instantiated
   */
  private ServiceLibraryHelper() {
    // functions only
  }

  /**
   * Given a deployed Service's Execution manifest, builds a reference to the Service API UI
   *
   * @param exec the service execution manifest
   * @param cfg  the Environment configuration
   * @return a URL pointing to the Service's API UI, if the service is deployed
   */
  public static Optional<URI> tryResolveOpenApiUI(
      @Nonnull final TrisotechExecutionArtifact exec,
      @Nonnull final TTWEnvironmentConfiguration cfg) {
    return cfg.tryGetTyped(TTWConfigParamsDef.API_ENDPOINT)
        .map(base -> base + "doc/?url=/" + getEndpointUrl(exec, cfg))
        .map(URI::create);
  }

  /**
   * Given a deployed Service's Execution manifest, builds a reference to the Service OpenAPI spec
   *
   * @param exec the service execution manifest
   * @param cfg  the Environment configuration
   * @return a URL pointing to the Service's OpenAPI spec, if the service is deployed
   */
  public static Optional<URI> tryResolveOpenApiSpec(
      @Nonnull final TrisotechExecutionArtifact exec,
      @Nonnull final TTWEnvironmentConfiguration cfg) {
    return cfg.tryGetTyped(TTWConfigParamsDef.BASE_URL)
        .map(base -> base + getEndpointUrl(exec, cfg))
        .map(URI::create);
  }

  /**
   * Assembles the relative endpoint (path) for a specific version of a specific service deployed in
   * the Service Library.
   *
   * @param x   the execution artifact metadata
   * @param cfg the Environment configuration
   * @return a (relative) path to where the execution artifact is exposed
   */
  private static String getEndpointUrl(
      @Nonnull final TrisotechExecutionArtifact x,
      @Nonnull final TTWEnvironmentConfiguration cfg) {
    return "execution"
        + "/" + x.getLanguage()
        + "/api/openapi"
        + "/" + cfg.getTyped(TTWConfigParamsDef.SERVICE_LIBRARY_ENVIRONMENT)
        + "/" + x.getGroupId()
        + "/" + x.getArtifactId()
        + ("dmn".equals(x.getLanguage()) ? "/" + UriUtils.encode(x.getName(), UTF_8) : "")
        + "/" + x.getVersion();
  }
}
