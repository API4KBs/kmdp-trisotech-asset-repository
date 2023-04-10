package edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors;

import static edu.mayo.kmdp.trisotechwrapper.config.TTConstants.KEY;
import static edu.mayo.kmdp.trisotechwrapper.config.TTConstants.TT_CUSTOM_ATTRIBUTE_ATTR;
import static edu.mayo.kmdp.trisotechwrapper.config.TTConstants.TT_METADATA_NS;
import static edu.mayo.kmdp.trisotechwrapper.config.TTConstants.VALUE;
import static edu.mayo.kmdp.util.XMLUtil.asElementStream;

import edu.mayo.kmdp.trisotechwrapper.components.weavers.Weaver;
import edu.mayo.kmdp.util.XPathUtil;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Helper class with functions to manipulate Models as XML Documents
 */
public class DocumentHelper {

  private DocumentHelper() {
    // functions only
  }

  /**
   * Get the customAttribute assetID from the document. This is a value that is stripped from a
   * woven file but can be retrieved from an unwoven file.
   *
   * @param dox the model XML document
   * @return ResourceIdenifier of the assetID
   */
  public static Optional<ResourceIdentifier> extractAssetIdFromDocument(
      Document dox, String assetIdAttribute) {
    return extractAssetIdFromRawDocument(dox, assetIdAttribute)
        .or(() -> extractAssetIdFromWovenDocument(dox));
  }

  /**
   * Gets the Asset ID from the annotation/extension in a {@link Document}, supposed to be a
   * manifestation of the Asset whose ID is extracted.
   * <p>
   * This method is applied before a {@link Weaver}: it looks for the Trisotech native custom
   * attribute specified by the modelers
   *
   * @param dox              the Model's Document
   * @param assetIdAttribute the name of the custom attribute used for the Asset ID value
   * @return the Asset ID, parsed as a {@link ResourceIdentifier}
   */
  @Nonnull
  private static Optional<ResourceIdentifier> extractAssetIdFromRawDocument(
      @Nonnull final Document dox,
      @Nonnull final String assetIdAttribute) {
    NodeList metas = dox.getElementsByTagNameNS(TT_METADATA_NS, TT_CUSTOM_ATTRIBUTE_ATTR);

    List<ResourceIdentifier> ids = asElementStream(metas)
        .filter(el -> assetIdAttribute.equals(el.getAttribute(KEY)))
        .map(el -> el.getAttribute(VALUE))
        .map(id -> SemanticIdentifier.newVersionId(URI.create(id))
            .withName(getName(dox)))
        .collect(Collectors.toList());

    return ids.isEmpty() ? Optional.empty() : Optional.ofNullable(ids.get(0));
  }

  /**
   * Gets the Asset ID from the annotation/extension in a {@link Document}, supposed to be a
   * manifestation of the Asset whose ID is extracted.
   * <p>
   * This method is applied after a {@link Weaver}: it looks for an extension that contains a
   * {@link ResourceIdentifier}
   *
   * @param dox the Model's Document
   * @return the Asset ID, parsed as a {@link ResourceIdentifier}
   */
  @Nonnull
  private static Optional<ResourceIdentifier> extractAssetIdFromWovenDocument(
      @Nonnull final Document dox) {
    XPathUtil xPath = new XPathUtil();
    String idNode = xPath.xString(dox, "//*[local-name()='resourceIdentifier']/@versionId");
    return Optional.ofNullable(idNode)
        .map(URI::create)
        .map(SemanticIdentifier::newVersionId)
        .map(id -> id.withName(getName(dox)));
  }

  /**
   * Gets the name of a Model, as contained in an attribute 'name' of the root element
   *
   * @param dox a Model Document
   * @return the Model's name
   */
  private static String getName(
      @Nonnull final Document dox) {
    return dox.getDocumentElement().getAttribute("name");
  }

}
