package edu.mayo.kmdp.trisotechwrapper.config;

import edu.mayo.kmdp.Opt;
import edu.mayo.kmdp.Option;
import edu.mayo.kmdp.registry.Registry;
import java.net.URI;
import org.omg.spec.api4kp._20200801.id.IdentifierConstants;

/**
 * Configuration Parameters Definitions, for use with a {@link TTWEnvironmentConfiguration}
 */
public enum TTWConfigParamsDef implements
    Option<TTWConfigParamsDef> {

  PUBLISHED_ONLY_FLAG(Opt.of(
      "edu.mayo.kmdp.application.flag.publishedOnly",
      Boolean.TRUE.toString(),
      "If true, will not expose model (versions) unless they have a publication status",
      Boolean.class,
      true)),
  ANONYMOUS_ASSETS_FLAG(Opt.of(
      "edu.mayo.kmdp.application.flag.allowAnonymous",
      Boolean.TRUE.toString(),
      "If true, allows models without an explicit Asset Id to be treated as Assets",
      Boolean.class,
      true)),


  ASSET_ID_ATTRIBUTE(Opt.of(
      "edu.mayo.kmdp.trisotechwrapper.assetIdAttributeName",
      "knowledgeAssetId",
      "Custom attribute used to assert a model's Asset ID",
      String.class,
      false)),

  SERVICE_ASSET_ID_ATTRIBUTE(Opt.of(
      "edu.mayo.kmdp.trisotechwrapper.serviceAssetIdAttributeName",
      "serviceAssetId",
      "Custom attribute used to assert a Service (Spec) Asset ID",
      String.class,
      false)),

  ASSET_NAMESPACE(Opt.of(
      "edu.mayo.kmdp.trisotechwrapper.assetNamespace",
      Registry.MAYO_ASSETS_BASE_URI,
      "Base namespace for Knowledge Assets",
      URI.class,
      false)),

  ARTIFACT_NAMESPACE(Opt.of(
      "edu.mayo.kmdp.trisotechwrapper.artifactNamespace",
      Registry.MAYO_ARTIFACTS_BASE_URI,
      "Base namespace for Knowledge Artifacts (models)",
      URI.class,
      false)),


  API_ENDPOINT(Opt.of(
      "edu.mayo.kmdp.trisotechwrapper.apiUrl",
      null,
      "The URL where the TT public APIs are exposed",
      URI.class,
      true)),

  SERVICE_LIBRARY_ENVIRONMENT(Opt.of(
      "edu.mayo.kmdp.trisotechwrapper.executionEnv",
      null,
      "A comma-separated value of names of the Execution Environment with deployed services",
      String.class,
      true)),

  BASE_URL(Opt.of(
      "edu.mayo.kmdp.trisotechwrapper.baseUrl",
      null,
      "The URL where the TT DES is exposed",
      String.class,
      true)),

  API_TOKEN(Opt.of(
      "edu.mayo.kmdp.trisotechwrapper.trisotechToken",
      null,
      "The API Token used to interact with the DES public API",
      String.class,
      true)),

  REPOSITORY_ID(Opt.of(
      "edu.mayo.kmdp.trisotechwrapper.repositoryId",
      null,
      "The UUID of the target Place",
      String.class,
      true)),

  REPOSITORY_NAME(Opt.of(
      "edu.mayo.kmdp.trisotechwrapper.repositoryName",
      null,
      "The Name of the target Place",
      String.class,
      true)),

  REPOSITORY_PATH(Opt.of(
      "edu.mayo.kmdp.trisotechwrapper.repositoryPath",
      "/",
      "The Path within the target Place",
      String.class,
      false)),

  REPOSITORY_PATHS(Opt.of(
      "edu.mayo.kmdp.trisotechwrapper.place.paths",
      null,
      "A comma separated list of (GUID/Path) to be indexed",
      String.class,
      true)),


  CACHE_EXPIRATION(Opt.of(
      "edu.mayo.kmdp.trisotechwrapper.expiration",
      "1440",
      "How long the cache is retained before becoming stale, in minutes",
      Long.class,
      false)),

  DEFAULT_VERSION_TAG(Opt.of(
      "edu.mayo.kmdp.trisotechwrapper.defaultVersionTag",
      IdentifierConstants.VERSION_ZERO,
      "",
      String.class,
      false)),

  DOMAIN_TERMS_NAMESPACE_PATTERN(Opt.of(
      "edu.mayo.kmdp.trisotechwrapper.domainTermsNamespacePattern",
      "(.*\\/api4kp.*|.*\\/clinicaltasks.*|.*\\/clinicalsituations.*|.*\\/propositionalconcepts.*)",
      "Namespace URI patterns for domain concepts",
      String.class,
      false)),

  HEALTHCARE_ONTOLOGY(Opt.of(
      "edu.mayo.kmdp.application.flag.healthOntology",
      "true",
      "Enables KEM support for healthcare ontologies (UMLS, SNOMED CT, LOINC, etc.)",
      Boolean.class,
      false)),

  ;

  private final Opt<TTWConfigParamsDef> opt;

  TTWConfigParamsDef(Opt<TTWConfigParamsDef> opt) {
    this.opt = opt;
  }

  @Override
  public Opt<TTWConfigParamsDef> getOption() {
    return opt;
  }

}