/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package edu.mayo.kmdp.kdcaci.knew.trisotech;


import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTAssetRepositoryConfig.TTWParams.DEFAULT_VERSION_TAG;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.KEY;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.TT_BASE_MODEL_URI;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.TT_CUSTOM_ATTRIBUTE_ATTR;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.TT_METADATA_NS;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.VALUE;
import static edu.mayo.kmdp.trisotechwrapper.components.TTGraphTerms.ASSET_ID;
import static edu.mayo.kmdp.trisotechwrapper.components.TTGraphTerms.MIME_TYPE;
import static edu.mayo.kmdp.trisotechwrapper.components.TTGraphTerms.MODEL;
import static edu.mayo.kmdp.trisotechwrapper.components.TTGraphTerms.STATE;
import static edu.mayo.kmdp.trisotechwrapper.components.TTGraphTerms.UPDATED;
import static edu.mayo.kmdp.trisotechwrapper.components.TTGraphTerms.VERSION;
import static edu.mayo.kmdp.util.NameUtils.getTrailingPart;
import static edu.mayo.kmdp.util.XMLUtil.asElementStream;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newId;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newVersionId;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.tryNewVersionId;

import edu.mayo.kmdp.kdcaci.knew.trisotech.TTAssetRepositoryConfig.TTWParams;
import edu.mayo.kmdp.kdcaci.knew.trisotech.exception.NotFoundException;
import edu.mayo.kmdp.kdcaci.knew.trisotech.exception.NotLatestVersionException;
import edu.mayo.kmdp.trisotechwrapper.TrisotechWrapper;
import edu.mayo.kmdp.trisotechwrapper.components.TTGraphTerms;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.util.DateTimeUtil;
import edu.mayo.kmdp.util.Util;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * IdentityMapper is used to map dependencies between Trisotech artifacts. SPARQL is used to query
 * the triples from Trisotech for this information. Each artifact has its own assetID, which is also
 * accessible via triples, so asset-to-asset mapping can be handled here as well. These queries only
 * return information about the latest versions of the artifacts.
 */
@Component
public class IdentityMapper {

  private static final Logger logger = LoggerFactory.getLogger(IdentityMapper.class);

  @Autowired
  TrisotechWrapper client;

  @Autowired(required = false)
  TTAssetRepositoryConfig config;

  @Autowired
  NamespaceManager names;

  private boolean publishedOnly;

  private String defaultVersion;

  /**
   * init is needed w/@PostConstruct because @Value values will not be set until after
   * construction.
   * <p>
   * /@PostConstruct will be called after the object is initialized.
   */
  @PostConstruct
  void init() {
    if (config == null) {
      config = new TTAssetRepositoryConfig();
    }
    publishedOnly = config.getTyped(TTWParams.PUBLISHED_ONLY);
    defaultVersion = config.getTyped(DEFAULT_VERSION_TAG);
  }


  private Map<String,Set<String>> getArtifactDependencyMap() {
    return client.getDependencyMap();
  }

  private Optional<Map<TTGraphTerms,String>> getStatementsByModel(String modelUri, boolean pub) {
    if (pub) {
      return Optional.ofNullable(client.getMetadataByModel(modelUri))
          .filter(soln -> soln.containsKey(STATE));
    } else {
      return Optional.ofNullable(client.getMetadataByModel(modelUri));
    }
  }


  private Optional<Map<TTGraphTerms,String>> getStatementsByAsset(UUID assetId, boolean pub) {
    if (pub) {
      return Optional.ofNullable(client.getMetadataByAsset(assetId))
          .filter(soln -> soln.containsKey(STATE));
    } else {
      return Optional.ofNullable(client.getMetadataByAsset(assetId));
    }
  }


  /**
   * Get the Asset that matches the model for the modelUri provided. Will return the asset that maps
   * to the LATEST version of the model.
   *
   * @param modelUri the id for the model
   * @return ResourceIdentifier for assetId of model
   */
  public Optional<ResourceIdentifier> resolveModelToCurrentAssetId(String modelUri) {
    logger.debug("getAssetId for modelUri: {}", modelUri);
    return getStatementsByModel(modelUri, publishedOnly)
        .map(soln -> {
          String statedIdStr = soln.get(ASSET_ID);
          URI statedId = URI.create(statedIdStr);
          if (statedId.getScheme() == null) {
            // need to investigate why 'tags' get detected as assetIds
            if (Util.isUUID(statedIdStr)) {
              return newId(statedId.toString(), defaultVersion);
            } else {
              throw new IllegalStateException(
                  "Invalid AssetID " + statedIdStr + " found on model " + modelUri);
            }
          }
          return newVersionId(statedId);
        });
  }


