package edu.mayo.kmdp.publication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatusSeries.Draft;
import static org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatusSeries.Unpublished;

import edu.mayo.kmdp.TrisotechAssetRepositoryTestConfig;
import edu.mayo.kmdp.kdcaci.knew.trisotech.TrisotechAssetRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.id.Pointer;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ContextConfiguration(classes = TrisotechAssetRepositoryTestConfig.class)
@TestPropertySource(properties = {
    "edu.mayo.kmdp.trisotechwrapper.repositoryName=MEA-Test",
    "edu.mayo.kmdp.trisotechwrapper.repositoryPath=/",
    "edu.mayo.kmdp.trisotechwrapper.repositoryId=d4aca01b-d446-4bc8-a6f0-85d84f4c1aaf",
    "edu.mayo.kmdp.trisotechwrapper.baseUrl=https://mc.trisotech.com/",
    "edu.mayo.kmdp.application.flag.assetsOnly=true",
    "edu.mayo.kmdp.application.flag.publishedOnly=false"
})
class AllAssetsTest {

  @Autowired
  TrisotechAssetRepository tar;

  @Test
  void testNoAccessForUnpublished() {
    List<Pointer> ptrs = tar.listKnowledgeAssets()
        .orElseGet(Assertions::fail);

    assertTrue(ptrs.stream().map(Pointer::getName).anyMatch("NeverPublished"::equals));
    assertTrue(ptrs.stream().map(Pointer::getName).anyMatch("PublishedBefore"::equals));
  }

  @Test
  void testGetAssetOnNeverPublishedArtifact() {
    // version X of asset Y is set, but on an unpublished model
    Answer<KnowledgeAsset> ka = tar.getKnowledgeAsset(
        UUID.fromString("1cbbf2bb-8224-42f3-b21c-60c026a4eff9"));
    assertTrue(ka.isSuccess());

    Answer<KnowledgeAsset> ka2 = tar.getKnowledgeAssetVersion(
        UUID.fromString("1cbbf2bb-8224-42f3-b21c-60c026a4eff9"), "0.0.0");
    assertTrue(ka2.isSuccess());

    KnowledgeAsset surr = ka.get();
    assertTrue(Unpublished.sameAs(surr.getLifecycle().getPublicationStatus()));

    assertFalse(surr.getCarriers().isEmpty());
    KnowledgeArtifact carr = surr.getCarriers().get(0);
    assertTrue(Unpublished.sameAs(carr.getLifecycle().getPublicationStatus()));

    ResourceIdentifier artifactId = carr.getArtifactId();
    assertNotNull(artifactId.getVersionTag());
    assertTrue(artifactId.getVersionTag().startsWith("0.0.0-"));
  }


  @Test
  void getAssetOnLatestPublished() {
    // latest version has an explicit publication status
    KnowledgeAsset surr = tar.getKnowledgeAssetVersion(
            UUID.fromString("3c66cf3a-93c4-4e09-b1aa-14088c76aded"), "1.1.1")
        .orElseGet(Assertions::fail);
    assertTrue(Draft.sameAs(surr.getLifecycle().getPublicationStatus()));

    assertEquals(1, surr.getCarriers().size());
    KnowledgeArtifact carrier = surr.getCarriers().get(0);

    assertTrue(Draft.sameAs(carrier.getLifecycle().getPublicationStatus()));
    assertEquals("2.1.1-1607494438000", carrier.getArtifactId().getVersionTag());
  }


  @Test
  void getAssePreviouslyPublished() {
    // latest version has an explicit publication status
    KnowledgeAsset surr = tar.getKnowledgeAssetVersion(
            UUID.fromString("a5fc2034-318c-4f3e-87b1-462ddb198ceb"), "0.0.1")
        .orElseGet(Assertions::fail);
    assertTrue(Unpublished.sameAs(surr.getLifecycle().getPublicationStatus()));

    assertEquals(1, surr.getCarriers().size());
    KnowledgeArtifact carrier = surr.getCarriers().get(0);

    assertTrue(Unpublished.sameAs(carrier.getLifecycle().getPublicationStatus()));
    assertTrue(carrier.getArtifactId().getVersionTag().startsWith("0.0.0-"));
  }

  @Test
  void getAssePreviouslyPublishedCanonicalCarrier() {
    // latest version has an explicit publication status
    KnowledgeCarrier carrier = tar.getKnowledgeAssetVersionCanonicalCarrier(
            UUID.fromString("a5fc2034-318c-4f3e-87b1-462ddb198ceb"), "0.0.1")
        .orElseGet(Assertions::fail);
    assertTrue(carrier.getArtifactId().getVersionTag().startsWith("0.0.0-"));
    String xml = carrier.asString()
        .orElseGet(Assertions::fail);
    assertTrue(xml.contains("Senseless Source"));
  }

  @Test
  void getAssePreviouslyPublishedCanonicalCarrierVersion() {
    // latest version has an explicit publication status
    KnowledgeCarrier carrier = tar.getKnowledgeAssetCarrierVersion(
            UUID.fromString("a5fc2034-318c-4f3e-87b1-462ddb198ceb"), "0.0.1",
            UUID.fromString("c979feb2-3baa-4276-bc4e-cb99ca5e6346"), "0.2.1")
        .orElseGet(Assertions::fail);
    assertTrue(carrier.getArtifactId().getVersionTag().startsWith("0.2.1-"));
    String xml = carrier.asString()
        .orElseGet(Assertions::fail);
    assertFalse(xml.contains("Senseless Source"));
  }
}
