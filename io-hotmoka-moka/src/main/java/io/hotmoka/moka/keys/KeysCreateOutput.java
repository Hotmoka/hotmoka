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

package io.hotmoka.moka.keys;

import java.nio.file.Path;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.moka.internal.keys.Create;

/**
 * The output of the moka keys create command.
 */
@Immutable
public interface KeysCreateOutput {

	/**
	 * The maximal length for the printed keys. After this length, the printout of the key gets truncated.
	 */
	final static int MAX_PRINTED_KEY = 200;

	/**
	 * Yields the output of the command from its JSON representation.
	 * 
	 * @param json the JSON representation
	 * @return the output of the command
	 */
	static KeysCreateOutput of(String json) {
		return Create.Output.of(json);
	}

	/**
	 * Yields the output of a command that created the given key pair
	 * with the given signature algorithm.
	 * 
	 * @param signature the signature algorithm
	 * @param keys the key pair
	 * @param alsoPrivate true if and only if also the private key must be reported in the output
	 * @return the resulting output
	 * @throws NoSuchAlgorithmException if the sha256 hashing algorithm is not available
	 */
	static KeysCreateOutput of(SignatureAlgorithm signature, KeyPair keys, boolean alsoPrivate) throws NoSuchAlgorithmException {
		return new Create.Output(signature, keys, alsoPrivate);
	}

	@Override
	boolean equals(Object other);

	@Override
	int hashCode();

	/**
	 * Yields the name of the signature algorithm of the key pair.
	 * 
	 * @return the name of the signature algorithm of the key pair
	 */
	String getSignature();

	/**
	 * The base58-encoded public key.
	 * 
	 * @return the base58-encoded public key
	 */
	String getPublicKeyBase58();

	/**
	 * The base64-encoded public key.
	 * 
	 * @return the base64-encoded public key
	 */	
	String getPublicKeyBase64();

	/**
	 * The public key represented as a Tendermint address.
	 * 
	 * @return the public key represented as a Tendermint address
	 */
	String getTendermintAddress();

	/**
	 * The base58-encoded private key.
	 * 
	 * @return the base58-encoded private key
	 */
	String getPrivateKeyBase58();

	/**
	 * The base64-encoded private key.
	 * 
	 * @return the base64-encoded private key
	 */
	String getPrivateKeyBase64();

	/**
	 * The base64-encoded concatenated private and public key.
	 * 
	 * @return the base64-encoded concatenated private and public key
	 */
	String getConcatenatedBase64();

	/**
	 * Yields the output of the command as a string.
	 * 
	 * @param file the path where the key pair file has been written
	 * @param json true if and only if the string must be in JSON format
	 * @return the output of the command as a string
	 */
	String toString(Path file, boolean json);
}