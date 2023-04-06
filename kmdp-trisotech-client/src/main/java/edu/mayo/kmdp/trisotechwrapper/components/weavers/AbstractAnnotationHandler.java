package edu.mayo.kmdp.trisotechwrapper.components.weavers;

import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

/**
 * Abstract class.
 * <p>
 * Subclasses are expected to handle TT "Annotations", asserted via semanticLinks to TT
 * accelerators, rewriting the native XML elements according to a standards-based representation
 *
 * @param <T> The type of the target annotation class, such that the TT annotations will be
 *            rewritten to
 */
public abstract class AbstractAnnotationHandler<T> {

  /**
   * Logger
   */
  private static final Logger logger = LoggerFactory.getLogger(AbstractAnnotationHandler.class);

  /**
   * Creates a JAXB Marshaller, so that the rewritten annotations can be serialized into XML and
   * woven into the source model DOM tree
   *
   * @param klass A seed class from the java package that contains the target annotation classes
   * @return a Marshaller that can serialize instances of the target annotation class
   */
  @Nullable
  protected static <X> Marshaller initMarshaller(
      @Nonnull final Class<X> klass) {
    try {
      return JAXBContext.newInstance(klass).createMarshaller();
    } catch (JAXBException e) {
      logger.error(e.getMessage(), e);
      return null;
    }
  }

  /**
   * Serializes an object, adding the resulting element as a child of a given node
   *
   * @param obj    the object to be serialized
   * @param mapper a JaxB element converter
   * @param parent the parent node to attach the subtree to
   * @param jxm    the Marshaller used to serialize obj
   */
  protected void toChildElement(
      @Nonnull final T obj,
      @Nonnull final Function<T, JAXBElement<T>> mapper,
      @Nonnull final Element parent,
      @Nonnull final Marshaller jxm) {
    try {
      jxm.marshal(mapper.apply(obj), parent);
    } catch (JAXBException e) {
      logger.error(e.getMessage(), e);
    }
  }

}
