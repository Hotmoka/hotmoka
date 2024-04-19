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

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import com.moandjiezana.toml.Toml;

import io.hotmoka.beans.api.nodes.ValidatorsConsensusConfig;
import io.hotmoka.beans.api.nodes.ValidatorsConsensusConfigBuilder;
import io.hotmoka.crypto.Base64ConversionException;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.node.internal.nodes.ValidatorsConsensusConfigImpl;

/**
 * The builder of a configuration object.
 * 
 * @param <C> the concrete type of the configuration
 * @param <B> the concrete type of the builder
 */
public abstract class AbstractValidatorsConsensusConfigBuilder<C extends ValidatorsConsensusConfig<C,B>, B extends ValidatorsConsensusConfigBuilder<C,B>> extends ValidatorsConsensusConfigImpl.ValidatorsConsensusConfigBuilderImpl<C,B> {

	/**
	 * Creates the builder.
	 * 
	 * @throws NoSuchAlgorithmException if the configuration refers to some unknown hashing algorithm
	 */
	protected AbstractValidatorsConsensusConfigBuilder() throws NoSuchAlgorithmException {
	}

	/**
	 * Creates a builder containing default data. but for the given signature.
	 * 
	 * @param signatureForRequests the signature algorithm to use for signing the requests
	 * @return the builder
	 */
	protected AbstractValidatorsConsensusConfigBuilder(SignatureAlgorithm signatureForRequests) {
		super(signatureForRequests);
	}

	/**
	 * Reads the properties of the given TOML file and sets them for
	 * the corresponding fields of this builder.
	 * 
	 * @param toml the file
	 * @throws NoSuchAlgorithmException if some hashing algorithm cannot be found
	 * @throws Base64ConversionException if some public key in the TOML file is not correctly Base64-encoded
	 * @throws InvalidKeySpecException if the specification of some public key in the TOML file is illegal
	 * @throws InvalidKeyException if some public key in the TOML file is invalid
	 */
	protected AbstractValidatorsConsensusConfigBuilder(Toml toml) throws NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException, Base64ConversionException {
		super(toml);
	}

	/**
	 * Creates a builder with properties initialized to those of the given configuration object.
	 * 
	 * @param config the configuration object
	 */
	protected AbstractValidatorsConsensusConfigBuilder(ValidatorsConsensusConfig<C,B> config) {
		super(config);
	}
}