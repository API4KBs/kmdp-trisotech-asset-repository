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
package edu.mayo.kmdp.trisotech.accel;

import edu.mayo.kmdp.trisotech.accel.beans.TrisotechAccelerator;
import edu.mayo.kmdp.trisotech.accel.beans.TrisotechAccelerator.TrisotechAcceleratorEntry;
import edu.mayo.kmdp.util.FileUtil;
import edu.mayo.kmdp.util.JSonUtil;
import edu.mayo.kmdp.util.URIUtil;
import edu.mayo.ontology.taxonomies.clinicalinterrogatives.snapshot.ClinicalInterrogative;
import edu.mayo.ontology.taxonomies.clinicalsituations._20201217.AnticoagulationClinicalSituation;
import edu.mayo.ontology.taxonomies.clinicalsituations._20201217.CardiologyClinicalSituation;
import edu.mayo.ontology.taxonomies.clinicalsituations._20201217.HeartfailureClinicalSituation;
import edu.mayo.ontology.taxonomies.clinicalsituations._20201217.HyperlipidemiaClinicalSituation;
import edu.mayo.ontology.taxonomies.clinicalsituations._20201217.HypertensionClinicalSituation;
import edu.mayo.ontology.taxonomies.clinicalsituations._20201217.TestClinicalSituation;
import edu.mayo.ontology.taxonomies.clinicaltasks.ClinicalTask;
import edu.mayo.ontology.taxonomies.kao.decisiontype.snapshot.DecisionType;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.omg.spec.api4kp._20200801.id.Term;
import org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.snapshot.ClinicalKnowledgeAssetType;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetrole._20190801.KnowledgeAssetRole;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype._20190801.KnowledgeAssetType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrisotechAcceleratorGenerator {

  private static final Logger logger = LoggerFactory.getLogger(TrisotechAcceleratorGenerator.class);

  public static void main(String... args) throws URISyntaxException {
    // Creates files in $HOME (C: on Windows) by default
    Path base = args.length > 0
        ? Path.of(args[0])
        : Paths.get("tt-stage/");

    TrisotechAcceleratorGenerator generator = new TrisotechAcceleratorGenerator();

    writeAccelerator(base, "mod-test-accel.json", generator::generateTestModule);

    writeAccelerator(base, "cs-accel.json", generator::generateFlatClinicalSituationAccelerator);

    writeAccelerator(base, "ctv-accel.json", generator::generateClinTask);

    writeAccelerator(base, "interr-accel.json", generator::generateInterrogative);

    writeAccelerator(base, "asset-type-accel.json", generator::generateAssetType);

    writeAccelerator(base, "fragment-type-accel.json", generator::generateFragType);

    writeAccelerator(base, "asset-role-accel.json", generator::generateAssetRole);

//    File profileDir = new File(
//        TrisotechAcceleratorGenerator.class.getResource("/fhir/stu3/profiles").toURI());
//    TDefinitions defs = FileUtil.streamChildFiles(profileDir)
//        .map(TrisotechAcceleratorGenerator::generateDMNDefinitions)
//        .reduce(new TDefinitions(), (d1, d2) -> {
//          d1.getItemDefinition().addAll(d2.getItemDefinition());
//          return d1;
//        });
//
//    System.out.println(defs);
  }

  private static void writeAccelerator(Path base, String fileName,
      Supplier<TrisotechAccelerator> generator) {
    try {
      TrisotechAccelerator accel = generator.get();
      Path accelFile = base.resolve(fileName);
      if (!Files.exists(base)) {
        Files.createDirectories(base);
      }
      JSonUtil.printJson(accel).ifPresent(json -> FileUtil.write(json, accelFile.toFile()));
    } catch (IOException e) {
      logger.error(e.getMessage(), e);
    }
  }
//
//  private static TDefinitions generateDMNDefinitions(File f) {
//    DeserializeApiInternal fhirParser = new LanguageDeSerializer(
//        Collections.singletonList(new FHIR3Deserializer()));
//    StructDefToDMNProjectingTranslator translator = new StructDefToDMNProjectingTranslator();
//
//    return Answer.of(f)
//        .flatOpt(FileUtil::read)
//        .map(b -> AbstractCarrier.of(b)
//            .withRepresentation(rep(FHIR_STU3, XML_1_1)))
//        .flatMap(kc -> fhirParser.applyLift(kc, Abstract_Knowledge_Expression))
//        .flatMap(
//            kc -> translator.applyTransrepresent(kc, ModelMIMECoder.encode(rep(DMN_1_2)), null))
//        .flatOpt(kc -> kc.as(TDefinitions.class))
//        .orElse(new TDefinitions());
//  }
//


  public TrisotechAccelerator generateTestModule() {
    return buildAccelerator(
        TestClinicalSituation.schemeVersionIdentifier.getResourceId(),
        TestClinicalSituation.SCHEME_NAME,
        Arrays.stream(TestClinicalSituation.values()));
  }

  private TrisotechAccelerator generateFlatClinicalSituationAccelerator() {
    return buildAccelerator(
        CardiologyClinicalSituation.schemeVersionIdentifier.getResourceId(),
        "Clinical Situations",
        Stream.of(
            Arrays.stream(TestClinicalSituation.values()),
            Arrays.stream(AnticoagulationClinicalSituation.values()),
            Arrays.stream(CardiologyClinicalSituation.values()),
            Arrays.stream(HyperlipidemiaClinicalSituation.values()),
            Arrays.stream(HeartfailureClinicalSituation.values()),
            Arrays.stream(HypertensionClinicalSituation.values())
        ).flatMap(s -> s)
    );
  }


  public TrisotechAccelerator generateClinTask() {
    return buildAccelerator(
        ClinicalTask.schemeVersionIdentifier.getResourceId(),
        ClinicalTask.SCHEME_NAME,
        Arrays.stream(ClinicalTask.values()));
  }

  public TrisotechAccelerator generateAssetType() {
    return buildAccelerator(
        KnowledgeAssetType.schemeVersionIdentifier.getResourceId(),
        KnowledgeAssetType.SCHEME_NAME,
        Stream.concat(
            Arrays.stream(ClinicalKnowledgeAssetType.values()),
            Arrays.stream(KnowledgeAssetType.values()))
    );
  }

  public TrisotechAccelerator generateFragType() {
    return buildAccelerator(
        DecisionType.schemeVersionIdentifier.getResourceId(),
        DecisionType.SCHEME_NAME,
        Arrays.stream(DecisionType.values()));
  }


  public TrisotechAccelerator generateAssetRole() {
    return buildAccelerator(
        KnowledgeAssetRole.schemeVersionIdentifier.getResourceId(),
        KnowledgeAssetRole.SCHEME_NAME,
        Arrays.stream(KnowledgeAssetRole.values()));
  }

  public TrisotechAccelerator generateInterrogative() {
    return buildAccelerator(
        ClinicalInterrogative.schemeVersionIdentifier.getResourceId(),
        ClinicalInterrogative.SCHEME_NAME,
        Arrays.stream(ClinicalInterrogative.values()));
  }


  public TrisotechAccelerator buildAccelerator(URI uri, String label, Stream<Term> terms) {
    URI nuri = URIUtil.normalizeURI(uri);
    TrisotechAccelerator accel = new TrisotechAccelerator(
        nuri,
        label,
        "");
    terms.map(pc ->
        new TrisotechAcceleratorEntry(
            pc.getResourceId(),
            pc.getName(),
            ""))
        .distinct()
        .forEach(accel::addChild);
    return accel;
  }
}
