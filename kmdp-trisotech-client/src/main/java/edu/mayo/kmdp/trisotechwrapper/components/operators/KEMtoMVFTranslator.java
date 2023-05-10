package edu.mayo.kmdp.trisotechwrapper.components.operators;

import static edu.mayo.kmdp.trisotechwrapper.components.operators.DataShapeHelper.getDataDefinition;
import static edu.mayo.kmdp.trisotechwrapper.components.operators.KEMHelper.ensureVocabulary;
import static edu.mayo.kmdp.trisotechwrapper.components.operators.KEMHelper.getArtifactId;
import static edu.mayo.kmdp.trisotechwrapper.components.operators.KEMHelper.getAssetId;
import static edu.mayo.kmdp.trisotechwrapper.components.operators.KEMHelper.getConceptName;
import static edu.mayo.kmdp.trisotechwrapper.components.operators.KEMHelper.getDocumentation;
import static edu.mayo.kmdp.trisotechwrapper.components.operators.KEMHelper.getExamples;
import static edu.mayo.kmdp.trisotechwrapper.components.operators.KEMHelper.getInternalConceptUri;
import static edu.mayo.kmdp.trisotechwrapper.components.operators.KEMHelper.getKEMConcepts;
import static edu.mayo.kmdp.trisotechwrapper.components.operators.KEMHelper.getNotes;
import static edu.mayo.kmdp.trisotechwrapper.components.operators.KEMHelper.getPrefixes;
import static edu.mayo.kmdp.trisotechwrapper.components.operators.KEMHelper.getSemanticAnnotation;
import static edu.mayo.kmdp.trisotechwrapper.components.operators.KEMHelper.getVocabName;
import static edu.mayo.kmdp.trisotechwrapper.components.operators.KEMHelper.getExternalConceptURI;
import static edu.mayo.kmdp.trisotechwrapper.components.operators.KEMHelper.getInternalUUID;
import static edu.mayo.kmdp.trisotechwrapper.components.operators.KEMHelper.resolveNamespace;
import static edu.mayo.kmdp.trisotechwrapper.config.TTWConfigParamsDef.ASSET_ID_ATTRIBUTE;

import edu.mayo.kmdp.trisotechwrapper.config.TTWEnvironmentConfiguration;
import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.Code;
import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.EdgeModelElement;
import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.KemConcept;
import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.KemModel;
import edu.mayo.kmdp.util.StreamUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.omg.spec.mvf._20220702.mvf.Abbreviation;
import org.omg.spec.mvf._20220702.mvf.MVFDictionary;
import org.omg.spec.mvf._20220702.mvf.MVFEntry;
import org.omg.spec.mvf._20220702.mvf.Vocabulary;
import org.omg.spec.mvf._20220702.mvf.VocabularyEntry;

/**
 * Converts a KEM model, augmented with API4KP annotations, into a more standard MVF model.
 * <p>
 * The trans-representation is based on a core mapping, plus optional 'profile'-based extensions.
 * The core mapping is partial. KEM targets the union of SDMN and SBVR, which in turn has an
 * ontology component (a subset of OWL) and a deontic rule component. The MVF core mapping targets
 * the ontology component of KEM, ignoring the data type definitions and the deontic rules - neither
 * of which can be expressed in MVF.
 * <p>
 * Scope note: this translator is meant to be applied as close as possible to the native Trisotech
 * DES API, so that the Trisotech KARS wrapper can be decoupled from the 'private' KEM model.
 */
public class KEMtoMVFTranslator {


  /**
   * The environment configuration
   */
  @Nonnull
  protected final TTWEnvironmentConfiguration cfg;

  /**
   * Optional extensible transformations
   */
  @Nonnull
  protected final List<KEMtoMVFTranslatorExtension> addOns = new ArrayList<>();

  /**
   * Constructor
   *
   * @param extensions A list of {@link KEMtoMVFTranslatorExtension} add-ons, which will be used to
   *                   extend the KEM to MVF mapping
   * @param cfg        the environment configuration
   */
  public KEMtoMVFTranslator(
      @Nonnull final List<KEMtoMVFTranslatorExtension> extensions,
      @Nonnull final TTWEnvironmentConfiguration cfg) {
    this.cfg = cfg;
    addOns.addAll(extensions);
  }

