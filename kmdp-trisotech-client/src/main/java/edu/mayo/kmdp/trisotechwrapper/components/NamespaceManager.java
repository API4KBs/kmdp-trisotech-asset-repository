package edu.mayo.kmdp.trisotechwrapper.components;

import edu.mayo.kmdp.trisotechwrapper.config.TTWConfigParamsDef;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import java.net.URI;
import java.util.Date;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.omg.spec.api4kp._20200801.id.KeyIdentifier;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;

/**
 * Adapter class used to rewrite the local/proprietary asset/artifact URIs to use the configured
 * enterprise namespaces instead of the proprietary ones set by the authoring tool and/or the model
 * authors
 * <p>
 * Artifact Ids are mapped from the UUID-based model Ids, preserving the UUID, plus the optional
 * model version and date, but rewriting them to use the public Artifact namespace.
 * <p>
 * Asset Ids are asserted by the modelers, and may consist of a full URI, or a UUID, with a
 * Sem/Calver version tag. Asst Ids are likewise rewritten.
 */
public interface NamespaceManager {

  /**
   * @return the configured Asset base URI
   * @see TTWConfigParamsDef#ASSET_NAMESPACE
   */
  URI getAssetNamespace();

  /**
   * @return the configured Artifact base URI
   * @see TTWConfigParamsDef#ASSET_NAMESPACE
   */
  URI getArtifactNamespace();


  /**
   * @param assetKey the UUID/version pair that identifies the Asset
   * @return the Asset Id for a given Asset Key
   */
  ResourceIdentifier assetKeyToId(
      @Nonnull final KeyIdentifier assetKey);

  /**
   * @param info the Model manifest, which may contain the Asset Id asserted at modeling time
   * @return the Asset Id associated to a given Model, if any
   */
  Optional<ResourceIdentifier> modelToAssetId(
      @Nonnull final SemanticModelInfo info);

  /**
   * @param info the Model manifest, which contains the original Model (Artifact) Id
   * @return the Artifact Id associated to a given Model, if any
   */
  ResourceIdentifier modelToArtifactId(
      @Nonnull final TrisotechFileInfo info);

  /**
   * Builds an Artifact Id from a model URI, a Model version, and a label
   *
   * @param modelUri   the internal Id of the Model
   * @param versionTag the version of the Model
   * @param label      the name of the Model
   * @return an artifact Id
   */
  default ResourceIdentifier modelToArtifactId(
      @Nonnull String modelUri,
      @Nonnull String versionTag,
      @Nonnull String label) {
    return modelToArtifactId(modelUri, versionTag, label, new Date());
  }


  /**
   * Builds an Artifact Id from a model URI, a Model version, a label and a last update Date
   *
   * @param modelUri      the internal Id of the Model
   * @param versionTag    the version of the Model
   * @param label         the name of the Model
   * @param establishedOn the Date when this version was last touched
   * @return an artifact Id
   */
  ResourceIdentifier modelToArtifactId(
      @Nonnull final String modelUri,
      @Nullable final String versionTag,
      @Nullable final String label,
      @Nullable final Date establishedOn);


  /**
   * Predicate
   *
   * @param baseURI the URI to be tested
   * @return true if baseURI is a domain concept namespace
   */
  boolean isDomainConcept(
      @Nonnull final String baseURI);

  /**
   * Predicate
   *
   * @param baseURI the URI to be tested
   * @return true if baseURI is a domain concept namespace
   */
  default boolean isDomainConcept(
      @Nullable final URI baseURI) {
    return baseURI != null && isDomainConcept(baseURI.toString());
  }

}
