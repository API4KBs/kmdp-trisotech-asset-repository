package edu.mayo.kmdp.kdcaci.knew.trisotech.components.weavers;

import edu.mayo.kmdp.util.JaxbUtil;
import edu.mayo.kmdp.util.XMLUtil;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import javax.xml.bind.JAXBElement;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public abstract class AbstractAnnotationHandler {

  protected void swap( Element oldEl, List<Element> newEl ) {
    Node parent = oldEl.getParentNode();
    parent.removeChild( oldEl );
    newEl.forEach( parent::appendChild );
  }

  public List<Element> wrap( List<Element> elements ) {
    return elements;
  }


  public static <T> Element toElement(Object ctx,
      T root,
      final Function<T, JAXBElement<? super T>> mapper) {

    Optional<Document> dox = JaxbUtil.marshall(Collections.singleton(ctx.getClass()),
        root,
        mapper,
        JaxbUtil.defaultProperties())
        .map(ByteArrayOutputStream::toByteArray)
        .map(ByteArrayInputStream::new)
        .flatMap(XMLUtil::loadXMLDocument);

    return dox
        .map(Document::getDocumentElement)
        .orElseThrow(IllegalStateException::new);
  }

}
