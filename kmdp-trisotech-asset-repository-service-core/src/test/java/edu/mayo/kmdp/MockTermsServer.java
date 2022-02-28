package edu.mayo.kmdp;

import edu.mayo.ontology.taxonomies.kao.decisiontype.DecisionTypeSeries;
import java.net.URI;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.terminology.v4.server.TermsApiInternal;
import org.omg.spec.api4kp._20200801.id.Term;
import org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries;
import org.omg.spec.api4kp._20200801.terms.model.ConceptDescriptor;

public class MockTermsServer implements TermsApiInternal {

  final URI ns = URI.create("http://mock.ontology.edu/clinicalsituations/test");
  final Date t0 = new Date();

  public MockTermsServer() {
    // nothing to do
  }

  @Override
  public Answer<ConceptDescriptor> lookupTerm(String conceptTag) {
    var uuid = UUID.fromString(conceptTag);
    Optional<Term> t = DecisionTypeSeries.resolveUUID(uuid).map(Term.class::cast)
        .or(() -> KnowledgeAssetTypeSeries.resolveUUID(uuid))
        .or(() -> ClinicalKnowledgeAssetTypeSeries.resolveUUID(uuid))
        .or(() -> Optional.ofNullable(mintNewTerm(conceptTag)));
    return Answer.ofTry(t.map(ConceptDescriptor::toConceptDescriptor));
  }

  private Term mintNewTerm(String conceptTag) {
    Term t = Term.newTerm(ns, conceptTag, "0.0.0", "Mock term " + conceptTag);

    return ConceptDescriptor.toConceptDescriptor(t)
        .withEstablishedOn(t0)
        .withReferentId(ns);
  }

//
//  <surr:annotation xmlns:api="https://www.omg.org/spec/API4KP/20200801/services"
//  xmlns:id="https://www.omg.org/spec/API4KP/20200801/id"
//  xmlns:idx="https://www.omg.org/spec/API4KP/20200801/id/resources"
//  xmlns:surrx="https://www.omg.org/spec/API4KP/20200801/surrogate/resources">
//            <surr:rel establishedOn="2021-04-01T00:00:00"
//  name="in terms of"
//  namespaceUri="https://ontology.mayo.edu/taxonomies/KMDO/SemanticAnnotationRelType"
//  referentId="https://www.omg.org/spec/API4KP/api4kp/defined-in-terms-of"
//  resourceId="https://ontology.mayo.edu/taxonomies/KMDO/SemanticAnnotationRelType#6e1048ea-46da-39b1-b650-445f31a17fc1"
//  tag="defined-in-terms-of"
//  versionId="https://ontology.mayo.edu/taxonomies/KMDO/SemanticAnnotationRelType/versions/20210401#6e1048ea-46da-39b1-b650-445f31a17fc1"
//  versionTag="20210401">
//               <id:uuid>6e1048ea-46da-39b1-b650-445f31a17fc1</id:uuid>
//            </surr:rel>
//            <surr:ref namespaceUri="http://mock.ontology.edu/clinicalsituations/test"
//  resourceId="http://mock.ontology.edu/clinicalsituations/test#13a3e25c-6848-373e-9676-8ecb62ab3e6a"
//  tag="13a3e25c-6848-373e-9676-8ecb62ab3e6a">
//               <id:uuid>13a3e25c-6848-373e-9676-8ecb62ab3e6a</id:uuid>
//            </surr:ref>
//         </surr:annotation>
}
