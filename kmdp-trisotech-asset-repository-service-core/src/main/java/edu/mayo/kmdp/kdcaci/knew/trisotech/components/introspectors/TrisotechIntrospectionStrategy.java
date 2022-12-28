/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors;

import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTAssetRepositoryConfig.TTWParams.DEFAULT_VERSION_TAG;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.KommunicatorHelper.tryAddKommunicatorLink;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.TrisotechMetadataHelper.addSemanticAnnotations;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.TrisotechMetadataHelper.detectRepLanguage;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.TrisotechMetadataHelper.extractAnnotations;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.TrisotechMetadataHelper.getDefaultAssetType;
import static edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.TrisotechMetadataHelper.getRepLanguage;
import static edu.mayo.kmdp.trisotechwrapper.TrisotechWrapper.applyTimestampToVersion;
import static edu.mayo.kmdp.util.JSonUtil.writeJsonAsString;
import static edu.mayo.kmdp.util.Util.isNotEmpty;
import static edu.mayo.kmdp.util.XMLUtil.asAttributeStream;
import static edu.mayo.kmdp.util.XMLUtil.asElementStream;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.codedRep;
import static org.omg.spec.api4kp._20200801.AbstractCarrier.rep;
import static org.omg.spec.api4kp._20200801.id.IdentifierConstants.VERSION_ZERO_SNAPSHOT;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newId;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newVersionId;
import static org.omg.spec.api4kp._20200801.id.VersionIdentifier.toSemVer;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.defaultSurrogateUUID;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.randomAssetId;
import static org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries.Care_Process_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries.Clinical_Case_Management_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries.Clinical_Decision_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.clinicalknowledgeassettype.ClinicalKnowledgeAssetTypeSeries.Clinical_Rule;
import static org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries.Depends_On;
import static org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries.Imports;
import static org.omg.spec.api4kp._20200801.taxonomy.iso639_2_languagecode._20190201.Language.English;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeartifactcategory._2020_01_20.KnowledgeArtifactCategory.Software;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries.Assessment_Predictive_And_Inferential_Models;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries.Plans_Processes_Pathways_And_Protocol_Definitions;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries.Rules_Policies_And_Guidelines;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Case_Management_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Decision_Model;
import static org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetTypeSeries.Protocol;
import static org.omg.spec.api4kp._20200801.taxonomy.krformat.SerializationFormatSeries.JSON;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.Knowledge_Asset_Surrogate_2_0;
import static org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries.asEnum;
import static org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatusSeries.Draft;
import static org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatusSeries.Final_Draft;
import static org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatusSeries.Published;
import static org.omg.spec.api4kp._20200801.taxonomy.publicationstatus.PublicationStatusSeries.Unpublished;

import com.github.zafarkhaja.semver.Version;
import edu.mayo.kmdp.kdcaci.knew.trisotech.IdentityMapper;
import edu.mayo.kmdp.kdcaci.knew.trisotech.NamespaceManager;
import edu.mayo.kmdp.kdcaci.knew.trisotech.TTAssetRepositoryConfig;
import edu.mayo.kmdp.trisotechwrapper.TrisotechWrapper;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.util.JSonUtil;
import edu.mayo.kmdp.util.StreamUtil;
import edu.mayo.kmdp.util.Util;
import edu.mayo.kmdp.util.XMLUtil;
import edu.mayo.kmdp.util.XPathUtil;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import org.omg.spec.api4kp._20200801.id.IdentifierConstants;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.surrogate.Dependency;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeArtifact;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.Link;
import org.omg.spec.api4kp._20200801.surrogate.Publication;
import org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategory;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassetcategory.KnowledgeAssetCategorySeries;
import org.omg.spec.api4kp._20200801.taxonomy.knowledgeassettype.KnowledgeAssetType;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Extract the data from the woven (by the Weaver) document to create KnowledgeAsset from model
 * data.
 */
@Component
public class TrisotechIntrospectionStrategy {

  private static final Logger logger = LoggerFactory
      .getLogger(TrisotechIntrospectionStrategy.class);

  @Autowired
  TrisotechWrapper client;

