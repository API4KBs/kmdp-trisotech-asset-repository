package edu.mayo.kmdp.trisotechwrapper.components.operators;

import static edu.mayo.kmdp.registry.Registry.BASE_UUID_URN;
import static edu.mayo.kmdp.trisotechwrapper.components.operators.KEMHelper.ensureVocabulary;
import static edu.mayo.kmdp.trisotechwrapper.components.operators.KEMHelper.getArtifactId;
import static edu.mayo.kmdp.trisotechwrapper.components.operators.KEMHelper.getAssetId;
import static edu.mayo.kmdp.trisotechwrapper.components.operators.KEMHelper.getConceptName;
import static edu.mayo.kmdp.trisotechwrapper.components.operators.KEMHelper.getDocumentation;
import static edu.mayo.kmdp.trisotechwrapper.components.operators.KEMHelper.getExamples;
import static edu.mayo.kmdp.trisotechwrapper.components.operators.KEMHelper.getKEMConcepts;
import static edu.mayo.kmdp.trisotechwrapper.components.operators.KEMHelper.getNotes;
import static edu.mayo.kmdp.trisotechwrapper.components.operators.KEMHelper.getPrefixes;
import static edu.mayo.kmdp.trisotechwrapper.components.operators.KEMHelper.getSemanticAnnotation;
import static edu.mayo.kmdp.trisotechwrapper.components.operators.KEMHelper.getVocabName;
import static edu.mayo.kmdp.trisotechwrapper.components.operators.KEMHelper.resolveNamespace;
import static edu.mayo.kmdp.trisotechwrapper.components.operators.KEMHelper.toConceptUUID;
import static edu.mayo.kmdp.trisotechwrapper.config.TTWConfigParamsDef.ASSET_ID_ATTRIBUTE;

import edu.mayo.kmdp.trisotechwrapper.config.TTWEnvironmentConfiguration;
import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.Code;
import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.EdgeModelElement;
import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.KemConcept;
import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.KemModel;
import java.util.Collections;
import java.util.LinkedHashMap;
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
  protected final Map<String, KEMtoMVFTranslatorExtension> addOns = new LinkedHashMap<>();

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
    for (var addOn : extensions) {
      addOns.put(addOn.getApplicableTag(), addOn);
    }
  }

  /**
   * Default Constructor
   * <p>
   * Creates a {@link KEMtoMVFTranslator} with no extensions
   */
  public KEMtoMVFTranslator(@Nonnull final TTWEnvironmentConfiguration cfg) {
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
  public MVFDictionary translate(KemModel kem) {
    var dict = new MVFDictionary()
        .withName(getVocabName(kem));
    dict.withUri(getArtifactId(kem, cfg));
    getAssetId(kem, cfg.getTyped(ASSET_ID_ATTRIBUTE))
        .ifPresent(dict::withReference);

    // Apply core mapping
    var kemConcepts = mapToConcepts(dict, kem, getPrefixes(kem));
    // Apply Extensions
    addOns.values().forEach(on -> on.apply(dict, kemConcepts, kem));

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
  private Map<UUID, KemConcept> mapToConcepts(
      MVFDictionary dictionary,
      KemModel kem,
      Map<String, String> namespaceMap) {

    // get the Concepts from KEM
    var kemConcepts = getKEMConcepts(kem);

    // maps KEM Concepts to MVF Concepts
    Map<UUID, MVFEntry> dictIndex = kemConcepts.values().stream()
        .map(kc -> linkMVFConcept(kc, dictionary, namespaceMap))
        .collect(Collectors.toMap(
            IndexableMVFEntry::getGuid,
            e -> e
        ));

    // Maps Relationships
    kem.getEdgeModelElements()
        .forEach(edg -> toRel(edg, dictIndex, kem));

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
   * @param kc           the KEM Concept to be mapped
   * @param dict         the MVFDictionary to attach the mapped MVF Concept to
   * @param namespaceMap the prefix/namespace map, to build concept URIs
   * @return the generated MVF Entry
   */
  private IndexableMVFEntry linkMVFConcept(
      KemConcept kc,
      MVFDictionary dict,
      Map<String, String> namespaceMap) {
    var mvf = toMVFConcept(kc, namespaceMap);
    dict.getEntry().add(mvf);

    kc.getProperties().getCode()
        .forEach(cd -> toTerm(cd, kc, dict));
    return mvf;
  }

  /**
   * @param kc
   * @param namespaceMap
   * @return
   */
  protected IndexableMVFEntry toMVFConcept(
      KemConcept kc,
      Map<String, String> namespaceMap) {
    var guid = toConceptUUID(kc);
    var entry = new IndexableMVFEntry(guid);

    entry.withName(getConceptName(kc));

    getSemanticAnnotation(kc).stream().findFirst()
        .ifPresent(entry::withExternalReference);
    entry.withUri(resolveNamespace(kc, namespaceMap) + guid);

    entry.withReference(kc.getProperties().getTypeRef());

    entry.withVocabularyEntry(toVocabularyEntry(kc));

    return entry;
  }

  private VocabularyEntry toVocabularyEntry(KemConcept kc) {
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


  private void toTerm(Code cd, KemConcept kc, MVFDictionary dict) {
    var voc = ensureVocabulary(dict, cd.getCodingSystem(), cd.getCodingSystemDisplay());
    var term = toTerm(cd);
    term.withMVFEntry(toRef(kc));
    voc.getEntry().add(term);
  }

  protected VocabularyEntry toTerm(Code cd) {
    var voc = new VocabularyEntry();
    voc.withName(cd.getDisplay())
        .withReference(cd.getCodingSystem() + "|" + cd.getValue())
        .withTerm(cd.getValue());
    return voc;
  }


  protected void toRel(EdgeModelElement edg, Map<UUID, MVFEntry> dictIndex, KemModel kem) {
    var src = dictIndex.get(toConceptUUID(edg.getSourceRef()));
    var tgt = dictIndex.get(toConceptUUID(edg.getTargetRef()));
    var tgtRef = toRef(tgt);

    var relTerms = (List<String>) edg.getProperties().getAdditionalProperties()
        .getOrDefault("triso:linkedTermsId", Collections.emptyList());
    var relConcepts = resolveTerms(relTerms, kem);
    var coll = "isA".equals(edg.getStencil().getId())
        ? src.getBroader()
        : src.getContext();

    for (int j = relConcepts.size() - 1; j >= 0; j--) {
      var link = relConcepts.get(j);
      var mvfLink = toRef(link);
      mvfLink.getContext().add(tgtRef);
      tgtRef = mvfLink;
    }

    coll.add(tgtRef);
  }


  protected List<KemConcept> resolveTerms(List<String> termIds, KemModel kem) {
    return termIds.stream()
        .map(ref -> kem.getNodeModelElements().stream()
            .filter(c -> ref.equals(c.getResourceId())).findFirst())
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());
  }


  public static MVFEntry toRef(MVFEntry entry) {
    return new MVFEntry()
        .withUri(entry.getUri());
  }

  protected MVFEntry toRef(KemConcept kc) {
    return new MVFEntry()
        .withUri(BASE_UUID_URN + toConceptUUID(kc.getResourceId()));
  }


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