  /**
   * Default Constructor
   * <p>
   * Creates a {@link KEMtoMVFTranslator} with no extensions
   */
  public KEMtoMVFTranslator(
      @Nonnull final TTWEnvironmentConfiguration cfg) {
    this(Collections.emptyList(), cfg);
  }

  /**
   * Implementation of the KEM to MVF mapping, in the spirit of the API4KP 'applyTransrepresent'
   * operation. Notice that, being intended as an internal component, this class does not explicitly
   * implement the public API.
   * <p>
   * Maps a KEM Model to an MVF Dictionary of the same name.
   * <p>
   * Uses the Model ID, rewritten as an enterprise URI, as the ID of the Dictionary
   * <p>
   * Includes a reference to the Asset ID, if any asserted, where the reference should be
   * interpreted as 'carries'
   *
   * @param kem the input {@link KemModel}
   * @return the mapped MVF model, as a {@link MVFDictionary}
   */
  @Nonnull
  public MVFDictionary translate(
      @Nonnull final KemModel kem) {
    var dict = new MVFDictionary()
        .withName(getVocabName(kem));
    dict.withUri(getArtifactId(kem, cfg));
    getAssetId(kem, cfg.getTyped(ASSET_ID_ATTRIBUTE))
        .ifPresent(dict::withReference);

    // Apply core mapping
    var kemConcepts = mapToConcepts(dict, kem, getPrefixes(kem));
    // Apply Extensions
    addOns.forEach(on -> on.apply(dict, kemConcepts, kem));

    return dict;
  }


  /**
   * Maps the KEM Concepts to mvf:MVFEntries (MVF Concepts)
   * <p>
   * Adds the generated MVEntries to the input dictionary, then returns the KEM Concepts for further
   * mapping into additional MVF entities
   *
   * @param dictionary   the target mvf:Dictionary to add the MVFEntries to
   * @param kem          the KEM Model to extract the concepts from
   * @param namespaceMap the namespace map, used to override URIs
   * @return the KEM Concepts, for further processing
   */
  @Nonnull
  private Map<UUID, KemConcept> mapToConcepts(
      @Nonnull final MVFDictionary dictionary,
      @Nonnull final KemModel kem,
      @Nonnull final Map<String, String> namespaceMap) {

    // get the Concepts from KEM
    var kemConcepts = getKEMConcepts(kem);

    // maps KEM Concepts to MVF Concepts
    Map<UUID, MVFEntry> dictIndex = kemConcepts.values().stream()
        .map(kc -> linkMVFConcept(kem, kc, dictionary, namespaceMap))
        .collect(Collectors.toMap(
            IndexableMVFEntry::getGuid,
            e -> e
        ));

    // Maps Relationships
    kem.getEdgeModelElements()
        .forEach(edg -> toRel(edg, dictIndex, kem, namespaceMap));

    return kemConcepts;
  }

  /**
   * Maps a KEM Concept to an MVF Entry, then attaches it to the {@link MVFDictionary}
   * <p>
   * Further, maps the Codes attached to a Concept to a set of Terms in a Vocabulary (the Code
   * System the Codes originated in), linking them to the Concept and the Dictionary
   * <p>
   * (Note: Codes are not guaranteed to be resolvable, or even to be URIs!)
   *
   * @param kem          the KEM Model to extract the concepts from
   * @param kc           the KEM Concept to be mapped
   * @param dict         the MVFDictionary to attach the mapped MVF Concept to
   * @param namespaceMap the prefix/namespace map, to build concept URIs
   * @return the generated MVF Entry
   * @see #toMVFConcept(KemModel, KemConcept, Map)
   */
  @Nonnull
  private IndexableMVFEntry linkMVFConcept(
      @Nonnull final KemModel kem,
      @Nonnull final KemConcept kc,
      @Nonnull final MVFDictionary dict,
      @Nonnull final Map<String, String> namespaceMap) {
    var mvf = toMVFConcept(kem, kc, namespaceMap);
    dict.getEntry().add(mvf);

    kc.getProperties().getCode()
        .forEach(cd -> toTerm(cd, kc, dict, namespaceMap));
    return mvf;
  }