  @Autowired
  IdentityMapper mapper;

  @Autowired
  NamespaceManager names;

  @Autowired(required = false)
  TTAssetRepositoryConfig config;

  @Autowired(required = false)
  KommunicatorHelper kommunicatorHelper;

  private final XPathUtil xPathUtil = new XPathUtil();

  public TrisotechIntrospectionStrategy() {
    //
  }

  @PostConstruct
  void init() {
    if (config == null) {
      config = new TTAssetRepositoryConfig();
    }
  }


  /**
   * Generates a {@link KnowledgeAsset} Surrogate from the introspection of a BPM+ model, combined
   * with the information in its corresponding Trisotech's internal manifest
   * <p>
   * FUTURE: Currently supports DMN and CMMN models, but not BPMN
   * <p>
   * Note that, at this point, the model document has already been standardized using the
   * {@link edu.mayo.kmdp.kdcaci.knew.trisotech.components.redactors.Redactor}, and (re)annotated
   * using the {@link edu.mayo.kmdp.kdcaci.knew.trisotech.components.weavers.Weaver}
   *
   * @param dox      the BPM+ model artifact to extract metadata from
   * @param manifest the model's internal manifest
   * @return a KnowledgeAsset surrogate with metadata for that model
   * @see #extractSurrogateFromDocument(Document, TrisotechFileInfo, ResourceIdentifier)
   */
  public KnowledgeAsset extractSurrogateFromDocument(Document dox, TrisotechFileInfo manifest) {
    Optional<ResourceIdentifier> tryAssetID;
    if (mapper.isLatest(manifest)) {
      // this only works when trying to process the LATEST version
      // since the Enterprise Graph does not contain historical information
      tryAssetID = mapper.resolveModelToCurrentAssetId(manifest.getId())
          .or(() -> mapper.extractAssetIdFromDocument(dox));
    } else {
      tryAssetID = mapper.extractAssetIdFromDocument(dox);
    }

    if (tryAssetID.isEmpty()) {
      logger.warn("Model {} does not have an Asset ID - providing a random one", manifest.getId());
    }
    ResourceIdentifier assetID = tryAssetID
        .orElse(randomAssetId(names.getAssetNamespace()));

    var id = dox.getDocumentElement().getAttribute("id");

    if (logger.isDebugEnabled()) {
      logger.debug("assetID: {} : {}", assetID.getResourceId(), assetID.getVersionTag());
      logger.debug("the id found in woven document {}", id);
    }

    if (id.isEmpty()) {
      logger.error("No ID found in the woven document");
      return new KnowledgeAsset();
    }

    return extractSurrogateFromDocument(dox, manifest, assetID);
  }

