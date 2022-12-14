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

import static edu.mayo.kmdp.trisotechwrapper.components.TTGraphTerms.ASSET_ID;
import static edu.mayo.kmdp.util.XMLUtil.loadXMLDocument;
import static java.util.Optional.ofNullable;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Encoded_Knowledge_Expression;

import edu.mayo.kmdp.language.parsers.surrogate.v2.Surrogate2Parser;
import edu.mayo.kmdp.trisotechwrapper.TrisotechWrapper;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.util.JSonUtil;
import java.io.InputStream;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DeserializeApiInternal._applyLower;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.SurrogateHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

/**
 * MetadataExtractor takes the output of the Weaver and the information of the artifact to create a
 * KnowledgeAsset surrogate.
 */
@Component
public class MetadataIntrospector {

  public enum AssetCategory {
    DOMAIN_ASSET,
    UNKNOWN, SERVICE_ASSET
  }

  @Autowired
  private TrisotechIntrospectionStrategy strategy;

  @Autowired
  private TrisotechWrapper client;

  @Autowired
  private TrisotechServiceIntrospectionStrategy serviceStrategy;

  private final _applyLower serializer = new Surrogate2Parser();

  public MetadataIntrospector(TrisotechIntrospectionStrategy delegate) {
    this.strategy = delegate;
  }

  public Optional<KnowledgeAsset> extract(UUID assetId, Document dox, TrisotechFileInfo meta) {
    var resolved = categorizeAsset(assetId, meta.getId());

    switch (resolved.category) {
      case DOMAIN_ASSET:
        return Optional.of(
            strategy.extractSurrogateFromDocument(dox, meta, resolved.assetId));
      case SERVICE_ASSET:
        return Optional.of(
            serviceStrategy.extractSurrogateFromDocument(dox, meta, resolved.assetId));
      case UNKNOWN:
      default:
        return Optional.empty();
    }
  }


  /**
   * Determines whether the given assetId refers to the model's domain knowledge asset, or to a
   * service asset defined/referenced by the model, so that the appropriate Surrogate can be built.
   * <p>
   * Note that the assetId will only be categorized within the scope of the given model: passing an
   * assetId that pertains to a different (version of the) model will not be treated differently
   * than providing an assetId that does not correspond to any model.
   *
   * @param assetId the assetId to be categorized
   * @param modelId the id of the model which scopes and categorizes the assetId
   * @return the category, if able to determine.
   */
  private ResolvedAssetId categorizeAsset(UUID assetId, String modelId) {
    var idStr = assetId.toString();

    var isModel = ofNullable(client.getMetadataByModel(modelId).get(ASSET_ID))
        .filter(id -> id.contains(idStr))
        .map(id -> SemanticIdentifier.newVersionId(URI.create(id)));

    if (isModel.isPresent()) {
      return new ResolvedAssetId(AssetCategory.DOMAIN_ASSET, isModel.get());
    }

    var isService = client.getServiceMetadataByModel(modelId)
        .map(m -> m.get(ASSET_ID))
        .filter(id -> id != null && id.contains(idStr))
        .findFirst()
        .map(id -> SemanticIdentifier.newVersionId(URI.create(id)));

    return isService.map(
            resourceIdentifier ->
                new ResolvedAssetId(AssetCategory.SERVICE_ASSET, resourceIdentifier))
        .orElseGet(() -> new ResolvedAssetId(AssetCategory.UNKNOWN, null));
  }


  /**
   * Test method
   *
   * @param dox  the Artifact
   * @param meta the Artifact manifest
   * @return the metadata surrogate for the Model
   */
  public Optional<KnowledgeAsset> extract(Document dox, TrisotechFileInfo meta) {
    return Optional.of(strategy.extractSurrogateFromDocument(dox, meta));
  }

  /**
   * Test method
   *
   * @param resource the Artifact, from an InputStream
   * @param meta     the Artifact manifest, from an InputStream
   * @return the metadata surrogate for the Model
   */
  public Optional<KnowledgeAsset> extract(
      InputStream resource, InputStream meta) {
    Optional<Document> dox = loadXMLDocument(resource);
    TrisotechFileInfo info = ofNullable(meta)
        .flatMap(json -> JSonUtil.readJson(json, TrisotechFileInfo.class))
        .orElseGet(TrisotechFileInfo::new);

    return dox
        .flatMap(document -> extract(document, info));
  }

  /**
   * Test method
   *
   * @param resource the Artifact, from an InputStream
   * @param meta     the Artifact manifest, from an InputStream
   * @param codedRep the extended MIME type that specifies how to serialize/encode the Surrogate
   * @return the metadata surrogate for the Model
   */
  public Optional<byte[]> extractBinary(
      InputStream resource, InputStream meta, String codedRep) {
    Optional<KnowledgeCarrier> kc = extract(resource, meta)
        .map(SurrogateHelper::carry);

    return kc.map(
        ast -> serializer.applyLower(
                ast,
                Encoded_Knowledge_Expression,
                codedRep,
                null)
            .flatOpt(AbstractCarrier::asBinary)
            .orElseGet(() -> new byte[0])
    );
  }


  private static final class ResolvedAssetId {

    final AssetCategory category;
    final ResourceIdentifier assetId;

    public ResolvedAssetId(AssetCategory assetCategory, ResourceIdentifier assetId) {
      this.category = assetCategory;
      this.assetId = assetId;
    }
  }

}
