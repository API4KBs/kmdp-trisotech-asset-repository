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

import static java.util.stream.Collectors.joining;

import edu.mayo.kmdp.language.parsers.surrogate.v2.Surrogate2Parser;
import edu.mayo.kmdp.trisotechwrapper.TTAPIAdapter;
import edu.mayo.kmdp.trisotechwrapper.components.SemanticModelInfo;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.util.StreamUtil;
import java.net.URI;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.omg.spec.api4kp._20200801.api.transrepresentation.v4.server.DeserializeApiInternal._applyLower;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

/**
 * MetadataExtractor takes the output of the Weaver and the information of the artifact to create a
 * KnowledgeAsset surrogate.
 */
@Component
public class DefaultMetadataIntrospector implements MetadataIntrospector {

  private static final Logger logger = LoggerFactory.getLogger(DefaultMetadataIntrospector.class);

  public enum AssetCategory {
    DOMAIN_ASSET,
    UNKNOWN, SERVICE_ASSET
  }

  @Autowired
  protected ModelIntrospector strategy;

  @Autowired
  protected TTAPIAdapter client;

  @Autowired
  protected ServiceIntrospector serviceStrategy;

  protected final _applyLower serializer = new Surrogate2Parser();

  public DefaultMetadataIntrospector(ModelIntrospector delegate) {
    this.strategy = delegate;
  }

  @Override
  public Optional<KnowledgeAsset> introspect(
      UUID assetId, Map<SemanticModelInfo, Optional<Document>> carriers) {

    var resolveds = carriers.keySet().stream()
        .map(meta -> categorizeAsset(assetId, meta.getId()))
        .flatMap(StreamUtil::trimStream)
        .collect(Collectors.toSet());

    if (resolveds.size() != 1) {
      if (logger.isErrorEnabled() && ! resolveds.isEmpty()) {
        logger.error("Carrier set is inconsistent : {}",
            resolveds.stream().map(Objects::toString).collect(joining(",")));
      }
      return Optional.empty();
    }
    var resolved = resolveds.iterator().next();

    var doxMap = carriers.entrySet().stream()
        .filter(e -> e.getValue().isPresent())
        .collect(Collectors.toMap(
            Entry::getKey,
            e -> e.getValue().get()
        ));

    switch (resolved.category) {
      case DOMAIN_ASSET:
        return introspectAsModel(resolved.assetId, doxMap);
      case SERVICE_ASSET:
        return introspectAsService(resolved.assetId, doxMap);
      case UNKNOWN:
      default:
        return Optional.empty();
    }
  }


  @Override
  public Optional<KnowledgeAsset> introspectAsService(ResourceIdentifier assetId, SemanticModelInfo manifest,
      Document carrier) {
    return Optional.of(
        serviceStrategy.extractSurrogateFromDocument(carrier, manifest, assetId));
  }

  protected Optional<KnowledgeAsset> introspectAsService(ResourceIdentifier assetId,
      Map<SemanticModelInfo, Document> doxMap) {
    if (doxMap.size() > 1) {
      if (DefaultMetadataIntrospector.logger.isErrorEnabled()) {
        DefaultMetadataIntrospector.logger.error(
            "Unable to support Service Asset {} distributed across multiple models [{}]",
            assetId,
            doxMap.keySet().stream()
                .map(TrisotechFileInfo::getId).collect(joining(",")));
      }
      return Optional.empty();
    }
    if (doxMap.isEmpty()) {
      return Optional.empty();
    }
    var entry = doxMap.entrySet().iterator().next();
    return introspectAsService(assetId, entry.getKey(), entry.getValue());
  }

  @Override
  public Optional<KnowledgeAsset> introspectAsModel(ResourceIdentifier assetId,
      Map<SemanticModelInfo, Document> doxMap) {
    return Optional.of(
        strategy.extractSurrogateFromDocument(doxMap, assetId));
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
  private Optional<ResolvedAssetId> categorizeAsset(UUID assetId, String modelId) {
    var idStr = assetId.toString();

    var isModel = client.getMetadataByModelId(modelId)
        .map(SemanticModelInfo::getAssetId)
        .filter(id -> id.contains(idStr))
        .map(id -> SemanticIdentifier.newVersionId(URI.create(id)));

    if (isModel.isPresent()) {
      return Optional.of(new ResolvedAssetId(AssetCategory.DOMAIN_ASSET, isModel.get()));
    }

    var isService = client.getServicesMetadataByModelId(modelId)
        .map(SemanticModelInfo::getServiceId)
        .filter(id -> id != null && id.contains(idStr))
        .findFirst()
        .map(id -> SemanticIdentifier.newVersionId(URI.create(id)));

    return isService.map(
            resourceIdentifier ->
                new ResolvedAssetId(AssetCategory.SERVICE_ASSET, resourceIdentifier));
  }




  protected static final class ResolvedAssetId {

    final AssetCategory category;
    final ResourceIdentifier assetId;

    public ResolvedAssetId(AssetCategory assetCategory, ResourceIdentifier assetId) {
      this.category = assetCategory;
      this.assetId = assetId;
    }

    @Override
    public String toString() {
      return "{" + category + " - " + assetId.asKey() + '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      ResolvedAssetId that = (ResolvedAssetId) o;
      return category == that.category && assetId.asKey().equals(that.assetId.asKey());
    }

    @Override
    public int hashCode() {
      return Objects.hash(category, assetId.asKey());
    }
  }

}