  /**
   * Generates a {@link KnowledgeAsset} Surrogate from the introspection of a BPM+ model, combined
   * with the information in its corresponding Trisotech's internal manifest, assuming that the
   * model is a manifestation of the Asset with the given ID.
   * <p>
   * FUTURE: Currently supports DMN and CMMN models, but not BPMN
   * <p>
   * Note that, at this point, the model document has already been standardized using the
   * {@link edu.mayo.kmdp.kdcaci.knew.trisotech.components.redactors.Redactor}, and (re)annotated
   * using the {@link edu.mayo.kmdp.kdcaci.knew.trisotech.components.weavers.Weaver}
   *
   * @param dox      the BPM+ model artifact to extract metadata from
   * @param manifest the model's internal manifest
   * @return a KnowledgeAsset surrogate with metadata for that model
   */
  public KnowledgeAsset extractSurrogateFromDocument(
      Document dox,
      TrisotechFileInfo manifest,
      ResourceIdentifier assetID) {

    if (logger.isTraceEnabled()) {
      Optional<String> modelToString = JSonUtil.printJson(manifest);
      String wovenToString = XMLUtil.toString(dox);
      logger.trace("Attempting to extract XML KnowledgeAsset with document: {}", wovenToString);
      logger.trace("Attempting to extract XML KnowledgeAsset with trisotechFileInfo: {}",
          modelToString);
      logger.trace("Attempting to extract XML KnowledgeAsset with ResourceIdentifier: {}", assetID);
    }

    KnowledgeAsset surr;

    // Publication Status
    var lifecycle = getArtifactPublicationStatus(manifest);

    // Identifiers
    var artifactId = extractArtifactId(dox, manifest);

    var formalTypes = mapper.getDeclaredAssetTypes(assetID,
        () -> getDefaultAssetType(manifest.getMimetype()));

    var formalCategory = inferFormalCategory(assetID, formalTypes);

    var annotations = extractAnnotations(dox.getDocumentElement());

    // get the language for the document to set the appropriate values
    var synRep = getRepLanguage(dox)
        .orElseThrow(() -> new IllegalStateException("Invalid language detected"));

    // artifact<->artifact relation
    var importedArtifactIds = getArtifactImports(artifactId, manifest);

    // asset<->asset relations
    // assets are derived from the artifact relations
    var importedAssets = getAssetImports(artifactId, importedArtifactIds, manifest);

    // towards the ideal
    surr = new org.omg.spec.api4kp._20200801.surrogate.resources.KnowledgeAsset()
        .withAssetId(assetID)
        .withName(manifest.getName().trim())
        .withFormalCategory(formalCategory)
        .withFormalType(formalTypes)
        .withLifecycle(lifecycle)
        .withLinks(mergeSortedLinks(
            mergeSortedLinks(
                getRelatedAssets(importedAssets),
                getRelatedServices(manifest)),
            getOtherDependencies(dox, synRep.getLanguage())))
        // carriers
        .withCarriers(new KnowledgeArtifact()
            .withArtifactId(artifactId)
            .withName(manifest.getName().trim())
            .withLifecycle(lifecycle)
            .withLocalization(English)
            .withExpressionCategory(Software)
            .withRepresentation(synRep)
            .withMimeType(codedRep(synRep))
            .withLinks( // artifact - artifact relation/dependency
                getRelatedArtifacts(importedArtifactIds)))
        .withSurrogate(
            new KnowledgeArtifact()
                .withArtifactId(newId(
                    names.getArtifactNamespace(),
                    defaultSurrogateUUID(assetID, Knowledge_Asset_Surrogate_2_0),
                    toSemVer(artifactId.getVersionTag())))
                .withRepresentation(rep(Knowledge_Asset_Surrogate_2_0, JSON))
                .withMimeType(codedRep(Knowledge_Asset_Surrogate_2_0, JSON))
        );

    addSemanticAnnotations(surr, annotations);

    tryAddKommunicatorLink(kommunicatorHelper, manifest, surr);

    if (logger.isTraceEnabled()) {
      logger.trace(
          "surrogate in JSON format: {} ", writeJsonAsString(surr).orElse("n/a"));
    }

    return surr;
  }

  /* ----------------------------------------------------------------------------------------- */

  /**
   * Creates an Artifact Id for the given model.
   * <p>
   * The Artifact Id is derived from the Trisotech native model Id. At this point, the namespace has
   * already been rewritten. This method further parses the Id, combining it with the version (tag)
   * and a time stamp.
   *
   * @param dox      the model which contains the un-versioned artifact URI
   * @param manifest the Trisotech metadata
   * @return the structured Artifact Id, as a {@link ResourceIdentifier}
   * @see #getArtifactID(Document)
   */
  private ResourceIdentifier extractArtifactId(Document dox, TrisotechFileInfo manifest) {
    Optional<String> docId = getArtifactID(dox);
    if (logger.isDebugEnabled()) {
      logger.debug("The document id found from document is: {}", docId);
    }
    if (docId.isEmpty()) {
      // error out. Can't proceed w/o Artifact -- How did we get this far?
      throw new IllegalStateException("Failed to find artifactId in Document");
    }

    Date modelDate = Date.from(Instant.parse(manifest.getUpdated()));
    String artifactTag = docId.get();
    String artifactVersionTag = manifest.getVersion() == null
        ? applyTimestampToVersion(config.getTyped(DEFAULT_VERSION_TAG, String.class),
        modelDate.getTime())
        : applyTimestampToVersion(toSemVer(manifest.getVersion()), modelDate.getTime());

    // for the surrogate, want the version of the artifact
    ResourceIdentifier artifactID =
        newVersionId(URI.create(artifactTag), artifactVersionTag)
            .withEstablishedOn(modelDate);

    if (logger.isDebugEnabled()) {
      logger.debug("The document artifactId ResourceIdentifier found is: {}", artifactID);
    }
    return artifactID;
  }


