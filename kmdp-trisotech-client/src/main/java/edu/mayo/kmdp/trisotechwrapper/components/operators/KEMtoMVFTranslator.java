package edu.mayo.kmdp.trisotechwrapper.components.operators;

import static edu.mayo.kmdp.registry.Registry.BASE_UUID_URN;
import static edu.mayo.kmdp.trisotechwrapper.components.operators.KEMHelper.ensureVocabulary;
import static edu.mayo.kmdp.trisotechwrapper.components.operators.KEMHelper.getArtifactId;
import static edu.mayo.kmdp.trisotechwrapper.components.operators.KEMHelper.getAssetId;
import static edu.mayo.kmdp.trisotechwrapper.components.operators.KEMHelper.getKEMConcepts;
import static edu.mayo.kmdp.trisotechwrapper.components.operators.KEMHelper.getSemanticAnnotation;
import static edu.mayo.kmdp.trisotechwrapper.components.operators.KEMHelper.getVocabName;
import static edu.mayo.kmdp.trisotechwrapper.components.operators.KEMHelper.toConceptId;

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

  protected final Map<String, KEMtoMVFTranslatorExtension> addOns = new LinkedHashMap<>();

  /**
   * Constructor
   *
   * @param extensions A list of {@link KEMtoMVFTranslatorExtension} add-ons, which will be used to
   *                   extend the KEM to MVF mapping
   */
  public KEMtoMVFTranslator(List<KEMtoMVFTranslatorExtension> extensions) {
    for (var addOn : extensions) {
      addOns.put(addOn.getApplicableTag(), addOn);
    }
  }

  /**
   * Default Constructor Creates a {@link KEMtoMVFTranslator}
   */
  public KEMtoMVFTranslator() {
    this(Collections.emptyList());
  }

  /**
   * Implementation of the KEM to MVF mapping, in the spirit of the API4KP 'applyTransrepresent'
   * operation. Notice that, being intended as an internal component, this class does not explicitly
   * implement the public API.
   *
   * @param kem the input {@link KemModel}
   * @return the mapped MVF model, as a {@link MVFDictionary}
   */
  public MVFDictionary translate(KemModel kem) {
    var dict = new MVFDictionary()
        .withName(getVocabName(kem))
        .withUri(getAssetId(kem).orElse(null))
        .withReference(getArtifactId(kem));

    // Apply core mapping
    var kemConcepts = applyCoreMapping(dict, kem);

    // Apply Extensions
    applyAddons(dict, kemConcepts, kem);

    return dict;
  }

  private Map<UUID, KemConcept> applyCoreMapping(MVFDictionary dict, KemModel kem) {

    // get the Concepts from KEM
    var kemConcepts = getKEMConcepts(kem);

    // maps KEM Concepts to MVF Concepts
    Map<UUID, MVFEntry> dictIndex = kemConcepts.values().stream()
        .map(kc -> toMVFConcept(kc, dict))
        .collect(Collectors.toMap(
            IndexableMVFEntry::getGuid,
            e -> e
        ));

    // Maps Relationships
    kem.getEdgeModelElements()
        .forEach(edg -> toRel(edg, dictIndex, kem));

    return kemConcepts;
  }

  private void applyAddons(MVFDictionary dict, Map<UUID, KemConcept> kemConcepts, KemModel kem) {
    addOns.values().forEach(addOn -> {
      addOn.preProcess(dict, kemConcepts, kem);
      kemConcepts.values().stream()
          .filter(addOn::appliesTo)
          .forEach(kc -> addOn.process(kc, dict, kem));
    });
  }

  private IndexableMVFEntry toMVFConcept(KemConcept kc, MVFDictionary dict) {
    var mvfe = toMVFConcept(kc);
    dict.getEntry().add(mvfe);

    kc.getProperties().getCode()
        .forEach(cd -> mapCode(cd, kc, dict));

    return mvfe;
  }

  protected IndexableMVFEntry toMVFConcept(KemConcept kc) {
    var entry = new IndexableMVFEntry();
    var guid = toConceptId(kc.getResourceId());

    entry.withName(kc.getProperties().getName())
        .withUri(BASE_UUID_URN + guid)
        .withDescription(kc.getProperties().getDocumentation());
    entry.getVocabularyEntry()
        .add(new VocabularyEntry()
            .withTerm(kc.getProperties().getName()));

    getSemanticAnnotation(kc)
        .ifPresent(entry::withExternalReference);

    entry.setGuid(guid);
    return entry;
  }


  private void mapCode(Code cd, KemConcept kc, MVFDictionary dict) {
    var voc = ensureVocabulary(dict, cd.getCodingSystem(), cd.getCodingSystemDisplay());
    var term = toTerm(cd);
    term.withMVFEntry(toRef(kc));
    voc.getEntry().add(term);
  }

  protected VocabularyEntry toTerm(Code cd) {
    var voc = new VocabularyEntry();
    voc.withName(cd.getDisplay())
        .withReference(cd.getCodingSystem())
        .withTerm(cd.getValue());
    return voc;
  }


  protected void toRel(EdgeModelElement edg, Map<UUID, MVFEntry> dictIndex, KemModel kem) {
    var src = dictIndex.get(toConceptId(edg.getSourceRef()));
    var tgt = dictIndex.get(toConceptId(edg.getTargetRef()));
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
        .withUri(BASE_UUID_URN + toConceptId(kc.getResourceId()));
  }

  static class IndexableMVFEntry extends MVFEntry {

    private UUID guid;

    public UUID getGuid() {
      return guid;
    }

    public void setGuid(UUID guid) {
      this.guid = guid;
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
