package edu.mayo.kmdp.kdcaci.knew.trisotech.components.translators;

import static org.apache.jena.rdf.model.ResourceFactory.createStatement;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.id.IdentifierConstants.SNAPSHOT;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.defaultSurrogateId;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.JSON;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;
import static org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatusSeries.Draft;

import com.google.common.collect.Lists;
import edu.mayo.kmdp.util.StreamUtil;
import edu.mayo.kmdp.util.Util;
import java.net.URI;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.transrepresentation.ModelMIMECoder;
import org.omg.spec.api4kp._20200801.surrogate.Annotation;
import org.omg.spec.api4kp._20200801.surrogate.Dependency;
import org.omg.spec.api4kp._20200801.surrogate.Derivative;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.Link;
import org.omg.spec.api4kp._20200801.surrogate.Publication;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetrole.KnowledgeAssetRole;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetType;
import org.omg.spec.api4kp._20200801.terms.model.ConceptDescriptor;
import org.semanticweb.owlapi.vocab.DublinCoreVocabulary;

public class MCBKSurrogateV2ToRDF {

  // All below are likely wrong until changed.
  public static final String LCC = "https://www.omg.org/spec/LCC/Languages/LanguageRepresentation/";
  public static final String API4KP = "https://www.omg.org/spec/API4KP/api4kp/";
  public static final String API4KP_SERIES = "https://www.omg.org/spec/API4KP/api4kp-series/";
  public static final String KMD = "http://ontology.mayo.edu/ontologies/kmdp/";
  public static final String DC = DublinCoreVocabulary.NAME_SPACE;


  public static final String ASSET = "KnowledgeAsset";
  public static final URI ASSET_URI = URI.create(API4KP + ASSET);

  public static final String IDENTIFIED_BY = "identifiedBy";
  public static final URI IDENTIFIED_BY_URI = URI.create(LCC + IDENTIFIED_BY);
  public static final String HAS_TAG = "hasTag";
  public static final URI HAS_TAG_URI = URI.create(LCC + HAS_TAG);
  // (Asset|Artifact) lcc:identified_by o lcc:hasTag
  public static final String TAG_ID = "tag";
  public static final URI TAG_ID_URI = URI.create(KMD + TAG_ID);

  public static final String HAS_VERSION = "hasVersion";
  public static final URI HAS_VERSION_URI = URI.create(API4KP_SERIES + HAS_VERSION);
  // (Version) lcc:identified_by o lcc:hasTag
  public static final String HAS_VERSION_TAG = "hasVersionTag";
  public static final URI HAS_VERSION_TAG_URI = URI.create(KMD + HAS_VERSION_TAG);
  public static final String ESTABLISHED = "hasObservedDateTime";
  public static final URI ESTABLISHED_URI = URI.create(API4KP_SERIES + ESTABLISHED);

  public static final String HAS_CARRIER = "isCarriedBy";
  public static final URI HAS_CARRIER_URI = URI.create(API4KP + HAS_CARRIER);
  public static final String HAS_SURROGATE = "hasAssetSurrogate";
  public static final URI HAS_SURROGATE_URI = URI.create(API4KP + HAS_SURROGATE);
  // subProperty of hasAssetSurrogate
  public static final String HAS_CANONICAL_SURROGATE = "hasCanonicalSurrogate";
  public static final URI HAS_CANONICAL_SURROGATE_URI = URI.create(KMD + HAS_CANONICAL_SURROGATE);

  public static final String FORMAT = DublinCoreVocabulary.FORMAT.getShortForm();
  public static final URI FORMAT_URI = URI.create(DC + FORMAT);