  /**
   * Extracts the Artifact ID URI from the XML attribute in the source model.
   * <p>
   * Assumes that the Artifact Id is used as the document namespace
   *
   * @param dox the XML model
   * @return the model default namespace, assumed to be the artifact Id
   */
  public Optional<String> getArtifactID(Document dox) {
    Optional<KnowledgeRepresentationLanguage> lang = detectRepLanguage(dox);
    return lang.map(l -> {
      switch (asEnum(l)) {
        case DMN_1_2:
          return xPathUtil.xString(dox, "//*/@namespace");
        case CMMN_1_1:
          return xPathUtil.xString(dox, "//*/@targetNamespace");
        case BPMN_2_0:
          return xPathUtil.xString(dox, "//*/@targetNamespace");
        default:
          return null;
      }
    });
  }

  /* ----------------------------------------------------------------------------------------- */

  private KnowledgeAssetCategorySeries inferFormalCategory(
      ResourceIdentifier assetId, List<KnowledgeAssetType> formalTypes) {
    var categories = formalTypes.stream()
        .map(this::inferFormalCategory)
        .collect(Collectors.toSet());
    if (categories.size() > 1) {
      logger.warn("Detected multiple categories in asset {} : {}, due to types {}",
          assetId, categories, formalTypes);
    }
    if (categories.isEmpty()) {
      logger.error("Unable to determine formal category for asset {}, given types {}",
          assetId, formalTypes);
    }
    return categories.iterator().next();
  }

  /**
   * Determines the {@link KnowledgeAssetCategory} for a given {@link KnowledgeAssetType}, based on
   * the hierarchies defined in the API4KP ontology.
   *
   * @param formalType the {@link KnowledgeAssetType}
   * @return the broader, parent {@link KnowledgeAssetCategory}
   */
  private KnowledgeAssetCategorySeries inferFormalCategory(KnowledgeAssetType formalType) {
    if (isA(formalType, Clinical_Rule)) {
      return Rules_Policies_And_Guidelines;
    }
    if (isA(formalType, Case_Management_Model) || isA(formalType, Clinical_Case_Management_Model)) {
      return Plans_Processes_Pathways_And_Protocol_Definitions;
    }
    if (isA(formalType, Decision_Model) || isA(formalType, Clinical_Decision_Model)) {
      return Assessment_Predictive_And_Inferential_Models;
    }
    if (isA(formalType, Protocol) || isA(formalType, Care_Process_Model)) {
      return Plans_Processes_Pathways_And_Protocol_Definitions;
    }
    throw new UnsupportedOperationException(
        "Unable to infer category for asset type " + formalType.getName());
  }

  /**
   * Determines whether subType is equivalent to, or subclass of, superType
   *
   * @param subType   the candidate narrower concept
   * @param superType the candidate broader concept
   * @return true if subType is narrower or equal to superType
   */
  private boolean isA(KnowledgeAssetType subType, KnowledgeAssetType superType) {
    return subType.sameTermAs(superType)
        || Arrays.stream(subType.getAncestors()).anyMatch(t -> t.sameTermAs(superType));
  }

  /* ----------------------------------------------------------------------------------------- */


  private Collection<Link> getRelatedServices(TrisotechFileInfo manifest) {
    return mapper.resolveModelToCurrentServiceAssetIds(manifest.getId())
        .map(svc -> new Dependency()
            .withHref(svc
                .withName(svc.getName() + " (API)"))
            .withRel(DependencyTypeSeries.Is_Supplemented_By))
        .collect(toList());
  }

