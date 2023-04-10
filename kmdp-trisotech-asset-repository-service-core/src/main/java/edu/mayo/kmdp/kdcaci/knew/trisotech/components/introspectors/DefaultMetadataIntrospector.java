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

import edu.mayo.kmdp.trisotechwrapper.TTAPIAdapter;
import edu.mayo.kmdp.trisotechwrapper.components.NamespaceManager;
import edu.mayo.kmdp.trisotechwrapper.components.SemanticModelInfo;
import edu.mayo.kmdp.trisotechwrapper.config.TTWEnvironmentConfiguration;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.util.StreamUtil;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.repository.asset.KARSHrefBuilder;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 * Default implementation of {@link MetadataIntrospector}
 */
public class DefaultMetadataIntrospector implements MetadataIntrospector {

  /**
   * Logger
   */
  private static final Logger logger = LoggerFactory.getLogger(DefaultMetadataIntrospector.class);

  /**
   * Category of Asset to extract metadata for
   */
  public enum AssetCategory {
    DOMAIN_ASSET,
    UNKNOWN,
    SERVICE_ASSET
  }

  /**
   * The delegate introspector used for Model Assets
   */
  @Nonnull
  protected final ModelIntrospector modelDelegate;
  /**
   * The delegate introspector used for Service Assets
   */
  @Nonnull
  protected final ServiceIntrospector serviceDelegate;

  /**
   * The Trisotech adapter
   */
  @Nonnull
  protected final TTAPIAdapter client;


  public DefaultMetadataIntrospector(
      @Nonnull TTWEnvironmentConfiguration cfg,
      @Nonnull TTAPIAdapter client,
      @Nonnull NamespaceManager names,
      @Nullable KARSHrefBuilder hrefBuilder) {
    this.client = client;
    this.modelDelegate = new BPMModelIntrospector(cfg, names, client, hrefBuilder);
    this.serviceDelegate = new BPMServiceIntrospector(cfg, names, client);
  }


  @Override
  @Nonnull
  public Optional<KnowledgeAsset> introspect(
      @Nonnull final ResourceIdentifier assetId,
      @Nonnull final Map<SemanticModelInfo, Optional<Document>> carriers) {

    var resolveds = carriers.keySet().stream()
        .map(meta -> categorizeAsset(assetId, meta.getId()))
        .flatMap(StreamUtil::trimStream)
        .collect(Collectors.toSet());

    if (resolveds.size() != 1) {
      if (logger.isErrorEnabled() && !resolveds.isEmpty()) {
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
        return modelDelegate.introspectAsModel(resolved.assetId, doxMap);
      case SERVICE_ASSET:
        return introspectAsService(resolved.assetId, doxMap);
      case UNKNOWN:
      default:
        return Optional.empty();
    }
  }


  /**
   * Bridge method
   * <p>
   * As of version 6.0.0, Service Assets can be carried by one and one model. Before the
   * {@link ServiceIntrospector} is invoked, ensures that the carriers Map contains one and only one
   * entry, using that entry to invoke the delegate
   *
   * @param assetId  the Asset ID
   * @param carriers the Manifest/Model map, expected to contain only one entry
   * @return the Surrogate returned by the {@link ServiceIntrospector} delegate
   */
  @Nonnull
  protected Optional<KnowledgeAsset> introspectAsService(
      @Nonnull final ResourceIdentifier assetId,
      @Nonnull final Map<SemanticModelInfo, Document> carriers) {
    if (carriers.size() > 1) {
      if (DefaultMetadataIntrospector.logger.isErrorEnabled()) {
        DefaultMetadataIntrospector.logger.error(
            "Unable to support Service Asset {} distributed across multiple models [{}]",
            assetId,
            carriers.keySet().stream()
                .map(TrisotechFileInfo::getId).collect(joining(",")));
      }
      return Optional.empty();
    }
    if (carriers.isEmpty()) {
      return Optional.empty();
    }
    var entry = carriers.entrySet().iterator().next();
    return serviceDelegate.introspectAsService(assetId, entry.getKey(), entry.getValue());
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
   * @param modelUri the id of the model which scopes and categorizes the assetId
   * @return the category, if able to determine.
   */
  protected Optional<ResolvedAssetId> categorizeAsset(
      @Nonnull final ResourceIdentifier assetId,
      @Nonnull final String modelUri) {

    var isModel = client.getMetadataByModelId(modelUri)
        .map(SemanticModelInfo::getAssetKey)
        .filter(id -> Objects.equals(id.getUuid(), assetId.getUuid()))
        .isPresent();

    if (isModel) {
      return Optional.of(new ResolvedAssetId(AssetCategory.DOMAIN_ASSET, assetId));
    }

    var isService = client.getServicesMetadataByModelId(modelUri)
        .map(SemanticModelInfo::getServiceKey)
        .anyMatch(id -> Objects.equals(id.getUuid(), assetId.getUuid()));

    return isService
        ? Optional.of(new ResolvedAssetId(AssetCategory.SERVICE_ASSET, assetId))
        : Optional.empty();
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
