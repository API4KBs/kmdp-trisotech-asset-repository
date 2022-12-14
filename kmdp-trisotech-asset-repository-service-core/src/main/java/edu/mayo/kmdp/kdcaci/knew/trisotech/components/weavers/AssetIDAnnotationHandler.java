/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.mayo.kmdp.kdcaci.knew.trisotech.components.weavers;


import static java.util.Collections.singletonList;

import edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.omg.spec.api4kp._20200801.id.ConceptIdentifier;
import org.omg.spec.api4kp._20200801.id.ObjectFactory;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.id.SemanticIdentifier;
import org.omg.spec.api4kp._20200801.surrogate.Annotation;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


/**
 * Rewrites proprietary tags explicitly
 */
public class AssetIDAnnotationHandler extends AbstractAnnotationHandler{

	protected static final ObjectFactory of = new ObjectFactory();


	public ResourceIdentifier getIdentifier(String uri) {
		return SemanticIdentifier.newVersionId(URI.create(uri));
	}

	public void replaceProprietaryElement( Element oldEl, ResourceIdentifier assetId) {
		swap(oldEl, wrap(assetId, oldEl));
	}

	public List<Element> wrap(ResourceIdentifier assetId, Element el) {
		return singletonList(toChildElement(assetId, el));
	}

	protected Element toChildElement(ResourceIdentifier rid, Element parent) {
		Element el;
		if (rid != null) {
			el = toElement(of,
					rid,
					of::createResourceIdentifier
			);
		} else {
			throw new IllegalStateException("Missing Resource ID");
		}
		parent.getOwnerDocument().adoptNode(el);
		parent.appendChild(el);
		return el;
	}

}
