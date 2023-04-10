package edu.mayo.kmdp.trisotechwrapper.components.execution;

import edu.mayo.kmdp.trisotechwrapper.components.SemanticModelInfo;
import edu.mayo.kmdp.trisotechwrapper.components.execution.KommunicatorHelper.KommunicatorDescr.FileDescr;
import edu.mayo.kmdp.trisotechwrapper.components.execution.KommunicatorHelper.KommunicatorDescr.RepoDescr;
import edu.mayo.kmdp.trisotechwrapper.config.TTWConfigParamsDef;
import edu.mayo.kmdp.trisotechwrapper.config.TTWEnvironmentConfiguration;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.util.JSonUtil;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import javax.annotation.Nonnull;

/**
 * Helper class to build links to the Kommunicator pre/view of a TT BPM+ model
 * <p>
 * The (implicit) Kommunicator API relies on a URL of the form
 * </p>
 * base/kommunicator/#model=REF
 * </p>
 * where REF is a Base64-encoded JSON object that contains the actual reference to the model to be
 * opened. Note that the payload may contain the API key (excluded in this implementation because it
 * is not needed with SSO), which makes the links potentially sensitive
 */
public final class KommunicatorHelper {

  private static final String KOMM_BASE = "/kommunicator/#";
  private static final String KOMM_ENDPOINT = "model=";

  /**
   * No-op Constructor. This class only contains static functions and should not be instantiated
   */
  private KommunicatorHelper() {
    // functions only
  }

  /**
   * Tries to build a Kommunicator link that is specific to a given model.
   * <p>
   * Constructs the complex metadata link object, Base64-encodes it, and adds it to the Kommunicator
   * base URL
   *
   * @param manifest the internal manifest of the model to be referenced
   * @param cfg      the environment configuration
   * @return an Optional Kommunicator URL
   */
  @Nonnull
  public static Optional<URI> getKommunicatorLink(
      @Nonnull final SemanticModelInfo manifest,
      @Nonnull final TTWEnvironmentConfiguration cfg) {
    return getKommunicatorMetadata(manifest, cfg)
        .flatMap(JSonUtil::printJson)
        .map(jStr -> Base64.getEncoder().encodeToString(jStr.getBytes(StandardCharsets.UTF_8)))
        .map(x -> buildURI(x, cfg));
  }

  /**
   * Constructs a Kommunicator URI (technically a URL, but in the form or URI for API
   * compatibility), from the server's base URL, the Kommunicator path, and the encoded metadata
   * object reference
   *
   * @param encodedRef the model-specific, Base64 encoded metadata reference
   * @param cfg        the Environment Configuration, providing the DES base URL
   * @return a Kommunicator URL
   */
  @Nonnull
  private static URI buildURI(
      @Nonnull final String encodedRef,
      @Nonnull final TTWEnvironmentConfiguration cfg) {
    String sb = cfg.getTyped(TTWConfigParamsDef.BASE_URL)
        + KOMM_BASE
        + KOMM_ENDPOINT
        + encodedRef;
    return URI.create(sb);
  }

  /**
   * Uses a Model manifest to build a Kommunicator reference object.
   *
   * @param manifest the manifest of the model to be referenced
   * @param cfg      the Environment Configuration, providing the DES API endpoint
   * @return a {@link KommunicatorDescr} object, if all the necessary metadata is provided
   */
  @Nonnull
  public static Optional<KommunicatorDescr> getKommunicatorMetadata(
      @Nonnull final SemanticModelInfo manifest,
      @Nonnull final TTWEnvironmentConfiguration cfg) {
    var f = getFileDescr(manifest);
    var r = getRepoDescr(manifest, cfg);
    return f.flatMap(fd ->
        r.map(rd ->
            new KommunicatorDescr(fd, rd)));
  }

  /**
   * Build the repository component of a {@link KommunicatorDescr} using the provided
   * {@link TTWEnvironmentConfiguration}
   *
   * @param manifest the manifest of the model to be referenced
   * @param cfg      the Environment Configuration, providing the DES API endpoint
   * @return a {@link RepoDescr}, if all the necessary metadata is provided
   */
  @Nonnull
  private static Optional<RepoDescr> getRepoDescr(
      @Nonnull final SemanticModelInfo manifest,
      @Nonnull final TTWEnvironmentConfiguration cfg) {
    var api = cfg.tryGetTyped(TTWConfigParamsDef.API_ENDPOINT, URI.class);
    if (api.isPresent()) {
      var rd = new RepoDescr(
          manifest.getPlaceName(),
          null, // ttEnv.getToken(), // do not include the API key - not necessary with SSO
          api.get().toString(),
          manifest.getPlaceId()
      );
      return Optional.of(rd);
    }
    return Optional.empty();
  }

  /**
   * Build the model file component of a {@link KommunicatorDescr} using the given
   * {@link TrisotechFileInfo}
   *
   * @param manifest the internal manifest of the model to be referenced
   * @return a {@link FileDescr}, if all the necessary metadata is provided
   */
  @Nonnull
  private static Optional<FileDescr> getFileDescr(
      @Nonnull final TrisotechFileInfo manifest) {
    var fd = new FileDescr(
        manifest.getPath(),
        manifest.getSku(),
        manifest.getName(),
        manifest.getMimetype()
    );
    return Optional.of(fd);
  }


  /**
   * Helper bean that mimics the structure of a Kommunicator metadata object. Composed of a
   * Model/File specific component, and a Repository/Place specific component
   */
  static class KommunicatorDescr {

    public final FileDescr f;
    public final RepoDescr r;
    public final String t;

    public KommunicatorDescr(FileDescr f, RepoDescr r) {
      this.f = f;
      this.r = r;
      this.t = "publicapi";
    }

    static class FileDescr {

      public final String path;
      public final String sku;
      public final String name;
      public final String mimetype;

      public FileDescr(String path, String sku, String name, String mimetype) {
        this.path = path;
        this.sku = sku;
        this.name = name;
        this.mimetype = mimetype;
      }
    }


    static class RepoDescr {

      public final String name;
      public final String apikey;
      public final String url;
      public final String repositoryId;

      public RepoDescr(String name, String apikey, String url, String repositoryId) {
        this.name = name;
        this.apikey = apikey;
        this.url = url;
        this.repositoryId = repositoryId;
      }
    }
  }
}
