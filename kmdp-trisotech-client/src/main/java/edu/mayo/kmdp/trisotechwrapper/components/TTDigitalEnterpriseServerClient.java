package edu.mayo.kmdp.trisotechwrapper.components;

import edu.mayo.kmdp.trisotechwrapper.TTAPIAdapter;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechExecutionArtifact;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechPlace;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.apache.jena.query.Query;
import org.apache.jena.query.ResultSet;
import org.w3c.dom.Document;

/**
 * Internal interface that provides a view on the Trisotech DES Web APIs.
 * <p>
 * Implementations are expected to encapsulate any actual HTTP Web client.
 * <p>
 * Creates a functional abstraction layer, exposing all and only the operations that support a
 * {@link TTAPIAdapter}, which in turn supports the implementation of the API4KP interfaces.
 */
public interface TTDigitalEnterpriseServerClient {

  /**
   * Retrieves the current version of a given Model, as metadata.
   * <p>
   * Note that the resulting information should coincide with
   *
   * @param repositoryId the ID of the Place where the Model is stored
   * @param modelUri     the ID of the Model
   * @return a {@link TrisotechFileInfo} for the current version of the given Model
   */
  @Nonnull
  Optional<TrisotechFileInfo> getModelLatestVersion(
      @Nonnull final String repositoryId,
      @Nonnull final String modelUri);

  /**
   * Retrieves the history of a given Model, as metadata references to the previous versions
   *
   * @param repositoryId the ID of the Place where the Model is stored
   * @param modelUri     the ID of the Model
   * @return a List of {@link TrisotechFileInfo}, one per previous version of the modelUri, sorted
   * by Date
   */
  @Nonnull
  List<TrisotechFileInfo> getModelPreviousVersions(
      @Nonnull final String repositoryId,
      @Nonnull final String modelUri);

  /**
   * Lists the Places accessible to the Client
   *
   * @return a Place descriptor that contains the
   */
  @Nonnull
  List<TrisotechPlace> getPlaces();

  /**
   * Acquires a copy of a Model, given its internal descriptor, which includes the Model ID,
   * mimeType and URL
   *
   * @param from the Model internal metadata
   * @return the Model as a Document, if successful
   */
  @Nonnull
  Optional<Document> downloadXmlModel(
      @Nonnull final TrisotechFileInfo from);

  /**
   * Acquires the descriptors of the decision / process services deployed in a given execution
   * environment.
   * <p>
   * TODO: the ServiceLibrary(s) should be configurable, and more than one environment should be supported
   *
   * @param execEnv the execution environment
   * @return metadata about the services deployed in that environment
   */
  @Nonnull
  List<TrisotechExecutionArtifact> getExecutionArtifacts(
      @Nonnull final String execEnv);

  /**
   * Uploads a Model data to a given Place in the DES server
   * <p>
   * The Model must be in a format such that the DES server can import that format.
   *
   * @param manifest     the Model metadata, used to determine where the content will be uploaded
   * @param fileContents the Model data, in binary form
   * @return true if successful, false otherwise
   */
  boolean uploadXmlModel(
      @Nonnull SemanticModelInfo manifest,
      @Nonnull final byte[] fileContents);

  /**
   * Submits a SPARQL Query to the Trisotech DES SPARQL endpoint, to query the DES Knowledge Graph
   * <p>
   *
   * @param query the SPARQL query
   * @return the query results, as a set of variable bindings
   */
  @Nonnull
  ResultSet askQuery(
      @Nonnull final Query query);
}
