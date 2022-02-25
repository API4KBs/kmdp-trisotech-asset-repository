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
package edu.mayo.kmdp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omg.spec.api4kp._20200801.taxonomy.dependencyreltype.DependencyTypeSeries.Depends_On;

import edu.mayo.kmdp.kdcaci.knew.trisotech.IdentityMapper;
import edu.mayo.kmdp.kdcaci.knew.trisotech.NamespaceManager;
import edu.mayo.kmdp.kdcaci.knew.trisotech.components.introspectors.MetadataIntrospector;
import edu.mayo.kmdp.kdcaci.knew.trisotech.components.redactors.Redactor;
import edu.mayo.kmdp.kdcaci.knew.trisotech.components.weavers.Weaver;
import edu.mayo.kmdp.util.StreamUtil;
import edu.mayo.kmdp.util.XMLUtil;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.omg.spec.api4kp._20200801.surrogate.Dependency;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = {TrisotechAssetRepositoryTestConfig.class})
class LinkedMetadataTest {

  // FYI: IDE may complain about
  // the following two not being able to be autowired, but the code works.
  @Autowired
  private MetadataIntrospector extractor;

  @Autowired
  NamespaceManager names;

  @Autowired
  private IdentityMapper mapper;

  @Autowired
  private Weaver weaver;

  @Autowired
  private Redactor redactor;

  private static final String dmnPath = "/Links Decision.dmn.xml";
  private static final String dmnMetaPath = "/Links Decision.meta.json";
  private static final String dmnSvcPath = "/Linked Service.dmn.xml";
  private static final String dmnSvcMetaPath = "/Linked Service.meta.json";
  private static final String cmmnPath = "/Links Test.cmmn.xml";
  private static final String cmmnMetaPath = "/Links Test.meta.json";
  private static boolean constructed = false;

  private static KnowledgeAsset dmnSurr;
  private static KnowledgeAsset cmmnSurr;
  private static KnowledgeAsset svcSurr;

  /**
   * Need to use @PostConstruct instead of @BeforeAll because @BeforeAll requires the method be
   * static, and cannot @Autowired on static variables which would be needed for the static method.
   * Have the check for constructed as otherwise @PostConstruct will be run before EVERY @Test. It
   * still will but now the processing won't happen after the first time.
   */
  @PostConstruct
  public void init() {

    if (!constructed) {

      Optional<byte[]> dmn = XMLUtil
          .loadXMLDocument(LinkedMetadataTest.class.getResourceAsStream(dmnPath))
          .map(weaver::weave)
          .map(redactor::redact)
          .map(XMLUtil::toByteArray);
      assertTrue(dmn.isPresent());
      byte[] annotatedDMN = dmn.get();
      dmnSurr = extractor.extract(new ByteArrayInputStream(annotatedDMN),
          LinkedMetadataTest.class.getResourceAsStream(dmnMetaPath))
          .orElseGet(Assertions::fail);

      Optional<byte[]> svc = XMLUtil
          .loadXMLDocument(LinkedMetadataTest.class.getResourceAsStream(dmnSvcPath))
          .map(weaver::weave)
          .map(redactor::redact)
          .map(XMLUtil::toByteArray);
      assertTrue(svc.isPresent());
      byte[] annotatedSVC = svc.get();
      svcSurr = extractor.extract(new ByteArrayInputStream(annotatedSVC),
          LinkedMetadataTest.class.getResourceAsStream(dmnSvcMetaPath))
          .orElseGet(Assertions::fail);

      Optional<byte[]> cmmn = XMLUtil
          .loadXMLDocument(LinkedMetadataTest.class.getResourceAsStream(cmmnPath))
          .map(weaver::weave)
          .map(redactor::redact)
          .map(XMLUtil::toByteArray);
      assertTrue(cmmn.isPresent());
      byte[] annotatedCMMN = cmmn.get();
      cmmnSurr = extractor.extract(new ByteArrayInputStream(annotatedCMMN),
          LinkedMetadataTest.class.getResourceAsStream(cmmnMetaPath))
          .orElseGet(Assertions::fail);

      constructed = true;
    }

  }

  @Test
  void testNoDuplicateAnnotations() {
    assertEquals(2, dmnSurr.getAnnotation().size());
    assertEquals(0, svcSurr.getAnnotation().size());
    assertEquals(2, cmmnSurr.getAnnotation().size());
  }

  @Test
  void testLinks() {
    assertEquals(1, cmmnSurr.getLinks().size());
    List<Dependency> deps1 = cmmnSurr.getLinks().stream()
        .flatMap(StreamUtil.filterAs(Dependency.class))
        .filter(dep -> Depends_On.sameAs(dep.getRel()))
        .collect(Collectors.toList());
    assertEquals(1, deps1.size());

    assertEquals(1, dmnSurr.getLinks().size());
    List<Dependency> deps2 = dmnSurr.getLinks().stream()
        .flatMap(StreamUtil.filterAs(Dependency.class))
        .filter(dep -> Depends_On.sameAs(dep.getRel()))
        .collect(Collectors.toList());
    assertEquals(1, deps2.size());

    assertEquals(1, svcSurr.getLinks().size());
    List<Dependency> deps3 = svcSurr.getLinks().stream()
        .flatMap(StreamUtil.filterAs(Dependency.class))
        .filter(dep -> Depends_On.sameAs(dep.getRel()))
        .collect(Collectors.toList());
    assertEquals(1, deps3.size());
  }

}