  public Model transform(KnowledgeAsset asset) {
    var surrogate = asset.getSurrogate().stream()
        .filter(ka -> Knowledge_Asset_Surrogate_2_0.sameAs(ka.getRepresentation().getLanguage()))
        .findFirst();
    var surrId = surrogate
        .map(KnowledgeArtifact::getArtifactId)
        .orElseGet(() -> defaultSurrogateId(asset.getAssetId(), Knowledge_Asset_Surrogate_2_0));
    var surrMime = surrogate
        .map(s -> codedRep(s.getRepresentation()))
        .orElseGet(() -> codedRep(Knowledge_Asset_Surrogate_2_0, JSON));
    var model = ModelFactory.createDefaultModel();
    model.add(registerAssetByCanonicalSurrogate(asset, surrId, surrMime)
        .collect(Collectors.toList()));
    return model;
  }


  public Stream<Statement> registerAssetByCanonicalSurrogate(
      KnowledgeAsset assetSurrogate,
      ResourceIdentifier surrogateId,
      String surrogateMimeType) {
    var s1 = registerAsset(
        assetSurrogate,
        surrogateId,
        surrogateMimeType);
    var s2 = assetSurrogate.getCarriers().stream()
        .flatMap(ka -> registerArtifactToAsset(
            assetSurrogate.getAssetId(),
            ka,
            Util.coalesce(ModelMIMECoder.encode(ka.getRepresentation()), ka.getMimeType())));
    // exclude the canonical surrogate, which is processed by 'registerAsset'
    var s3 = assetSurrogate.getSurrogate().stream()
        .filter(surr -> !surr.getArtifactId().sameAs(surrogateId))
        .flatMap(surr -> registerSurrogateToAsset(
            assetSurrogate.getAssetId(),
            surr,
            ModelMIMECoder.encode(surr.getRepresentation())));

    return Stream.concat(s1, Stream.concat(s2, s3));
  }


  public Stream<Statement> registerArtifactToAsset(ResourceIdentifier assetPointer,
      KnowledgeArtifact artifact, String mimeType) {
    ResourceIdentifier artifactId = artifact.getArtifactId();
    return Stream.concat(
            Stream.of(
                toStatement(assetPointer.getVersionId(), HAS_CARRIER_URI, artifactId.getResourceId()),
                toStatement(artifactId.getResourceId(), HAS_VERSION_URI, artifactId.getVersionId()),
                toStringValueStatement(artifactId.getResourceId(), TAG_ID_URI,
                    artifactId.getUuid().toString()),
                toStringValueStatement(artifactId.getVersionId(), HAS_VERSION_TAG_URI,
                    artifactId.getVersionTag()),
                toStringValueStatement(artifactId.getResourceId(), FORMAT_URI,
                    Util.isNotEmpty(mimeType) ? mimeType : "/"),
                toLongValueStatement(artifactId.getVersionId(),
                    ESTABLISHED_URI, getEstablishedOn(artifact.getLifecycle(), artifactId))),
            artifact.getLinks().stream()
                .flatMap(StreamUtil.filterAs(Dependency.class))
                .flatMap(dependency -> toDependencyStatements(artifactId.getVersionId(), dependency)));
  }


  public Stream<Statement> registerSurrogateToAsset(ResourceIdentifier assetPointer,
      KnowledgeArtifact surrogate, String mimeType) {
    ResourceIdentifier surrogateId = surrogate.getArtifactId();
    var l = Arrays.asList(
        toStatement(assetPointer.getVersionId(), HAS_SURROGATE_URI, surrogateId.getResourceId()),
        toStatement(surrogateId.getResourceId(), HAS_VERSION_URI, surrogateId.getVersionId()),
        toStringValueStatement(surrogateId.getResourceId(), TAG_ID_URI,
            surrogateId.getUuid().toString()),
        toStringValueStatement(surrogateId.getResourceId(), FORMAT_URI,
            Util.isNotEmpty(mimeType) ? mimeType : null),
        toStringValueStatement(surrogateId.getVersionId(), HAS_VERSION_TAG_URI,
            surrogateId.getVersionTag()),
        toLongValueStatement(surrogateId.getVersionId(),
            ESTABLISHED_URI, getEstablishedOn(surrogate.getLifecycle(), surrogateId))
    );
    return l.stream();
  }