  /**
   * Validates that the given assetID+versionTag corresponds to a model whose LATEST version
   * is annotated with the corresponding enterprise Asset ID
   *
   * @param assetId    The enterprise assetId looking for
   * @param versionTag The version of the enterprise asset looking for
   * @param pub looking for any model, vs only published models
   * @return The enterprise asset version ID
   * @throws NotLatestVersionException Because models only contains the LATEST version, the version
   *                                   being requested might not be the latest. This exception is
   *                                   thrown to indicate the version requested is not the latest
   *                                   version. Consumers of the exception may then try an alternate
   *                                   route to finding the version needed. The exception will
   *                                   return the artifactId for the asset requested. The artifactId
   *                                   can be used with Trisotech APIs
   */
  public Optional<ResourceIdentifier> resolveAssetToCurrentAssetId(UUID assetId, String versionTag, boolean pub)
      throws NotLatestVersionException {
    Optional<Map<TTGraphTerms,String>> soln = getStatementsByAsset(assetId, pub);
    if (soln.isPresent()) {
      String enterpriseAssetVersionId = soln.get().get(ASSET_ID);
      ResourceIdentifier versionId = newVersionId(URI.create(enterpriseAssetVersionId));
      if (versionTag.equals(versionId.getVersionTag())) {
        return Optional.of(versionId);
      } else {
        // there is an artifact, but the latest does not match the version seeking
        throw new NotLatestVersionException(soln.get().get(MODEL));
      }
    } else {
      return Optional.empty();
    }
  }


  /**
   * Need to be able to retrieve the asset ResourceIdentifier given the assetId NOTE: This only
   * checks the information for the LATEST version of the model, which is available in the models.
   *
   * @param assetUUID the assetId to get the ResourceIdentifier for
   * @return ResourceIdentifier for the assetId or Empty
   */
  public Optional<URI> getCurrentAssetSeriesUri(UUID assetUUID) {
    return getStatementsByAsset(assetUUID, publishedOnly)
        .map(soln -> asSeriesURI(
            URI.create(soln.get(ASSET_ID))));
  }


  /**
   * Get the artifact Id for the asset, assuming the .
   *
   * @param assetId enterprise asset version id
   * @return artifact Id for the asset. artifactId can be used with the APIs.
   * @throws NotLatestVersionException Because models only contains the LATEST version, the version
   *                                   being requested might not be the latest. This exception is
   *                                   thrown to indicate the version requested is not the latest
   *                                   version. Consumers of the exception may then try an alternate
   *                                   route to finding the version needed. The exception will
   *                                   return the artifactId for the asset requested. The artifactId
   *                                   can be used with Trisotech APIs
   */
  public String getCurrentModelId(ResourceIdentifier assetId, boolean pub)
      throws NotLatestVersionException, NotFoundException {

    Optional<Map<TTGraphTerms,String>> solnOpt = getStatementsByAsset(assetId.getUuid(), pub);
    if (solnOpt.isPresent()) {
      Map<TTGraphTerms,String> soln = solnOpt.get();
      Optional<ResourceIdentifier> rid
          = tryNewVersionId(URI.create(soln.get(ASSET_ID)));

      if (logger.isDebugEnabled()) {
        logger.debug("assetId.getResourceId(): {}", assetId.getResourceId());
        logger.debug("assetId.getVersionId(): {}", assetId.getVersionId());
        logger.debug("assetId.getTag(): {}", assetId.getTag());
      }

      if (rid.isPresent()) {
        // versionId value has the UUID of the asset/versions/versionTag, so this will match id and version
        if (rid.get().asKey().equals(assetId.asKey())) {
          return soln.get(MODEL);
          // the requested version of the asset doesn't exist on the latest model, check if the
          // asset is the right asset for the model and if so, throw error with fileId
        } else if (soln.get(ASSET_ID).contains(assetId.getTag())) {
          throw new NotLatestVersionException(soln.get(MODEL));
        } else {
          throw new IllegalStateException("Inconsistent asset IDs");
        }
      }
    }

    throw new NotFoundException(assetId.getTag());
  }

  /**
   * Get the fileId for the asset from models
   *
   * @param assetId Id of the asset looking for.
   * @param pub     search any model, or only published models
   * @return the fileId for the asset; the fileId can be used in the APIs
   */
  public Optional<String> getCurrentModelId(UUID assetId, boolean pub) {
    return getStatementsByAsset(assetId, pub)
        .map(soln -> soln.get(MODEL));
  }


