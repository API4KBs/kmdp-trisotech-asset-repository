package edu.mayo.kmdp.kdcaci.knew.trisotech;

import edu.mayo.kmdp.trisotechwrapper.TTAPIAdapter;
import edu.mayo.kmdp.trisotechwrapper.components.NamespaceManager;
import edu.mayo.kmdp.trisotechwrapper.components.SemanticModelInfo;
import edu.mayo.kmdp.trisotechwrapper.config.TTWEnvironmentConfiguration;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechPlace;
import edu.mayo.kmdp.util.XMLUtil;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.repository.artifact.v4.server.KnowledgeArtifactApiInternal;
import org.omg.spec.api4kp._20200801.api.repository.artifact.v4.server.KnowledgeArtifactRepositoryApiInternal;
import org.omg.spec.api4kp._20200801.api.repository.artifact.v4.server.KnowledgeArtifactSeriesApiInternal;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KPServer;
import org.omg.spec.api4kp._20200801.services.repository.KnowledgeArtifactRepository;
import org.omg.spec.api4kp._20200801.services.repository.artifact.KArtfHrefBuilder;
import org.omg.spec.api4kp._20200801.services.repository.artifact.KArtfHrefBuilder.HrefType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * Knowledge Artifact Repository backed by Trisotech DES Places
 */
