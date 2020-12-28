package edu.mayo.kmdp.kdcaci.knew.trisotech;

import edu.mayo.kmdp.kdcaci.knew.trisotech.TTAssetRepositoryConfig.TTWParams;
import edu.mayo.kmdp.util.Util;
import java.net.URI;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NamespaceManager {

  @Autowired(required = false)
  TTAssetRepositoryConfig config;

  private Pattern domainConceptPattern;
  private URI artifactNamespace;
  private URI assetNamespace;

  @PostConstruct
  void init() {
    if (config == null) {
      config = new TTAssetRepositoryConfig();
    }
    domainConceptPattern =
        Pattern.compile(config.getTyped(TTWParams.DOMAIN_TERMS_NAMESPACE_PATTERN));
    artifactNamespace = URI.create(config.getTyped(TTWParams.ARTIFACT_NAMESPACE));
    assetNamespace = URI.create(config.getTyped(TTWParams.ASSET_NAMESPACE));
  }


  public URI getAssetNamespace() {
    return assetNamespace;
  }

  public URI getArtifactNamespace() {
    return artifactNamespace;
  }

  public boolean isDomainConcept(String baseURI) {
    if (Util.isEmpty(baseURI)) {
      return false;
    }
    return domainConceptPattern.matcher(baseURI.toLowerCase()).matches();
  }

  public boolean isDomainConcept(URI baseURI) {
    if (baseURI == null) {
      return false;
    }
    return isDomainConcept(baseURI.toString());
  }
}
