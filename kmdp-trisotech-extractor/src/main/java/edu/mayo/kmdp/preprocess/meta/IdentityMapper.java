/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.mayo.kmdp.preprocess.meta;

import edu.mayo.kmdp.id.helper.DatatypeHelper;
import edu.mayo.kmdp.preprocess.NoArtifactVersionException;
import edu.mayo.kmdp.terms.generator.util.HierarchySorter;
import edu.mayo.kmdp.trisotechwrapper.TrisotechApiUrls;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.apache.http.Header;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Resource;
import org.omg.spec.api4kp._1_0.identifiers.URIIdentifier;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static edu.mayo.kmdp.trisotechwrapper.TrisotechApiUrls.TOKEN;
import static edu.mayo.kmdp.util.XMLUtil.loadXMLDocument;
import static org.apache.http.HttpHeaders.AUTHORIZATION;

// TODO: Rework for Trisotech data CAO
// What is the purpose of this class? CAO

/**
 * IdentityMapper is used to map dependencies between Trisotech artifacts.
 * SPARQL is used to query the triples from Trisotech for this information.
 * Each artifact has its own assetID, which is also accessible via triples,
 * so asset-to-asset mapping can be handled here as well.
 *
 */
public class IdentityMapper {

  // PREFIX sets up the namespaces to be used in the query
  private static final String QUERYSTRINGPREFIX =
      "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
          + "PREFIX tt: <http://www.trisotech.com/graph/1.0/element#>"
          + "PREFIX ttr: <http://www.trisotech.com/graph/1.0/elementRel#>"
          + "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>"
          + "PREFIX owl: <http://www.w3.org/2002/07/owl#>"
          + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
          + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>";
  private static final String TRISOTECH_GRAPH = "<http://trisotech.com/graph/1.0/graph#";
  public static final String ASSET_ID = "?assetId";
  public static final String FILE_ID = "?fileId";
  public static final String MODEL = "?model";
  public static final String MIME_TYPE = "?mimeType";

  private HierarchySorter hierarchySorter = new HierarchySorter();
  private Map<Resource, Set<Resource>> artifactToArtifactIDMap = new HashMap<>();
  private ResultSetRewindable models;
  private List<Resource> orderedModels;


  public IdentityMapper() {
    // TODO: default to MEA-Test for testing, but MEA for prod CAO
    this(TrisotechApiUrls.MEA_TEST_ID);
  }

  public IdentityMapper(String place) {
    createMap(query(getQueryStringRelations(place)));
    models = ResultSetFactory.makeRewindable(query(getQueryStringModels(place)));
    orderedModels = hierarchySorter.linearize(getModelList(models), artifactToArtifactIDMap);
  }

  public List<Resource> getOrderedModels() {
    return orderedModels;
  }

  private void createMap(ResultSet results) {
    while (results.hasNext()) {
      QuerySolution soln = results.nextSolution();
      artifactToArtifactIDMap
          .computeIfAbsent(soln.getResource(MODEL), s -> new HashSet<Resource>())
          .add(soln.getResource("?dModel"));
    }
  }

  /**
   * return the list of models from the triples
   *
   * @param modelResults the resultSet from a SPARQL query
   * @return
   */
  private List<Resource> getModelList(ResultSet modelResults) {
    List<Resource> modelList = new ArrayList<>();
    while (modelResults.hasNext()) {
      QuerySolution soln = modelResults.nextSolution();
      modelList.add(soln.getResource(MODEL));
    }
    return modelList;
  }

  private ResultSet query(String queryString) {
    Query query = QueryFactory.create(queryString);

    // TODO: have as a property? CAO
    String endpoint = "https://mc.trisotech.com/ds/query";
    Header header = new BasicHeader(AUTHORIZATION, "Bearer " + TOKEN);

    HttpClient httpClient = HttpClientBuilder.create()
        .setDefaultHeaders(Collections.singleton(header))
        .build();
    QueryExecution qexec = QueryExecutionFactory
        .sparqlService(endpoint, query, httpClient);
    return qexec.execSelect();
  }


