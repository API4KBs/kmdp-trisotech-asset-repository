/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package edu.mayo.kmdp.kdcaci.knew.trisotech.preprocess;

import static edu.mayo.kmdp.registry.Registry.MAYO_ARTIFACTS_BASE_URI_URI;
import static org.apache.http.HttpHeaders.AUTHORIZATION;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newVersionId;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.tryNewVersionId;

import edu.mayo.kmdp.trisotechwrapper.TrisotechWrapper;
import edu.mayo.kmdp.util.FileUtil;
import edu.mayo.kmdp.util.Util;
import edu.mayo.kmdp.util.graph.HierarchySorter;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.annotation.PostConstruct;
import org.apache.http.Header;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.shared.NotFoundException;
import org.apache.logging.log4j.util.Strings;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * IdentityMapper is used to map dependencies between Trisotech artifacts. SPARQL is used to query
 * the triples from Trisotech for this information. Each artifact has its own assetID, which is also
 * accessible via triples, so asset-to-asset mapping can be handled here as well. These queries only
 * return information about the latest versions of the artifacts.
 */
@Component
public class IdentityMapper {

  private static final Logger logger = LoggerFactory.getLogger(IdentityMapper.class);

  // SPARQL Strings
  // SPARQL endpoint for repository
  private static final String ENDPOINT = "https://mc.trisotech.com/ds/query";

  private static final String TRISOTECH_GRAPH = "http://trisotech.com/graph/1.0/graph#";
  private static final String ASSET_ID = "?assetId";
  private static final String FILE_ID = "?fileId";
  private static final String MODEL = "?model";
  private static final String STATE = "?state";
  private static final String MIME_TYPE = "?mimeType";
  private static final String VERSION = "?version";
  private static final String UPDATED = "?updated";
  private static final String ARTIFACT_NAME = "?artifactName";

  private static final String VERSIONS = "/versions";

  // HierarchySorter will sort in reverse order of use, so an artifact imported by another will be at the top of the tree after the sort
  private HierarchySorter<Resource> hierarchySorter = new HierarchySorter<>();
  // map of artifact relations (artifact = model)
  private Map<Resource, Set<Resource>> artifactToArtifactIDMap = new HashMap<>();
  // make the resultSet rewindable because will need to access it many times; if not rewindable, cannot get back to the beginning
  private ResultSetRewindable publishedModels;
  private ResultSetRewindable models;

  // results of the hierarchySorter
  private List<Resource> orderedModels;

  @Autowired
  TrisotechWrapper client;

  /**
   * init is needed w/@PostConstruct because @Value values will not be set until after
   * construction.
   *
   * /@PostConstruct will be called after the object is initialized.
   */
  @PostConstruct
  void init() {
    String place = client.getConfig().getRepositoryId();
    logger.debug("place in init {}", place);
    createMap(query(getQueryStringRelations(), place));
    models = ResultSetFactory.makeRewindable(query(getQueryStringModels(), place));
    publishedModels = ResultSetFactory
        .makeRewindable(query(getQueryStringPublishedModels(), place));
    // TODO: orderedModels needed? Not currently used; overflow error from hierarchySorter (infinite loop)
//    orderedModels = hierarchySorter
//        .linearize(getModelList(publishedModels), artifactToArtifactIDMap);
  }

  private void createMap(ResultSet results) {
    while (results.hasNext()) {
      QuerySolution soln = results.nextSolution();
      artifactToArtifactIDMap
          .computeIfAbsent(soln.getResource(MODEL), s -> new HashSet<>())
          .add(soln.getResource("?dModel"));
    }
  }

  /**
   * return the list of models from the triples
   *
   * @param modelResults the resultSet from a SPARQL query
   */
  private List<Resource> getModelList(ResultSet modelResults) {
    List<Resource> modelList = new ArrayList<>();
    while (modelResults.hasNext()) {
      QuerySolution soln = modelResults.nextSolution();
      modelList.add(soln.getResource(MODEL));
    }
    return modelList;
  }

