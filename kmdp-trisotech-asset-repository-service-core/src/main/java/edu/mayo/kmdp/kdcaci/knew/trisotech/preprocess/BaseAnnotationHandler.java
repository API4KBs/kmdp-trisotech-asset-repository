/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package edu.mayo.kmdp.kdcaci.knew.trisotech.preprocess;

import static edu.mayo.kmdp.registry.Registry.MAYO_ARTIFACTS_BASE_URI_URI;
import static org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries.Imports;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import edu.mayo.ontology.taxonomies.kmdo.semanticannotationreltype.SemanticAnnotationRelTypeSeries;
import org.omg.spec.api4kp._20200801.id.ConceptIdentifier;
import org.omg.spec.api4kp._20200801.id.Term;
import org.omg.spec.api4kp._20200801.surrogate.Annotation;
import org.w3c.dom.Element;

public abstract class BaseAnnotationHandler {

  public abstract void replaceProprietaryElement(Element original, Element replace);

  public abstract void replaceProprietaryElement(Element original, List<Element> replace);

  public abstract List<Element> wrap(List<Element> element);

  public List<Annotation> getAnnotation(List<ConceptIdentifier> rows) {
    return rows.stream().map(row -> newAnnotation(Collections.singletonList(row)))
        .collect(Collectors.toList());
  }

  public List<Annotation> getAnnotation(String name,
      SemanticAnnotationRelTypeSeries defaultRel,
      List<ConceptIdentifier> rows) {
    ConceptIdentifier rel = null;

    if(null != defaultRel) {
      rel = defaultRel.asConceptIdentifier();
    }

    return
        Collections.singletonList(
            newAnnotation(rel, /*rol,*/ rows));
  }

  public List<Annotation> getDataAnnotation(
      String modelId,
      String elementId) {

    return Collections.singletonList(new Annotation()
        .withRel(Imports.asConceptIdentifier())
        .withRef(Term.newTerm(MAYO_ARTIFACTS_BASE_URI_URI, modelId + "#" + elementId).asConceptIdentifier()));
  }

  protected Annotation newAnnotation(List<ConceptIdentifier> rows) {
    Annotation anno;
    switch (rows.size()) {
      case 0:
        anno = new Annotation();
        break;
      default:
        anno = new Annotation()
            .withRef(rows.get(0));
//            .withExpr(rows);
    }
    return anno;
  }

  protected Annotation newAnnotation(ConceptIdentifier rel, List<ConceptIdentifier> rows) {
    Annotation anno;
    switch (rows.size()) {
      case 0:
        anno = new Annotation();
        break;
      default:
        anno = new Annotation()
            .withRef(rows.get(0));
    }
    if (null != rel) {
      return anno.withRel(rel);
    } else {
      return anno;
    }
  }

}
