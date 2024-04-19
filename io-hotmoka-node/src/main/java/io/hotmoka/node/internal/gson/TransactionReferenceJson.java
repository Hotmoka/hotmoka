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

package io.hotmoka.node.internal.gson;

import io.hotmoka.crypto.Hex;
import io.hotmoka.crypto.HexConversionException;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.websockets.beans.api.JsonRepresentation;

/**
 * The JSON representation of a {@link TransactionReference}.
 */
public abstract class TransactionReferenceJson implements JsonRepresentation<TransactionReference> {
	private final String hash;

	protected TransactionReferenceJson(TransactionReference reference) {
		this.hash = Hex.toHexString(reference.getHash());
	}

	@Override
	public TransactionReference unmap() throws HexConversionException {
		return TransactionReferences.of(Hex.fromHexString(hash));
	}
}