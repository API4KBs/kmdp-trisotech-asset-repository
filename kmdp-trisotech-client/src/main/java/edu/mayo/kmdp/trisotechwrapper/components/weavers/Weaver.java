package edu.mayo.kmdp.trisotechwrapper.components.weavers;

import edu.mayo.kmdp.trisotechwrapper.components.redactors.Redactor;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nonnull;
import org.w3c.dom.Document;

/**
 * Weavers are used to rewrite model fragments, and/or inject fragments from other models. As such,
 * weavers can be used to augment the information in a Model, or normalize some fragments before a
 * {@link Redactor} is used to remove them.
 * <p>
 * The most common use case is to resolve references from one model to another, when the reference
 * does not require to import the entirety of the target model. A weaver would resolve the
 * reference, extract the fragment(s) that carry the minimum amount of necessary information, and
 * inject that information in the source model.
 * <p>
 * Example: resolve a person ID to add a name and an email contact, without pulling or referencing
 * the entire directory; resolve a concept (ID) to its preferred term, definition and parent.
 */
public interface Weaver {


  /**
   * Applies the weaving process to the given document, returning a modified version of that
   * Document. Uses a resolver to gather the additional models (Documents), from which to extract
   * the fragments to be woven into the target Document
   * <p>
   * Implementations of this method may or may not operate on the source Document directly
   * <p>
   * Note: this interface does not specify the context from which to pull the information to be
   * woven, which is
   *
   * @param dox      the original Document
   * @param resolver the URI (string) to Document mapper used to pull referenced models, or
   *                 fragments thereof, containing the fragments to be woven
   * @return the redacted Document
   */
  @Nonnull
  Document weave(
      @Nonnull final Document dox,
      @Nonnull final Function<String, Optional<Document>> resolver);


  /**
   * Applies the weaving process to the given document
   * <p>
   * Assumes that the Documnent is self-contained, with no need to resolve external fragments
   *
   * @param dox the original Document
   * @see #weave(Document, Function)
   */
  @Nonnull
  default Document weave(
      @Nonnull final Document dox) {
    return weave(dox, s -> Optional.empty());
  }

  /**
   * Applies the weaving process to the given document
   *
   * @param dox      the original Document
   * @param resolver the URI (string) to Document mapper used to pull referenced models, or
   *                 fragments thereof, containing the fragments to be woven
   * @see #weave(Document, Function)
   */
  default void applyWeave(
      @Nonnull final Document dox,
      @Nonnull final Function<String, Optional<Document>> resolver) {
    weave(dox, resolver);
  }

}