  /**
   * Build the queryString needed to query the DMN->DMN and CMMN->DMN relations in the place requested.
   * This is specific to Trisotech.
   *
   * TODO: only get those models that are Published? CAO
   *
   * @param place
   * @return
   */
  private String getQueryStringRelations(String place) {
    // value after # specifies the 'place/repository' in Trisotech we are querying
    String graph = TRISOTECH_GRAPH + place + ">";
    // The following gets the DMN->DMN AND the CMMN->DMN mappings
    // documenting here for future reference if new to SPARQL, or just forgot ;-)
    return QUERYSTRINGPREFIX
        // ?model and ?dModel are our variables we will expect in the output
        // ?model will be a DMN or CMMN model and ?dModel will be the DMN model that is used in the ?model
        + "SELECT ?model ?dModel"
        // Trisotech requires a NAMED GRAPH be used or will fail
        + " FROM NAMED " + graph
        + "WHERE { "
        + " GRAPH " + graph
        + "  {"
        + "    {"
        // gets the models that are of type DMNModel
        + "                             ?model a tt:DMNModel."
        // gets the models that are of type DMNModel
        + "                             ?dModel a tt:DMNModel."
        // gets elements that are childOf for the model; the '*' will get all 'childOf' matches
        + "                             ?cElement ttr:childOf* ?model."
        // do the same thing here with the second set of DMNModels
        + "                             ?dElement ttr:childOf* ?dModel."
        // Now match the elements on the 'semantic' -- this is the link between the models
        + "                             ?cElement ttr:semantic ?dElement."
        + "    }"
        + " UNION " // now do the same for CMMN->DMN models
        + "    {"
        // gets the models that are of type CMMNModel
        + "                             ?model a tt:CMMNModel."
        + "                             ?dModel a tt:DMNModel."
        + "                             ?cElement ttr:childOf* ?model."
        + "                             ?dElement ttr:childOf* ?dModel."
        + "                             ?cElement ttr:semantic ?dElement."
        + "    }"
        + "  }"
        + "}";

  }


  /**
   * Build the queryString needed to query models with their fileId and assetId.
   * This is specific to Trisotech.
   * TODO: only return models that have a version (i.e. are published)? CAO
   *
   * @param place the location the query should use 'place/repository/directory'
   * @return
   */
  private String getQueryStringModels(String place) {
    // value after # specifies the 'place/repository' in Trisotech we are querying
    String graph = TRISOTECH_GRAPH + place + ">";
    // The following gets the DMN->DMN AND the CMMN->DMN mappings
    // documenting here for future reference if new to SPARQL, or just forgot ;-)
    return QUERYSTRINGPREFIX
        // ?model is our variable we will expect in the output
        // ?model will be a DMN or CMMN model
        // ?fileId is the id for the physical file (artifact)
        // ?assetId is the enterprise ID that is associated with this artifact; can vary by version
        // ?version is the latest version of the model
        // ?mimeType helps with the ability to retrieve the correct filetype for the model
        + "SELECT ?model ?fileId ?assetId ?version ?mimeType"
        // Trisotech requires a NAMED GRAPH be used or will fail
        + " FROM NAMED " + graph
        + "WHERE { "
        + " GRAPH " + graph
        + "  {"
        + "    {"
        // gets the models that are of type DMNModel
        + "         ?model a tt:DMNModel."
        // gets the fileId
        + "         ?model ttr:fileId ?fileId."
        // gets the version - only latest version is returned; there will only be a version if the model is published
        + "         ?model ttr:version ?version."
        // gets the mimeType
        + "         ?model ttr:mimeType ?mimeType."
        // the following processing gets the assetId
        // first get the children of the model
        + "         ?cElement ttr:childOf* ?model."
        // then get the customAttribute of the children
        + "         ?cElement ttr:customAttribute ?caElement."
        // then get the assetId from the customAttribute
        // TODO: do we need to confirm the customAttributeKey to verify it is what we expect? and how would that happen? CAO
        + "         ?caElement ttr:customAttributeValue ?assetId."
        // TODO: only return published models? CAO
        + "    }"
        + " UNION "
        + "    {"
        // gets the models that are of type CMMNModel
        + "         ?model a tt:CMMNModel."
        // gets the fileId
        + "         ?model ttr:fileId ?fileId."
        // gets the version
        + "         ?model ttr:version ?version."
        // gets the mimeType
        + "         ?model ttr:mimeType ?mimeType."
        // the following processing gets the assetId
        // first get the children of the model
        + "         ?cElement ttr:childOf* ?model."
        // then get the customAttribute of the children
        + "         ?cElement ttr:customAttribute ?caElement."
        // then get the assetId from the customAttribute
        // TODO: do we need to confirm the customAttributeKey to verify it is what we expect? CAO
        + "         ?caElement ttr:customAttributeValue ?assetId."
        + "    }"
        + "  }"
        + "}";

  }

