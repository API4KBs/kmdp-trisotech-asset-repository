package edu.mayo.kmdp.trisotechwrapper.components.operators;

import edu.mayo.kmdp.util.XPathUtil;
import java.net.URI;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.w3c.dom.Document;


/**
 * Helper class to Select nodes of an MVF Document
 */
public final class KEMSelectorHelper {

  private static final XPathUtil xPath = new XPathUtil();
  private static final String CONCEPT_REF_PATH = "//mvf:entry[mvf:reference = '%s']/mvf:uri/text()";

  private KEMSelectorHelper() {
    // functions only
  }

  /**
   * Selects the URI of the MVFEntry in a given MFV Document, where the Entry was derived, and
   * references, a given KEM Concept (URI)
   *
   * @param dox    the MVF Document
   * @param kemURI the URI of the KEM Concept in the original KEM moodel
   * @return the URI of the MVFEntry
   */
  public static Optional<URI> lookupMVFEntryURI(
      @Nonnull final Document dox,
      @Nonnull final URI kemURI) {
    var cid = xPath.xString(dox, String.format(CONCEPT_REF_PATH, kemURI));
    return Optional.of(URI.create(cid));
  }

}
