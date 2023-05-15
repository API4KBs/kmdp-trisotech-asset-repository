package edu.mayo.kmdp.trisotechwrapper.components.execution;

import edu.mayo.kmdp.trisotechwrapper.models.TrisotechExecutionArtifact;
import java.net.URI;
import java.util.Optional;
import javax.annotation.Nonnull;

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
   * @return a URL pointing to the Service's API UI, if the service is deployed
   */
  @Nonnull
  public static Optional<URI> tryResolveOpenApiUI(
      @Nonnull final TrisotechExecutionArtifact exec) {
    return tryResolveArtifact(exec, "form");
  }

  /**
   * Given a deployed Service's Execution manifest, builds a reference to the Service OpenAPI spec
   *
   * @param exec the service execution manifest
   * @return a URL pointing to the Service's OpenAPI spec, if the service is deployed
   */
  @Nonnull
  public static Optional<URI> tryResolveOpenApiSpec(
      @Nonnull final TrisotechExecutionArtifact exec) {
    return tryResolveArtifact(exec, "api/openapi");
  }

  /**
   * Given a deployed Service's Execution manifest, builds a reference to one of its variants
   *
   * @param exec the service execution manifest
   * @param mode the kind of artifact to reference
   * @return a URL pointing to the Service's variant form, if the service is deployed
   */
  @Nonnull
  public static Optional<URI> tryResolveArtifact(
      @Nonnull final TrisotechExecutionArtifact exec,
      @Nonnull final String mode) {
    return Optional.ofNullable(exec.getContainer())
        .map(ctr -> {
          var base = ctr.substring(0, ctr.indexOf('/', 10));
          var ep = base + getEndpointUrl(exec, mode);
          return URI.create(ep);
        });
  }

  /**
   * Assembles the relative endpoint (path) for a specific version of a specific service deployed in
   * the Service Library.
   * <p>
   * Supports: OpenAPI specs ('/api/openapi'), and interactive forms ('forms')
   *
   * @param x    the execution artifact metadata
   * @param mode the kind of path to build
   * @return a (relative) path to where the execution artifact is exposed
   */
  @Nonnull
  private static String getEndpointUrl(
      @Nonnull final TrisotechExecutionArtifact x,
      @Nonnull final String mode) {
    return "/execution"
        + "/" + x.getLanguage()
        + "/" + mode
        + "/" + x.getEnvironment()
        + "/" + x.getGroupId()
        + "/" + x.getArtifactId()
        + "/" + x.getVersion();
  }
}
