package edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors;

import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.defaultArtifactId;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.BPMN_2_0;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.CMMN_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.DMN_1_2;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.HTML;

import edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.KommunicatorHelper.KommunicatorDescr.FileDescr;
import edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.KommunicatorHelper.KommunicatorDescr.RepoDescr;
import edu.mayo.kmdp.trisotechwrapper.config.TTWEnvironmentConfiguration;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.util.JSonUtil;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
@Component
public class KommunicatorHelper {

  private static final String KOMM_BASE = "/kommunicator/#";
  private static final String KOMM_ENDPOINT = "model=";

  @Autowired
  TTWEnvironmentConfiguration ttEnv;

  /**
   * If possible, adds an optional KnowledgeCarrier that references the Kommunicator version of a
   * BPM+ model: an interactive, read-only, animatable variant of the model
   *
   * @param helper   an instance of {@link KommunicatorHelper}, injected with a
   *                 {@link TTWEnvironmentConfiguration}
   * @param manifest the internal manifest of the model to be referenced
   * @param surr     the base {@link KnowledgeAsset} surrogate
   */
  public static void tryAddKommunicatorLink(
      KommunicatorHelper helper,
      TrisotechFileInfo manifest,
      KnowledgeAsset surr) {
    if (helper != null) {
      helper.getKommunicatorLink(manifest)
          .ifPresent(link ->
              surr.withCarriers(new KnowledgeArtifact()
                  .withArtifactId(makeArtifactId(surr.getAssetId(), manifest.getMimetype()))
                  .withName(manifest.getName().trim() + " -- Kommunicator Preview")
                  .withLocator(link)));
    }
  }

  private static ResourceIdentifier makeArtifactId(ResourceIdentifier assetId, String mimetype) {
    KnowledgeRepresentationLanguage baseLang;
    if (mimetype.contains("bpmn")) {
      baseLang = BPMN_2_0;
    } else if (mimetype.contains("cmmn")) {
      baseLang = CMMN_1_1;
    } else {
      baseLang = DMN_1_2;
    }
    var id = defaultArtifactId(assetId, baseLang);
    return defaultArtifactId(id, HTML);
  }

  /**
   * Tries to build a Kommunicator link that is specific to a given model.
   * <p>
   * Constructs the complex metadata link object, Base64-encodes it, and adds it to the Kommunicator
   * base URL
   *
   * @param manifest the internal manifest of the model to be referenced
   * @return an Optional Kommunicator URL
   * @see #getKommunicatorMetadata(TrisotechFileInfo)
   * @see #buildURI(String)
   */
  public Optional<URI> getKommunicatorLink(TrisotechFileInfo manifest) {
    return getKommunicatorMetadata(manifest)
        .flatMap(JSonUtil::printJson)
        .map(jstr -> Base64.getEncoder().encodeToString(jstr.getBytes(StandardCharsets.UTF_8)))
        .map(this::buildURI);
  }

  /**
   * Constructs a Kommunicator URI (techically a URL, but in the form or URI for API compatibility),
   * from the server's base URL, the Kommunicator path, and the encoded metadata object reference
   *
   * @param encodedRef the model-specific, Base64 encoded metadata reference
   * @return a Kommunicator URL
   */
  private URI buildURI(String encodedRef) {
    String sb = ttEnv.getBaseURL()
        + KOMM_BASE
        + KOMM_ENDPOINT
        + encodedRef;
    return URI.create(sb);
  }

  /**
   * Uses a Trisotech model manifest to build a Kommunicator reference object.
   *
   * @param manifest the internal manifest of the model to be referenced
   * @return a {@link KommunicatorDescr} object, or {@link Optional#empty()} if any of the necessary
   * metadata is not available
   */
  private Optional<KommunicatorDescr> getKommunicatorMetadata(TrisotechFileInfo manifest) {
    var f = getFileDescr(manifest);
    var r = getRepoDescr();
    return f.flatMap(fd ->
        r.map(rd ->
            new KommunicatorDescr(fd, rd)));
  }

  /**
   * Build the repository component of a {@link KommunicatorDescr} using the injected
   * {@link TTWEnvironmentConfiguration}
   *
   * @return a {@link RepoDescr}, of {@link Optional#empty()} if any of the necessary metadata is
   * missing
   */
  private Optional<RepoDescr> getRepoDescr() {
    if (ttEnv == null) {
      return Optional.empty();
    }
    var api = ttEnv.getApiEndpoint();
    if (api.isPresent()) {
      var rd = new RepoDescr(
          ttEnv.getRepositoryName(),
          // ttEnv.getToken(), // do not include the API key - not necessary with SSO
          null,
          api.get(),
          ttEnv.getRepositoryId()
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
   * @return a {@link FileDescr}, of {@link Optional#empty()} if any of the necessary metadata is
   * missing
   */
  private Optional<FileDescr> getFileDescr(TrisotechFileInfo manifest) {
    if (manifest == null) {
      return Optional.empty();
    }
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
