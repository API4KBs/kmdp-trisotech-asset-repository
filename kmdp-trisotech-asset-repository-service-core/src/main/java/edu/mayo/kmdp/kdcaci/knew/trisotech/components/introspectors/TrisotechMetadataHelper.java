package edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors;

import static edu.mayo.kmdp.util.JaxbUtil.unmarshall;
import static edu.mayo.kmdp.util.XMLUtil.asElementStream;
import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.Captures;
import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.Defines;
import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.Has_Focus;
import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.Has_Primary_Subject;
import static edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries.In_Terms_Of;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.Comparator.comparing;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.CMMN_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.DMN_1_2;

import edu.mayo.kmdp.util.StreamUtil;
import edu.mayo.kmdp.util.XPathUtil;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.surrogate.Annotation;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetType;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Helper class that gathers functions used across the various Asset metadata introspectors
 */
public class TrisotechMetadataHelper {

  private static final String SEMANTIC_EXTENSION_ELEMENTS = "semantic:extensionElements";
  private static final String EXTENSION_ELEMENTS = "extensionElements";


  public static final String CMMN_DEFINITIONS = "//cmmn:definitions";
  public static final String DMN_DEFINITIONS = "//dmn:definitions";

  private static final XPathUtil xPathUtil = new XPathUtil();


  protected TrisotechMetadataHelper() {
    // functions only
  }

  /**
   * Injects a List of Annotations, extracted from a model, into that model's surrogate.
   * <p>
   * Filters, sorts and de-duplicates the Annotations in the process
   *
   * @param surr        the Surrogate to be augmented
   * @param annotations the List of {@link Annotation} to inject
   */
  public static void addSemanticAnnotations(KnowledgeAsset surr, List<Annotation> annotations) {
    List<Annotation> annos = annotations.stream()
        .filter(ann -> Captures.sameTermAs(ann.getRel())
            || Defines.sameTermAs(ann.getRel())
            || Has_Primary_Subject.sameTermAs(ann.getRel())
            || Has_Focus.sameTermAs(ann.getRel())
            || In_Terms_Of.sameTermAs(ann.getRel()))
        .map(ann -> (Annotation) ann.copyTo(new Annotation()))
        .collect(Collectors.toList());

    List<Annotation> uniqueAnnos = new LinkedList<>();
    for (Annotation ann : annos) {
      if (uniqueAnnos.stream()
          .noneMatch(a ->
              a.getRef().sameTermAs(ann.getRef())
                  && a.getRel().sameTermAs(ann.getRel()))) {
        uniqueAnnos.add(ann);
      }
    }
    Comparator<Annotation> comp = comparing(annotation -> annotation.getRel().getTag());
    comp = comp.thenComparing(ann -> ann.getRef().getTag());
    uniqueAnnos.stream()
        .sorted(comp)
        .forEach(surr::withAnnotation);
  }


  /**
   * Pulls the semantic {@link Annotation} from the service.
   * <p>
   * Note that the {@link edu.mayo.kmdp.kdcaci.knew.trisotech.components.weavers.Weaver} should have
   * already rewritten the annotations into the platform format
   *
   * @param serviceNode the annotated service
   * @return the List of {@link Annotation} in the service
   */
  public static List<Annotation> extractAnnotations(Element serviceNode) {
    return asElementStream(
        serviceNode.getElementsByTagName(SEMANTIC_EXTENSION_ELEMENTS))
        .filter(Objects::nonNull)
        .filter(el -> el.getLocalName().equals(EXTENSION_ELEMENTS))
        .flatMap(el -> asElementStream(el.getChildNodes()))
        .filter(child -> child.getLocalName().equals("annotation"))
        .map(child -> unmarshall(Annotation.class, Annotation.class, child))
        .flatMap(StreamUtil::trimStream)
        .collect(Collectors.toCollection(LinkedList::new));
  }


  /**
   * Determines the default {@link KnowledgeAssetType} for a model of a given representation,
   * when no other explicit information is available;
   *
   * @param mime the (Trisotech) mime type that encodes the model's representation
   * @return the default {@link KnowledgeAssetType}
   */
  public static KnowledgeAssetType getDefaultAssetType(String mime) {
    if (mime.contains("dmn")) {
      return KnowledgeAssetTypeSeries.Decision_Model;
    } else if (mime.contains("cmmn")) {
      return KnowledgeAssetTypeSeries.Case_Management_Model;
    } else if (mime.contains("bpmn")) {
      // TODO - we need 'process model' in the KAO
      return KnowledgeAssetTypeSeries.Protocol;
    } else {
      throw new IllegalStateException("Unrecognized model type " + mime);
    }
  }


  /**
   * Constructs a {@link SyntacticRepresentation} for the binary encoded manifestation of a given
   * model.
   *
   * @param dox the model
   * @return the {@link SyntacticRepresentation} at the Encoded level
   */
  public static Optional<SyntacticRepresentation> getRepLanguage(Document dox) {
    if (dox == null) {
      return Optional.empty();
    }
    if (xPathUtil.xNode(dox, CMMN_DEFINITIONS) != null) {
      return Optional.of(rep(CMMN_1_1, XML_1_1, defaultCharset(), Encodings.DEFAULT));
    }
    if (xPathUtil.xNode(dox, DMN_DEFINITIONS) != null) {
      return Optional.of(rep(DMN_1_2, XML_1_1, defaultCharset(), Encodings.DEFAULT));
    }
    return Optional.empty();
  }

  /**
   * Detects the {@link KnowledgeRepresentationLanguage} used in a model.
   *
   * @param dox the model
   * @return the {@link KnowledgeRepresentationLanguage}
   */
  public static Optional<KnowledgeRepresentationLanguage> detectRepLanguage(Document dox) {
    if (dox == null) {
      return Optional.empty();
    }
    if (xPathUtil.xNode(dox, CMMN_DEFINITIONS) != null) {
      return Optional.of(CMMN_1_1);
    }
    if (xPathUtil.xNode(dox, DMN_DEFINITIONS) != null) {
      return Optional.of(DMN_1_2);
    }
    return Optional.empty();
  }


}