  private List<Link> getOtherDependencies(
      Document woven,
      KnowledgeRepresentationLanguage language) {
    Stream<Attr> assetURIs;
    switch (asEnum(language)) {
      case DMN_1_2:
        assetURIs = asAttributeStream(xPathUtil.xList(woven,
            "//dmn:knowledgeSource[not(./dmn:type='*/*')]/@locationURI"));
        break;
      case CMMN_1_1:
        assetURIs = asAttributeStream(xPathUtil.xList(woven,
            "//cmmn:caseFileItemDefinition[@definitionType='http://www.omg.org/spec/CMMN/DefinitionType/CMISDocument']/@structureRef"));
        break;
      default:
        assetURIs = Stream.empty();
    }

    List<Link> links = assetURIs
        .map(Attr::getValue)
        .filter(Util::isNotEmpty)
        // only supported URIs
        .filter(str -> str.startsWith("urn:")
            || str.startsWith(names.getAssetNamespace().toString())
            || str.startsWith("assets:"))
        .map(this::normalizeQualifiedName)
        .map(URI::create)
        .map(SemanticIdentifier::newVersionId)
        .map(id -> {
          if (id.getVersionId() == null) {
            String separator = id.getResourceId().toString().startsWith("urn") ? ":" : "/";
            return newVersionId(
                URI.create(
                    id.getNamespaceUri().toString() + separator + UUID.fromString(id.getTag())),
                VERSION_ZERO_SNAPSHOT);
          } else {
            return id;
          }
        })
        .map(id -> {
          String label = ofNullable(xPathUtil.xNode(woven,
              "//*[@locationURI='" + id + "' or @structureRef='" + id + "']/@name"))
              .map(Node::getNodeValue)
              .orElse(null);
          return id.withName(label);
        })
        .map(id -> new Dependency().withRel(Depends_On).withHref(id))
        .collect(Collectors.toList());

    Stream<Element> linkElements;
    switch (asEnum(language)) {
      case CMMN_1_1:
        linkElements = asElementStream(xPathUtil.xList(woven,
            "//cmmn:processTask/cmmn:processRefExpression"));
        break;
      default:
        linkElements = Stream.empty();
    }
    linkElements
        .map(n -> n.getTextContent().trim().split(" "))
        .filter(s -> DependencyTypeSeries.resolveTag(s[0]).isPresent())
        .map(s -> new Dependency()
            .withRel(DependencyTypeSeries.resolveTag(s[0]).get())
            .withHref(SemanticIdentifier.newVersionId(URI.create(s[1]))))
        .forEach(links::add);

    return links;
  }

  private String normalizeQualifiedName(String id) {
    if (id.startsWith("assets:")) {
      String str = id.replace("assets:", names.getAssetNamespace().toString());
      if (str.contains(":")) {
        str = str.replace(":", IdentifierConstants.VERSIONS);
      }
      return str;
    } else {
      return id;
    }
  }

  private List<ResourceIdentifier> getArtifactImports(ResourceIdentifier artifactId,
      TrisotechFileInfo model) {
    // if dealing with the latest of the model, return the latest of the imports
    List<ResourceIdentifier> imports;
    if (mapper.isLatest(model.getId(), model.getVersion())) {
      imports = mapper.getLatestArtifactDependencies(artifactId.getResourceId().toString());
    } else {
      imports = getImportVersions(artifactId, model);
    }
    return imports != null ? imports : emptyList();
  }

  private List<ResourceIdentifier> getAssetImports(
      ResourceIdentifier artifactId,
      List<ResourceIdentifier> importedArtifactIds,
      TrisotechFileInfo model) {
    if (mapper.isLatest(model.getId(), model.getVersion())) {
      return mapper.getLatestAssetDependencies(artifactId.getResourceId().toString());
    } else {
      return importedArtifactIds
          .stream()
          .map(aid -> mapper.getAssetIdForHistoricalArtifact(aid))
          .flatMap(StreamUtil::trimStream)
          .collect(Collectors.toList());
    }
  }

