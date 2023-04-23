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

package io.hotmoka.stores.internal;

import java.io.IOException;

import io.hotmoka.beans.references.LocalTransactionReference;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.marshalling.AbstractMarshallable;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;

/**
 * An array of transaction references that can be marshalled into an object stream.
 */
public class MarshallableArrayOfTransactionReferences extends AbstractMarshallable {
	final TransactionReference[] transactions;

	public MarshallableArrayOfTransactionReferences(TransactionReference[] transactions) {
		this.transactions = transactions.clone();
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		// we do not try to share repeated transaction references, since they do not occur in histories
		// and provision for sharing would just make the size of the histories larger
		context.writeCompactInt(transactions.length);
		for (TransactionReference reference: transactions)
			context.write(reference.getHashAsBytes());
	}

	/**
	 * Factory method that unmarshals an array of transaction references from the given stream.
	 * 
	 * @param context the unmarshalling context
	 * @return the array
	 * @throws IOException if the array could not be unmarshalled
	 * @throws ClassNotFoundException if the array could not be unmarshalled
	 */
	static MarshallableArrayOfTransactionReferences from(UnmarshallingContext context) throws IOException, ClassNotFoundException {
		int size = TransactionRequest.REQUEST_HASH_LENGTH;

		// we do not share repeated transaction references, since they do not occur in histories
		// and provision for sharing would just make the size of the histories larger
		return new MarshallableArrayOfTransactionReferences(context.readArray
				(_context -> new LocalTransactionReference(_context.readBytes(size, "inconsistent length of transaction reference")),
						TransactionReference[]::new));
	}
}