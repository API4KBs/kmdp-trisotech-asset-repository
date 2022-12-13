package edu.mayo.kmdp.kdcaci.knew.trisotech;

import edu.mayo.kmdp.ConfigProperties;
import edu.mayo.kmdp.Opt;
import edu.mayo.kmdp.Option;
import edu.mayo.kmdp.kdcaci.knew.trisotech.TTAssetRepositoryConfig.TTWParams;
import edu.mayo.kmdp.registry.Registry;
import edu.mayo.kmdp.util.PropertiesUtil;
import java.util.Properties;
import org.omg.spec.api4kp._20200801.id.IdentifierConstants;

public class TTAssetRepositoryConfig extends
    ConfigProperties<TTAssetRepositoryConfig, TTWParams> {

  private static final String PARAM_NAMESPACE = "edu.mayo.kmdp.kdcaci.knew.trisotech.";

  private static final Properties DEFAULTS = defaulted(TTWParams.class);

  public TTAssetRepositoryConfig() {
    super(DEFAULTS);
  }

  public TTAssetRepositoryConfig(Properties defaults) {
    super(defaults);
  }

  @Override
  public TTWParams[] properties() {
    return TTWParams.values();
  }

  public String encode() {
    return PropertiesUtil.serializeProps(this);
  }


  public enum TTWParams implements
      Option<TTWParams> {

    PUBLISHED_ONLY(Opt.of(
        PARAM_NAMESPACE + "publishedModelsOnly",
        Boolean.TRUE.toString(),
        "If true, will not expose model (versions) unless they have a publication status",
        Boolean.class,
        true)),
    ASSETS_ONLY(Opt.of(
        PARAM_NAMESPACE + "assetsOnly",
        Boolean.TRUE.toString(),
        "If true, will not expose models unless their latest version is the carrier of a"
            + " named enterprise Knowledge Asset",
        Boolean.class,
        true)),

    ASSET_ID_ATTRIBUTE(Opt.of(
        PARAM_NAMESPACE + "assetIdAttributeName",
        "knowledgeAssetId",
        "Custom attribute used to assert a model's Asset ID",
        String.class,
        false)),

    SERVICE_ASSET_ID_ATTRIBUTE(Opt.of(
        PARAM_NAMESPACE + "serviceAssetIdAttributeName",
        "serviceAssetId",
        "Custom attribute used to assert a Service (Spec) Asset ID",
        String.class,
        false)),

    DOMAIN_TERMS_NAMESPACE_PATTERN(Opt.of(
        PARAM_NAMESPACE + "domainTermsNamespacePattern",
        "(.*\\/api4kp.*|.*\\/clinicaltasks.*|.*\\/clinicalsituations.*|.*\\/propositionalconcepts.*)",
        "Namespace URI patterns for domain concepts",
        String.class,
        false)),

    ASSET_NAMESPACE(Opt.of(
        PARAM_NAMESPACE + "assetNamespace",
        Registry.MAYO_ASSETS_BASE_URI,
        "Base namespace for Knowledge Assets",
        String.class,
        false)),

    ARTIFACT_NAMESPACE(Opt.of(
        PARAM_NAMESPACE + "artifactNamespace",
        Registry.MAYO_ARTIFACTS_BASE_URI,
        "Base namespace for Knowledge Artifacts (models)",
        String.class,
        false)),

    DEFAULT_VERSION_TAG(Opt.of(
        PARAM_NAMESPACE + "defaultVersionTag",
        IdentifierConstants.VERSION_ZERO,
        "",
        String.class,
        false)),
    ;

    private Opt<TTWParams> opt;

    TTWParams(Opt<TTWParams> opt) {
      this.opt = opt;
    }

    @Override
    public Opt<TTWParams> getOption() {
      return opt;
    }

  }
}