  private Stream<Statement> registerAsset(KnowledgeAsset asset,
      ResourceIdentifier surrogate, String surrogateMimeType) {
    return this.toRdf(asset.getAssetId(), asset.getSecondaryId(),
        asset.getName(), surrogate, surrogateMimeType,
        asset.getFormalType(), asset.getRole(), asset.getAnnotation(),
        asset.getLinks(), asset.getLifecycle());
  }

  public Stream<Statement> toRdf(
      ResourceIdentifier asset, List<ResourceIdentifier> aliases,
      String assetName,
      ResourceIdentifier surrogate, String surrogateMimeType,
      List<KnowledgeAssetType> types, List<KnowledgeAssetRole> roles, List<Annotation> annotations,
      List<Link> related, Publication lifecycle) {
    List<Statement> statements = Lists.newArrayList();

    URI assetId = asset.getResourceId();
    URI assetVersionId = asset.getVersionId();
    URI surrogateId = surrogate.getResourceId();
    URI surrogateVersionId = surrogate.getVersionId();

    // annotations
    statements.addAll(annotations.stream().map(
        annotation -> this.toStatement(
            assetVersionId,
            annotation.getRel().getReferentId(),
            annotation.getRef().getEvokes())
    ).collect(Collectors.toList()));

    // related
    statements.addAll(related.stream()
        .flatMap(StreamUtil.filterAs(Dependency.class))
        .flatMap(dependency -> toDependencyStatements(assetVersionId, dependency))
        .collect(Collectors.toList()));

    // composites
    statements.addAll(related.stream()
        .flatMap(StreamUtil.filterAs(org.omg.spec.api4kp._20200801.surrogate.Component.class))
        .flatMap(part -> toParthoodStatements(assetVersionId, part))
        .collect(Collectors.toList()));

    // derivation
    statements.addAll(related.stream()
        .flatMap(StreamUtil.filterAs(Derivative.class))
        .flatMap(derivative -> toDerivationStatements(assetVersionId, derivative))
        .collect(Collectors.toList()));

    // type of Asset
    statements
        .add(this.toStatement(
            assetVersionId,
            URI.create(RDF.type.getURI()),
            ASSET_URI));

    // version
    statements.add(this.toStringValueStatement(
        assetId,
        TAG_ID_URI,
        asset.getTag()));
    statements.add(this.toStatement(
        assetId,
        HAS_VERSION_URI,
        assetVersionId));
    statements.add(this.toStringValueStatement(
        assetVersionId,
        HAS_VERSION_TAG_URI,
        asset.getVersionTag()));
    statements.add(this.toLongValueStatement(
        assetVersionId,
        ESTABLISHED_URI,
        getEstablishedOn(lifecycle, asset)));

    // Surrogate link
    statements.add(this.toStatement(
        assetVersionId,
        HAS_CANONICAL_SURROGATE_URI,
        surrogateId));
    statements.add(this.toStatement(
        assetVersionId,
        HAS_SURROGATE_URI,
        surrogateId));
    statements.add(this.toStringValueStatement(
        surrogateId,
        FORMAT_URI,
        surrogateMimeType));
    statements.add(this.toStatement(
        surrogateId,
        HAS_VERSION_URI,
        surrogateVersionId));
    statements.add(this.toStringValueStatement(
        surrogateId,
        TAG_ID_URI,
        surrogate.getUuid().toString()));
    statements.add(this.toStringValueStatement(
        surrogateVersionId,
        HAS_VERSION_TAG_URI,
        surrogate.getVersionTag()));
    statements.add(this.toLongValueStatement(
        surrogateVersionId,
        ESTABLISHED_URI,
        surrogate.getEstablishedOn().toInstant().toEpochMilli()));

    // Asset types
    statements.addAll(types.stream().map(type ->
        this.toStatement(
            assetVersionId,
            URI.create(RDF.type.getURI()),
            type.getReferentId())
    ).collect(Collectors.toList()));

    // Asset roles
    statements.addAll(roles.stream().map(role ->
        this.toStatement(
            assetVersionId,
            URI.create(RDF.type.getURI()),
            role.getReferentId())
    ).collect(Collectors.toList()));

    // Asset name
    if (!Util.isEmpty(assetName)) {
      statements.add(
          toStringValueStatement(assetVersionId, URI.create(RDFS.label.getURI()), assetName));
    }

    aliases.stream()
        .filter(alias -> alias.getVersionId() != null)
        .forEach(alias -> statements.add(
            toStatement(assetVersionId, URI.create(OWL.SAMEAS.toString()), alias.getVersionId())));

    return statements.stream();
  }

