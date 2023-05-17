package edu.mayo.kmdp.kdcaci.knew.trisotech.components.translators;

import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeoperation.KnowledgeProcessingOperationSeries.Syntactic_Translation_Task;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.JSON;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.TXT;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.OWL_2;

import edu.mayo.kmdp.language.parsers.rdf.JenaRdfParser;
import edu.mayo.kmdp.language.parsers.surrogate.v2.Surrogate2Parser;
import edu.mayo.kmdp.language.translators.AbstractSimpleTranslator;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import javax.inject.Named;
import org.apache.jena.rdf.model.Model;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DeserializeApiInternal._applyLift;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DeserializeApiInternal._applyLower;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KPOperation;
import org.omg.spec.api4kp._20200801.services.KPSupport;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.omg.spec.api4kp._20200801.taxonomy.krserialization.KnowledgeRepresentationLanguageSerializationSeries;

@Named
@KPOperation(Syntactic_Translation_Task)
@KPSupport(Knowledge_Asset_Surrogate_2_0)
public class MCBKSurrogateV2ToRDFTranslator extends
    AbstractSimpleTranslator<org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset, Model> {

  public static final UUID id = UUID.fromString("55ee85b6-7d02-4e02-b0ce-0dbda2f1ee09");
  public static final String version = "1.0.0";

  public MCBKSurrogateV2ToRDFTranslator() {
    setId(SemanticIdentifier.newId(id, version));
  }

  @Override
  public List<SyntacticRepresentation> getFrom() {
    return Arrays.asList(
        rep(Knowledge_Asset_Surrogate_2_0),
        rep(Knowledge_Asset_Surrogate_2_0, JSON),
        rep(Knowledge_Asset_Surrogate_2_0, JSON, Charset.defaultCharset()),
        rep(Knowledge_Asset_Surrogate_2_0, JSON, Charset.defaultCharset(), Encodings.DEFAULT));
  }

  @Override
  public List<SyntacticRepresentation> getInto() {
    return Arrays.asList(
        rep(OWL_2),
        rep(OWL_2, XML_1_1),
        rep(OWL_2, XML_1_1, Charset.defaultCharset()),
        rep(OWL_2, XML_1_1, Charset.defaultCharset(), Encodings.DEFAULT),
        rep(OWL_2, KnowledgeRepresentationLanguageSerializationSeries.Turtle, TXT),
        rep(OWL_2, KnowledgeRepresentationLanguageSerializationSeries.Turtle, TXT, Charset.defaultCharset()),
        rep(OWL_2, KnowledgeRepresentationLanguageSerializationSeries.Turtle, TXT, Charset.defaultCharset(), Encodings.DEFAULT));
  }

  @Override
  protected Answer<_applyLift> getParser() {
    return Answer.of(new Surrogate2Parser());
  }

  @Override
  protected Answer<_applyLower> getTargetParser() {
    return Answer.of(new JenaRdfParser());
  }



  @Override
  protected Optional<Model> transformAst(ResourceIdentifier assetId,
      ResourceIdentifier srcArtifactId, KnowledgeAsset expression,
      SyntacticRepresentation srcRep,
      SyntacticRepresentation tgtRep,
      Properties config) {
    return Optional.ofNullable(new MCBKSurrogateV2ToRDF().transform(expression));
  }

  @Override
  public KnowledgeRepresentationLanguage getSupportedLanguage() {
    return Knowledge_Asset_Surrogate_2_0;
  }

  @Override
  public KnowledgeRepresentationLanguage getTargetLanguage() {
    return OWL_2;
  }
}
