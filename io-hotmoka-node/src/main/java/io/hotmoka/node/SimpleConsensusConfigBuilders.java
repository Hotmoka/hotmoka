/*
Copyright 2023 Fausto Spoto

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

package io.hotmoka.node;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import io.hotmoka.crypto.Base64ConversionException;
import io.hotmoka.node.api.SimpleConsensusConfigBuilder;
import io.hotmoka.node.internal.SimpleConsensusConfigImpl.SimpleConsensusConfigBuilderImpl;

/**
 * Providers of consensus configuration builders.
 */
public abstract class SimpleConsensusConfigBuilders {

	private SimpleConsensusConfigBuilders() {}

	/**
	 * Creates a builder containing default data.
	 * 
	 * @return the builder
	 * @throws NoSuchAlgorithmException if some cryptographic algorithm is not available
	 */
	public static SimpleConsensusConfigBuilder defaults() throws NoSuchAlgorithmException {
		return new SimpleConsensusConfigBuilderImpl();
	}

	/**
	 * Creates a builder from the given TOML configuration file.
	 * The resulting builder will contain the information in the file,
	 * and use defaults for the data not contained in the file.
	 * 
	 * @param path the path to the TOML file
	 * @return the builder
	 * @throws FileNotFoundException if {@code path} cannot be found
	 * @throws NoSuchAlgorithmException if some cryptographic algorithm in the TOML file is not available
	 * @throws Base64ConversionException if some public key in the TOML file is not correctly Base64-encoded
	 * @throws InvalidKeySpecException if the specification of some public key in the TOML file is illegal
	 * @throws InvalidKeyException if some public key in the TOML file is invalid
	 */
	public static SimpleConsensusConfigBuilder load(Path path) throws FileNotFoundException, NoSuchAlgorithmException, InvalidKeySpecException, Base64ConversionException, InvalidKeyException {
		return new SimpleConsensusConfigBuilderImpl(path);
	}
}