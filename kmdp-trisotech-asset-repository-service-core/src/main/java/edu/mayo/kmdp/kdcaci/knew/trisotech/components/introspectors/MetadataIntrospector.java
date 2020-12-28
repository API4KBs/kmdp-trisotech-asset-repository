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
package edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors;

import static edu.mayo.kmdp.util.XMLUtil.loadXMLDocument;
import static org.omg.spec.api4kp._20200801.taxonomy.parsinglevel.ParsingLevelSeries.Encoded_Knowledge_Expression;

import edu.mayo.kmdp.language.parsers.surrogate.v2.Surrogate2Parser;
import edu.mayo.kmdp.trisotechwrapper.models.TrisotechFileInfo;
import edu.mayo.kmdp.util.JSonUtil;
import java.io.InputStream;
import java.util.Optional;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.services.SyntacticRepresentation;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.SurrogateHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

/**
 * MetadataExtractor takes the output of the Weaver and the information of the artifact to create
 * a KnowledgeAsset surrogate.
 */
@Component
public class MetadataIntrospector {

  private static final Logger logger = LoggerFactory.getLogger(MetadataIntrospector.class);

  @Autowired
  private TrisotechIntrospectionStrategy strategy;

  private Surrogate2Parser lifter = new Surrogate2Parser();

  public KnowledgeAsset extract(Document dox, TrisotechFileInfo meta) {
    return strategy.extractXML(dox, meta);
  }

  public Optional<KnowledgeAsset> extract(InputStream resource, InputStream meta) {
    Optional<Document> dox = loadXMLDocument(resource);
    TrisotechFileInfo info = Optional.ofNullable(meta)
        .flatMap(json -> JSonUtil.readJson(json, TrisotechFileInfo.class))
        .orElseGet(TrisotechFileInfo::new);

    return dox
        .map(document -> extract(document, info));
  }

  public Optional<byte[]> extractBinary(InputStream resource, InputStream meta, String codedRep) {
    Optional<KnowledgeCarrier> kc = extract(resource, meta)
        .map(SurrogateHelper::carry);

    return kc.map(
        ast -> lifter.applyLower(
            ast,
            Encoded_Knowledge_Expression,
            codedRep,
            null)
        .flatOpt(AbstractCarrier::asBinary)
        .orElseGet(() -> new byte[0])
    );
  }

  public Optional<SyntacticRepresentation> getRepLanguage(Document document, boolean concrete) {
    return strategy.getRepLanguage(document, concrete);
  }

}