  /**
   * Maps a KEM Concept to an MVF Entry
   *
   * <p>
   * (Note: Codes are not guaranteed to be resolvable, or even to be URIs!)
   *
   * @param kem          the KEM Model to extract the concepts from
   * @param kc           the KEM Concept to be mapped
   * @param namespaceMap the prefix/namespace map, to build concept URIs
   * @return the generated MVF Entry
   */
  @Nonnull
  protected IndexableMVFEntry toMVFConcept(
      @Nonnull final KemModel kem,
      @Nonnull final KemConcept kc,
      @Nonnull final Map<String, String> namespaceMap) {
    var guid = KEMHelper.getInternalUUID(kc);
    var entry = new IndexableMVFEntry(guid);

    entry.withName(getConceptName(kc));
    entry.withUri(resolveNamespace(kc,namespaceMap) + guid);

    getSemanticAnnotation(kc).stream().findFirst()
        .ifPresent(entry::withExternalReference);
    getExternalConceptURI(kc, namespaceMap)
        .ifPresent(entry::withExternalReference);

    entry.withReference(
        getInternalConceptUri(kem, guid));
    getDataDefinition(kc)
        .ifPresent(entry::withReference);

    entry.withVocabularyEntry(toVocabularyEntry(kc));

    return entry;
  }


  /**
   * Constructs the Vocabulary Entry (Term) associated to a MVF Concept, using the human-readable
   * parts of the original KEM Concept
   *
   * @param kc the KEM Concept
   * @return a {@link VocabularyEntry} with the descriptive elements of the KEM Concept
   */
  private VocabularyEntry toVocabularyEntry(
      @Nonnull final KemConcept kc) {
    var voc = new VocabularyEntry()
        .withTerm(getConceptName(kc));

    getDocumentation(kc).ifPresent(voc::withDefinition);
    getNotes(kc).ifPresent(voc::withNote);
    getExamples(kc).ifPresent(voc::withExample);

    voc.withAbbreviation(
        kc.getProperties().getAlternatives().stream()
            .map(alt -> new Abbreviation()
                .withTerm(alt)
                .withIsAbbreviation(true))
            .collect(Collectors.toList()));
    return voc;
  }


  /**
   * Maps a (healthcare) "Code" annotation of a KEM Concept to a {@link VocabularyEntry} (Term) in a
   * {@link Vocabulary} mapped from the Code's Code System
   * <p>
   * Codes are codified designators that evoke some Concept. However, their association to a KEM
   * Concept is unspecified - should be of equivalence, but there is no guarantee. Moreover, it is
   * not always possible to derive the URI of the Code's concept from the Code itself. As a
   * consequence, it is not safe to infer an equivalence/aliasing relationship between the Code and
   * the KEM Concept/MVFEntry
   *
   * @param cd           The healthcare Code associated to the KEM Concept, to be mapped to a
   *                     VocabularyEntry
   * @param kc           the KEM Concept
   * @param dict         the scoping {@link MVFDictionary}, to which the CodeSystem/Vocabulary will
   *                     be attached
   * @param namespaceMap the prefix/namespace map, to build concept URIs
   * @see #toTerm(Code)
   */
  private void toTerm(
      @Nonnull final Code cd,
      @Nonnull final KemConcept kc,
      @Nonnull final MVFDictionary dict,
      @Nonnull final Map<String, String> namespaceMap) {
    var voc = ensureVocabulary(dict, cd.getCodingSystem(), cd.getCodingSystemDisplay());
    var term = toTerm(cd);
    term.withMVFEntry(toRef(kc, namespaceMap));
    voc.getEntry().add(term);
  }

  /**
   * Core mapping between a Code and a {@link VocabularyEntry}
   * <p>
   * Maps code, CodeSystem and Label
   *
   * @param cd the source {@link Code}
   * @return the mapped {@link VocabularyEntry}
   */
  protected VocabularyEntry toTerm(Code cd) {
    var voc = new VocabularyEntry();
    voc.withName(cd.getDisplay())
        .withReference(cd.getCodingSystem() + "|" + cd.getValue())
        .withTerm(cd.getValue());
    return voc;
  }