  /**
   * Resolves an id+version to a knowledge asset for which there exist (at least) one carrier model.
   * (Note: a model should not implement more than one knowledge asset through its series)
   * Finds the (one) model annotated with that asset id, starting with the most recent version
   * of the model, and backtracking in history if necessary.
   *
   * @param assetId The UUID of the asset (series)
   * @param assetVersionTag the version of the asset
   * @return A ResourceIdentifier for the
   *
   * @throws NotLatestVersionException The requested assetId is associated with a version of a model
   * that is not the latest version of that model
   * @throws NotFoundException no model for the given asset version
   */
  public ResourceIdentifier getCarrierArtifactId(UUID assetId, String assetVersionTag)
      throws NotLatestVersionException, NotFoundException {
    String modelUri = resolveInternalArtifactID(assetId, assetVersionTag, publishedOnly);
    String modelVersion = getLatestCarrierVersionTag(assetId)
        .orElse(defaultVersion);
    Optional<String> modelUpdated = getLatestCarrierMostRecentUpdateDateTime(assetId);
    return modelUpdated.isPresent()
      ? names.rewriteInternalId(modelUri,modelVersion,modelUpdated.get())
      : names.rewriteInternalId(modelUri,modelVersion);
  }

  /**
   * Get the fileId for use with the APIs from the internal model Id
   *
   * @param internalId the internal trisotech model ID
   * @return the id that can be used with the APIs
   * These values are the same now, but sometimes only have the tag of the internalId, and need the
   * full uri for the queries now.
   */
  public Optional<String> resolveModelId(String internalId) {
    String modelURI;
    if (Util.isUUID(internalId)) {
      modelURI = TT_BASE_MODEL_URI + internalId;
    } else if (internalId.startsWith(TT_BASE_MODEL_URI)) {
      modelURI = internalId;
    } else {
      modelURI = (TT_BASE_MODEL_URI + getTrailingPart(internalId));
    }
    return getStatementsByModel(modelURI, publishedOnly).isPresent()
        ? Optional.of(modelURI)
        : Optional.empty();
  }


  /**
   * Get the mimeType for the asset All models have a mimetype. If this becomes a performance
   * bottleneck, can look at separating out searches for published models only.
   *
   * @param assetId The id of the asset looking for
   * @return the mimetype as specified in the triples
   */
  public String getMimetype(UUID assetId) {
    return getStatementsByAsset(assetId, false)
        .map(soln -> soln.get(MIME_TYPE))
        .orElseThrow(() ->
            new IllegalStateException("Asset " + assetId + "does not have a MIME type"));
  }

  /**
   * Get the mimetype using the model id All models have a mimetype. If performance becomes an
   * issue, might want to separate out searching in published models instead of all models.
   *
   * @param modelUri the id of the model
   * @return the mimetype as specified in the triples
   */
  public String getMimetype(String modelUri) {
    return getStatementsByModel(modelUri, publishedOnly)
        .map(soln -> soln.get(MIME_TYPE))
        .orElseThrow(() ->
            new IllegalStateException("Model " + modelUri + "does not have a MIME type"));
  }


  /**
   * Get the state using the model id State only exists on published models
   *
   * @param modelUri the id of the model
   * @return the state as specified in the triples
   */
  public Optional<String> getPublicationState(String modelUri) {
    return getStatementsByModel(modelUri, publishedOnly)
        .map(soln -> soln.get(STATE));
  }

  /**
   * Get the version using the model id State only exists on published models
   *
   * @param modelId the id of the model
   * @return the version as specified in the triples
   */
  public Optional<String> getLatestVersionTag(String modelId) {
    return getStatementsByModel(modelId, true)
        .map(soln -> soln.get(VERSION));
  }