  public URI getVersionedAsset(UUID assetId, String versionTag) {
    return null; // TODO: fix this CAO
  }

  /**
   * Get the assetId for the artifact given
   * TODO: is the artifactId the ID for the artifact per the triple, OR is it the FILE id?
   * TODO cont: This code is assuming artifact per the triple;
   * TODO cont: this code appears to be all internal, so I can decide which one it is and document as such. CAO
   *
   * @param artifactId Id for a artifact in the Trisotech models
   * @return
   */
  public Optional<URIIdentifier> getAssetId(URIIdentifier artifactId) {
    return getAssetId(artifactId.getUri().toString());
  }

  // input should be artifactId (from the triple); version is version of artifact
  public Optional<URIIdentifier> getAssetId(URIIdentifier artifactId, String versionTag)
      throws NoArtifactVersionException {
    models.reset();
    while (models.hasNext()) {
      QuerySolution soln = models.nextSolution();

      if (soln.getResource(MODEL).getURI().equals(artifactId.getUri().toString())) {
        if (soln.getLiteral("?version").getString().equals(versionTag)) {
          return Optional.of(DatatypeHelper.uri(soln.getLiteral(ASSET_ID).toString()));
        } else { // have the artifact, but not version looking for; more work to see if the version exists
          throw new NoArtifactVersionException(soln.getResource(MODEL).getURI());
        }
      }

    }
    return Optional.empty();

  }


  /**
   * get the Asset that matches the model for the fileId provided
   * model ID is not the same as fileId, but fileInfo has fileid.
   * Will return the asset that maps to the latest version of the model.
   *
   * @param fileId the fileid for the model
   * @return
   */
  public Optional<URIIdentifier> getAssetId(String fileId) {
    System.out.println("getAssetId for fileId: " + fileId);
    models.reset();
    while (models.hasNext()) {
      QuerySolution soln = models.nextSolution();

      if (soln.getLiteral(FILE_ID).getString().equals(fileId)) {
        return Optional.of(DatatypeHelper.uri(soln.getLiteral(ASSET_ID).toString()));
      }
    }
    return Optional.empty();
  }

  public String getArtifactId(URIIdentifier assetId) throws NoArtifactVersionException {
    models.reset();
    // this will only match if the exact version of the asset is available on a latest model
    while (models.hasNext()) {
      QuerySolution soln = models.nextSolution();
      if (soln.getLiteral(ASSET_ID).getString().equals(assetId.getUri().toString())) {
        return soln.getResource(MODEL).getURI();
        // the requested version of the asset doesn't exist on the latest model, check if the
        // asset is the right asset for the model and if so, throw error with fileId
      } else if (soln.getLiteral(ASSET_ID).getString().contains(assetId.getTag())) {
        throw new NoArtifactVersionException(soln.getResource(MODEL).getURI());
      }
    }
    return null;
  }

