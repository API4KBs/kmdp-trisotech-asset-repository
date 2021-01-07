package edu.mayo.kmdp.kdcaci.knew.trisotech;

import static edu.mayo.kmdp.kdcaci.knew.trisotech.TTConstants.TT_BASE_MODEL_URI;
import static edu.mayo.kmdp.util.DateTimeUtil.parseDateTime;
import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newId;

import edu.mayo.kmdp.kdcaci.knew.trisotech.TTAssetRepositoryConfig.TTWParams;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.util.Util;
import java.net.URI;
import java.util.Date;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NamespaceManager {

  @Autowired(required = false)
  TTAssetRepositoryConfig config;

  private Pattern domainConceptPattern;
  private URI artifactNamespace;
  private URI assetNamespace;

  private String defaultVersion;

  @PostConstruct
  void init() {
    if (config == null) {
      config = new TTAssetRepositoryConfig();
    }
    defaultVersion = config.getTyped(TTWParams.DEFAULT_VERSION_TAG);
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


  /**
   * @param info Internal metadata
   * @return ResourceIdentifier with the KMDP-ified internal id
   */
  public ResourceIdentifier rewriteInternalId(TrisotechFileInfo info) {
    return rewriteInternalId(
        info.getId(),
        info.getVersion(),
        parseDateTime(info.getUpdated())
    );
  }

  public ResourceIdentifier rewriteInternalId(
      String internalId,
      String versionTag,
      String establishedOn) {
    return rewriteInternalId(internalId,versionTag,parseDateTime(establishedOn));
  }

  public ResourceIdentifier rewriteInternalId(
      String internalId,
      String versionTag) {
    return rewriteInternalId(internalId, versionTag, new Date());
  }

  public ResourceIdentifier rewriteInternalId(
      String internalId) {
    return rewriteInternalId(internalId, null);
  }

  /**
   * Need the Trisotech path converted to KMDP path and underscores removed
   *
   * @param internalId the Trisotech internal id for the model
   * @param establishedOn  the date/time of the update time for this version of the model
   * @return ResourceIdentifier with the KMDP-ified internal id
   */
  public ResourceIdentifier rewriteInternalId(
      String internalId,
      String versionTag,
      Date establishedOn) {
    // fast way to determine if internalId is a full URI, or a UUID tag
    String artifactUUID = internalId.charAt(0) == TT_BASE_MODEL_URI.charAt(0)  // == 'h'
        ? internalId.substring(TT_BASE_MODEL_URI.length())
        : internalId;
    String vTag = versionTag != null ? versionTag : defaultVersion;
    Date timestamp = establishedOn != null ? establishedOn : new Date();

    String stampedVersionTag = vTag + "+" + timestamp.getTime();
    return newId(getArtifactNamespace(), artifactUUID, stampedVersionTag)
        .withEstablishedOn(timestamp);
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
