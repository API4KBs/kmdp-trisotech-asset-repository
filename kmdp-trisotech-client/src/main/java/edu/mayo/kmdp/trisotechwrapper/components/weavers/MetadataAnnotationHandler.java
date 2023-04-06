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
package edu.mayo.kmdp.trisotechwrapper.components.weavers;


import edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelType;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.bind.Marshaller;
import org.omg.spec.api4kp._20200801.id.ConceptIdentifier;
import org.omg.spec.api4kp._20200801.surrogate.Annotation;
import org.omg.spec.api4kp._20200801.surrogate.ObjectFactory;
import org.w3c.dom.Element;


/**
 * Annotation handler that rewrites Annotations with Domain Semantics
 */
public class MetadataAnnotationHandler extends AbstractAnnotationHandler<Annotation> {

  protected static final ObjectFactory of;
  protected static Marshaller jxm;

  static {
    of = new ObjectFactory();
    jxm = initMarshaller(Annotation.class);
  }

  /**
   * Replaces a custom attribute extension carrying a Semantic Annotation - the combination of a
   * referred concept plus an optionally implied relationship - with an element resulting from the
   * XML serialization of a {@link Annotation}
   * <p>
   *
   * @param el         the TT custom attribute element holding the original semantic information
   * @param defaultRel the default property linking the model element and the referred concept
   * @param concept    the referred concept
   */
  public void replaceProprietaryElement(
      @Nonnull final Element el,
      @Nullable final SemanticAnnotationRelType defaultRel,
      @Nullable final ConceptIdentifier concept) {
    var parent = (Element) el.getParentNode();
    parent.removeChild(el);

    if (jxm != null && concept != null) {
      var ann = getAnnotation(defaultRel, concept);
      toChildElement(ann, of::createAnnotation, parent, jxm);
    }
  }

  /**
   * Construct an Annotation, given an optional relationship and a referent concept
   *
   * @param defaultRel the relationship
   * @param concept    the concept
   * @return an Annotation (rel,concept)
   */
  @Nonnull
  protected Annotation getAnnotation(
      @Nullable final SemanticAnnotationRelType defaultRel,
      @Nonnull final ConceptIdentifier concept) {
    ConceptIdentifier rel = defaultRel != null
        ? defaultRel.asConceptIdentifier()
        : null;
    return new Annotation()
        .withRel(rel)
        .withRef(concept);
  }


}