  /**
   * Need to get the correct versions of the dependent artifacts for this artifact. It is possible
   * the latest artifact by date may not be the latest artifact by version. Need to get the latest
   * version
   *
   * @param artifactId the id used to query from Trisotech
   * @param model      the latest model information
   * @return the list of ResourceIdentifier for the dependencies
   */
  private List<ResourceIdentifier> getImportVersions(ResourceIdentifier artifactId,
      TrisotechFileInfo model) {
    List<ResourceIdentifier> dependencies = new ArrayList<>();
    // need to find the dependency artifact versions that map to this artifact version
    // using this algorithm:
    // map to the 'latest' version of the dependency that is not timestamped later then the next
    // version of this artifact
    logger.debug(
        "current artifactVersion: {} {} {} ",
        model.getName(), model.getVersion(), model.getUpdated());
    // getTrisotechModelVersions returns all versions of the model EXCEPT the latest
    List<TrisotechFileInfo> artifactModelVersions
        = client.getModelVersions(model.getId());
    // get next version
    TrisotechFileInfo nextArtifactVersion = null;
    Date artifactDate = Date.from(Instant.parse(model.getUpdated()));
    Date nextVersionDate = null;
    for (TrisotechFileInfo tfi : artifactModelVersions) {
      // compare timestamp
      // Need to compare version too? yes, per e-mail exchange w/Davide 04/21
      nextVersionDate = Date.from(Instant.parse(tfi.getUpdated()));
      if (nextVersionDate.after(artifactDate) && tfi.getVersion() != null &&
          (Version.valueOf(tfi.getVersion()).greaterThan(Version.valueOf(model.getVersion())))) {
        nextArtifactVersion = tfi;
        break;
      }
    }
    // the latest version is NOT included in the list of versions;
    // if next is null, it needs to be set to latest
    if (null == nextArtifactVersion) {
      nextArtifactVersion = client.getLatestModelFileInfo(model.getId(), false).orElse(null);
      if (null != nextArtifactVersion) {
        nextVersionDate = Date.from(Instant.parse(nextArtifactVersion.getUpdated()));
        logger.debug("nextArtifactVersion: {} {} {} ", nextArtifactVersion.getName(),
            nextArtifactVersion.getVersion(), nextArtifactVersion.getUpdated());
        logger.debug("nextVersionDate: {}", nextVersionDate);
      }
    }

    // get versions of the imported artifacts
    List<ResourceIdentifier> artifactImports =
        mapper.getLatestArtifactDependencies(artifactId.getResourceId().toString());
    for (ResourceIdentifier ri : artifactImports) {
      logger.debug("have resourceIdentifier from artifactImports: {} ", ri.getVersionId());
      String importedModelID = mapper.resolveModelId(ri.getTag()).orElseThrow();
      List<TrisotechFileInfo> importVersions =
          client.getModelVersions(importedModelID);
      // will need to use convertInternalId to get the KMDP resourceId to return
      // use the tag of the artifact with the version and timestamp found to match
      if (importVersions.isEmpty()) {
        return dependencies;
      }
      TrisotechFileInfo matchVersion = findVersionMatch(importVersions, artifactDate,
          nextVersionDate);
      if (null != matchVersion) { // shouldn't happen
        dependencies.add(names.rewriteInternalId(ri.getTag(),
            matchVersion.getVersion(),
            matchVersion.getUpdated()));
      }
    }
    return dependencies;
  }

