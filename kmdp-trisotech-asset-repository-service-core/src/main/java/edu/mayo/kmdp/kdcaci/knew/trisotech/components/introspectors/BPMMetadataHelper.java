package edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors;

import static edu.mayo.kmdp.trisotechwrapper.components.execution.KommunicatorHelper.getKommunicatorLink;
import static edu.mayo.kmdp.util.DateTimeUtil.parseDateTime;
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
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.defaultArtifactId;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeartifactcategory.KnowledgeArtifactCategorySeries.Interactive_Resource;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.XML_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.BPMN_2_0;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.CMMN_1_1;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.DMN_1_2;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.HTML;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.MVF_1_0;

import edu.mayo.kmdp.trisotechwrapper.components.SemanticModelInfo;
import edu.mayo.kmdp.trisotechwrapper.components.weavers.Weaver;
import edu.mayo.kmdp.trisotechwrapper.config.TTLanguages;
import edu.mayo.kmdp.trisotechwrapper.config.TTNotations;
import edu.mayo.kmdp.trisotechwrapper.config.TTWEnvironmentConfiguration;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.util.StreamUtil;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.surrogate.Annotation;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.Publication;
import org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetType;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatus;
import org.w3c.dom.Element;

/**
 * Helper class that gathers functions used across the various Asset metadata introspectors
 */
public class BPMMetadataHelper {

  private static final String SEMANTIC_EXTENSION_ELEMENTS = "semantic:extensionElements";
  private static final String EXTENSION_ELEMENTS = "extensionElements";


