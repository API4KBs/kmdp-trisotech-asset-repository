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
package edu.mayo.kmdp.preprocess.meta;

import static edu.mayo.kmdp.preprocess.meta.Weaver.CLINICALKNOWLEDGEMANAGEMENT_MAYO_ARTIFACTS_BASE_URI;

import edu.mayo.kmdp.metadata.annotations.Annotation;
import edu.mayo.kmdp.metadata.annotations.BasicAnnotation;
import edu.mayo.kmdp.metadata.annotations.DatatypeAnnotation;
import edu.mayo.kmdp.metadata.annotations.MultiwordAnnotation;
import edu.mayo.kmdp.metadata.annotations.SimpleAnnotation;
import edu.mayo.ontology.taxonomies.kao.rel.dependencyreltype._20190801.DependencyType;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.omg.spec.api4kp._1_0.identifiers.ConceptIdentifier;
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
      KnownAttributes defaultRel,
      List<ConceptIdentifier> rows) {

    KnownAttributes rel = KnownAttributes.resolve(name).orElse(defaultRel);

    return
        Collections.singletonList(
            newAnnotation(rel, /*rol,*/ rows));
  }

  public List<Annotation> getDataAnnotation(
      String modelId,
      String elementId) {

    return Collections.singletonList(new DatatypeAnnotation()
        .withRel(DependencyType.Imports.asConcept())
        .withValue(
						// TODO: This should be Registry.MAYO_ARTIFACTS_BASE_URI -- Davide is adding CAO
            CLINICALKNOWLEDGEMANAGEMENT_MAYO_ARTIFACTS_BASE_URI + modelId + "#" + elementId));
  }

  protected Annotation newAnnotation(List<ConceptIdentifier> rows) {
    Annotation anno;
    switch (rows.size()) {
      case 0:
        anno = new SimpleAnnotation();
        break;
      case 1:
        anno = new SimpleAnnotation()
            .withExpr(rows.get(0));
        break;
      default:
        anno = new MultiwordAnnotation()
            .withExpr(rows);
    }
    return anno;
  }

  protected Annotation newAnnotation(KnownAttributes rel, List<ConceptIdentifier> rows) {
    Annotation anno;
    switch (rows.size()) {
      case 0:
        anno = new SimpleAnnotation();
        break;
      case 1:
        anno = new SimpleAnnotation()
            .withExpr(rows.get(0));
        break;
      default:
        anno = new MultiwordAnnotation()
            .withExpr(rows);
    }
    if (null != rel) {
      return anno.withRel(rel.asConcept());
    } else {
      return anno;
    }
  }

  public Annotation getBasicAnnotation(KnownAttributes attr, String v) {
    return new BasicAnnotation().withRel(attr.asConcept())
        .withExpr(URI.create(v));
  }

}
