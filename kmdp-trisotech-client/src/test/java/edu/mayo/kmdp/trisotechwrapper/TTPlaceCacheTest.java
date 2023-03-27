
package edu.mayo.kmdp.trisotechwrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.mayo.kmdp.trisotechwrapper.components.TTDigitalEnterpriseServerClient;
import edu.mayo.kmdp.trisotechwrapper.components.TTWebClient;
import edu.mayo.kmdp.trisotechwrapper.components.cache.CaffeineCacheManager;
import edu.mayo.kmdp.trisotechwrapper.components.graph.TTGraphQueryHelper;
import edu.mayo.kmdp.trisotechwrapper.config.TTWConfigParamsDef;
import edu.mayo.kmdp.trisotechwrapper.config.TTWEnvironmentConfiguration;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechPlace;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechPlaceData;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.jupiter.api.Test;

class TTPlaceCacheTest {

  static final String TEST_PLACE_ID = "37cf0951-332f-49b0-8e8c-3d8e157780f1";

  @Test
  void loadPlaceData() {
    var ppi = mockCacheManager().getPlaceCache()
        .get(new TrisotechPlace(TEST_PLACE_ID, "mock"));

    assertNotNull(ppi);
    assertEquals(7, ppi.getModelToManifestMappings().size());
    assertEquals(5, ppi.getAssetToManifestMappings().size());

    var src = "http://www.trisotech.com/definitions/_ed4a5a45-3304-4117-b09f-865673219ef4";
    var tgt = "http://www.trisotech.com/definitions/_99302f65-b27b-4830-b7cb-a64c1578e0fc";
    assertTrue(ppi.getModelToManifestMappings().containsKey(src));

    var info = ppi.getModelToManifestMappings().get(src);
    assertTrue(info.getModelDependencies().contains(tgt));

    ppi.getModelToManifestMappings().values().stream()
        .flatMap(x -> x.getExposedServices().stream())
        .forEach(k -> assertEquals(1, ppi.getAssetToManifestMappings()
            .getOrDefault(k, Collections.emptySortedSet()).size()));
  }

  CaffeineCacheManager mockCacheManager() {
    var cfg = new TTWEnvironmentConfiguration();
    cfg.setTyped(TTWConfigParamsDef.BASE_URL, "http://mock.org/des");
    cfg.setTyped(TTWConfigParamsDef.REPOSITORY_PATHS, TEST_PLACE_ID + "/");
    return new CaffeineCacheManager(
        mockWebClient(cfg),
        dox -> dox,
        cfg
    );
  }

  private TTDigitalEnterpriseServerClient mockWebClient(TTWEnvironmentConfiguration cfg) {
    return new TTWebClient(cfg) {
      private final Dataset graph = loadTestGraph();

      private Dataset loadTestGraph() {
        var url = TTPlaceCacheTest.class.getResource("/testPlaceGraph.rdf");
        assertNotNull(url);
        var model = ModelFactory.createDefaultModel().read(url.toString());

        return DatasetFactory.create().addNamedModel(
            "http://trisotech.com/graph/1.0/graph#" + TEST_PLACE_ID,
            model);
      }

      @Override
      public ResultSet askQuery(Query query) {
        return QueryExecutionFactory.create(query, graph).execSelect();
      }

      @Override
      public List<TrisotechFileInfo> getModelPreviousVersions(String repositoryId,
          String fileId) {
        var mockHxInfo = new TrisotechFileInfo();
        mockHxInfo.setId(fileId);
        mockHxInfo.setVersion("1.0.1");
        mockHxInfo.setState("Draft");
        return List.of(mockHxInfo);
      }

      @Override
      public Optional<TrisotechPlaceData> getPlaces() throws IOException {
        var places = new TrisotechPlaceData();
        places.setData(List.of(new TrisotechPlace(TEST_PLACE_ID, "Mock")));
        return Optional.of(places);
      }
    };
  }


  static void reloadPlaceData() throws IOException, URISyntaxException {

    var cfg = new TTWEnvironmentConfiguration();
    cfg.setTyped(TTWConfigParamsDef.API_TOKEN,
        System.getenv(TTWConfigParamsDef.API_TOKEN.getName()));
    cfg.setTyped(TTWConfigParamsDef.BASE_URL,
        System.getenv(TTWConfigParamsDef.BASE_URL.getName()));
    cfg.ensureVariablesSet();

    if (cfg.get(TTWConfigParamsDef.API_TOKEN).isEmpty()) {
      return;
    }

    var webClient = new TTWebClient(cfg);
    var accessible = webClient.getPlaces()
        .map(pd -> pd.getData().stream().anyMatch(p -> p.getId().equals(TEST_PLACE_ID)))
        .orElse(false);
    if (!accessible) {
      return;
    }

    var model = TTGraphQueryHelper.exportPlace(webClient, TEST_PLACE_ID);
    var url = TTPlaceCacheTest.class.getResource("/testPlaceGraph.rdf");
    assertNotNull(url);

    model.write(new FileOutputStream(new File(url.toURI())));
  }

  public static void main(String[] args) throws IOException, URISyntaxException {
    reloadPlaceData();
  }

}
