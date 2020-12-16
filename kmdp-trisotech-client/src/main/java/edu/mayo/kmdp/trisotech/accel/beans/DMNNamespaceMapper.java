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
package edu.mayo.kmdp.trisotech.accel.beans;

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;
import java.util.HashMap;
import java.util.Map;

public class DMNNamespaceMapper extends NamespacePrefixMapper {

  private Map<String, String> namespaceMap = new HashMap<>();

  public DMNNamespaceMapper(String deafultNS) {
    namespaceMap.put("http://www.omg.org/spec/DMN/20180521/MODEL/", "semantic");
    namespaceMap.put("http://www.omg.org/spec/DMN/20180521/DI/", "di");
    namespaceMap.put("http://www.omg.org/spec/DMN/20180521/DMNDI/", "dmndi");
    namespaceMap.put("http://www.omg.org/spec/DMN/20180521/DC/", "dc");
    namespaceMap.put("http://www.trisotech.com/2015/triso/modeling", "triso");
    namespaceMap.put(deafultNS, "");
  }

  @Override
  public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
    return namespaceMap.getOrDefault(namespaceUri, suggestion);
  }
}
