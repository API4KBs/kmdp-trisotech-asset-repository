package edu.mayo.kmdp.trisotechwrapper.components.redactors;

import edu.mayo.kmdp.trisotechwrapper.components.weavers.Weaver;
import javax.annotation.Nonnull;
import org.w3c.dom.Document;

/**
 * Redactors are used to remove vendor-specific model elements
 * <p>
 * Redactors should produce Models that are as much as possible standards compliant. Guidelines are
 * based on the notion of Relevance (would a client care about the element?), Necessity (would the
 * model interpretation be 'corrupted' without the element?) and Mappability (is there a
 * standards-based equivalent for the element?).
 * <p>
 * Redactors are used in conjunction with {@link Weaver}s, which can rewrite the fragments,
 * preserving the information, before the Redactor removes the non-standard elements
 *
 * <p>
 * Given a Proprietary fragment (element/attribute):
 * <ul>
 *   <li>not Relevant: Should Remove</li>
 *   <li>Relevant and Necessary and Mappable : Must Rewrite, then Remove</li>
 *   <li>Relevant and Necessary and not Mappable : Must Encapsulate into Extensions</li>
 *   <li>Relevant and not Necessary and Mappable : Should/Could Rewrite, then Remove</li>
 *   <li>Relevant and not Necessary and not Mappable : Should Remove</li>
 * </ul>
 */
public interface Redactor {

  /**
   * Applies the redaction rules to the given document, returning a redacted version of that
   * Document
   * <p>
   * Implementations of this method may or may not operate on the source Document directly
   *
   * @param dox the original Document
   * @return the redacted Document
   */
  @Nonnull
  Document redact(@Nonnull final Document dox);

  /**
   * Applies the redaction rules to the given document
   *
   * @param dox the original Document
   */
  default void applyRedact(@Nonnull final Document dox) {
    redact(dox);
  }
}