  public Optional<String> getFileId(UUID assetId) {
    models.reset();
    while (models.hasNext()) {
      QuerySolution soln = models.nextSolution();
      System.out.println("soln model: " + soln.getResource(MODEL)
          + " soln assetId: " + soln.getLiteral(IdentityMapper.ASSET_ID).getString()
          + " soln fileId: " + soln.getLiteral(FILE_ID)
          + " compared to assetId: " + assetId.toString());
      if (soln.getLiteral(ASSET_ID).getString().contains(assetId.toString())) {
        return Optional.ofNullable(soln.getLiteral(FILE_ID).getString());
      }
    }
    return Optional.empty();
  }


  public Optional<String> getMimetype(UUID assetId) {
    models.reset();
    while (models.hasNext()) {
      QuerySolution soln = models.nextSolution();
      System.out.println("soln model: " + soln.getResource(MODEL)
          + " soln assetId: " + soln.getLiteral(ASSET_ID)
          + " soln fileId: " + soln.getLiteral(FILE_ID)
          + " soln mimeType: " + soln.getLiteral(MIME_TYPE)
          + " compared to assetId: " + assetId.toString());
      if (soln.getLiteral(ASSET_ID).getString().contains(assetId.toString())) {
        return Optional.ofNullable(soln.getLiteral(MIME_TYPE).getString());
      }
    }
    return Optional.empty();
  }

  public Optional<String> getFileId(String internalId) {
    models.reset();
    while (models.hasNext()) {
      QuerySolution soln = models.nextSolution();
      if (soln.getResource(MODEL).getURI().equals(internalId)) {
        return Optional.ofNullable(soln.getLiteral(FILE_ID).getString());
      }
    }
    return Optional.empty();
  }

  public Set<Resource> getArtifactImports(Optional<String> docId) {
    Set<Resource> resources = null;

    String id = docId.get().substring(docId.get().lastIndexOf('/') + 1);
    System.out.println("id: " + id);
    for (Entry<Resource, Set<Resource>> entry : artifactToArtifactIDMap.entrySet()) {
      Resource k = entry.getKey();
      Set<Resource> v = entry.getValue();
      if (k.getLocalName().substring(1).equals(id)) {
        System.out.println("found id in artifactToArtifactIDMap");
        resources = artifactToArtifactIDMap.get(k);
      }

    }
    orderedModels.forEach(model -> {
      System.out.println("orderedModels URI: " + model.getURI()
          + " localName: " + model.getLocalName()
          + " nameSpace: " + model.getNameSpace()
          + " toString: " + model.toString()
          + " listProperties: " + model.listProperties());
    });
    orderedModels.forEach(model -> {
      if (model.getLocalName().substring(1).equals(id)) {
        System.out.println("found model that matches id: " + id);
        while (model.listProperties().hasNext()) {
          System.out.println("model property: " + model.listProperties().next().getString());
        }
      }

    });
    return resources;
  }

  public List<URIIdentifier> getAssetRelations(Optional<String> docId) {
    // first find the artifact in the artifactToArtifact mapping
    String id = docId.get().substring(docId.get().lastIndexOf('/') + 1);
    List<URIIdentifier> assets = new ArrayList<>();
    System.out.println("id: " + id);
    artifactToArtifactIDMap.forEach((k, v) -> {
      if (k.getLocalName().substring(1).equals(id)) {
        System.out.println("found id in artifactToArtifactIDMap");
        // once found, for each of the artifacts it is dependent on, find the asset id for those artifacts in the models
        models.reset();
        while (models.hasNext()) {
          QuerySolution soln = models.nextSolution();
          if (soln.getResource(MODEL).getURI().equals(k.getLocalName())) {
            try {
              assets
                  .add(new URIIdentifier().withUri(new URI(soln.getLiteral(ASSET_ID).getString())));
            } catch (URISyntaxException e) {
              e.printStackTrace();
            }
          }

        }
      }
    });
    return assets;
  }

}
