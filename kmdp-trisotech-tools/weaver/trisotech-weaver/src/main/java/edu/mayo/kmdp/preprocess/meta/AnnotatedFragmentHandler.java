///**
// * Copyright © 2019 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
package edu.mayo.kmdp.preprocess.meta;
//

import org.w3c.dom.Element;

import java.util.Collections;
import java.util.List;

//
//
//import static edu.mayo.kmdp.preprocess.ReaderOptions.p_ATTR_GX;

// TODO: Is this class needed? CAO
//
public class AnnotatedFragmentHandler extends BaseAnnotationHandler {
  //
  private ReaderConfig config;

  private final String NS;

  //	private final String ATTR;
//
  public AnnotatedFragmentHandler(ReaderConfig config) {
    // TODO: Needed? If so, how to handle config CAO
    this.NS = config.getTyped(ReaderOptions.p_METADATA_NS);
		this.config = config;
  }


  @Override
  public void replaceProprietaryElement(Element oldEl, Element newEl) {
//		oldEl.removeAttributeNS( NS, ATTR );
//		oldEl.insertBefore( newEl, oldEl.getFirstChild() );
  }

  //
  @Override
  public void replaceProprietaryElement(Element oldEl, List<Element> newEls) {
//		oldEl.removeAttributeNS( NS, ATTR );
//		Node x = oldEl.getFirstChild();
//		newEls.forEach( (newEl) -> oldEl.insertBefore( newEl, x ) );
  }

  //
  // TODO: Needed? CAO
  @Override
  public List<Element> wrap(List<Element> elements) {
    return Collections.emptyList();
//		Element ext = modelExt( elements.get( 0 ).getOwnerDocument(), config.getTyped( p_EL_MODEL_EXT ) );
//		elements.forEach( ext::appendChild );
//		return Collections.singletonList( ext );
  }


//	private Element modelExt( Document dox, String elementName ) {
//		return dox.createElementNS( config.getTyped( p_MODEL_NS ),
//		                            getPrefix( dox, config.getTyped( p_MODEL_NS ) )  + elementName );
//	}

}
