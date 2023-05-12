package edu.mayo.kmdp.tools;

import com.google.common.io.Files;
import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.Code;
import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.EdgeModelElement;
import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.ExtensionElement;
import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.KemConcept;
import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.KemConceptProperties;
import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.KemModel;
import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.KemModelProperties;
import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.KemVersionManifest;
import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.RelationshipProperties;
import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.Stencil;
import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.Tag;
import edu.mayo.kmdp.util.JSonUtil;
import edu.mayo.kmdp.util.Util;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AsOWLClass;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.HasIRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.SCTHelper;

public class SCTtoKEMGenerator {

  private static final Logger logger = LoggerFactory.getLogger(SCTtoKEMGenerator.class);
  private static final String SCT_SOURCE = "/sctUpper.owl";
  private static final String SCT_NAME = "SNOMEDCT";
  private static final String SCT = "http://snomed.info/sct";
  private static final String SID = "http://snomed.info/id";


  private static final String TRISO = "http://www.trisotech.com/2015/triso/modeling";
  private static final String TRISO_DEF = "http://www.trisotech.com/definitions/_";
  private static final UUID MODEL_ID = UUID.fromString("d67e2180-4b1e-4fc0-99ed-f0d2cb2191ed");

  Map<String, KemConcept> index = new HashMap<>();

  public static void main(String[] args) {
    loadOntology()
        .map(sct -> new SCTtoKEMGenerator().toKem(sct))
        .ifPresent(SCTtoKEMGenerator::saveKem);
  }

  public KemModel toKem(OWLOntology onto) {
    var kem = new KemModel()
        .withModelVersion(KemVersionManifest.VERSION)
        .withDiagramId("_" + MODEL_ID);
    kem.setProperties(new KemModelProperties()
        .withName("SCT Upper Vocabulary (2023)")
        .withTargetNamespace(TRISO_DEF + MODEL_ID));

    onto.classesInSignature()
        .filter(this::isConcrete)
        .forEach(klass -> addConcept(klass, kem, onto));
    onto.objectPropertiesInSignature()
        .filter(this::isConcrete)
        .forEach(prop -> addConcept(prop, kem, onto));

    onto.axioms()
        .filter(x -> x.getAxiomType() == AxiomType.SUBCLASS_OF)
        .map(OWLSubClassOfAxiom.class::cast)
        .filter(x -> x.getSubClass().isNamed()
            && x.getSuperClass().isNamed())
        .filter(x -> isConcrete(x.getSubClass().asOWLClass())
            && isConcrete(x.getSuperClass().asOWLClass()))
        .forEach(sub -> addIsA(
            sub.getSubClass().asOWLClass(),
            sub.getSuperClass().asOWLClass(),
            kem));
    onto.axioms()
        .filter(x -> x.getAxiomType() == AxiomType.SUB_OBJECT_PROPERTY)
        .map(OWLSubObjectPropertyOfAxiom.class::cast)
        .filter(x -> x.getSubProperty().isNamed()
            && x.getSuperProperty().isNamed())
        .filter(x -> isConcrete(x.getSubProperty().asOWLObjectProperty())
            && isConcrete(x.getSuperProperty().asOWLObjectProperty()))
        .forEach(sub -> addIsA(
            sub.getSubProperty().asOWLObjectProperty(),
            sub.getSuperProperty().asOWLObjectProperty(),
            kem));

    onto.objectPropertiesInSignature()
        .filter(this::isConcrete)
        .forEach(prop -> addRelationships(prop, kem, onto));

    return kem;
  }

  private void addRelationships(OWLObjectProperty prop, KemModel kem, OWLOntology onto) {
    var doms = onto.objectPropertyDomainAxioms(prop)
        .flatMap(dom -> dom.getDomain().asDisjunctSet().stream())
        .filter(this::isConcreteClass)
        .map(AsOWLClass::asOWLClass)
        .collect(Collectors.toSet());
    var rans = onto.objectPropertyRangeAxioms(prop)
        .flatMap(dom -> dom.getRange().asDisjunctSet().stream())
        .filter(this::isConcreteClass)
        .map(AsOWLClass::asOWLClass)
        .collect(Collectors.toSet());

    doms.forEach(dom ->
        rans.forEach(ran -> {
          if (index.containsKey(dom.getIRI().getIRIString())
              && index.containsKey(ran.getIRI().getIRIString())
              && index.containsKey(prop.getIRI().getIRIString())) {
            addRelationship(dom, prop, ran, kem);
          } else {
            throw new IllegalStateException();
          }
        }));
  }

