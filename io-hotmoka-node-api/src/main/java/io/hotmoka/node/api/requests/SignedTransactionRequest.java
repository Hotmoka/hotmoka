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

package io.hotmoka.node.api.requests;

import java.io.IOException;

import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.values.StorageReference;

/**
 * A request signed with a signature of its caller.
 * 
 * @param <R> the type of the response expected for this request
 */
public interface SignedTransactionRequest<R extends TransactionResponse> extends TransactionRequest<R> {

	/**
	 * Yields the caller that signs the transaction request.
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
	 * Marshals this object into a given stream. The difference with
	 * {@link TransactionRequest#into(MarshallingContext)} is that the signature
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
	 */
	byte[] toByteArrayWithoutSignature();
}