  /**
   * Perform the query
   *
   * @param queryString the queryString to be executed
   * @param place the place to query models from
   * @return The ResultSet with the results from the query
   */
  private ResultSet query(String queryString, String place) {
    ParameterizedSparqlString sparqlString = new ParameterizedSparqlString(queryString);
    String graph = TRISOTECH_GRAPH + place;
    // set NAMED
    sparqlString.setIri(0, graph);
    // set GRAPH
    sparqlString.setIri(1, graph);
    Query query = sparqlString.asQuery();

    Header header = new BasicHeader(AUTHORIZATION, client.getBearerTokenHeader());

    HttpClient httpClient = HttpClientBuilder.create()
        .setDefaultHeaders(Collections.singleton(header))
        .build();
    QueryExecution qexec = QueryExecutionFactory
        .sparqlService(ENDPOINT, query, httpClient);
    return qexec.execSelect();
  }


  /**
   * Build the queryString needed to query the DMN->DMN and CMMN->DMN relations in the place
   * requested. This is specific to Trisotech.
   *
   * Should only get those models that are Published? CAO | DS
   *
   * @return the query string to query the relations between models
   */
  private String getQueryStringRelations() {
    return FileUtil.read(IdentityMapper.class.getResourceAsStream("/queryRelations.tt.sparql"))
        .orElseThrow(IllegalStateException::new);
  }


  /**
   * Build the queryString needed to query models with their fileId and assetId. This is specific to
   * Trisotech. By requesting version in the query, and not making it OPTIONAL, only models that
   * have a version (i.e. are published) will be returned. HOWEVER, having a version does not mean
   * they have a STATE of 'Published'. The State could be other values, such as 'Draft'. Per Kevide
   * meeting, 8/21, this is ok. Want all published even if state is not 'Published'.
   *
   * @return the query string needed to query the models
   */
  private String getQueryStringPublishedModels() {
    return FileUtil
        .read(IdentityMapper.class.getResourceAsStream("/queryPublishedModels.tt.sparql"))
        .orElseThrow(IllegalStateException::new);
  }

  /**
   * Build the queryString needed to query models with their fileId and assetId. This is specific to
   * Trisotech. This query is for ALL models, published and not, though because none of the
   * selectors are given as optional, if they don't exist on a model, that model will not be
   * returned.
   *
   * @return the query string needed to query the models
   */
  private String getQueryStringModels() {
    return FileUtil.read(IdentityMapper.class.getResourceAsStream("/queryModels.tt.sparql"))
        .orElseThrow(IllegalStateException::new);
  }

  /**
   * Get the asset for the artifact and version provided.
   *
   * @param artifactId Id for an artifact in the Trisotech models
   * @param versionTag the version of the artifact
   * @return ResourceIdentifier for the asset
   * @throws NotLatestVersionException thrown if this version is not the latest for the artifact It
   * is possible the version exists, but is not the latest. Additional processing may be needed.
   */
  // input should be artifactId (from the triple); version is version of artifact
  public Optional<ResourceIdentifier> getAssetId(ResourceIdentifier artifactId, String versionTag)
      throws NotLatestVersionException {
    publishedModels.reset();
    while (publishedModels.hasNext()) {
      QuerySolution soln = publishedModels.nextSolution();

      if (soln.getResource(MODEL).getURI().equals(artifactId.getResourceId().toString())) {
        if (soln.getLiteral(VERSION).getString().equals(versionTag)) {
          return Optional.of(newVersionId(URI.create(soln.getLiteral(ASSET_ID).toString())));
        } else { // have the artifact, but not version looking for; more work to see if the version exists
          throw new NotLatestVersionException(soln.getResource(MODEL).getURI());
        }
      }

    }
    return Optional.empty();

  }

  /**
   * Get the Asset that matches the model for the fileId provided. Will return the asset that maps
   * to the LATEST version of the model.
   *
   * @param fileId the id for the file representing the model
   * @return ResourceIdentifier for assetId of model
   */
  public Optional<ResourceIdentifier> getAssetId(String fileId) {
    logger.debug("getAssetId for fileId: {}", fileId);
    publishedModels.reset();
    while (publishedModels.hasNext()) {
      QuerySolution soln = publishedModels.nextSolution();

      if (soln.getLiteral(FILE_ID).getString().equals(fileId)) {
        return Optional.of(
            newVersionId(
                URI.create(soln.getLiteral(ASSET_ID).toString())));
      }
    }
    return Optional.empty();
  }

