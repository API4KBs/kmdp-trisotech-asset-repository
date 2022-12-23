package edu.mayo.kmdp.kdcaci.knew.trisotech;

import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;

import edu.mayo.kmdp.kdcaci.knew.trisotech.components.TTServerContextAwareHrefBuilder;
import edu.mayo.kmdp.language.LanguageDeSerializer;
import edu.mayo.kmdp.language.LanguageValidator;
import edu.mayo.kmdp.language.parsers.cmmn.v1_1.CMMN11Parser;
import edu.mayo.kmdp.language.parsers.dmn.v1_2.DMN12Parser;
import edu.mayo.kmdp.language.parsers.surrogate.v2.Surrogate2Parser;
import edu.mayo.kmdp.language.validators.cmmn.v1_1.CCPMProfileCMMNValidator;
import edu.mayo.kmdp.language.validators.dmn.v1_2.CCPMProfileDMNValidator;
import edu.mayo.kmdp.util.ws.ResponseHelper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.Explainer;
import org.omg.spec.api4kp._20200801.Severity;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.CompositeKnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.CompositeStructType;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.repository.asset.KARSHrefBuilder.HrefType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.Problem;

/**
 * Adapter class that exposes the {@link CCPMProfileCMMNValidator} and
 * {@link CCPMProfileDMNValidator} as a web endpoint
 * <p>
 * Supplements a {@link TrisotechAssetRepository}, and uses it as a source of models to validate.
 * Adapts the 'shape' of the models to fit the expected input of the Validators - a Set of
 * Asset/Artifact pairs. Formats the results into basic HTML.
 * <p>
 * This class should eventually be refactored to separate the presentation layer, and/or deployed
 * separately
 */
@RestController
public class ValidationReportEndpoint {

  @Autowired
  TrisotechAssetRepository triso;

  @Autowired
  TTServerContextAwareHrefBuilder hrefBuilder;

  LanguageValidator validator = new LanguageValidator(
      Arrays.asList(new CCPMProfileDMNValidator(), new CCPMProfileCMMNValidator()));

  LanguageDeSerializer parser = new LanguageDeSerializer(
      Arrays.asList(new Surrogate2Parser(), new DMN12Parser(), new CMMN11Parser()));

  /**
   * Runs the {@link CCPMProfileDMNValidator} and {@link CCPMProfileDMNValidator} on a cCPM, given
   * the Asset id / version of the root case model (though one could reference a naturalistic
   * decision model)
   *
   * @param assetId    Root Asset Id
   * @param versionTag Root Asset Version
   * @param refresh    force the refresh of the underlying TTW before running validation
   * @return the report, in HTML
   */
  @GetMapping(value = "/validate/ccpms/{assetId}/versions/{versionTag}",
      produces = "text/html")
  public ResponseEntity<byte[]> validateAsset(
      @PathVariable UUID assetId,
      @PathVariable String versionTag,
      @RequestParam(required = false, value = "refresh") String refresh) {

    if (Boolean.parseBoolean(refresh)) {
      triso.deleteKnowledgeAssets();
    }

    var report = buildReport(assetId, versionTag);
    return ResponseHelper.asResponse(report);
  }


  private Answer<byte[]> buildReport(UUID assetId, String versionTag) {
    try {
      /* Retrieves the CCPM, and reshapes it into a List of Asset/Artifact pairs */
      var payloads = compose(assetId, versionTag);
      if (payloads.isFailure()) {
        return Answer.of(payloads.printExplanation().getBytes());
      }

      /* Runs the validation logic */
      var results = payloads.get().stream()
          .map(pair -> validator.applyValidate(pair, null))
          .reduce(Answer::merge)
          .map(Explainer::getExplanation);

      /* Formats the report as basic HTML */
      var report = results.map(x -> toHTML(x, hrefBuilder))
          .map(String::getBytes);

      return Answer.ofTry(report);
    } catch (Exception e) {
      return Answer.failed(e);
    }
  }

