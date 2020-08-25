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
package edu.mayo.kmdp.preprocess.meta.beans;

import com.fasterxml.jackson.databind.JsonNode;
import edu.mayo.kmdp.preprocess.meta.MetadataExtractor;
import edu.mayo.kmdp.preprocess.meta.Weaver;
import edu.mayo.kmdp.util.JSonUtil;
import edu.mayo.kmdp.util.XMLUtil;
import org.omg.spec.api4kp._20200801.taxonomy.krlanguage.KnowledgeRepresentationLanguageSeries;
import java.io.InputStream;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;


/**
 * Perform the weave and extract of the model from Trisotech, either DMN or CMMN.
 */
@Component
public class ChainConverter {

  @Autowired
  Weaver weaver;

  @Autowired
  MetadataExtractor extractor;

  public ChainConverter() { }

  /**
   *
   * @param meta The file info from Trisotech
   * @param model Either a serialized DMN/XML or CMMN/XML
   * @param type {DMN, CMMN}
   * @return
   */
  public Model convert( InputStream meta, InputStream model, KnowledgeRepresentationLanguageSeries type ) {
    switch ( type ) {
      case DMN_1_2:
      case CMMN_1_1:
        return convertModel( meta, model, type );
      default:
        throw new IllegalArgumentException( "Unexpected source type " + type );
    }
  }


  /**
   * perform the weave and extract to convert the model
   *
   * @param meta
   * @param modelXml
   * @param src
   * @return
   */
  protected Model convertModel( InputStream meta, InputStream modelXml, KnowledgeRepresentationLanguageSeries src ) {

    final Model model = new Model();

    Optional<Document> modelDox = XMLUtil.loadXMLDocument( modelXml );
    Optional<JsonNode> surrJson = JSonUtil.readJson(meta);

    if ( ! modelDox.isPresent() || ! surrJson.isPresent() ) {
      throw new IllegalArgumentException( "Unable to convert model" );
    }

    modelDox.map( weaver::weave )
        .map( model::addModel )
        .flatMap( (wovenDox) -> extractor.doExtract( wovenDox, surrJson.get() ) )
        .map( model::addSurrogate );

    return model;
  }

}
