package edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors;

import static edu.mayo.kmdp.trisotechwrapper.config.TTConstants.KEY;
import static edu.mayo.kmdp.trisotechwrapper.config.TTConstants.TT_CUSTOM_ATTRIBUTE_ATTR;
import static edu.mayo.kmdp.trisotechwrapper.config.TTConstants.TT_METADATA_NS;
import static edu.mayo.kmdp.trisotechwrapper.config.TTConstants.VALUE;
import static edu.mayo.kmdp.util.XMLUtil.asElementStream;

import edu.mayo.kmdp.util.XPathUtil;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class DocumentHelper {

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

  private static Optional<ResourceIdentifier> extractAssetIdFromRawDocument(
      Document dox,
      String assetIdAttribute) {
    NodeList metas = dox.getElementsByTagNameNS(TT_METADATA_NS, TT_CUSTOM_ATTRIBUTE_ATTR);

    List<ResourceIdentifier> ids = asElementStream(metas)
        .filter(el -> isIdentifier(el, assetIdAttribute))
        .map(el -> el.getAttribute(VALUE))
        .map(id -> SemanticIdentifier.newVersionId(URI.create(id))
            .withName(getName(dox)))
        .collect(Collectors.toList());

    return ids.isEmpty() ? Optional.empty() : Optional.ofNullable(ids.get(0));
  }

  private static Optional<ResourceIdentifier> extractAssetIdFromWovenDocument(Document dox) {
    XPathUtil xPath = new XPathUtil();
    String idNode = xPath.xString(dox, "//*[local-name()='resourceIdentifier']/@versionId");
    return Optional.ofNullable(idNode)
        .map(URI::create)
        .map(SemanticIdentifier::newVersionId)
        .map(id -> id.withName(getName(dox)));
  }

  private static String getName(Document dox) {
    return dox.getDocumentElement().getAttribute("name");
  }

  private static boolean isIdentifier(Element el, String assetIdAttribute) {
    return assetIdAttribute.equals(el.getAttribute(KEY));
  }


}
