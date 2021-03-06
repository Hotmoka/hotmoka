/*
Copyright 2021 Fausto Spoto

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

package io.hotmoka.beans.requests;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.SignatureException;

import io.hotmoka.beans.MarshallingContext;
import io.hotmoka.beans.SignatureAlgorithm;
import io.hotmoka.beans.values.StorageReference;

/**
 * A request signed with a signature of its caller.
 */
public interface SignedTransactionRequest {

	/**
	 * Used as empty signature for view transaction requests.
	 */
	byte[] NO_SIG = new byte[0];

	/**
	 * The caller that signs the transaction request.
	 * 
	 * @return the caller
	 */
	StorageReference getCaller();

	/**
	 * Yields the chain identifier where this request can be executed, to forbid transaction replay across chains.
	 * 
	 * @return the chain identifier
	 */
	String getChainId();

	/**
	 * Yields the signature of the request. This must be the signature of its byte representation (excluding the signature itself)
	 * with the private key of the caller, or otherwise the signature is illegal and the request will be rejected.
	 * 
	 * @return the signature
	 */
	byte[] getSignature();

	/**
	 * Marshals this object into a given stream. This method in general
	 * performs better than standard Java serialization, wrt the size of the marshalled data.
	 * The difference with {@link TransactionRequest#into(MarshallingContext)} is that the signature
	 * is not marshalled into the stream.
	 * 
	 * @param context the context holding the stream
	 * @throws IOException if this object cannot be marshalled
	 */
	void intoWithoutSignature(MarshallingContext context) throws IOException;

	/**
	 * Marshals this object into a byte array, without taking its signature into account.
	 * 
	 * @return the byte array resulting from marshalling this object
	 * @throws IOException if this object cannot be marshalled
	 */
	default byte[] toByteArrayWithoutSignature() throws IOException {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); MarshallingContext context = new MarshallingContext(baos)) {
			intoWithoutSignature(context);
			context.flush();
			return baos.toByteArray();
		}
	}

	/**
	 * An object that provides the signature of a request.
	 */
	interface Signer {

		/**
		 * Computes the signature of the given request.
		 * 
		 * @param what the request to sign
		 * @return the signature of the request
		 * @throws InvalidKeyException if the private key used for signing is invalid
		 * @throws SignatureException if the request cannot be signed
		 */
		byte[] sign(SignedTransactionRequest what) throws InvalidKeyException, SignatureException;

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
}