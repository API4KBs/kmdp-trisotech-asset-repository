package edu.mayo.kmdp;

import static edu.mayo.kmdp.util.XMLUtil.loadXMLDocument;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.CMMN_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Abstract_Knowledge_Expression;

import edu.mayo.kmdp.kdcaci.knew.trisotech.NamespaceManager;
import edu.mayo.kmdp.kdcaci.knew.trisotech.components.weavers.Weaver;
import edu.mayo.kmdp.language.parsers.cmmn.v1_1.CMMN11Parser;
import javax.xml.namespace.QName;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.cmmn._20151109.model.TDecisionTask;
import org.omg.spec.cmmn._20151109.model.TDefinitions;
import org.omg.spec.cmmn._20151109.model.TPlanItemDefinition;
import org.w3c.dom.Document;

public class CMMNDecisionTaskRefFixTest {

  Weaver weaver = new Weaver(new NamespaceManager());

  String brokenCMMN = "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"yes\"?>\n"
      + "<semantic:definitions xmlns:semantic=\"http://www.omg.org/spec/CMMN/20151109/MODEL\"  "
      + "   xmlns:cmmndi=\"http://www.omg.org/spec/CMMN/20151109/CMMNDI\"  "
      + "   xmlns:di=\"http://www.omg.org/spec/CMMN/20151109/DI\"  "
      + "   xmlns:dc=\"http://www.omg.org/spec/CMMN/20151109/DC\"  "
      + "   xmlns:trisocmmn=\"http://www.trisotech.com/2014/triso/cmmn\"  "
      + "   xmlns:triso=\"http://www.trisotech.com/2015/triso/modeling\"  "
      + "   xmlns:feel=\"http://www.omg.org/spec/DMN/20180521/FEEL/\"  "
      + "   xmlns:rss=\"http://purl.org/rss/2.0/\"   "
      + "   xmlns:i18n=\"http://www.omg.org/spec/BPMN/non-normative/extensions/i18n/1.0\"  "
      + "   xmlns=\"http://www.trisotech.com/definitions/_b1616985-14ed-4f3a-b7ec-861df7a7215e\" "
      + "   id=\"_b1616985-14ed-4f3a-b7ec-861df7a7215e\" "
      + "   name=\"Decision ref\" "
      + "   targetNamespace=\"http://www.trisotech.com/definitions/_b1616985-14ed-4f3a-b7ec-861df7a7215e\" "
      + "   expressionLanguage=\"http://www.omg.org/spec/DMN/20180521/FEEL/\" "
      + "   exporter=\"Case Modeler\" exporterVersion=\"7.4.2\" xml:lang=\"en\" triso:logoChoice=\"Default\">\n"

      + "    <semantic:case id=\"Case_c5a56f71-aaaf-402e-9e35-a4e744efde92\" name=\"Page 1\">\n"
      + "        <semantic:casePlanModel id=\"_c5a56f71-aaaf-402e-9e35-a4e744efde92\" name=\"Page 1\">\n"
      + "            <semantic:planItem id=\"_27135bdf-d86c-4b40-bb73-e1bd2d8c3570\" definitionRef=\"PID__27135bdf-d86c-4b40-bb73-e1bd2d8c3570\"/>\n"
      + "            <semantic:decisionTask id=\"PID__27135bdf-d86c-4b40-bb73-e1bd2d8c3570\" name=\"Age at First Diagnosis of XYZ\" triso:linkedTermsId=\"\">\n"
      + "                <semantic:extensionElements>\n"
      + "                    <triso:reuseLink id=\"_4311d662-6f24-4eea-b330-681c33155475\" modelName=\"Age Alternatives\" itemName=\"Age at First Diagnosis of XYZ\" modelType=\"http://www.trisotech.com/graph/1.0/element#DMNModel\" mimeType=\"application/vnd.triso-dmn+json\" uri=\"http://www.trisotech.com/definitions/_6bd89a34-5b38-4d95-9b41-e65a75e94423#_bb489ba3-2d9e-4697-872e-a57015830667\" graphType=\"http://www.trisotech.com/graph/1.0/decision/element#Decision\" graphURI=\"http://trisotech.com/graph/1.0/graph#%7Cpersonal%7C/sottara.davide%40mayo.edu\"/>\n"
      + "                </semantic:extensionElements>\n"
      + "                <semantic:defaultControl>\n"
      + "                    <semantic:manualActivationRule/>\n"
      + "                </semantic:defaultControl>\n"
      + "            </semantic:decisionTask>\n"
      + "        </semantic:casePlanModel>\n"
      + "    </semantic:case>\n"
      + "</semantic:definitions>";

  @Test
  void testFix() {
    // sets up the document for conversion by setting namespaceaware
    Document dox = loadXMLDocument(brokenCMMN).orElseGet(Assertions::fail);
    Document rewrittenDox = weaver.weave(dox);

    TDefinitions caseModel = parse(rewrittenDox);

    assertNotNull(caseModel);

    TPlanItemDefinition taskDef = caseModel.getCase().get(0).getCasePlanModel().getPlanItemDefinition().get(0).getValue();
    assertTrue(taskDef instanceof TDecisionTask);

    QName ref = ((TDecisionTask) taskDef).getDecisionRef();
    assertNotNull(ref);

    assertNotNull(caseModel.getDecision());
    assertEquals(1, caseModel.getDecision().size());
  }

  private TDefinitions parse(Document dox) {
    return new CMMN11Parser()
        .applyLift(AbstractCarrier.ofTree(dox).withRepresentation(rep(CMMN_1_1,XML_1_1)),
            Abstract_Knowledge_Expression, codedRep(CMMN_1_1), null)
        .flatOpt(kc -> kc.as(TDefinitions.class))
        .orElseGet(Assertions::fail);
  }

}
