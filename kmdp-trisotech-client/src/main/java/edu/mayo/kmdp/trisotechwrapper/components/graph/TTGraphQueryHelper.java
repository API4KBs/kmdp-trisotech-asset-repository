package edu.mayo.kmdp.trisotechwrapper.components.graph;

import static edu.mayo.kmdp.trisotechwrapper.config.TTConstants.TRISOTECH_GRAPH;

import edu.mayo.kmdp.trisotechwrapper.components.TTDigitalEnterpriseServerClient;
import edu.mayo.kmdp.trisotechwrapper.components.TTWebClient;
import edu.mayo.kmdp.trisotechwrapper.config.TTWEnvironmentConfiguration;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechPlace;
import edu.mayo.kmdp.util.FileUtil;
import java.util.Set;
import javax.annotation.Nonnull;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class used to construct and submit SPARQL queries to the TT DES SPARQL Endpoint
 */
public final class TTGraphQueryHelper {

  /**
   * Logger
   */
  private static final Logger logger = LoggerFactory.getLogger(TTGraphQueryHelper.class);

  /**
   * No-op Constructor. This class only contains static functions and should not be instantiated
   */
  private TTGraphQueryHelper() {
    // helper
  }


  /**
   * Indexes a Place content, using information queried from the TT DES Knowledge Graph
   * <p>
   * Performs three queries:
   * <ol>
   *   <li>Queries for Models and their descriptive metadata</li>
   *   <li>Queries for Model/Model relationships</li>
   *   <li>Queries for Services inferred to be exposed by a Model</li>
   * </ol>
   * This method also insures that the Place/Path filters are applied, excluding any model that does
   * not match the criteria from the index
   *
   * @param webClient  the DES Client, used to interact with the SPARQL endpoint
   * @param focusPlace the Place to be (re)indexed
   * @param paths      the Place/Path filter
   * @param cfg        the Environment configuration
   * @return a {@link PlacePathIndex} for the given Place
   */
  public static PlacePathIndex reindexPlace(
      @Nonnull final TTDigitalEnterpriseServerClient webClient,
      @Nonnull final TrisotechPlace focusPlace,
      @Nonnull final Set<String> paths,
      @Nonnull final TTWEnvironmentConfiguration cfg) {
    long t0 = 0;
    if (logger.isDebugEnabled()) {
      t0 = System.currentTimeMillis();
      logger.debug("Start Indexing of Place {}", focusPlace);
    }

    var allModels = query(webClient, getQueryStringModels(), focusPlace.getId());
    var relations = query(webClient, getQueryStringRelations(), focusPlace.getId());
    var services = query(webClient, getQueryStringServices(), focusPlace.getId());
    var ppi = PlacePathIndex.index(
        focusPlace, paths,
        allModels, relations, services,
        webClient::getModelPreviousVersions,
        cfg);
    if (logger.isDebugEnabled()) {
      logger.debug("... Indexing of place {} completed in {} ms",
          focusPlace, (System.currentTimeMillis() - t0));
    }
    return ppi;
  }


  /**
   * Perform the query
   *
   * @param webClient   the DES Client, used to interact with the SPARQL endpoint
   * @param queryString the SPARQL query to be executed, as a String
   * @param placeId     the UUID of the place to apply the Query to
   * @return The ResultSet with the results from the query
   */
  public static ResultSet query(
      @Nonnull final TTDigitalEnterpriseServerClient webClient,
      @Nonnull final String queryString,
      @Nonnull final String placeId) {
    ParameterizedSparqlString sparqlString = new ParameterizedSparqlString(queryString);
    String graph = TRISOTECH_GRAPH + placeId;
    // set NAMED
    sparqlString.setIri(0, graph);
    // set GRAPH
    sparqlString.setIri(1, graph);
    Query query = sparqlString.asQuery();

    return webClient.askQuery(query);
  }


  /**
   * Build the queryString needed to query the BPM+ to BPM+ relations in the place requested. .
   *
   * @return the SPARQL query string to query the relations between models
   */
  public static String getQueryStringRelations() {
    return FileUtil.read(TTGraphQueryHelper.class
            .getResourceAsStream("/queryRelations.tt.sparql"))
        .orElseThrow(IllegalStateException::new);
  }

  /**
   * Build the queryString needed to describe Models
   *
   * @return the SPARQL query string needed to query the models
   */
  public static String getQueryStringModels() {
    return FileUtil.read(TTGraphQueryHelper.class
            .getResourceAsStream("/queryModels.tt.sparql"))
        .orElseThrow(IllegalStateException::new);
  }

  /**
   * Build the queryString needed to query models for asset / service relationships
   *
   * @return the SPARQL query string needed to query the models / asset / service relationships
   */
  public static String getQueryStringServices() {
    return FileUtil.read(TTGraphQueryHelper.class
            .getResourceAsStream("/queryServiceToModels.tt.sparql"))
        .orElseThrow(IllegalStateException::new);
  }

  /**
   * Build the queryString needed to query an entire place.
   * <p>
   * Note: this query should only be used offline, for testing or discovery purposes
   *
   * @return the SPARQL query string to get an entire place content
   */
  public static String getQueryAll() {
    return FileUtil.read(TTGraphQueryHelper.class
            .getResourceAsStream("/queryEntirePlace.tt.sparql"))
        .orElseThrow(IllegalStateException::new);
  }


  /**
   * Exports an entire Place in the TT DES Graph, as a Jena RDF Graph
   * <p>
   * Note: this method should only be used offline, for testing or discovery purposes
   *
   * @param client       the DES Client, used to interact with the SPARQL endpoint
   * @param focusPlaceId the Place to be exported
   * @return the Place content as an RDF graph
   */
  public static Model exportPlace(
      @Nonnull final TTWebClient client,
      @Nonnull final String focusPlaceId) {
    var all = query(client, getQueryAll(), focusPlaceId);
    var model = ModelFactory.createDefaultModel();
    while (all.hasNext()) {
      var sol = all.next();
      var s = sol.getResource("s");
      var p = model.createProperty(sol.get("p").asNode().getURI());
      var o = sol.get("o");
      model.add(s, p, o);
    }
    return model;
  }


}