  private BPMMetadataHelper() {
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
  public static void addSemanticAnnotations(
      @Nonnull final KnowledgeAsset surr,
      @Nonnull final List<Annotation> annotations) {
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
   * Note that the {@link Weaver} should have already rewritten the annotations into the platform
   * format
   *
   * @param serviceNode the annotated service
   * @return the List of {@link Annotation} in the service
   */
  @Nonnull
  public static Stream<Annotation> extractAnnotations(
      @Nonnull final Element serviceNode) {
    return asElementStream(
        serviceNode.getElementsByTagName(SEMANTIC_EXTENSION_ELEMENTS))
        .filter(Objects::nonNull)
        .filter(el -> el.getLocalName().equals(EXTENSION_ELEMENTS))
        .flatMap(el -> asElementStream(el.getChildNodes()))
        .filter(child -> child.getLocalName().equals("annotation"))
        .map(child -> unmarshall(Annotation.class, Annotation.class, child))
        .flatMap(StreamUtil::trimStream);
  }


  /**
   * Determines the default {@link KnowledgeAssetType} for a model of a given representation, when
   * no other explicit information is available;
   *
   * @param mime the (Trisotech) mime type that encodes the model's representation
   * @return the default {@link KnowledgeAssetType}
   */
  @Nonnull
  public static KnowledgeAssetType getDefaultAssetType(
      @Nullable final String mime) {
    var lang = TTNotations.detectTTLanguage(mime);
    switch (lang) {
      case DMN:
        return KnowledgeAssetTypeSeries.Decision_Model;
      case KEM:
        return KnowledgeAssetTypeSeries.Lexicon;
      case CMMN:
        return KnowledgeAssetTypeSeries.Case_Management_Model;
      case BPMN:
        return KnowledgeAssetTypeSeries.Protocol;
      case UNSUPPORTED:
      default:
        throw new UnsupportedOperationException("Unrecognized model type " + mime);
    }
  }


  /**
   * Constructs a {@link SyntacticRepresentation} for the binary encoded manifestation of a given
   * model.
   *
   * @return the {@link SyntacticRepresentation} at the Encoded level
   */
  @Nonnull
  public static Optional<SyntacticRepresentation> getDefaultRepresentation(
      @Nullable final TrisotechFileInfo info) {
    if (info == null || info.getMimetype() == null) {
      return Optional.empty();
    }
    return getDefaultRepresentation(info.getMimetype());
  }

  /**
   * Maps a mimeType to the default {@link SyntacticRepresentation} for the language implies by the
   * mimeType
   *
   * @param mimeType the mimeType
   * @return the default representation for the given mimeType
   */
  public static Optional<SyntacticRepresentation> getDefaultRepresentation(
      @Nonnull final String mimeType) {
    var lang = TTNotations.detectTTLanguage(mimeType);
    return getDefaultRepresentation(lang);
  }

  /**
   * Maps a representation language to the default {@link SyntacticRepresentation} for artifacts in
   * that language
   *
   * @param lang the language
   * @return the default representation for the given mimeType
   */
  @Nonnull
  public static Optional<SyntacticRepresentation> getDefaultRepresentation(
      @Nonnull final TTLanguages lang) {
    switch (lang) {
      case BPMN:
        return Optional.of(rep(BPMN_2_0, XML_1_1, defaultCharset(), Encodings.DEFAULT));
      case DMN:
        return Optional.of(rep(DMN_1_2, XML_1_1, defaultCharset(), Encodings.DEFAULT));
      case CMMN:
        return Optional.of(rep(CMMN_1_1, XML_1_1, defaultCharset(), Encodings.DEFAULT));
      case KEM:
        return Optional.of(rep(MVF_1_0, XML_1_1, defaultCharset(), Encodings.DEFAULT));
      case UNSUPPORTED:
      default:
        return Optional.empty();
    }
  }

  /**
   * Detects the {@link KnowledgeRepresentationLanguage} used in a model.
   *
   * @return the {@link KnowledgeRepresentationLanguage}
   */
  @Nonnull
  public static Optional<KnowledgeRepresentationLanguage> detectRepLanguage(
      @Nullable SemanticModelInfo manifest) {
    if (manifest == null) {
      return Optional.empty();
    }
    return detectLanguage(manifest.getMimetype());
  }

  /**
   * Detects the {@link KnowledgeRepresentationLanguage} implied by a mimeType.
   *
   * @return the {@link KnowledgeRepresentationLanguage}
   */
  @Nonnull
  public static Optional<KnowledgeRepresentationLanguage> detectLanguage(
      @Nullable String mimeType) {
    var lang = TTNotations.detectTTLanguage(mimeType);
    switch (lang) {
      case BPMN:
        return Optional.of(BPMN_2_0);
      case DMN:
        return Optional.of(DMN_1_2);
      case CMMN:
        return Optional.of(CMMN_1_1);
      case KEM:
        return Optional.of(MVF_1_0);
      case UNSUPPORTED:
      default:
        return Optional.empty();
    }
  }


  /**
   * If possible, adds an optional KnowledgeCarrier that references the Kommunicator version of a
   * BPM+ model: an interactive, read-only, animatable variant of the model
   *
   * @param artifactId the artifact ID
   * @param manifest   the internal manifest of the model to be referenced
   * @param status     the publication Status
   * @param config     the environment configuration
   */
  @Nonnull
  public static Optional<KnowledgeArtifact> tryAddKommunicatorArtifact(
      @Nonnull final ResourceIdentifier artifactId,
      @Nonnull final SemanticModelInfo manifest,
      @Nonnull final PublicationStatus status,
      @Nonnull final TTWEnvironmentConfiguration config) {

    return getKommunicatorLink(manifest, config)
        .flatMap(link ->
            detectLanguage(manifest.getMimetype())
                .map(lang ->
                    new KnowledgeArtifact()
                        .withArtifactId(
                            defaultArtifactId(artifactId, HTML)
                                .withEstablishedOn(parseDateTime(manifest.getUpdated())))
                        .withRepresentation(rep(lang))
                        .withExpressionCategory(Interactive_Resource)
                        .withLifecycle(new Publication()
                            .withPublicationStatus(status))
                        .withName(manifest.getName().trim() + " -- Kommunicator Preview")
                        .withLocator(link)));
  }


  /**
   * Retrieves the {@link KnowledgeAssetType} for a given asset ID.
   * <p>
   * Supports both Knowledge Assets and Service Assets, using the semantic metadata annotated on the
   * model, and queried via Knowledge Graph
   *
   * @param manifest    the Model manifest
   * @param defaultType the default Asset type, if none other can be detected
   * @return the KnowledgeAssetType associated to the asset according to the carrier model's
   * annotations, if any
   */
  @Nonnull
  public static List<KnowledgeAssetType> getDeclaredAssetTypes(
      @Nonnull final SemanticModelInfo manifest,
      @Nonnull final Supplier<KnowledgeAssetType> defaultType) {
    var declared = manifest.getAssetTypes().stream()
        .map(type -> KnowledgeAssetTypeSeries.resolveId(type)
            .or(() -> ClinicalKnowledgeAssetTypeSeries.resolveId(type)))
        .flatMap(StreamUtil::trimStream)
        .distinct()
        .collect(Collectors.toList());
    return declared.isEmpty()
        ? List.of(defaultType.get())
        : declared;
  }


}