  private void addRelationship(OWLClass dom, OWLObjectProperty prop, OWLClass ran, KemModel kem) {
    var srcIri = dom.getIRI().getIRIString();
    var tgtIri = ran.getIRI().getIRIString();
    var prpIri = prop.getIRI().getIRIString();
    var src = index.get(srcIri);
    var tgt = index.get(tgtIri);
    var prp = index.get(prpIri);
    kem.getEdgeModelElements().add(new EdgeModelElement()
        .withProperties(new RelationshipProperties()
            .withName(prp.getProperties().getName())
            .withNameHtml(prp.getProperties().getName(), prp.getResourceId())
            .withLinkedTerm(prp.getResourceId()))
        .withStencil(new Stencil()
            .withId("relation"))
        .withResourceId("_" + Util.uuid(srcIri + " " + prpIri + " " + tgtIri))
        .withSourceRef(src.getResourceId())
        .withTargetRef(tgt.getResourceId()));
  }

  private void addIsA(HasIRI subEntity, HasIRI supEntity, KemModel kem) {
    var sub = subEntity.getIRI().getIRIString();
    var sup = supEntity.getIRI().getIRIString();
    kem.getEdgeModelElements().add(new EdgeModelElement()
        .withStencil(new Stencil()
            .withId("isA"))
        .withResourceId("_" + Util.uuid(sub + " isA " + sup))
        .withSourceRef(index.get(sub).getResourceId())
        .withTargetRef(index.get(sup).getResourceId()));
  }


  private boolean isConcreteClass(OWLClassExpression expr) {
    return expr.isNamed() && isConcrete(expr.asOWLClass());
  }

  private boolean isConcrete(HasIRI iri) {
    var str = iri.getIRI().getIRIString();
    var isSct = str.startsWith(SID);
    var isRoot = "http://snomed.info/id/138875005".equals(str)
        || "http://snomed.info/id/762705008".equals(str);
    return isSct && !isRoot;
  }

  private void addConcept(HasIRI entity, KemModel kem, OWLOntology onto) {
    var label = getAnnotation(entity, SKOS.prefLabel.getURI(), onto);
    var code = getCode(entity);
    var tag = getTag(entity, onto);
    var def = getAnnotation(entity, SKOS.definition.getURI(), onto);

    KemConcept concept = new KemConcept()
        .withResourceId("_" + Util.uuid(entity.getIRI().getIRIString()))
        .withProperties(new KemConceptProperties()
            .withName(label)
            .withCode(List.of(new Code()
                .withId("_" + Util.uuid(SID + code))
                .withValue(code)
                .withSemanticType("code")
                .withDisplay(label)
                .withCodingSystem(SCT)
                .withCodingSystemDisplay(SCT_NAME)))
            .withDocumentation(def)
            .withExtensionElements(List.of(new ExtensionElement()
                .withNamespace(TRISO)
                .withSemanticType("tags")
                .withTag(List.of(new Tag()
                    .withSemanticType("tag")
                    .withContent(tag)))
            )))
        .withStencil(new Stencil()
            .withId("term"));

    kem.getNodeModelElements().add(concept);
    index.put(entity.getIRI().getIRIString(), concept);

  }

  private String getTag(HasIRI iriable, OWLOntology onto) {
    var label = getAnnotation(iriable, RDFS.label.getURI(), onto);
    try {
      var tag = label.substring(label.lastIndexOf('(') + 1, label.lastIndexOf(')'));
      if (!SCTHelper.isTag(tag)) {
        throw new IllegalStateException("Unrecognized tag " + tag);
      }
      return tag;
    } catch (StringIndexOutOfBoundsException se) {
      logger.error("No tag detected in " + iriable, se);
      return "";
    }
  }

  private String getCode(HasIRI iriable) {
    var iri = iriable.getIRI().getIRIString();
    if (iri.startsWith(SID)) {
      return iri.substring(SID.length() + 1);
    }
    throw new IllegalStateException("Unexpected entity " + iri);
  }

  public static String getAnnotation(HasIRI iriable, String prop, OWLOntology onto) {
    return onto.annotationAssertionAxioms(iriable.getIRI(), Imports.EXCLUDED)
        .filter(x -> x.getProperty().getIRI().getIRIString().equals(prop))
        .findFirst()
        .flatMap(x -> x.getValue().asLiteral()
            .map(OWLLiteral::getLiteral))
        .orElse("");
  }


  private static Optional<OWLOntology> loadOntology() {
    var mgr = OWLManager.createOWLOntologyManager();

    return Optional.ofNullable(SCTtoKEMGenerator.class.getResourceAsStream(SCT_SOURCE))
        .flatMap(is -> {
          try {
            return Optional.ofNullable(mgr.loadOntologyFromOntologyDocument(is));
          } catch (OWLOntologyCreationException e) {
            logger.error(e.getMessage(), e);
            return Optional.empty();
          }
        });
  }


  private static void saveKem(KemModel kem) {
    try {
      var base = SCTtoKEMGenerator.class.getResource("/"); // /target/classes
      if (base != null) {
        var file = Path.of(base.toURI())
            .getParent() // use /target
            .resolve("sct.kem.json");
        var bytes = JSonUtil.writeJson(kem)
            .map(ByteArrayOutputStream::toByteArray)
            .orElse(new byte[0]);
        Files.write(bytes, file.toFile());
      }
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    }
  }

}
