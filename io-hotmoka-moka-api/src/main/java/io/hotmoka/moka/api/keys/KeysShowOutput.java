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

package io.hotmoka.moka.api.keys;

import java.io.PrintStream;
import java.util.Optional;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.crypto.api.SignatureAlgorithm;

/**
 * The output of the {@code moka keys show} command.
 */
@Immutable
public interface KeysShowOutput {

	/**
	 * The maximal length for the printed keys. After this length, the printout of the key gets truncated.
	 */
	final static int MAX_PRINTED_KEY = 200;

	/**
	 * Yields the signature algorithm of the key pair.
	 * 
	 * @return the signature algorithm of the key pair
	 */
	SignatureAlgorithm getSignature();

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
	 * @return the base58-encoded private key, if this output reports it
	 */
	Optional<String> getPrivateKeyBase58();

	/**
	 * The base64-encoded private key.
	 * 
	 * @return the base64-encoded private key, if this output reports it
	 */
	Optional<String> getPrivateKeyBase64();

	/**
	 * The base64-encoded concatenated private and public key.
	 * 
	 * @return the base64-encoded concatenated private and public key, if this output reports it
	 */
	Optional<String> getConcatenatedBase64();

	/**
	 * Prints this output as a string.
	 * 
	 * @param out the destination print stream
	 * @param json true if and only if the string must be in JSON format
	 */
	void println(PrintStream out, boolean json);
}