  /**
   * When looking for matches for imported versions, need to identify the correct version for THIS
   * version of the artifact. Finding the correct version relies on knowing what the *next* version
   * of this artifact is.
   *
   * @param importVersions  versions of the imported models for this model
   * @param artifactDate    the date for this model
   * @param nextVersionDate the date for the next version of this model
   * @return the TrisotechFileInfo for the version of the import that is correct for THIS version of
   * the model
   */
  private TrisotechFileInfo findVersionMatch(List<TrisotechFileInfo> importVersions,
      Date artifactDate, Date nextVersionDate) {
    // as loop through the dependency versions, need to keep track of the previous one
    // as artifact version will depend on the dependency version that is
    // JUST BEFORE the next version of the artifact
    TrisotechFileInfo prevVersion;
    TrisotechFileInfo thisVersion = null;
    TrisotechFileInfo matchVersion = null;
    for (TrisotechFileInfo tfi : importVersions) {
      prevVersion = thisVersion;
      Date depDate = Date.from(Instant.parse(tfi.getUpdated()));

      if (logger.isDebugEnabled()) {
        logger.debug("version: {}", tfi.getVersion());
        logger.debug("updated: {}", tfi.getUpdated());
        // find the version that is a match for the artifact

        logger.debug("dependency date: {} ", depDate);
        logger.debug("nextVersion compareTo depDate: {}",
            nextVersionDate.compareTo(depDate));
        logger.debug("artifactDate compareTo depDate: {}", artifactDate.compareTo(depDate));
        logger.debug("depDate before artifactDate? {}", depDate.before(artifactDate));
        logger.debug("depDate after artifactDate? {}", depDate.after(artifactDate));
        logger.debug("depDate before nextDate? {}", depDate.before(nextVersionDate));
        logger.debug("depDate after nextDate? {}", depDate.after(nextVersionDate));
      }
      // will need to use convertInternalId to get the KMDP resourceId to return
      if ((depDate.after(artifactDate)) &&
          ((depDate.before(nextVersionDate)) ||
              (depDate.after(nextVersionDate)))) {
        matchVersion = prevVersion;
      }
      if (matchVersion != null) {
        break; // for loop
      }
      thisVersion = tfi;
    }
    if (null != matchVersion) {
      return matchVersion;
    } else {
      return thisVersion;
    }
  }


  /**
   * create the information to be returned identifying the artifacts that are imported by the
   * artifact being processed.
   *
   * @param theTargetArtifactId The list of artifact IDs for the imports to the current version of
   *                            the model being processed
   * @return The Link information for the imports; only keeping the identifier
   */
  private Collection<Link> getRelatedArtifacts(List<ResourceIdentifier> theTargetArtifactId) {
    return theTargetArtifactId.stream()
        // TODO: Do something different for null id? means related artifact was not published
        //  log warning was already noted in gathering of related artifacts CAO
        .filter(Objects::nonNull)
        .map(resourceIdentifier -> new Dependency().withRel(Imports)
            .withHref(resourceIdentifier))
        .collect(toList());

  }

  /**
   * Similar to the relatedArtifacts, related Assets are the asset identifiers for the artifacts
   * that are imported or used (dependency) of the current processed artifact. Want to retain the
   * identifiers.
   *
   * @param theTargetAssetId the list of assets this model depends on
   * @return the link information for the assets; only keeping the identifier
   */
  private Collection<Link> getRelatedAssets(List<ResourceIdentifier> theTargetAssetId) {
    return theTargetAssetId.stream()
        .map(resourceIdentifier ->
            new Dependency()
                .withRel(Imports)
                .withHref(resourceIdentifier))
        .collect(toList());
  }


  private Collection<Link> mergeSortedLinks(Collection<Link> link1, Collection<Link> link2) {
    return Stream.concat(
            link1.stream(),
            link2.stream())
        .sorted(comparing(l -> l.getHref().getTag()))
        .collect(Collectors.toList());
  }

  /* ----------------------------------------------------------------------------------------- */

  /**
   * Converts the Trisotech internal publication status to the platform {@link Publication}
   * <p>
   * Note that the instance data reflects, strictly speaking, the publication status of the model
   * Artifact, not the carried Asset itself
   *
   * @param manifest the internal model manifest.
   * @return a {@link Publication} object, to be used as a {@link KnowledgeAsset} surrogate fragment
   */
  private Publication getArtifactPublicationStatus(TrisotechFileInfo manifest) {
    Publication lifecycle = new Publication();
    if (isNotEmpty(manifest.getState())) {
      switch (manifest.getState()) {
        case "Published":
          lifecycle.withPublicationStatus(Published);
          break;
        case "Draft":
          lifecycle.withPublicationStatus(Draft);
          break;
        case "Pending Approval":
          lifecycle.withPublicationStatus(Final_Draft);
          break;
        default:
          throw new IllegalStateException("Unrecognized state " + manifest.getState());
      }
    } else {
      lifecycle.withPublicationStatus(Unpublished);
    }
    logger.debug("lifecycle = {}", lifecycle.getPublicationStatus());

    return lifecycle;
  }
}