  private Stream<Statement> toDependencyStatements(URI subj, Dependency dependency) {
    var dependencyType = ConceptDescriptor.toConceptDescriptor(dependency.getRel());
    URI tgt = dependency.getHref().getVersionId();

    return toRelatedStatements(subj, dependencyType, tgt);
  }

  private Stream<Statement> toDerivationStatements(URI subj, Derivative derivative) {
    var derivationType = ConceptDescriptor.toConceptDescriptor(derivative.getRel());
    URI tgt = derivative.getHref().getVersionId();

    return toRelatedStatements(subj, derivationType, tgt);
  }

  private Stream<Statement> toParthoodStatements(URI subj,
      org.omg.spec.api4kp._20200801.surrogate.Component part) {
    var partType = ConceptDescriptor.toConceptDescriptor(part.getRel());
    URI tgt = part.getHref().getVersionId();

    return toRelatedStatements(subj, partType, tgt);
  }

  private Stream<Statement> toRelatedStatements(URI subj, ConceptDescriptor rel, URI tgt) {
    return Stream.concat(
        Arrays.stream(rel.getClosure())
            .map(anc -> toStatement(subj, anc.getReferentId(), tgt)),
        Stream.of(this.toStatement(subj, rel.getReferentId(), tgt))
    );
  }

  public Statement toStatement(URI subject, URI predicate, URI object) {
    return createStatement(
        ResourceFactory.createResource(subject.toString()),
        ResourceFactory.createProperty(predicate.toString()),
        ResourceFactory.createResource(object.toString()));
  }

  public Statement toStringValueStatement(URI subject, URI predicate, String object) {
    return createStatement(
        ResourceFactory.createResource(subject.toString()),
        ResourceFactory.createProperty(predicate.toString()),
        ResourceFactory.createStringLiteral(object));
  }

  public Statement toLongValueStatement(URI subject, URI predicate, Long object) {
    return createStatement(
        ResourceFactory.createResource(subject.toString()),
        ResourceFactory.createProperty(predicate.toString()),
        ResourceFactory.createTypedLiteral(object));
  }


  private Long getEstablishedOn(Publication lifecycle, ResourceIdentifier resourceId) {
    return Optional.ofNullable(lifecycle)
        .flatMap(pub -> getSurrogateEstablished(pub, resourceId.getVersionTag()))
        .orElseGet(resourceId::getEstablishedOn)
        .getTime();
  }

  @Deprecated
  private Optional<Date> getSurrogateEstablished(Publication pub, String versionTag) {
    if (Draft.sameAs(pub.getPublicationStatus()) || versionTag != null && versionTag.contains(
        SNAPSHOT)) {
      return Optional.ofNullable(pub.getLastReviewedOn())
          .or(() -> Optional.ofNullable(pub.getCreatedOn()));
    } else {
      return Optional.ofNullable(pub.getCreatedOn())
          .or(() -> Optional.ofNullable(pub.getLastReviewedOn()));
    }
  }


}