  /**
   * Maps a KEM Concept/Concept relationship to a MVFEntry/MVFEntry relationship. KEM "isA" is
   * mapped to MVF "broader" and KEM "related" is mapped to MVF "context". Adds each relationship to
   * the source Concept's MVFEntry, as a MVF reference to the target Concept's MVFEntry.
   * <p>
   * When handling generic relationships: if the relationship is named and reified (i.e. there
   * exists a KEM Concept referenced by the relationship name), creates a chain of references. For
   * example, [A -(rel)-> B] becomes [A --> Rel --> B]
   *
   * @param edg          the KEM Concept/Concept relationship
   * @param dictIndex    the MVFEntries derived from the KEM Concepts, indexed
   * @param kem          the scoping KEM Model
   * @param namespaceMap the prefix/namespace map, to build concept URIs
   */
  protected void toRel(
      @Nonnull final EdgeModelElement edg,
      @Nonnull final Map<UUID, MVFEntry> dictIndex,
      @Nonnull final KemModel kem,
      @Nonnull final Map<String, String> namespaceMap) {
    var src = dictIndex.get(getInternalUUID(edg.getSourceRef()));
    var tgt = dictIndex.get(getInternalUUID(edg.getTargetRef()));
    var tgtRef = toRef(tgt);

    if ("isA".equals(edg.getStencil().getId())) {
      src.getBroader().add(tgtRef);
    } else if ("relation".equals(edg.getStencil().getId())) {
      var links = edg.getProperties().getLinkedTerms();
      if (! links.isEmpty()) {
        var relConcepts = links.stream()
            .flatMap(StreamUtil.filterAs(String.class))
            .flatMap(ref -> resolveTerm(ref, kem).stream())
            .collect(Collectors.toList());

        for (int j = relConcepts.size() - 1; j >= 0; j--) {
          var link = relConcepts.get(j);
          var mvfLink = toRef(link, namespaceMap);
          mvfLink.getContext().add(tgtRef);
          tgtRef = mvfLink;
        }
        src.getContext().add(tgtRef);
      }
    }
  }


  /**
   * Resolves a KEM concept reference, as used in a 'linkedTermId', to the referenced KEM Concept
   * LinkedTermIds appear when the text/term/documentation of one Concept includes a linked,
   * navigable reference to another KEM Concept
   *
   * @param ref the 'linkedTermId' String
   * @param kem the scoping KEM model
   * @return the resolved KEM concept, if found
   */
  @Nonnull
  protected Optional<KemConcept> resolveTerm(
      @Nonnull final String ref,
      @Nonnull final KemModel kem) {
    return kem.getNodeModelElements().stream()
        .filter(c -> ref.equals(c.getResourceId()))
        .findFirst()
        .or(() -> kem.getItemModelElements().getGraphTerms().stream()
            .filter(gt -> ref.equals(gt.getResourceId()))
            .map(KEMHelper::graphTermToKemConcept)
            .findFirst());
  }


  /**
   * Creates an MVFEntry reference from another MVFEntry
   *
   * @param entry the source MVFEntry
   * @return a 'reference' to the source entry
   */
  @Nonnull
  public static MVFEntry toRef(
      @Nonnull final MVFEntry entry) {
    return new MVFEntry()
        .withUri(entry.getUri());
  }

  /**
   * Maps a KEM Concept to a MVFEntry reference - MVF Entry that only holds the URI of another
   * MVFEntry
   *
   * @param kc           the KEM Concept
   * @param namespaceMap the prefix/namespace map, to build concept URIs
   * @return the MVFEntry 'reference'
   */
  @Nonnull
  protected MVFEntry toRef(
      @Nonnull final KemConcept kc,
      @Nonnull final Map<String, String> namespaceMap) {
    return new MVFEntry()
        .withUri(resolveNamespace(kc, namespaceMap) + getInternalUUID(kc));
  }


  /**
   * {@link MVFEntry} that adds a parsed UUID, to be used as a key
   */
  static class IndexableMVFEntry extends MVFEntry {

    public IndexableMVFEntry(UUID guid) {
      this.guid = guid;
    }

    private final UUID guid;

    public UUID getGuid() {
      return guid;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      if (!super.equals(o)) {
        return false;
      }
      IndexableMVFEntry that = (IndexableMVFEntry) o;
      return Objects.equals(guid, that.guid);
    }

    @Override
    public int hashCode() {
      return Objects.hash(super.hashCode(), guid);
    }
  }
}
