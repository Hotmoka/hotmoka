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

package io.hotmoka.moka.internal.json;

import io.hotmoka.moka.GasCosts;
import io.hotmoka.moka.api.objects.ObjectsCreateOutput;
import io.hotmoka.moka.internal.objects.Create;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import io.hotmoka.websockets.beans.api.JsonRepresentation;

/**
 * The JSON representation of the output of the {@code moka objects create} command.
 */
public class ObjectsCreateOutputJson implements JsonRepresentation<ObjectsCreateOutput> {
	private final TransactionReferences.Json transaction;
	private final StorageValues.Json object;
	private final GasCosts.Json gasCost;
	private final String errorMessage;

	protected ObjectsCreateOutputJson(ObjectsCreateOutput output) {
		this.transaction = new TransactionReferences.Json(output.getTransaction());
		this.object = output.getObject().map(StorageValues.Json::new).orElse(null);
		this.gasCost = output.getGasCost().map(GasCosts.Json::new).orElse(null);
		this.errorMessage = output.getErrorMessage().orElse(null);
	}

	public TransactionReferences.Json getTransaction() {
		return transaction;
	}

	public StorageValues.Json getObject() {
		return object;
	}

	public GasCosts.Json getGasCost() {
		return gasCost;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	@Override
	public ObjectsCreateOutput unmap() throws InconsistentJsonException {
		return new Create.Output(this);
	}
}