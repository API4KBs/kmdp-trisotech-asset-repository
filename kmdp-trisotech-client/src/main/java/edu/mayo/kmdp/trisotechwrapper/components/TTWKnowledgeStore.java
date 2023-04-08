package edu.mayo.kmdp.trisotechwrapper.components;

import edu.mayo.kmdp.trisotechwrapper.config.TTNotations;
import edu.mayo.kmdp.trisotechwrapper.config.TTWConfigParamsDef;
import edu.mayo.kmdp.trisotechwrapper.config.TTWEnvironmentConfiguration;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechPlace;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.UUID;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.omg.spec.api4kp._20200801.id.KeyIdentifier;
import org.w3c.dom.Document;

/**
 * TTW Knowledge Stores provide access to Models, and Manifests/Metadata thereof.
 * <p>
 * Models are curated and stored in the TT DES as native expressions, exported as
 * standards-compliant Knowledge Artifacts, and described by 'Info' manifests.
 * <p>
 * All models have a unique modelId (a UUID-based URI) set by DES, and may have a user assigned
 * Asset (Series) Identifiers. If enabled via ({@link TTWConfigParamsDef#ANONYMOUS_ASSETS_FLAG)},
 * placeholder asset Ids will be assigned by the TTW. Either ID is mapped to an 'Info' manifest, *
 * which is based on a Shape view of the DES Knowledge Graph.
 * <p>
 * Some Models (Decision and/or Process) expose one or more Services. Services are also treated as
 * Assets, with an implicit manifestation as an OpenAPI spec document. The Service Assets are
 * accessed indirectly through the ID of the model that exposes them, or through their Asset ID.
 * <p>
 * Info metadata is used to negotiate (via mimeType) and ultimately acquire a copy of a Model
 * Artifact, as an XML Document, sanitized of vendor-specific elements, to be replaced with
 * standards-based one.
 */
public interface TTWKnowledgeStore {

  /**
   * Returns manifests for all available Models, possibly filtering by MIME type.
   * <p>
   * Implementations should filter models based on classes of equivalence of MIME types, based on
   * language but not format. For example, {@link TTNotations#CMMN_11_XML} and
   * {@link TTNotations#CMMN_JSON} should be considered equivalent, because the specific format can
   * be negotiated when retrieving the actual Model.
   * <p>
   * If no mimeType is provided, no filtering should be applied.
   *
   * @param mimeType an optional mimeType filter, as the representative of a MIME class of
   *                 equivalent types
   * @return Manifests for all available Models, possibly filtered, as a Stream
   */
  @Nonnull
  Stream<SemanticModelInfo> listAllModelsInfoByMimeClass(
      @Nullable final String mimeType);

  /**
   * Returns manifests for all available Models in a given Place, possibly filtering by MIME type.
   * <p>
   * If no mimeType is provided, no filtering should be applied.
   *
   * @param placeId  the placeId
   * @param mimeType an optional mimeType filter, as the representative of a MIME class of
   *                 equivalent types
   * @return Manifests for all available Models in the Place, possibly filtered, as a Stream
   */
  @Nonnull
  Stream<SemanticModelInfo> listAllModelsInfoByPlaceAndMimeClass(
      @Nonnull final String placeId,
      @Nullable final String mimeType);

  /**
   * Retrieves the Manifest for the Model with a given model ID
   *
   * @param modelUri the ID of the Model
   * @return the metadata manifest for the model with the given ID, if any
   */
  @Nonnull
  Optional<SemanticModelInfo> getMetadataByArtifact(
      @Nonnull final String modelUri);

  /**
   * Retrieves the Manifest for the Models with a given Asset Version ID.
   * <p>
   * Note that a given Asset may be carried by multiple, distinct Models, all of which should be
   * returned.
   *
   * @param assetId the ID of an Asset Version
   * @return the metadata manifest for all the Models which carry that Asset Version, as a Stream
   */
  @Nonnull
  Stream<SemanticModelInfo> getMetadataByAssetVersion(
      @Nonnull final KeyIdentifier assetId);

  /**
   * Retrieves the Manifests for the Models with a given Asset ID.
   *
   * @param assetId the ID of an Asset Series
   * @return the metadata manifest for all the Models which carry that Asset, as a Map indexed by
   * Asset version
   */
  @Nonnull
  Map<KeyIdentifier, SortedSet<SemanticModelInfo>> getMetadataByAsset(
      @Nonnull final UUID assetId);

  /**
   * Retrieves the Manifests for the Models with the GREATEST version a given Asset ID.
   * <p>
   * Assumes asset version tags to follow the SemVer/CalVer paradigm, sorts according to that, and
   * returns the greatest
   *
   * @param assetId the ID of an Asset Series
   * @return the metadata manifest for all the Models which carry the greatest version of a given
   * Asset Series
   */
  @Nonnull
  Stream<SemanticModelInfo> getMetadataByGreatestAsset(
      @Nonnull final UUID assetId);

  /**
   * Retrieves the Manifest of the Services exposed by the Model with a given ID
   *
   * @param modelUri the ID of the Model
   * @return the metadata manifest for the Service(s) exposed by the model, if any
   */
  @Nonnull
  Stream<SemanticModelInfo> getServiceMetadataByModel(
      @Nonnull final String modelUri);

  /* ---------------------------------------------------------------------------------------- */


  /**
   * Given a Model Manifest, retrieves a copy of the described Model.
   * <p>
   * Implementations should use the modelId, mimeType and other information in the manifest to
   * interact with the DES API. Clients can further use a mimeType to negotiate the form of the
   * Model, if supported and the required format does not match the default one stated in the
   * manifest.
   *
   * @param info     the Manifest of the Model
   * @param mimeType an optional mimeType used to negotiate
   * @return the Model as a Document, if possible
   */
  @Nonnull
  Optional<Document> downloadXmlModel(
      @Nonnull final TrisotechFileInfo info,
      @Nullable final String mimeType);

  /**
   * Given a Model Manifest, retrieves a copy of the described Model, in its default form.
   *
   * @param info the Manifest of the Model
   * @return the Model as a Document, if possible
   * @see #downloadXmlModel(TrisotechFileInfo, String)
   */
  @Nonnull
  default Optional<Document> downloadXmlModel(TrisotechFileInfo info) {
    return downloadXmlModel(info, null);
  }

  /* ---------------------------------------------------------------------------------------- */

  /**
   * Based on the {@link TTWEnvironmentConfiguration}, returns a Map associating Places to the Paths
   * within those Places, such that this KnowledgeStore will only expose Models from those Paths
   *
   * @return the Map of configured Path/Places
   */
  @Nonnull
  Map<TrisotechPlace, Set<String>> getConfiguredPlacePathScopes();

}
