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
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.nodes.ConsensusConfigBuilder;
import io.hotmoka.node.internal.json.ConsensusConfigDecoder;
import io.hotmoka.node.internal.json.ConsensusConfigEncoder;
import io.hotmoka.node.internal.json.ConsensusConfigJson;
import io.hotmoka.node.internal.nodes.BasicConsensusConfigBuilder;

/**
 * Providers of consensus configurations.
 */
public abstract class ConsensusConfigBuilders {

	private ConsensusConfigBuilders() {}

	/**
	 * Creates a builder containing default data.
	 * 
	 * @return the builder
	 * @throws NoSuchAlgorithmException if some cryptographic algorithm is not available
	 */
	public static ConsensusConfigBuilder<?,?> defaults() throws NoSuchAlgorithmException {
		return new BasicConsensusConfigBuilder();
	}

	/**
	 * Creates a builder containing default data. but for the given signature.
	 * 
	 * @param signatureForRequests the signature algorithm to use for signing the requests
	 * @return the builder
	 */
	public static ConsensusConfigBuilder<?,?> defaults(SignatureAlgorithm signatureForRequests) {
		return new BasicConsensusConfigBuilder(signatureForRequests);
	}

	/**
	 * Creates a builder from the given TOML configuration file.
	 * The resulting builder will contain the information in the file,
	 * and use defaults for the data not contained in the file.
	 * 
	 * @param path the path to the TOML file
	 * @return the builder
	 * @throws FileNotFoundException if {@code path} cannot be found
	 * @throws NoSuchAlgorithmException if some cryptographic algorithm is not available
	 * @throws Base64ConversionException if some public key in the TOML file is not correctly Base64-encoded
	 * @throws InvalidKeySpecException if the specification of some public key in the TOML file is illegal
	 * @throws InvalidKeyException if some public key in the TOML file is invalid
	 */
	public static ConsensusConfigBuilder<?,?> load(Path path) throws NoSuchAlgorithmException, FileNotFoundException, InvalidKeyException, InvalidKeySpecException, Base64ConversionException {
		return new BasicConsensusConfigBuilder(path);
	}

	/**
	 * Gson encoder.
	 */
	public static class Encoder extends ConsensusConfigEncoder {

		/**
		 * Creates a new encoder.
		 */
		public Encoder() {}
	}

	/**
	 * Gson decoder.
	 */
	public static class Decoder extends ConsensusConfigDecoder {

		/**
		 * Creates a new decoder.
		 */
		public Decoder() {}
	}

    /**
     * JSON representation.
     */
    public static class Json extends ConsensusConfigJson {

    	/**
    	 * Creates the JSON representation for the given configuration.
    	 * 
    	 * @param config the configuration
    	 */
    	public Json(ConsensusConfig<?,?> config) {
    		super(config);
    	}
    }
}