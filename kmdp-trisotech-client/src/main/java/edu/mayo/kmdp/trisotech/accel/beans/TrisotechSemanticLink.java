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

import java.util.UUID;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="semanticLink",namespace = "http://www.trisotech.com/2015/triso/modeling")
@XmlAccessorType(XmlAccessType.FIELD)
public class TrisotechSemanticLink {

  @XmlAttribute
  private String name;
  @XmlAttribute
  private String id;
  @XmlAttribute
  private String modelURI;
  @XmlAttribute
  private String uri;

  public TrisotechSemanticLink() {
  }

  public TrisotechSemanticLink(String name, String modelURI, String uri) {
    this.id = "_" + UUID.nameUUIDFromBytes((name+uri).getBytes());
    this.name = name;
    this.modelURI = modelURI;
    this.uri = uri;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getModelURI() {
    return modelURI;
  }

  public void setModelURI(String modelURI) {
    this.modelURI = modelURI;
  }

  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }
}
