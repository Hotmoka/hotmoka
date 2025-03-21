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

package io.hotmoka.node.internal.responses;

import java.io.IOException;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.api.responses.InitializationTransactionResponse;
import io.hotmoka.node.internal.gson.TransactionResponseJson;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

/**
 * Implementation of a response for a transaction that initializes a node.
 */
@Immutable
public class InitializationTransactionResponseImpl extends TransactionResponseImpl implements InitializationTransactionResponse {
	final static byte SELECTOR = 14;

	/**
	 * Builds the transaction response.
	 */
	public InitializationTransactionResponseImpl() {}

	/**
	 * Unmarshals a response from the given stream.
	 * The selector of the response has been already processed.
	 * 
	 * @param context the unmarshalling context
	 * @throws IOException if the response could not be unmarshalled
	 */
	public InitializationTransactionResponseImpl(UnmarshallingContext context) throws IOException {
		this();
	}

	/**
	 * Creates a response from the given JSON representation.
	 * 
	 * @param json the JSON representation
	 * @throws InconsistentJsonException if {@code json} is inconsistent
	 */
	public InitializationTransactionResponseImpl(TransactionResponseJson json) throws InconsistentJsonException {
		this();
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof InitializationTransactionResponse;
	}

	@Override
	public int hashCode() {
		return 13011973;
	}

	@Override
	public String toString() {
        return getClass().getSimpleName();
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.writeByte(SELECTOR);
	}

	/**
	 * Factory method that unmarshals a response from the given stream.
	 * The selector of the response has been already processed.
	 * 
	 * @param context the unmarshalling context
	 * @return the request
     */
	public static InitializationTransactionResponseImpl from(UnmarshallingContext context) {
		return new InitializationTransactionResponseImpl();
	}
}