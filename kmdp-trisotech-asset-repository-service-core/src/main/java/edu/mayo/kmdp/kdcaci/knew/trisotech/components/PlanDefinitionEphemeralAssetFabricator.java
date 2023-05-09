package edu.mayo.kmdp.kdcaci.knew.trisotech.components;

import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newId;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.JSON;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.FHIR_STU3;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Encoded_Knowledge_Expression;

import edu.mayo.kmdp.knowledgebase.introspectors.fhir.stu3.PlanDefinitionMetadataIntrospector;
import edu.mayo.kmdp.ops.tranx.bpm.KarsAnonymousCcpmToPlanDefPipeline;
import edu.mayo.kmdp.trisotechwrapper.components.NamespaceManager;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nonnull;
import org.omg.spec.api4kp._20200801.AbstractCarrier.Encodings;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.api.knowledgebase.v4.server.TranscreateApiInternal._applyNamedIntrospectDirect;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.KnowledgeAssetCatalogApi;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.KnowledgeAssetRepositoryApi;
import org.omg.spec.api4kp._20200801.datatypes.Bindings;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries;

public class PlanDefinitionEphemeralAssetFabricator
    implements EphemeralAssetFabricator {

  private final KnowledgeAssetCatalogApi cat;

  private final KarsAnonymousCcpmToPlanDefPipeline pipeline;

  private final _applyNamedIntrospectDirect introspector;

  private final NamespaceManager names;

  public PlanDefinitionEphemeralAssetFabricator(
      @Nonnull final NamespaceManager names,
      @Nonnull final KnowledgeAssetCatalogApi cat,
      @Nonnull final KnowledgeAssetRepositoryApi repo) {
    this.cat = cat;
    this.names = names;

    this.pipeline = new KarsAnonymousCcpmToPlanDefPipeline(
        cat,
        repo,
        (modelId, vt, query, xParams) -> Answer.of(
            List.of(new Bindings())),
        URI.create("https://ontology.mayo.edu/taxonomies/clinicalsituations"));

    this.introspector = new PlanDefinitionMetadataIntrospector();
  }

  @Override
  public Answer<KnowledgeCarrier> fabricate(
      @Nonnull final UUID sourceAssetId,
      @Nonnull final String sourceVersionTag) {
    var root = newId(
        names.getAssetNamespace(),
        UUIDEncrypter.decrypt(sourceAssetId, FHIR_STU3.getUuid()),
        sourceVersionTag);
    return pipeline.trigger(mapAssetId(root), root, Encoded_Knowledge_Expression);
  }

  @Override
  public Answer<KnowledgeCarrier> fabricateSurrogate(
      @Nonnull final UUID sourceAssetId,
      @Nonnull final String sourceVersionTag) {
    return fabricate(sourceAssetId, sourceVersionTag)
        .flatMap(kc -> introspector.applyNamedIntrospectDirect(
            PlanDefinitionMetadataIntrospector.id,
            kc,
            null));
  }

  @Override
  public Optional<String> getFabricatableVersion(
      @Nonnull final UUID sourceAssetId) {
    var rootId = UUIDEncrypter.decrypt(sourceAssetId, FHIR_STU3.getUuid());
    return cat.getKnowledgeAsset(rootId)
        // need the (latest) version
        .map(ka -> ka.getAssetId().getVersionTag())
        .getOptionalValue();
  }


  @Override
  public Optional<Pointer> pledge(
      @Nonnull final Pointer source) {
    if (!canFabricate(source)) {
      return Optional.empty();
    }
    var ptr = mapAssetId(source)
        .toPointer()
        .withType(ClinicalKnowledgeAssetTypeSeries.Cognitive_Care_Process_Model.getReferentId())
        .withMimeType(codedRep(FHIR_STU3, JSON, Charset.defaultCharset(), Encodings.DEFAULT))
        .withName(source.getName() + " (Ephemeral)");
    if (source.getHref() != null) {
      var url = source.getHref().toString().replace(source.getTag(), ptr.getTag());
      ptr.withHref(URI.create(url));
    }
    return Optional.of(ptr);
  }

  @Override
  public boolean canFabricate(
      @Nonnull Pointer source) {
    return Objects.equals(
        ClinicalKnowledgeAssetTypeSeries.Clinical_Case_Management_Model.getReferentId(),
        source.getType());
  }

  @Override
  public Answer<KnowledgeCarrier> applyNamedTransform(UUID operatorId, UUID kbaseId,
      String versionTag, String xParams) {
    return pipeline.applyNamedTransform(operatorId, kbaseId, versionTag, xParams);
  }


  /**
   * Maps the Asset ID of a root Case Model to the Asset ID of the derivative cCPM,
   * deterministically, based on the adopted transcreation
   *
   * @param source the ID of the root Case Model
   * @return a Pointer to the mapped cCPM
   */
  private ResourceIdentifier mapAssetId(ResourceIdentifier source) {
    return SemanticIdentifier.newId(
        UUIDEncrypter.encrypt(source.getUuid(), FHIR_STU3.getUuid()),
        source.getVersionTag()
    );
  }


  public static class UUIDEncrypter {

    private UUIDEncrypter() {
      // functions only
    }

    public static UUID encrypt(UUID original, UUID key) {
      var secretKey = convertUUIDToBytes(key);
      var src = convertUUIDToBytes(original);
      var encrypted = xor(src, secretKey);
      return convertBytesToUUID(encrypted);
    }

    public static UUID decrypt(UUID encrypted, UUID key) {
      var secretKey = convertUUIDToBytes(key);
      var enc = convertUUIDToBytes(encrypted);
      var decrypted = xor(enc, secretKey);
      return convertBytesToUUID(decrypted);
    }

    public static byte[] convertUUIDToBytes(UUID uuid) {
      ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
      bb.putLong(uuid.getMostSignificantBits());
      bb.putLong(uuid.getLeastSignificantBits());
      return bb.array();
    }

    public static UUID convertBytesToUUID(byte[] bytes) {
      ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
      long high = byteBuffer.getLong();
      long low = byteBuffer.getLong();
      return new UUID(high, low);
    }

    public static byte[] xor(byte[] ba1, byte[] ba2) {
      byte[] result = new byte[ba1.length];
      for (int i = 0; i < ba1.length; i++) {
        result[i] = (byte) (ba1[i] ^ ba2[i]);
      }
      return result;
    }

  }


}
