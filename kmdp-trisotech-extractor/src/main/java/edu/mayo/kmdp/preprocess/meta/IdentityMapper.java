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
package edu.mayo.kmdp.preprocess.meta;

import static org.apache.http.HttpHeaders.AUTHORIZATION;

import edu.mayo.kmdp.id.helper.DatatypeHelper;
import edu.mayo.kmdp.preprocess.NotLatestVersionException;
import edu.mayo.kmdp.util.FileUtil;
import edu.mayo.kmdp.util.Util;
import edu.mayo.kmdp.util.graph.HierarchySorter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
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
import org.omg.spec.api4kp._1_0.identifiers.URIIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


/**
 * IdentityMapper is used to map dependencies between Trisotech artifacts. SPARQL is used to query
 * the triples from Trisotech for this information. Each artifact has its own assetID, which is also
 * accessible via triples, so asset-to-asset mapping can be handled here as well.
 */
@Component
public class IdentityMapper {

  @Value("${edu.mayo.kmdp.trisotechwrapper.trisotechToken}")
  private String token;

//  @Value("${edu.mayo.kmdp.trisotechwrapper.repositoryName}")
//  private String repositoryName;

  @Value("${edu.mayo.kmdp.trisotechwrapper.repositoryId}")
  String place;

  private static final Logger logger = LoggerFactory.getLogger(IdentityMapper.class);

  // SPARQL Strings
  // SPARQL endpoint for repository
  private static final String ENDPOINT = "https://mc.trisotech.com/ds/query";
  // PREFIX sets up the namespaces to be used in the query
  private static final String QUERYSTRINGPREFIX =
      "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
          + "PREFIX tt: <http://www.trisotech.com/graph/1.0/element#>"
          + "PREFIX ttr: <http://www.trisotech.com/graph/1.0/elementRel#>"
          + "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>"
          + "PREFIX owl: <http://www.w3.org/2002/07/owl#>"
          + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
          + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>";
  private static final String TRISOTECH_GRAPH = "http://trisotech.com/graph/1.0/graph#";
  private static final String ASSET_ID = "?assetId";
  private static final String FILE_ID = "?fileId";
  private static final String MODEL = "?model";
  private static final String STATE = "?state";
  private static final String MIME_TYPE = "?mimeType";
  private static final String VERSION = "?version";
  private static final String ARTIFACTNAME= "?artifactName";

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

  /**
   * init is needed w/@PostConstruct because @Value values will not be set until after
   * construction.
   *
   * /@PostConstruct will be called after the object is initialized.
   */
  @PostConstruct
  void init() {
    logger.debug("place in init {}", place);
    createMap(query(getQueryStringRelations(),place));
    models = ResultSetFactory.makeRewindable(query(getQueryStringModels(),place));
    publishedModels = ResultSetFactory.makeRewindable(query(getQueryStringPublishedModels(),place));
    orderedModels = hierarchySorter
        .linearize(getModelList(publishedModels), artifactToArtifactIDMap);
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

    Header header = new BasicHeader(AUTHORIZATION, "Bearer " + token);

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
    return FileUtil.read(IdentityMapper.class.getResourceAsStream("/queryPublishedModels.tt.sparql"))
        .orElseThrow(IllegalStateException::new);
  }