  /**
   * get the system ID for the internal ID of the resource (artifact) only published models are
   * considered
   *
   * @param modelUri the resource for the artifact desired
   * @return ResourceIdentifier in appropriate format
   */
  private Optional<ResourceIdentifier> getLatestVersionArtifactId(String modelUri) {
    Optional<Map<TTGraphTerms,String>> qsOpt =
        getStatementsByModel(modelUri, publishedOnly);

    Optional<ResourceIdentifier> ridOpt = qsOpt.map(qs -> {
      if (qs.get(VERSION) != null) {
        return qs.containsKey(UPDATED)
            ? names.rewriteInternalId(qs.get(MODEL), qs.get(VERSION), qs.get(UPDATED))
            : names.rewriteInternalId(qs.get(MODEL), qs.get(VERSION));
      } else {
        return names.rewriteInternalId(
            qs.get(MODEL),
            defaultVersion);
      }
    });

    if (ridOpt.isEmpty()) {
      // TODO: return something different? Error? CAO
      logger.warn("Artifact {} is not a published model.", modelUri);
      if (!publishedOnly) {
        ridOpt = Optional.ofNullable(
            names.rewriteInternalId(modelUri, defaultVersion));
      }
    }
    return ridOpt;
  }




  /**
   * Get the version of the artifact for the asset provided Versions only exist on published
   * models.
   *
   * @param assetId The enterprise asset Id
   * @return the version of the artifact
   */
  public Optional<String> getLatestCarrierVersionTag(UUID assetId) {
    // only publishedModels have a version
    return getStatementsByAsset(assetId, true)
        .map(soln -> soln.get(VERSION));
  }

  /**
   * Get the version of the artifact for the asset provided Versions only exist on published
   * models.
   *
   * @param assetId The enterprise asset Id
   * @return the version of the artifact
   */
  public Optional<String> getLatestCarrierTimestampedVersionTag(UUID assetId) {
    // only publishedModels have a versionre
    return getStatementsByAsset(assetId, true)
        .flatMap(soln -> {
          String versionTag = soln.get(VERSION);
          return Optional.ofNullable(soln.get(UPDATED))
              .map(DateTimeUtil::dateTimeStrToMillis)
              .map(timestamp -> versionTag + "+" + timestamp);
        });
  }

  /**
   * Get the updated dateTime of the artifact for the asset provided
   *
   * @param assetId The enterprise asset Id
   * @return the updated value of the artifact
   */
  public Optional<String> getLatestCarrierMostRecentUpdateDateTime(UUID assetId) {
    return getStatementsByAsset(assetId, publishedOnly)
        .map(soln -> soln.get(UPDATED));
  }

  /**
   * Get the updated dateTime of the artifact for the asset provided
   *
   * @param assetId The enterprise asset Id
   * @return the updated field of the artifact in MS
   */
  public Optional<String> getLatestCarrierMostRecentUpdateTimestamp(UUID assetId) {
    // only publishedModels have a version
    return getStatementsByAsset(assetId, publishedOnly)
        .map(soln -> soln.get(UPDATED))
        .map(DateTimeUtil::dateTimeStrToMillis);
  }

  /**
   * Given the internal id for the model, get the information about other models it imports. This is
   * based on the latest version of the model.
   *
   * @param artifactId the artifact id
   * @return a list of ResourceIdentifier for the artifacts used by this artifact (dependencies)
   */
  public List<ResourceIdentifier> getLatestArtifactDependencies(String artifactId) {
    return processDependencies(artifactId, this::getArtifactRelation);
  }

  /**
   * given the internal id for the model (artifact), get the information for the assets the model
   * imports each artifact should have an assetID, this allows us to map asset<->asset relations
   *
   * @param artifactId the artifact id
   * @return a set of resources for the assets used by this model (dependencies)
   */
  public List<ResourceIdentifier> getLatestAssetDependencies(String artifactId) {
    return processDependencies(artifactId, this::getAssetRelation);
  }


  private List<ResourceIdentifier> processDependencies(String artifactId,
      BiFunction<String,String, Optional<ResourceIdentifier>> mapper) {
    List<ResourceIdentifier> assets = new ArrayList<>();

    if (!Util.isEmpty(artifactId)) {
      // first find the artifact in the artifactToArtifact mapping
      getArtifactDependencyMap().forEach((k, v) -> {
        if (matches(k,artifactId)) {
          // once found, for each of the artifacts it is dependent on, find the asset id for those artifacts in the models
          v.forEach(dependent -> mapper.apply(dependent, k)
              .ifPresent(assets::add));
        }
      });
    }
    return assets;
  }

  private Optional<ResourceIdentifier> getAssetRelation(String dependent, String subject) {
    return getRelatedResource(dependent, subject, this::resolveModelToCurrentAssetId);
  }

  private Optional<ResourceIdentifier> getArtifactRelation(String dependent, String subject) {
    return getRelatedResource(dependent, subject, this::getLatestVersionArtifactId);
  }

