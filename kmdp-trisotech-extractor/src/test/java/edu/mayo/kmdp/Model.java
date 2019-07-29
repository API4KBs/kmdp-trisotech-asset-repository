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
package edu.mayo.kmdp;

import edu.mayo.kmdp.util.XMLUtil;
import org.w3c.dom.Document;

import java.io.ByteArrayOutputStream;

public class Model {

	private Document model;
	private Document surrogate;

	public Model() {
	}

	public Model( Document model, Document surrogate ) {
		this.model = model;
		this.surrogate = surrogate;
	}

	public Document getModel() {
		return model;
	}

	public Document getSurrogate() {
		return surrogate;
	}

	public void setModel( Document model ) {
		this.model = model;
	}

	public Document addModel( Document model ) {
		setModel( model );
		return model;
	}

	public Document addSurrogate( Document surrogate ) {
		setSurrogate( surrogate );
		return surrogate;
	}

	public void setSurrogate( Document surrogate ) {
		this.surrogate = surrogate;
	}

	public byte[] streamModel() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		XMLUtil.streamXMLDocument( model, baos );
		return baos.toByteArray();
	}

	public byte[] streamSurrogate() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		XMLUtil.streamXMLDocument( surrogate, baos );
		return baos.toByteArray();
	}

}
