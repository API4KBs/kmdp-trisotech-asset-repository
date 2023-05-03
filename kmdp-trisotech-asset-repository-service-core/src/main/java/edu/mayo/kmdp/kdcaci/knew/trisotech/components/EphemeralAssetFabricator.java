package edu.mayo.kmdp.kdcaci.knew.trisotech.components;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;

public interface EphemeralAssetFabricator {

  Answer<KnowledgeCarrier> fabricate(UUID assetId, String versionTag);
  Optional<String> getFabricatableVersion(UUID assetId);

  Stream<Pointer> promise(Pointer source);

  default boolean canFabricate(Pointer ptr) {
    return false;
  }

}