  private List<CompositeKnowledgeCarrier> join(
      List<KnowledgeCarrier> surrogates,
      List<KnowledgeCarrier> carriers) {

    if (surrogates.size() != carriers.size()) {
      throw new IllegalStateException("Asset/Artifact set mismatch");
    }

    List<CompositeKnowledgeCarrier> pairs = new ArrayList<>();
    for (int j = 0; j < surrogates.size(); j++) {
      var asset = surrogates.get(j);
      var artifact = carriers.get(j);
      if (!asset.getAssetId().asKey().equals(artifact.getAssetId().asKey())) {
        throw new IllegalStateException("Asset/Artifact mismatch");
      }
      var pair = new CompositeKnowledgeCarrier()
          .withAssetId(artifact.getAssetId())
          .withRootId(artifact.getAssetId())
          .withArtifactId(artifact.getArtifactId())
          .withStructType(CompositeStructType.NONE)
          .withRepresentation(artifact.getRepresentation())
          .withComponent(asset, artifact)
          .withLabel(asset.getLabel());
      pairs.add(pair);
    }
    return pairs;
  }

  private Answer<List<CompositeKnowledgeCarrier>> compose(UUID assetId, String versionTag) {
    var s = triso.getAnonymousCompositeKnowledgeAssetSurrogate(assetId, versionTag);
    if (s.isFailure()) {
      return Answer.failed(s);
    }
    var assets = s.get().components()
        .sorted(Comparator.comparing(kc -> kc.getAssetId().asKey()))
        .collect(Collectors.toList());

    var c = triso.getAnonymousCompositeKnowledgeAssetCarrier(assetId, versionTag);
    if (c.isFailure()) {
      return Answer.failed(c);
    }
    var artifacts = c.get().components()
        .map(kc -> parser.applyLift(kc, Abstract_Knowledge_Expression))
        .flatMap(Answer::trimStream)
        .sorted(Comparator.comparing(kc -> kc.getAssetId().asKey()))
        .collect(Collectors.toList());

    var payloads = join(assets, artifacts);

    return Answer.of(payloads);
  }


  protected String toHTML(KnowledgeCarrier explanation,
      TTServerContextAwareHrefBuilder hrefBuilder) {
    return ProblemTableWriter.write(
        explanation.componentsAs(Problem.class).collect(Collectors.toList()), hrefBuilder);
  }


  private static class ProblemTableWriter {

    static final String[] HEADERS = new String[]{
        "Asset ID",
        "Rule",
        "Status",
        "Detail"};

    String field;

    public static String write(List<Problem> reportEntries,
        TTServerContextAwareHrefBuilder hrefBuilder) {
      // create a List which contains String array
      List<String[]> data = reportEntries.stream()
          .map(p -> ProblemTableWriter.toRowData(p, hrefBuilder))
          .collect(Collectors.toList());

      return toHtml(data);
    }

    private static String[] toRowData(Problem p, TTServerContextAwareHrefBuilder hrefBuilder) {
      var id = p.getInstance() != null
          ? href(SemanticIdentifier.newVersionId(p.getInstance()), hrefBuilder)
          : null;
      String rule = p.getTitle();
      String status = Severity.severityOf(p).name();
      String msg = p.getDetail();
      return new String[]{id, rule, status, msg};
    }

    private static String href(ResourceIdentifier id, TTServerContextAwareHrefBuilder hrefBuilder) {
      if (id == null) {
        return ("N/A");
      }
      return String.format("<a href='%s'>%s</a>",
          hrefBuilder.getHref(id, HrefType.ASSET),
          id.asKey());
    }

    private static String toHtml(List<String[]> data) {
      var header = row(Arrays.stream(HEADERS), true);

      var body = data.stream()
          .map(x -> row(Arrays.stream(x), false))
          .collect(Collectors.joining("\n"));

      return String.format("<table>"
          + "%s"
          + "%s"
          + "</table>", header, body);
    }

    private static String row(Stream<String> data, boolean header) {
      return String.format("<tr>%s</tr>%n", cells(data, header));
    }

    private static String cells(Stream<String> values, boolean header) {
      var t = header ? "th" : "td";
      return values.map(
              x -> String.format("<%s %s>%s</%s>", t, color(x), x, t))
          .collect(Collectors.joining());
    }

    private static Object color(String x) {
      if (Severity.ERR.name().equalsIgnoreCase(x)) {
        return "bgcolor='#FFCCCB'";
      }
      if (Severity.INF.name().equalsIgnoreCase(x)) {
        return "bgcolor='#C5E3EC'";
      }
      if (Severity.WRN.name().equalsIgnoreCase(x)) {
        return "bgcolor='#FFFFE0'";
      }
      if (Severity.OK.name().equalsIgnoreCase(x)) {
        return "bgcolor='CCFFCC'";
      }
      return "";
    }

  }


}
