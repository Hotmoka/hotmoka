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
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import io.hotmoka.crypto.Base64ConversionException;
import io.hotmoka.node.TendermintConsensusConfigBuilders;
import io.hotmoka.node.api.nodes.TendermintConsensusConfig;
import picocli.CommandLine.ITypeConverter;

/**
 * A converter of a string option into a configuration for a Hotmoka node based on Tendermint.
 */
public class TendermintConsensusConfigOptionConverter implements ITypeConverter<TendermintConsensusConfig<?, ?>> {

	@Override
	public TendermintConsensusConfig<?, ?> convert(String value) throws FileNotFoundException, InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, Base64ConversionException {
		return TendermintConsensusConfigBuilders.load(Paths.get(value))
			.build();
	}
}