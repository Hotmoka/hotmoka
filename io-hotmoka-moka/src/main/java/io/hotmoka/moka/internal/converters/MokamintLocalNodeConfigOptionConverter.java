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
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;

import io.mokamint.node.local.LocalNodeConfigBuilders;
import io.mokamint.node.local.api.LocalNodeConfig;
import picocli.CommandLine.ITypeConverter;

/**
 * A converter of a string option into a transaction reference.
 */
public class MokamintLocalNodeConfigOptionConverter implements ITypeConverter<LocalNodeConfig> {

	@Override
	public LocalNodeConfig convert(String value) throws FileNotFoundException, NoSuchAlgorithmException, URISyntaxException {
		return LocalNodeConfigBuilders.load(Paths.get(value))
			.build();
	}
}