  /**
   * Need to be able to retrieve the asset ResourceIdentifier given the assetId NOTE: This only
   * checks the information for the LATEST version of the model, which is available in the models.
   *
   * @param assetId the assetId to get the ResourceIdentifier for
   * @return ResourceIdentifier for the assetId or Empty
   */
  public Optional<URI> getEnterpriseAssetIdForAsset(UUID assetId) {
    publishedModels.reset();
    while (publishedModels.hasNext()) {
      QuerySolution soln = publishedModels.nextSolution();

      String enterpriseAssetVersionId = soln.getLiteral(ASSET_ID).getString();

      if (enterpriseAssetVersionId.contains(assetId.toString())) {
        // want this to return the ASSETID, NOT the VersionID (which is what this value is)
        // return only the portion of the URI that is the enterprise Asset ID (no version info)
        return Optional
            .ofNullable(getEnterpriseAssetIdForAssetVersionId(URI.create(enterpriseAssetVersionId))
            );
      }
    }
    return Optional.empty();
  }

  /**
   * Return the enterpriseAssetId for the enterpriseAssetVersionId provided.
   *
   * @param enterpriseAssetVersionId the enterprise asset version id
   * @return the enterpriseAssetId
   */
  public URI getEnterpriseAssetIdForAssetVersionId(URI enterpriseAssetVersionId) {
    String assetVersionId = enterpriseAssetVersionId.toString();
    if (enterpriseAssetVersionId.getPath().contains(VERSIONS)) {
      // remove the /versions/... part of the URI; enterpriseAssetId is the part without the version
      return URI.create(assetVersionId
          .substring(0, assetVersionId.lastIndexOf(VERSIONS)));
    } else {
      return enterpriseAssetVersionId; // just give back what was given
    }
  }

  /**
   * Get the enterprise asset version ID for the assetId provided.
   *
   * @param assetId The enterprise assetId looking for
   * @param versionTag The version of the enterprise asset looking for
   * @param any True is looking for any model; false to look only at published models
   * @return The enterprise asset version ID
   * @throws NotLatestVersionException Because models only contains the LATEST version, the version
   * being requested might not be the latest. This exception is thrown to indicate the version
   * requested is not the latest version. Consumers of the exception may then try an alternate route
   * to finding the version needed. The exception will return the artifactId for the asset
   * requested. The artifactId can be used with Trisotech APIs
   */
  public Optional<URI> getEnterpriseAssetVersionIdForAsset(UUID assetId, String versionTag,
      boolean any)
      throws NotLatestVersionException {
    if (any) {
      return enterpriseAssetVersionIdForAssetInModel(models, assetId, versionTag);
    } else {
      return enterpriseAssetVersionIdForAssetInModel(publishedModels, assetId, versionTag);
    }
  }

  private Optional<URI> enterpriseAssetVersionIdForAssetInModel(
      ResultSetRewindable theModels, UUID assetId, String versionTag)
      throws NotLatestVersionException {
    theModels.reset();
    while (theModels.hasNext()) {
      QuerySolution soln = theModels.nextSolution();
      String enterpriseAssetVersionId = soln.getLiteral(ASSET_ID).getString();
      if (enterpriseAssetVersionId.contains(assetId.toString())) {
        // found an artifact that has this asset; now check the version
        SemanticIdentifier versionId
            = newVersionId(URI.create(enterpriseAssetVersionId));
        if (versionTag.equals(versionId.getVersionTag())) {
          return Optional.of(versionId.getVersionId());
        } else {
          // there is an artifact, but the latest does not match the version seeking
          throw new NotLatestVersionException(soln.getResource(MODEL).getURI());
        }
      }
    }
    throw new NotFoundException(
        "No enterprise assetId for asset " + assetId + " version: " + versionTag);
  }

  /**
   * Get the artifact Id for the asset.
   *
   * @param assetId enterprise asset version id
   * @return artifact Id for the asset. artifactId can be used with the APIs.
   * @throws NotLatestVersionException Because models only contains the LATEST version, the version
   * being requested might not be the latest. This exception is thrown to indicate the version
   * requested is not the latest version. Consumers of the exception may then try an alternate route
   * to finding the version needed. The exception will return the artifactId for the asset
   * requested. The artifactId can be used with Trisotech APIs
   */
  public String getArtifactId(ResourceIdentifier assetId, boolean any)
      throws NotLatestVersionException {
    if (any) {
      return artifactIdFromModel(models, assetId);
    } else {
      return artifactIdFromModel(publishedModels, assetId);
    }
  }