@KPServer
@Component
public class TrisotechArtifactRepository implements KnowledgeArtifactRepositoryApiInternal,
    KnowledgeArtifactSeriesApiInternal, KnowledgeArtifactApiInternal {

  public static final String ALL_REPOS = "default";

  @Autowired
  TTWEnvironmentConfiguration cfg;

  @Autowired(required = false)
  KArtfHrefBuilder hrfefBuilder;

  @Autowired
  TTAPIAdapter client;

  @Autowired
  NamespaceManager names;

  /**
   * @return a list of {@link KnowledgeArtifactRepository} descriptors, one per configured Place
   */
  @Override
  public Answer<List<KnowledgeArtifactRepository>> listKnowledgeArtifactRepositories() {
    try {
      var repos = client.getCacheablePlaces().values().stream()
          .map(this::toRepositoryDescr)
          .collect(Collectors.toList());
      repos.add(0, allPlacesDescr());
      return Answer.of(repos);
    } catch (Exception e) {
      return Answer.failed(e);
    }
  }


  /**
   * Tests whether repositoryId is the Id of a configured repository (Place)
   *
   * @param repositoryId the tentative repository Id
   * @return success, if repositoryId is the Id of a configured Place
   */
  @Override
  public Answer<Void> isKnowledgeArtifactRepository(
      @Nonnull final String repositoryId) {
    try {
      return ALL_REPOS.equals(repositoryId)
          || client.getCacheablePlaces().containsKey(repositoryId)
          ? Answer.succeed()
          : Answer.notFound();
    } catch (Exception e) {
      return Answer.failed(e);
    }
  }

  /**
   * Looks up the repository (place) for the given Id, returning a descriptor
   *
   * @param repositoryId the Id of the repository
   * @return the descriptor of the given repository
   */
  @Override
  public Answer<KnowledgeArtifactRepository> getKnowledgeArtifactRepository(
      @Nonnull final String repositoryId) {
    try {
      if (ALL_REPOS.equals(repositoryId)) {
        return Answer.of(allPlacesDescr());
      }

      return Answer.ofNullable(client.getCacheablePlaces().get(repositoryId))
          .map(this::toRepositoryDescr);
    } catch (Exception e) {
      return Answer.failed(e);
    }
  }


  /**
   * Lists the Artifacts in a given Repository (place)
   * <p>
   * Supports basic pagination
   *
   * @param repositoryId the Repository to list
   * @param offset       skip ahead, default 0
   * @param limit        max num of results, default MAX_INT
   * @param deleted      not supported
   * @return the list of Artifacts in the Repository, as Pointers
   */
  @Override
  public Answer<List<Pointer>> listKnowledgeArtifacts(
      @Nonnull final String repositoryId,
      @Nullable final Integer offset,
      @Nullable final Integer limit,
      @Nullable final Boolean deleted) {
    try {
      var modelInfos = ALL_REPOS.equals(repositoryId)
          ? client.listModels()
          : client.listModelsByPlace(repositoryId);

      var artifactPtrs = modelInfos
          .map(info -> toPointer(info, false))
          .skip(offset != null ? offset : 0)
          .limit(limit != null ? limit : Integer.MAX_VALUE)
          .collect(Collectors.toList());
      return Answer.of(artifactPtrs);
    } catch (Exception e) {
      return Answer.failed(e);
    }
  }


  /**
   * Retrieves a copy of the artifact with a given Id, from the given repository
   *
   * @param repositoryId the repository Id
   * @param artifactId   the artifact Id
   * @param deleted      not supported
   * @return success if the model exists in the repository, not found otherwise
   */
  @Override
  public Answer<byte[]> getLatestKnowledgeArtifact(
      @Nonnull final String repositoryId,
      @Nonnull final UUID artifactId,
      @Nonnull final Boolean deleted) {
    try {
      var manifest = lookupArtifactInPlace(repositoryId, artifactId);
      return Answer.ofTry(manifest
          .flatMap(info -> client.getModel(info))
          .map(XMLUtil::toByteArray));
    } catch (Exception e) {
      return Answer.failed(e);
    }
  }


  /**
   * Lists the known versions of a given Artifact, if existing in a given repository
   *
   * @param repositoryId the Id of the repository
   * @param artifactId   the Id of the artifact
   * @param deleted      not supported
   * @param offset       skip ahead, default 0
   * @param limit        max num of results, default MAX_INT
   * @param beforeTag    not supported
   * @param afterTag     not supported
   * @param sort         not supported
   * @return a List of versions
   */
  @Override
  public Answer<List<Pointer>> getKnowledgeArtifactSeries(
      @Nonnull final String repositoryId,
      @Nonnull final UUID artifactId,
      @Nullable final Boolean deleted,
      @Nullable final Integer offset,
      @Nullable final Integer limit,
      @Nullable final String beforeTag,
      @Nullable final String afterTag,
      @Nullable final String sort) {
    try {
      var seed = lookupArtifactInPlace(repositoryId, artifactId);
      if (seed.isEmpty()) {
        return Answer.notFound();
      }

      var ptrs = client.getVersionsMetadataByModelId(names.artifactToModelId(artifactId)).stream()
          .map(info -> toPointer(new SemanticModelInfo(info, seed.get()), true))
          .skip(offset != null ? offset : 0)
          .limit(limit != null ? limit : Integer.MAX_VALUE)
          .sorted(Comparator.comparing(Pointer::getEstablishedOn).reversed())
          .collect(Collectors.toList());

      return Answer.of(ptrs);
    } catch (Exception e) {
      return Answer.failed(e);
    }
  }

  /**
   * Checks whether an Artifact with the given Id exists in the given repository
   *
   * @param repositoryId the repository Id
   * @param artifactId   the artifact Id
   * @param deleted      not supported
   * @return success if the model exists in the repository, not found otherwise
   */
  @Override
  public Answer<Void> isKnowledgeArtifactSeries(
      @Nonnull final String repositoryId,
      @Nonnull final UUID artifactId,
      @Nonnull final Boolean deleted) {
    try {
      return lookupArtifactInPlace(repositoryId, artifactId).isPresent()
          ? Answer.succeed()
          : Answer.notFound();
    } catch (Exception e) {
      return Answer.failed(e);
    }
  }

  /**
   * Retrieves a copy of the artifact with a given Id and version, from the given repository
   *
   * @param repositoryId the repository Id
   * @param artifactId   the artifact Id
   * @param versionTag   the artifact version
   * @param deleted      not supported
   * @return success if the model exists in the repository, not found otherwise
   */
  @Override
  public Answer<byte[]> getKnowledgeArtifactVersion(
      @Nonnull final String repositoryId,
      @Nonnull final UUID artifactId,
      @Nonnull final String versionTag,
      @Nonnull final Boolean deleted) {
    try {
      var latest = lookupArtifactInPlace(repositoryId, artifactId);
      if (latest.isEmpty()) {
        return Answer.notFound();
      }
      var model = latest
          .flatMap(info -> client.getModelByIdAndVersion(info.getId(), versionTag));
      return Answer.ofTry(
          model.map(XMLUtil::toByteArray));
    } catch (Exception e) {
      return Answer.failed(e);
    }
  }


  /**
   * Looks up an Artifact by Model Id, ensuring that it is located in the given Repoository
   *
   * @param repositoryId the Id of the repository
   * @param artifactId   the Id of the artifact
   * @return the Manifest of the model identified by artifactId, if it exists in the given
   * repository
   */
  protected Optional<SemanticModelInfo> lookupArtifactInPlace(
      @Nonnull final String repositoryId,
      @Nonnull final UUID artifactId) {
    return client.getMetadataByModelId(names.artifactToModelId(artifactId))
        .filter(info -> ALL_REPOS.equals(repositoryId) || repositoryId.equals(info.getPlaceId()));
  }

  /**
   * Generates a Pointer from a model Manifest
   *
   * @param info        the Model manifest
   * @param withVersion if true, points to the model version, else the artifact series
   * @return a Pointer referencing the Model
   */
  protected Pointer toPointer(
      @Nonnull final SemanticModelInfo info,
      final boolean withVersion) {
    var ptr = names.modelToArtifactId(info)
        .toPointer();

    if (hrfefBuilder != null) {
      var ref = withVersion
          ? hrfefBuilder.getHref(
          info.getPlaceId(), ptr.getTag(), ptr.getVersionTag(), HrefType.ARTIFACT_VERSION)
          : hrfefBuilder.getHref(
              info.getPlaceId(), ptr.getTag(), null, HrefType.ARTIFACT_SERIES);
      ptr.withHref(ref);
    }
    return ptr;
  }

  /**
   * Creates a KnowledgeArtifactRepository descriptor from a Place descriptor
   *
   * @param tp the Place descriptor
   * @return a KnowledgeArtifactRepository for the Place, as a repository
   */
  protected KnowledgeArtifactRepository toRepositoryDescr(
      @Nonnull final TrisotechPlace tp) {
    var descr = new KnowledgeArtifactRepository()
        .withId(SemanticIdentifier.newId(UUID.fromString(tp.getId())))
        .withName(tp.getName());
    if (hrfefBuilder != null) {
      descr.withHref(hrfefBuilder.getHref(tp.getId(), null, null, HrefType.REPO));
    }
    return descr;
  }

  private KnowledgeArtifactRepository allPlacesDescr() {
    var descr = new KnowledgeArtifactRepository()
        .withId(SemanticIdentifier.newId(ALL_REPOS))
        .withDefaultRepository(true)
        .withName("(ALL)");
    if (hrfefBuilder != null) {
      descr.withHref(
          hrfefBuilder.getHref(ALL_REPOS, null, null, HrefType.REPO));
    }
    return descr;
  }
}
