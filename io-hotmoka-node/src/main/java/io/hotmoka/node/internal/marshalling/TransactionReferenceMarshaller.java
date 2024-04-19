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

package io.hotmoka.node.internal.marshalling;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.hotmoka.marshalling.AbstractObjectMarshaller;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.node.api.transactions.TransactionReference;

/**
 * Knowledge about how a transaction reference can be marshalled.
 */
class TransactionReferenceMarshaller extends AbstractObjectMarshaller<TransactionReference> {
	
	private final Map<TransactionReference, Integer> memory = new HashMap<>();

	TransactionReferenceMarshaller() {
		super(TransactionReference.class);
	}

	@Override
	public void write(TransactionReference transaction, MarshallingContext context) throws IOException {
		Integer index = memory.get(transaction);
		if (index != null) {
			if (index < 254)
				context.writeByte(index);
			else {
				context.writeByte(254);
				context.writeInt(index);
			}
		}
		else {
			int next = memory.size();
			if (next == Integer.MAX_VALUE) // irrealistic
				throw new IllegalStateException("Too many transaction references in the same context");

			memory.put(transaction, next);

			context.writeByte(255);
			context.writeBytes(transaction.getHash());
		}
	}
}