  private String artifactIdFromModel(ResultSetRewindable theModels,
      ResourceIdentifier assetId) throws NotLatestVersionException {
    theModels.reset();
    // this will only match if the exact version of the asset is available on a latest model
    while (theModels.hasNext()) {
      QuerySolution soln = theModels.nextSolution();

      if (logger.isDebugEnabled()) {
        logger.debug("assetId.getResourceId(): {}", assetId.getResourceId());
        logger.debug("assetId.getVersionId(): {}", assetId.getVersionId());
        logger.debug("assetId.getTag(): {}", assetId.getTag());
      }

      Optional<ResourceIdentifier> rid
          = tryNewVersionId(URI.create(soln.getLiteral(ASSET_ID).toString()));

      if (rid.isPresent()) {
        // versionId value has the UUID of the asset/versions/versionTag, so this will match id and version
        if (rid.get().asKey().equals(assetId.asKey())) {
          return soln.getResource(MODEL).getURI();
          // the requested version of the asset doesn't exist on the latest model, check if the
          // asset is the right asset for the model and if so, throw error with fileId
        } else if (soln.getLiteral(ASSET_ID).getString().contains(assetId.getTag())) {
          throw new NotLatestVersionException(soln.getResource(MODEL).getURI());
        }
      }
    }
    throw new NotFoundException(assetId.getTag());
  }

  /**
   * Get the fileId for the asset from models
   *
   * @param assetId Id of the asset looking for.
   * @param any search any model? true, else search only published models
   * @return the fileId for the asset; the fileId can be used in the APIs
   */
  public Optional<String> getFileId(UUID assetId, boolean any) {
    if (any) {
      return getFileIdFromModels(models, assetId);
    } else {
      return getFileIdFromModels(publishedModels, assetId);
    }
  }

  private Optional<String> getFileIdFromModels(ResultSetRewindable theModels, UUID assetId) {
    theModels.reset();
    List<String> fileIds = new ArrayList<>();
    while (theModels.hasNext()) {
      QuerySolution soln = theModels.nextSolution();

      if (soln.getLiteral(ASSET_ID).getString().contains(assetId.toString())) {
        fileIds.add(soln.getLiteral(FILE_ID).getString());
      }
    }
    if (fileIds.size() > 1) {
      logger.warn("BUG : The same AssetID has been used across multiple models, "
          + "which is admissible but not supported");
      logger.warn("Asset ID {}, model IDs {}", assetId, Strings.join(fileIds,','));
    }
    return fileIds.stream().findAny();
  }

  /**
   * Get the fileId for use with the APIs from the internal model Id
   *
   * @param internalId the internal trisotech model ID
   * @return the fileId that can be used with the APIs
   */
  public Optional<String> getFileId(String internalId) {
    publishedModels.reset();
    while (publishedModels.hasNext()) {
      QuerySolution soln = publishedModels.nextSolution();
      // use contains as sometimes internalId is just the tag
      if (soln.getResource(MODEL).getURI().contains(internalId)) {
        return Optional.ofNullable(soln.getLiteral(FILE_ID).getString());
      }
    }
    return Optional.empty();
  }


  /**
   * Get the mimeType for the asset All models have a mimetype. If this becomes a performance
   * bottleneck, can look at separating out searches for published models only.
   *
   * @param assetId The id of the asset looking for
   * @return the mimetype as specified in the triples
   */
  public Optional<String> getMimetype(UUID assetId) {
    models.reset();
    while (models.hasNext()) {
      QuerySolution soln = models.nextSolution();

      if (soln.getLiteral(ASSET_ID).getString().contains(assetId.toString())) {
        return Optional.ofNullable(soln.getLiteral(MIME_TYPE).getString());
      }
    }
    return Optional.empty();
  }


  /**
   * Get the name of the artifact for the asset If this becomes a performance bottleneck, can look
   * at separating out searches for published models only.
   *
   * @param assetId The id of the asset looking for
   * @return the name of the artifact as specified in the triples
   */
  public Optional<String> getArtifactNameByAssetId(ResourceIdentifier assetId) {
    models.reset();
    while (models.hasNext()) {
      QuerySolution soln = models.nextSolution();
      if (soln.getLiteral(ASSET_ID).getString().contains(assetId.getResourceId().toString())) {
        return Optional.ofNullable(soln.getLiteral(ARTIFACT_NAME).getString());
      }
    }
    return Optional.empty();
  }