  /**
   * Build the queryString needed to query models with their fileId and assetId. This is specific to
   * Trisotech. This query is for ALL models, published and not, though because none of the
   * selectors are given as optional, if they don't exist on a model, that model will not be returned.
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
   * @return URIIdentifier for the asset
   * @throws NotLatestVersionException thrown if this version is not the latest for the artifact It
   * is possible the version exists, but is not the latest. Additional processing may be needed.
   */
  // input should be artifactId (from the triple); version is version of artifact
  public Optional<URIIdentifier> getAssetId(URIIdentifier artifactId, String versionTag)
      throws NotLatestVersionException {
    publishedModels.reset();
    while (publishedModels.hasNext()) {
      QuerySolution soln = publishedModels.nextSolution();

      if (soln.getResource(MODEL).getURI().equals(artifactId.getUri().toString())) {
        if (soln.getLiteral(VERSION).getString().equals(versionTag)) {
          return Optional.of(DatatypeHelper.uri(soln.getLiteral(ASSET_ID).toString()));
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
   */
  public Optional<URIIdentifier> getAssetId(String fileId) {
    logger.debug("getAssetId for fileId: {}", fileId);
    publishedModels.reset();
    while (publishedModels.hasNext()) {
      QuerySolution soln = publishedModels.nextSolution();

      if (soln.getLiteral(FILE_ID).getString().equals(fileId)) {
        return Optional.of(DatatypeHelper.uri(soln.getLiteral(ASSET_ID).toString()));
      }
    }
    return Optional.empty();
  }

  /**
   * Need to be able to retrieve the asset URIIdentifier given the assetId NOTE: This only checks
   * the information for the LATEST version of the model, which is available in the models.
   *
   * @param assetId the assetId to get the URIIdentifier for
   * @return URIIdentifier for the assetId or Empty
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
    if(enterpriseAssetVersionId.getPath().contains(VERSIONS)) {
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
    if(any) {
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
        String versionFromId = enterpriseAssetVersionId.substring(
            enterpriseAssetVersionId.lastIndexOf(VERSIONS));
        if (versionFromId.equals(VERSIONS + "/" + versionTag)) {
          return Optional.of(
              URI.create(enterpriseAssetVersionId));
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
  public String getArtifactId(URIIdentifier assetId, boolean any) throws NotLatestVersionException {
    if (any) {
      return artifactIdFromModel(models, assetId);
    } else {
      return artifactIdFromModel(publishedModels, assetId);
    }
  }

  private String artifactIdFromModel(ResultSetRewindable theModels,
      URIIdentifier assetId) throws NotLatestVersionException {
    theModels.reset();
    // this will only match if the exact version of the asset is available on a latest model
    while (theModels.hasNext()) {
      QuerySolution soln = theModels.nextSolution();
      if (logger.isDebugEnabled()) {
        logger.debug("getArtifactId for assetId: {}", assetId.toStringId());
        logger.debug("assetId.getUri().toString: {}", assetId.getUri());
        logger.debug("assetId.getVersionId().toString: {}", assetId.getVersionId());
        logger.debug("assetId.getTag(): {}", assetId.getTag());
      }
      // versionId value has the UUID of the asset/versions/versionTag, so this will match id and version
      if (soln.getLiteral(ASSET_ID).getString().contains(assetId.getVersionId().toString())) {
        return soln.getResource(MODEL).getURI();
        // the requested version of the asset doesn't exist on the latest model, check if the
        // asset is the right asset for the model and if so, throw error with fileId
      } else if (soln.getLiteral(ASSET_ID).getString().contains(assetId.getTag())) {
        throw new NotLatestVersionException(soln.getResource(MODEL).getURI());
      }
    }
    return null;
  }

  /**
   * Get the fileId for the asset from published models
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
    while (theModels.hasNext()) {
      QuerySolution soln = theModels.nextSolution();

      if (soln.getLiteral(ASSET_ID).getString().contains(assetId.toString())) {
        return Optional.ofNullable(soln.getLiteral(FILE_ID).getString());
      }
    }
    return Optional.empty();
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
      if (soln.getResource(MODEL).getURI().equals(internalId)) {
        return Optional.ofNullable(soln.getLiteral(FILE_ID).getString());
      }
    }
    return Optional.empty();
  }


  /**
   * Get the mimeType for the asset
   * All models have a mimetype.
   * If this becomes a performance bottleneck, can look at separating out searches for published
   * models only.
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
   * Get the name of the artifact for the asset
   * If this becomes a performance bottleneck, can look at separating out searches for published
   * models only.
   *
   * @param assetId The id of the asset looking for
   * @return the name of the artifact as specified in the triples
   */
  public Optional<String> getArtifactName(URIIdentifier assetId) {
    models.reset();
    while (models.hasNext()) {
      QuerySolution soln = models.nextSolution();
      if (soln.getLiteral(ASSET_ID).getString().contains(assetId.getUri().toString())) {
        return Optional.ofNullable(soln.getLiteral(ARTIFACTNAME).getString());
      }
    }
    return Optional.empty();
  }


  /**
   * Get the mimetype using the model id
   * All models have a mimetype. If performance becomes an issue, might want to separate
   * out searching in published models instead of all models.
   *
   * @param internalId the internal id of the model
   * @return the mimetype as specified in the triples
   */
  public Optional<String> getMimetype(String internalId) {
    models.reset();
    while (models.hasNext()) {
      while (models.hasNext()) {
        QuerySolution soln = models.nextSolution();
        if (soln.getResource(MODEL).getURI().equals(internalId)) {
          return Optional.ofNullable(soln.getLiteral(MIME_TYPE).getString());
        }
      }
    }
    return Optional.empty();
  }

  /**
   * Get the state using the model id
   * State only exists on published models
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
   * Get the version of the model artifact
   * Versions only exist on published models.
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
   * Given the internal id for the model, get the information about other models it imports.
   *
   * @param docId the artifact id
   * @return a set of resources for the artifacts used by this artifact (dependencies)
   */
  public Set<Resource> getArtifactImports(String docId) {
    Set<Resource> resources = null;

    if (!Util.isEmpty(docId)) {
      String id = docId.substring(docId.lastIndexOf('/') + 1);
      for (Entry<Resource, Set<Resource>> entry : artifactToArtifactIDMap.entrySet()) {
        Resource k = entry.getKey();
        if (k.getLocalName().substring(1).equals(id)) {
          resources = artifactToArtifactIDMap.get(k);
        }
      }
    }
    return resources;
  }

  /**
   * given the internal id for the model (artifact),
   * get the information for the assets the model imports
   * each artifact should have an assetID, this allows us to map asset<->asset relations
   *
   * @param docId the artifact id
   * @return a set of resources for the assets used by this model (dependencies)
   */
  public List<URIIdentifier> getAssetRelations(String docId) {
    List<URIIdentifier> assets = new ArrayList<>();

    if (!Util.isEmpty(docId)) {
      // first find the artifact in the artifactToArtifact mapping
      String id = docId.substring(docId.lastIndexOf('/') + 1);
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

  private Optional<URIIdentifier> getAssetRelation(Resource dependent, Resource subject) {
    logger.debug("dependent URI: {}", dependent.getURI());
    String dependentId = dependent.getURI().substring(dependent.getURI().lastIndexOf('/'));
    publishedModels.reset();
    while (publishedModels.hasNext()) {
      QuerySolution soln = publishedModels.nextSolution();
      logger.debug("soln MODEL URI: {} ", soln.getResource(MODEL).getURI());
      if (soln.getResource(MODEL).getURI().equals(dependent.getURI())) {
        logger.debug("Asset ID for Model: {}", soln.getLiteral(ASSET_ID));
        return Optional.of(new URIIdentifier()
            .withUri(URI.create(soln.getLiteral(ASSET_ID).getString())));
      } else {
        logger.debug("Dependency {} for {} is NOT Published", dependentId, subject.getLocalName());
      }
    }
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

}
