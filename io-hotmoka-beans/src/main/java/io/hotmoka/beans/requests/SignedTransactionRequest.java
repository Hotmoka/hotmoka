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

import io.hotmoka.beans.api.requests.TransactionRequest;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.beans.marshalling.BeanMarshallingContext;
import io.hotmoka.marshalling.api.MarshallingContext;

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
	 */
	default byte[] toByteArrayWithoutSignature() {
		try (var baos = new ByteArrayOutputStream(); var context = new BeanMarshallingContext(baos)) {
			intoWithoutSignature(context);
			context.flush();
			return baos.toByteArray();
		}
		catch (IOException e) {
			// impossible with a byte array output stream
			throw new RuntimeException("Unexpected exception", e);
		}
	}
}