  public Optional<String> getArtifactNameByArtifactId(ResourceIdentifier artifactId) {
    models.reset();
    while (models.hasNext()) {
      QuerySolution soln = models.nextSolution();
      if (soln.getResource(MODEL).getURI().contains(artifactId.getTag())) {
        return Optional.ofNullable(soln.getLiteral(ARTIFACT_NAME).getString());
      }
    }
    return Optional.empty();
  }


  /**
   * Get the mimetype using the model id All models have a mimetype. If performance becomes an
   * issue, might want to separate out searching in published models instead of all models.
   *
   * @param internalId the internal id of the model
   * @return the mimetype as specified in the triples
   */
  public Optional<String> getMimetype(String internalId) {
    models.reset();
    while (models.hasNext()) {
      while (models.hasNext()) {
        QuerySolution soln = models.nextSolution();
        // use contains as sometimes internalId is just the tag
        if (soln.getResource(MODEL).getURI().contains(internalId)) {
          return Optional.ofNullable(soln.getLiteral(MIME_TYPE).getString());
        }
      }
    }
    return Optional.empty();
  }

  /**
   * Get the state using the model id State only exists on published models
   *
   * @param fileId the file id of the model
   * @return the state as specified in the triples
   */
  public Optional<String> getState(String fileId) {
    publishedModels.reset();
    while (publishedModels.hasNext()) {
      while (publishedModels.hasNext()) {
        QuerySolution soln = publishedModels.nextSolution();
        if (soln.getLiteral(FILE_ID).getString().equals(fileId)) {
          return Optional.ofNullable(soln.getLiteral(STATE).getString());
        }
      }
    }
    return Optional.empty();
  }

  /**
   * Get the version using the model id State only exists on published models
   *
   * @param fileId the file id of the model
   * @return the version as specified in the triples
   */
  public Optional<String> getVersion(String fileId) {
    publishedModels.reset();
    while (publishedModels.hasNext()) {
      while (publishedModels.hasNext()) {
        QuerySolution soln = publishedModels.nextSolution();
        if (soln.getLiteral(FILE_ID).getString().equals(fileId)) {
          logger.debug("returning version of: {}", soln.getLiteral(VERSION).getString());
          return Optional.ofNullable(soln.getLiteral(VERSION).getString());
        }
      }
    }
    return Optional.empty();
  }


  /**
   * Get the version of the artifact for the asset provided Versions only exist on published
   * models.
   *
   * @param assetId The enterprise asset Id
   * @return the version of the artifact
   */
  public Optional<String> getArtifactIdVersion(UUID assetId) {
    // only publishedModels have a version
    publishedModels.reset();
    while (publishedModels.hasNext()) {
      QuerySolution soln = publishedModels.nextSolution();
      if (soln.getLiteral(ASSET_ID).getString().contains(assetId.toString())) {
        return Optional.ofNullable(soln.getLiteral(VERSION).getString());
      }
    }
    return Optional.empty();
  }


  /**
   * Get the updated dateTime of the artifact for the asset provided
   *
   * @param assetId The enterprise asset Id
   * @return the updated value of the artifact
   */
  public Optional<String> getArtifactIdUpdateTime(UUID assetId) {
    // only publishedModels have a version
    publishedModels.reset();
    while (publishedModels.hasNext()) {
      QuerySolution soln = publishedModels.nextSolution();
      if (soln.getLiteral(ASSET_ID).getString().contains(assetId.toString())) {
        return Optional.ofNullable(soln.getLiteral(UPDATED).getString());
      }
    }
    return Optional.empty();
  }


  /**
   * Given the internal id for the model, get the information about other
   * models it imports. This is based on the latest version of the model.
   *
   * @param docId the artifact id
   * @return a list of ResourceIdentifier for the artifacts used by this artifact (dependencies)
   */
  public List<ResourceIdentifier> getArtifactImports(String docId) {
    Set<Resource> resources = null;
    List<ResourceIdentifier> artifacts = new ArrayList<>();

    // TODO: potential issue when working with older versions of the model? -
    //  maybe the artifact->artifact map has changed and has different artifact dependencies
    //  artifactToArtifactIDMap is based on latest
    if (!Util.isEmpty(docId)) {
      String id = docId.substring(docId.lastIndexOf('/') + 1);
      for (Entry<Resource, Set<Resource>> entry : artifactToArtifactIDMap.entrySet()) {
        Resource k = entry.getKey();
        if (k.getLocalName().substring(1).equals(id)) {
          resources = artifactToArtifactIDMap.get(k);
          break;
        }
      }
    }

    if (null != resources) {
      for (Resource resource : resources) {
        artifacts.add(getArtifactIdentifier(resource));
      }
    }
    return artifacts;
  }

