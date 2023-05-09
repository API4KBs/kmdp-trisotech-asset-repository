package edu.mayo.kmdp.trisotechwrapper.components;

import static edu.mayo.kmdp.trisotechwrapper.components.TTVersioningStrategyHelper.ensureArtifactVersionSemVerStyle;
import static edu.mayo.kmdp.trisotechwrapper.config.TTConstants.TT_BASE_MODEL_URI;
import static edu.mayo.kmdp.util.DateTimeUtil.parseDateTime;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newId;

import edu.mayo.kmdp.trisotechwrapper.config.TTWConfigParamsDef;
import edu.mayo.kmdp.trisotechwrapper.config.TTWEnvironmentConfiguration;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import java.net.URI;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.omg.spec.api4kp._20200801.id.KeyIdentifier;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;

/**
 * Default implementation of {@link NamespaceManager}
 */
public class DefaultNamespaceManager implements NamespaceManager {

  @Nonnull
  private final Pattern domainConceptPattern;
  @Nonnull
  private final URI artifactNamespace;
  @Nonnull
  private final URI assetNamespace;

  private final String defaultVersion;

  public DefaultNamespaceManager(
      @Nonnull TTWEnvironmentConfiguration config) {
    defaultVersion = config.getTyped(TTWConfigParamsDef.DEFAULT_VERSION_TAG);
    domainConceptPattern =
        Pattern.compile(config.getTyped(TTWConfigParamsDef.DOMAIN_TERMS_NAMESPACE_PATTERN));
    artifactNamespace = config.getTyped(TTWConfigParamsDef.ARTIFACT_NAMESPACE);
    assetNamespace = config.getTyped(TTWConfigParamsDef.ASSET_NAMESPACE);
  }


  @Override
  @Nonnull
  public URI getAssetNamespace() {
    return assetNamespace;
  }

  @Override
  @Nonnull
  public URI getArtifactNamespace() {
    return artifactNamespace;
  }


  @Override
  @Nonnull
  public Optional<ResourceIdentifier> modelToAssetId(
      @Nonnull final SemanticModelInfo info) {
    return Optional.ofNullable(info.getAssetKey())
        .map(this::assetKeyToId);
  }

  @Override
  @Nonnull
  public ResourceIdentifier assetKeyToId(
      @Nonnull final KeyIdentifier assetKey) {
    return newId(getAssetNamespace(), assetKey.getUuid(), assetKey.getVersionTag());
  }

  @Override
  @Nonnull
  public ResourceIdentifier modelToArtifactId(
      @Nonnull final TrisotechFileInfo info) {
    return modelToArtifactId(
        info.getId(),
        info.getVersion(),
        info.getName(),
        info.getState(),
        parseDateTime(info.getUpdated())
    );
  }


  @Override
  @Nonnull
  public ResourceIdentifier modelToArtifactId(
      @Nonnull final String internalId,
      @Nullable final String versionTag,
      @Nullable final String label,
      @Nullable final String state,
      @Nullable final Date establishedOn) {
    // fast way to determine if internalId is a full URI, or a UUID tag
    String artifactUUID = internalId.charAt(0) == TT_BASE_MODEL_URI.charAt(0)  // == 'h'
        ? internalId.substring(TT_BASE_MODEL_URI.length())
        : internalId;
    String vTag = versionTag != null ? versionTag : defaultVersion;
    Date timestamp = establishedOn != null ? establishedOn : new Date();

    String stampedVersionTag = ensureArtifactVersionSemVerStyle(vTag, state, timestamp);
    return newId(getArtifactNamespace(), artifactUUID, stampedVersionTag)
        .withName(label)
        .withEstablishedOn(timestamp);
  }

  @Override
  @Nonnull
  public String artifactToModelId(
      @Nonnull UUID artifactId) {
    return TT_BASE_MODEL_URI + artifactId;
  }


  /**
   * {@inheritDoc}
   * <p>
   * Matches the baseUri against a configurable RegEx
   */
  @Override
  public boolean isDomainConcept(
      @Nullable final String baseURI) {
    if (baseURI == null) {
      return false;
    }
    return domainConceptPattern.matcher(baseURI.toLowerCase()).matches();
  }

}
