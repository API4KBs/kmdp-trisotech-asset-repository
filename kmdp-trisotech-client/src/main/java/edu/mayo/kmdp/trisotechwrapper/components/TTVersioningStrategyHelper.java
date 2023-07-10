package edu.mayo.kmdp.trisotechwrapper.components;

import static edu.mayo.kmdp.util.Util.isEmpty;
import static edu.mayo.kmdp.util.Util.isNotEmpty;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.logger;

import com.github.zafarkhaja.semver.Version;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechPublicationStates;
import java.util.Date;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Helper class that implement the version/status alignment for BPM+ models
 * <p>
 * Asset versions are asserted explicitly in the custom attribute. CalVer is the recommended
 * strategy, to capture the Year, approximate month and possibly day when a piece of knowledge was
 * made socially available. Asset are considered 'published' as soon as at least one carrier
 * artifact is available in draft, and must be published for any carrier to be published.
 * <p>
 * BPM+ Artifacts use SemVer, and the tags are aligned with the publication states. Published
 * Artifacts should have a stable version number. 'Pending Approval' are treated as 'Release
 * Candidates', and 'Drafts' are treated as SNAPSHOTS. RCs and SNAPSHOTS are decorated with a
 * timestamp from the publication date.
 * <p>
 * While the timestamp could be considered build info, the '+' does not work well with APIs
 * (/versionTag). For additional alignment with Maven -SNAPSHOTs are rewritten as -(timestamp),
 * considering the timestamp pre-release metadata. Note that the
 */
public class TTVersioningStrategyHelper {

  private TTVersioningStrategyHelper() {
    // static functions only
  }

  /**
   * Ensures that an artifact version tag (assigned manually) conforms to the usage pattern of
   * pre-release and build info
   *
   * @param artifactVersionTag       the version Tag of the Artifact
   * @param artifactPublicationState the publication state of the Artifact
   * @param artifactPublicationDate  the date and time when the publication state was assigned to
   *                                 the Artifact
   * @return the versionTag, with normalized pre-release and build metadata based on the state and
   * date
   * @see #normalizePublishedVersion(Version)
   * @see #normalizeRCVersion(Version, Date)
   * @see #normalizeSnapshotVersion(Version, Date)
   */
  @Nonnull
  public static String ensureArtifactVersionSemVerStyle(
      @Nonnull final String artifactVersionTag,
      @Nullable final String artifactPublicationState,
      @Nonnull final Date artifactPublicationDate) {
    var semver = Version.valueOf(artifactVersionTag);
    switch (TrisotechPublicationStates.parse(artifactPublicationState)) {
      case PUBLISHED:
        return normalizePublishedVersion(semver);
      case PENDING_APPROVAL:
        return normalizeRCVersion(semver, artifactPublicationDate);
      case DRAFT:
      case UNPUBLISHED:
      default:
        return normalizeSnapshotVersion(semver, artifactPublicationDate);
    }
  }

  @Nonnull
  private static String normalizeSnapshotVersion(
      @Nonnull final Version semver,
      @Nonnull final Date artifactPublicationDate) {
    Version normSemver = semver;
    if (isNotEmpty(normSemver.getBuildMetadata())) {
      logger.warn("Removing build info from draft/unpublished artifact version: {}", semver);
      normSemver = normSemver.setBuildMetadata(null);
    }
    if (isNotEmpty(normSemver.getPreReleaseVersion())) {
      logger.warn("Adding artifactPublicationDate to draft/unpublished artifact version: {}",
          semver);
      normSemver = normSemver.setPreReleaseVersion(
          normSemver.getPreReleaseVersion() + "." + artifactPublicationDate.getTime());
    } else {
      normSemver = normSemver.setPreReleaseVersion(Long.toString(artifactPublicationDate.getTime()));
    }
    return normSemver.toString();
  }

  @Nonnull
  private static String normalizeRCVersion(
      @Nonnull final Version semver,
      @Nonnull final Date artifactPublicationDate) {
    Version normSemver = semver;
    if (isNotEmpty(normSemver.getBuildMetadata())) {
      logger.warn("Removing build info from pre-published artifact version: {}", semver);
      normSemver = normSemver.setBuildMetadata(null);
    }
    if (isEmpty(normSemver.getPreReleaseVersion())) {
      logger.warn("Adding RC infor to pre-published artifact version: {}", semver);
      normSemver = semver.setPreReleaseVersion("RC");
    }
    normSemver = normSemver.setPreReleaseVersion(
        normSemver.getPreReleaseVersion() + "." + artifactPublicationDate.getTime());
    return normSemver.toString();
  }

  @Nonnull
  private static String normalizePublishedVersion(
      @Nonnull final Version semver) {
    if (isNotEmpty(semver.getBuildMetadata())) {
      logger.warn("Published artifact versions should not have build info: {}", semver);
    }
    if (isNotEmpty(semver.getPreReleaseVersion())) {
      logger.warn("Published artifact versions should not have pre-release info: {}", semver);
    }
    return Version.forIntegers(
            semver.getMajorVersion(),
            semver.getMinorVersion(),
            semver.getPatchVersion())
        .toString();
  }

}