  public boolean isLatest(String fileId, String version) {
    return getVersion(fileId).equals(Optional.of(version));
  }

  /**
   * get the system ID for the internal ID of the resource (artifact) only published models are
   * considered
   *
   * @param resource the resource for the artifact desired
   * @return ResourceIdentifier in appropriate format
   */
  private ResourceIdentifier getArtifactIdentifier(Resource resource) {
    publishedModels.reset();
    while (publishedModels.hasNext()) {
      QuerySolution soln = publishedModels.nextSolution();
      if (soln.getResource(MODEL).equals(resource)) {
        if (soln.getLiteral(VERSION) != null) {
          return convertInternalId(soln.getResource(MODEL).getURI(),
              soln.getLiteral(VERSION).getString(),
              soln.getLiteral(UPDATED).getString());
        } else {
          // TODO: Still use timestamp?
          return convertInternalId(soln.getResource(MODEL).getURI(), null, null);

        }
      }
    }
    // TODO: return something different? Error? CAO
    logger.warn("Artifact {} is not a published model.", resource);
    return null;
  }

  /**
   * given the internal id for the model (artifact), get the information for the assets the model
   * imports each artifact should have an assetID, this allows us to map asset<->asset relations
   *
   * @param artifactId the artifact id
   * @return a set of resources for the assets used by this model (dependencies)
   */
  public List<ResourceIdentifier> getAssetRelations(String artifactId) {
    List<ResourceIdentifier> assets = new ArrayList<>();

    if (!Util.isEmpty(artifactId)) {
      // first find the artifact in the artifactToArtifact mapping
      String id = artifactId.substring(artifactId.lastIndexOf('/') + 1);
      artifactToArtifactIDMap.forEach((k, v) -> {
        if (k.getLocalName().substring(1).equals(id)) {
          logger.debug("found id in artifactToArtifactIDMap");
          // once found, for each of the artifacts it is dependent on, find the asset id for those artifacts in the models
          v.forEach(dependent -> getAssetRelation(dependent, k).ifPresent(assets::add));
        }
      });
    }
    return assets;
  }

  private Optional<ResourceIdentifier> getAssetRelation(Resource dependent, Resource subject) {
    logger.debug("dependent URI: {}", dependent.getURI());
    String dependentId = dependent.getURI().substring(dependent.getURI().lastIndexOf('/'));
    publishedModels.reset();
    while (publishedModels.hasNext()) {
      QuerySolution soln = publishedModels.nextSolution();
      logger.debug("soln MODEL URI: {} ", soln.getResource(MODEL).getURI());
      if (soln.getResource(MODEL).getURI().equals(dependent.getURI())) {
        logger.debug("Asset ID for Model: {}", soln.getLiteral(ASSET_ID));
        return Optional
            .of(newVersionId(URI.create(soln.getLiteral(ASSET_ID).getString())));
      }
    }
    // if not found, because not published
    logger.warn("Dependency {} for {} is NOT Published", dependentId, subject.getLocalName());
    return Optional.empty();
  }


  /**
   * Get the models in order of dependencies so that the models at the bottom of the dependency tree
   * are first in the list.
   *
   * @return dependency ordered list of the models
   */
  public List<Resource> getOrderedModels() {
    return orderedModels;
  }

  /**
   * Need the Trisotech path converted to KMDP path and underscores removed
   *
   * @param internalId the Trisotech internal id for the model
   * @param timestamp the timestamp/updated time for this version of the model
   * @return ResourceIdentifier with the KMDP-ified internal id
   */
  public ResourceIdentifier convertInternalId(String internalId, String versionTag,
      String timestamp) {
    String id = internalId.substring(internalId.lastIndexOf('/') + 1).replace("_", "");
    if (null != timestamp) {
      Date modelDate = Date.from(Instant.parse(timestamp));
      String timestampVersion = versionTag + "+" + modelDate.getTime();
      return SemanticIdentifier.newId(MAYO_ARTIFACTS_BASE_URI_URI, id, timestampVersion);
    } else {
      return SemanticIdentifier.newId(MAYO_ARTIFACTS_BASE_URI_URI, id, versionTag);
    }
  }
}