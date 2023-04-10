package edu.mayo.kmdp.kdcaci.knew.trisotech;

import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newId;
import static org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries.Clinical_Case_Management_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Encoded_Knowledge_Expression;

import edu.mayo.kmdp.kdcaci.knew.trisotech.components.TTServerContextAwareHrefBuilder;
import edu.mayo.kmdp.ops.tranx.bpm.KarsAnonymousCcpmToPlanDefPipeline;
import edu.mayo.kmdp.util.ws.ResponseHelper;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.KnowledgeAssetCatalogApi;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.KnowledgeAssetRepositoryApi;
import org.omg.spec.api4kp._20200801.datatypes.Bindings;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adapter class that exposes the CCPM PlanDefinition Pipeline
 */
@RestController
public class CCPMPreviewEndpoint {

  protected final
  TrisotechAssetRepository triso;

  protected final
  TTServerContextAwareHrefBuilder hrefBuilder;

  protected final KarsAnonymousCcpmToPlanDefPipeline pipeline;

  @Autowired
  public CCPMPreviewEndpoint(
      TrisotechAssetRepository triso,
      TTServerContextAwareHrefBuilder hrefBuilder) {
    this.triso = triso;
    this.hrefBuilder = hrefBuilder;

    this.pipeline = new KarsAnonymousCcpmToPlanDefPipeline(
        KnowledgeAssetCatalogApi.newInstance(triso),
        KnowledgeAssetRepositoryApi.newInstance(triso),
        (modelId, versionTag1, query, xParams) -> Answer.of(List.of(new Bindings())),
        URI.create("https://ontology.mayo.edu/taxonomies/clinicalsituations"));
  }

  @GetMapping(value = "/ccpms",
      produces = "text/html")
  public ResponseEntity<List<Pointer>> listCCPMs() {

    var cases = triso.listKnowledgeAssets(
        Clinical_Case_Management_Model.getTag(), null, null, 0, -1);
    cases.ifSuccess(ptrs -> ptrs.forEach(this::rewriteUrl));

    return ResponseHelper.asResponse(cases);
  }

  private void rewriteUrl(Pointer ptr) {
    var original = ptr.getHref().toString();
    var rerouted = original.replace("/cat/assets", "/ccpms");
    ptr.setHref(URI.create(rerouted));
  }


  @GetMapping(value = "/ccpms/{assetId}/versions/{versionTag}",
      produces = "application/json")
  public ResponseEntity<byte[]> generatePlanDefinition(
      @PathVariable UUID assetId,
      @PathVariable String versionTag) {
    var rootId = newId(assetId, versionTag);

    var ans = pipeline.trigger(rootId, Encoded_Knowledge_Expression)
        .flatOpt(AbstractCarrier::asBinary);

    return ResponseHelper.asResponse(ans);
  }


}
