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


import static org.omg.spec.api4kp._20200801.id.SemanticIdentifier.newVersionId;

import edu.mayo.kmdp.trisotechwrapper.config.TTConstants;
import java.net.URI;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.bind.Marshaller;
import org.omg.spec.api4kp._20200801.id.ObjectFactory;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;


/**
 * Annotation handler that rewrites Knowledge (Service) Asset Identifiers
 */
public class AssetIDAnnotationHandler extends AbstractAnnotationHandler<ResourceIdentifier> {

  protected static final Logger logger = LoggerFactory.getLogger(AssetIDAnnotationHandler.class);

  @Nonnull
  protected static ObjectFactory of;
  @Nullable
  protected static Marshaller jxm;

  static {
    of = new ObjectFactory();
    jxm = initMarshaller(ResourceIdentifier.class);
  }

  /**
   * Replaces a custom attribute extension carrying an Asset ID in URI form, with an element
   * resulting from the XML serialization of a {@link ResourceIdentifier}
   * <p>
   * Parses the Asset ID URI, serializes the ResourceIdentifier, and finally swaps the custom
   * attribute element
   *
   * @param el the TT custom attribute element holding the asset ID
   */
  public void replaceProprietaryElement(
      @Nonnull final Element el) {
    var parent = (Element) el.getParentNode();
    parent.removeChild(el);

    if (jxm != null) {
      String assetUri = el.getAttribute(TTConstants.VALUE);
      var assetId = newVersionId(URI.create(assetUri));

      toChildElement(assetId, of::createResourceIdentifier, parent, jxm);
    }
  }


}
