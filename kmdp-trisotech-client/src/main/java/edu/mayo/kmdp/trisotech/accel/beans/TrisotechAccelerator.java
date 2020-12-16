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

import com.fasterxml.jackson.annotation.JsonValue;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class TrisotechAccelerator {

  private URI id;
  private String title;
  private String description;
  private List<TrisotechAcceleratorEntry> children;

  public TrisotechAccelerator() {
  }

  public TrisotechAccelerator(URI id, String title, String description) {
    this.id = id;
    this.title = title;
    this.description = description;
    this.children = new ArrayList<>();
  }

  public URI getId() {
    return id;
  }

  public void setId(URI id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public List<TrisotechAcceleratorEntry> getChildren() {
    return children;
  }

  public void setChildren(
      List<TrisotechAcceleratorEntry> children) {
    this.children = children;
  }

  public void addChild(TrisotechAcceleratorEntry child) {
    this.children.add(child);
  }

  public static class TrisotechAcceleratorEntry extends TrisotechAccelerator {

    private AcceleratorTypes type = AcceleratorTypes.ENTITY;
    private DragTypes drag = DragTypes.ITEM;

    public enum AcceleratorTypes {
      ENTITY("entity");

      private String val;

      AcceleratorTypes(String value) {
        this.val = value;
      }

      @JsonValue
      public String getVal() {
        return val;
      }
    }
    public enum DragTypes {
      ITEM("item");

      private String val;

      DragTypes(String value) {
        this.val = value;
      }

      @JsonValue
      public String getVal() {
        return val;
      }
    }

    public TrisotechAcceleratorEntry() {
    }

    public TrisotechAcceleratorEntry(URI id, String title, String description) {
      super(id, title, description);
    }

    public AcceleratorTypes getType() {
      return type;
    }

    public DragTypes getDrag() {
      return drag;
    }

    @Override
    public int hashCode() {
      return getId().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof TrisotechAcceleratorEntry &&
          this.getId().equals(((TrisotechAcceleratorEntry)obj).getId());
    }
  }

}