  private Optional<ResourceIdentifier> getRelatedResource(String dependent, String subject,
      Function<String,Optional<ResourceIdentifier>> mapper) {
    logger.debug("dependent URI: {}", dependent);

    Optional<ResourceIdentifier> rid = mapper.apply(dependent);

    if (rid.isEmpty() && logger.isWarnEnabled()) {
      logger.warn("Model Dependency {} for {} is NOT Published",
          getTrailingPart(dependent),
          getTrailingPart(subject));
    }
    return rid;
  }


  /**
   * internalArtifactID is the id of the Carrier/model in Trisotech
   *
   * @param assetId the assetId for which an artifact is needed
   * @param versionTag the version of the asset requesting
   * @param publishedOnly should any model be searched, vs only published models
   * @return the internalArtifactId or NotLatestVersionException
   *
   * The exception will be thrown if the latest version of the artifact does not
   * map to the requested version of the asset.
   */
  public String resolveInternalArtifactID(UUID assetId, String versionTag, boolean publishedOnly)
      throws NotLatestVersionException, NotFoundException {
    // need to find the artifactId for this version of assetId
    // ResourceIdentifier built with assetId URI and versionTag; allows for finding the artifact associated with this asset/version
    ResourceIdentifier id = SemanticIdentifier.newId(assetId,versionTag);
    return getCurrentModelId(id, publishedOnly);
  }


  /**
   * enterpriseAssetId is the assetId found in the Carrier/model/XML file from Trisotech
   *
   * @param modelUri the Trisotech model ID to resolve to an enterprise ID
   * @return the enterprise ID
   */
  public ResourceIdentifier resolveEnterpriseAssetID(String modelUri) {
    return resolveModelToCurrentAssetId(modelUri)
        // Defensive exception. Published models should have an assetId | CAO | DS
        .orElseThrow(() -> {
          String modelName = client.getFileInfo(modelUri)
              .map(TrisotechFileInfo::getName)
              .orElse("(unknown)");
          return new IllegalStateException(
              "Defensive: Unable to resolve model ID " + modelUri
                  + " for model " + modelName
                  + " to a known Enterprise ID");
        });
  }


  public Optional<ResourceIdentifier> getAssetIdForHistoricalArtifact(ResourceIdentifier artifactID) {
    return client.getFileInfoByIdAndVersion(artifactID.getUuid(),artifactID.getVersionTag())
        .flatMap(info -> client.getModel(info)
            .flatMap(this::extractAssetIdFromDocument));
  }

  /**
   * Get the customAttribute assetID from the document. This is a value that is stripped from a
   * woven file but can be retrieved from an unwoven file.
   *
   * @param dox the model XML document
   * @return ResourceIdenifier of the assetID
   */
  public Optional<ResourceIdentifier> extractAssetIdFromDocument(Document dox) {
    NodeList metas = dox.getElementsByTagNameNS(TT_METADATA_NS, TT_CUSTOM_ATTRIBUTE_ATTR);

    List<ResourceIdentifier> ids = asElementStream(metas)
        .filter(this::isIdentifier)
        .map(el -> el.getAttribute(VALUE))
        .map(id -> SemanticIdentifier.newVersionId(URI.create(id)))
        .collect(Collectors.toList());

    return ids.isEmpty() ? Optional.empty() : Optional.ofNullable(ids.get(0));
  }


  private boolean isIdentifier(Element el) {
    return config.getTyped(TTWParams.ASSET_ID_ATTRIBUTE)
        .equals(el.getAttribute(KEY));
  }

  /**
   * Return the series (version agnostic) URI for the versionedURI provided.
   *
   * @param versionedURI the versioned id
   * @return the
   */
  public URI asSeriesURI(URI versionedURI) {
    return newVersionId(versionedURI).getResourceId();
  }

  public boolean isLatest(String modelId, String modelVersionTag) {
    return getLatestVersionTag(modelId)
        .equals(Optional.ofNullable(modelVersionTag));
  }

  public boolean isLatest(TrisotechFileInfo meta) {
    return isLatest(meta.getId(),meta.getVersion());
  }

  /**
   * Compares a modelId (in the TT namespace)
   * with an artifactId (in the Enterprise namespace)
   * based on having a common UUID
   * @param modelId
   * @param artifactId
   * @return
   */
  private boolean matches(String modelId, String artifactId) {
    int j = TT_BASE_MODEL_URI.length();
    String key1 = modelId.substring(j);
    int k = names.getArtifactNamespace().toString().length();
    String key2 = artifactId.substring(k);
    return key1.equals(key2);
  }
}
