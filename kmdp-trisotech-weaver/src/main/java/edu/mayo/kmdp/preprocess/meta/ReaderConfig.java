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
package edu.mayo.kmdp.preprocess.meta;

import edu.mayo.kmdp.ConfigProperties;
import java.util.Properties;
import org.springframework.stereotype.Component;

@Component
public class ReaderConfig extends ConfigProperties<ReaderConfig, ReaderOptions>  {

	private static final Properties defaultedProperties = defaulted( ReaderOptions.class );

	public ReaderConfig() {
		super(defaultedProperties);
	}

	@Override
	public ReaderOptions[] properties() {
		return ReaderOptions.values();
	}


}
