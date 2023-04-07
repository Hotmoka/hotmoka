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

package io.hotmoka.nodes;

import java.security.KeyPair;
import java.security.PrivateKey;

import io.hotmoka.beans.requests.SignedTransactionRequest;
import io.hotmoka.crypto.api.SignatureAlgorithm;

/**
 * An object that provides the signature of a request.
 */
public interface Signer extends io.hotmoka.beans.requests.SignedTransactionRequest.Signer {

	/**
	 * Yields a signer for the given algorithm with the given key pair.
	 * 
	 * @param signature the signing algorithm
	 * @param keys the key pair
	 * @return the signer
	 */
	static Signer with(SignatureAlgorithm<SignedTransactionRequest> signature, KeyPair keys) {
		return with(signature, keys.getPrivate());
	}

	/**
	 * Yields a signer for the given algorithm with the given private key.
	 * 
	 * @param signature the signing algorithm
	 * @param key the private key
	 * @return the signer
	 */
	static Signer with(SignatureAlgorithm<SignedTransactionRequest> signature, PrivateKey key) {
		return what -> signature.sign(what, key);
	}
}