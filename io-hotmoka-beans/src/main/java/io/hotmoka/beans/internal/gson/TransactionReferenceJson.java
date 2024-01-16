/*
Copyright 2024 Fausto Spoto

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

package io.hotmoka.beans.internal.gson;

import io.hotmoka.beans.TransactionReferences;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.websockets.beans.api.JsonRepresentation;

/**
 * The JSON representation of a {@link TransactionReference}.
 */
public abstract class TransactionReferenceJson implements JsonRepresentation<TransactionReference> {
	private final String hash;

	protected TransactionReferenceJson(TransactionReference reference) {
		this.hash = reference.getHash();
	}

	@Override
	public TransactionReference unmap() throws IllegalArgumentException {
		return TransactionReferences.of(hash);
	}
}