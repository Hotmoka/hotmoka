/*
Copyright 2025 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.moka.internal.converters;

import java.io.FileNotFoundException;
import java.nio.file.Paths;

import io.hotmoka.node.mokamint.MokamintNodeConfigBuilders;
import io.hotmoka.node.mokamint.api.MokamintNodeConfig;
import picocli.CommandLine.ITypeConverter;

/**
 * A converter of a string option into a Mokamint Hotmoka node configuration.
 */
public class MokamintNodeConfigOptionConverter implements ITypeConverter<MokamintNodeConfig> {

	@Override
	public MokamintNodeConfig convert(String value) throws FileNotFoundException {
		return MokamintNodeConfigBuilders.load(Paths.get(value))
			.build();
	}
}