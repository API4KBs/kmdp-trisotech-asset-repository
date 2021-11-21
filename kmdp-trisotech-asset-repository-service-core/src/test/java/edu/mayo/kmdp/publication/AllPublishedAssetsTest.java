package edu.mayo.kmdp.publication;

import static edu.mayo.kmdp.util.DateTimeUtil.parseDateTime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatusSeries.Draft;

import edu.mayo.kmdp.TrisotechAssetRepositoryTestConfig;
import edu.mayo.kmdp.kdcaci.knew.trisotech.TrisotechAssetRepository;
import edu.mayo.kmdp.util.DateTimeUtil;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.Answer;
import org.omg.spec.api4kp._20200801.id.Pointer;
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
    "edu.mayo.kmdp.application.flag.publishedOnly=true"
})
class AllPublishedAssetsTest {

  @Autowired
  TrisotechAssetRepository tar;

  @Test
  void testNoAccessForUnpublished() {
    List<Pointer> ptrs = tar.listKnowledgeAssets()
        .orElseGet(Assertions::fail);

    assertTrue(ptrs.stream().map(Pointer::getName).noneMatch("NeverPublished"::equals));
    assertTrue(ptrs.stream().map(Pointer::getName).anyMatch("PublishedBefore"::equals));
  }

  @Test
  void testGetAssetOnNeverPublishedArtifact() {
    // the Asset carrier was never published -> the asset is not findable
    Answer<KnowledgeAsset> ka = tar.getKnowledgeAsset(
        UUID.fromString("1cbbf2bb-8224-42f3-b21c-60c026a4eff9"));
    assertTrue(ka.isNotFound());

    Answer<KnowledgeAsset> ka2 = tar.getKnowledgeAssetVersion(
        UUID.fromString("1cbbf2bb-8224-42f3-b21c-60c026a4eff9"), "0.0.0");
    assertTrue(ka2.isNotFound());
  }


  @Test
  void getKnowledgeAssetLastPublished() {
    KnowledgeAsset surr = tar
        .getKnowledgeAsset(UUID.fromString("ee8aa50a-e642-43ac-8757-2cdc46224891"), null)
        .orElseGet(Assertions::fail);

    assertEquals(Draft.getTag(), surr.getLifecycle().getPublicationStatus().getTag());
    Date d = surr.getCarriers().get(0).getArtifactId().getEstablishedOn();
    assertEquals(parseDateTime("2021-01-07T22:45:23-06:00"), d);
  }

  @Test
  void getKnowledgeAssetLastPublished2() {
    KnowledgeAsset surr = tar
        .getKnowledgeAsset(UUID.fromString("ec0d31bb-4c20-454a-b7e1-e4bbd5350eaa"), null)
        .orElseGet(Assertions::fail);

    assertEquals(Draft.getTag(), surr.getLifecycle().getPublicationStatus().getTag());
    Date d = surr.getCarriers().get(0).getArtifactId().getEstablishedOn();
    assertEquals(parseDateTime("2021-01-08T00:44:15-06:00"), d);

    KnowledgeAsset surr2 = tar
        .getKnowledgeAssetVersion(UUID.fromString("ec0d31bb-4c20-454a-b7e1-e4bbd5350eaa"), "0.0.1",
            null)
        .orElseGet(Assertions::fail);

    assertEquals(Draft.getTag(), surr2.getLifecycle().getPublicationStatus().getTag());
    Date d2 = surr2.getCarriers().get(0).getArtifactId().getEstablishedOn();
    assertEquals(parseDateTime("2021-01-08T00:44:15-06:00"), d2);
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
    assertTrue(Draft.sameAs(surr.getLifecycle().getPublicationStatus()));

    assertEquals(1, surr.getCarriers().size());
    KnowledgeArtifact carrier = surr.getCarriers().get(0);

    assertTrue(Draft.sameAs(carrier.getLifecycle().getPublicationStatus()));
    assertTrue(carrier.getArtifactId().getVersionTag().startsWith("0.2.1-"));

    long timestamp = Long.parseLong(carrier.getArtifactId().getVersionTag().substring(6));
    Date d = Date.from(Instant.ofEpochMilli(timestamp));
    // time of publication, not time of last save
    Date d2 = DateTimeUtil.parseDateTime("2021-11-30T21:48:36-06:00");
    assertEquals(d, d2);
  }

  @Test
  void getAssePreviouslyPublishedLatestAsset() {
    // latest version has an explicit publication status
    KnowledgeAsset surr = tar.getKnowledgeAsset(
            UUID.fromString("a5fc2034-318c-4f3e-87b1-462ddb198ceb"))
        .orElseGet(Assertions::fail);
    assertTrue(Draft.sameAs(surr.getLifecycle().getPublicationStatus()));

    assertEquals(1, surr.getCarriers().size());
    KnowledgeArtifact carrier = surr.getCarriers().get(0);

    assertTrue(Draft.sameAs(carrier.getLifecycle().getPublicationStatus()));
    assertTrue(carrier.getArtifactId().getVersionTag().startsWith("0.2.1-"));

    long timestamp = Long.parseLong(carrier.getArtifactId().getVersionTag().substring(6));
    Date d = Date.from(Instant.ofEpochMilli(timestamp));
    // time of publication, not time of last save
    Date d2 = DateTimeUtil.parseDateTime("2021-11-30T21:48:36-06:00");
    assertEquals(d, d2);
  }

  @Test
  void getAssePreviouslyPublishedCanonicalCarrier() {
    // latest version has an explicit publication status
    KnowledgeCarrier carrier = tar.getKnowledgeAssetVersionCanonicalCarrier(
            UUID.fromString("a5fc2034-318c-4f3e-87b1-462ddb198ceb"), "0.0.1")
        .orElseGet(Assertions::fail);
    assertTrue(carrier.getArtifactId().getVersionTag().startsWith("0.2.1"));
    String xml = carrier.asString()
        .orElseGet(Assertions::fail);
    assertFalse(xml.contains("Senseless Source"));
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
