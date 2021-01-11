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


import edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.omg.spec.api4kp._20200801.id.ConceptIdentifier;
import org.omg.spec.api4kp._20200801.surrogate.Annotation;
import org.omg.spec.api4kp._20200801.surrogate.ObjectFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


/**
 * Rewrites proprietary tags explicitly
 */
public class MetadataAnnotationHandler extends AbstractAnnotationHandler {

	protected static final ObjectFactory of = new ObjectFactory();

	public void replaceProprietaryElement( Element oldEl, List<Annotation> annos ) {
		swap(oldEl, wrap(annos, oldEl));
	}

	public List<Element> wrap(List<Annotation> annos, Element el) {
		return toChildElements(annos,el);
	}

	public List<Annotation> getAnnotation(SemanticAnnotationRelTypeSeries defaultRel,
			List<ConceptIdentifier> rows) {
		ConceptIdentifier rel = null;

		if(null != defaultRel) {
			rel = defaultRel.asConceptIdentifier();
		}

		return
				Collections.singletonList(
						newAnnotation(rel, /*rol,*/ rows));
	}

	protected Annotation newAnnotation(ConceptIdentifier rel, List<ConceptIdentifier> rows) {
		Annotation anno;
		switch (rows.size()) {
			case 0:
				anno = new Annotation();
				break;
			case 1:
				anno = new Annotation()
						.withRef(rows.get(0));
				break;
			default:
				throw new UnsupportedOperationException();
		}
		if (null != rel) {
			return anno.withRel(rel);
		} else {
			return anno;
		}
	}


	protected List<Element> toChildElements(List<Annotation> annos, Element parent) {
		return annos.stream()
				.map(ann -> toChildElement(ann, parent))
				.collect(Collectors.toList());
	}

	protected Element toChildElement(Annotation ann, Element parent) {
		Element el;
		if (ann != null) {
			el = toElement(of,
					ann,
					of::createAnnotation
			);
		} else {
			throw new IllegalStateException("Unmanaged annotation type" + ann.getClass().getName());
		}
		parent.getOwnerDocument().adoptNode(el);
		parent.appendChild(el);
		return el;
	}

}
