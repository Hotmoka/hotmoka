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

import java.util.Optional;

import io.hotmoka.moka.api.GasCostOutput;
import io.hotmoka.node.TransactionReferences;

/**
 * The JSON representation of the output of a command the reports the gas cost of a transaction in its output.
 */
public abstract class GasCostOutputJson {
	private final TransactionReferences.Json transaction;
	private final GasCostJson gasCost;
	private final String errorMessage;

	protected GasCostOutputJson(GasCostOutput output) {
		this.transaction = new TransactionReferences.Json(output.getTransaction());
		this.gasCost = output.getGasCost().map(GasCostJson::new).orElse(null);
		this.errorMessage = output.getErrorMessage().orElse(null);
	}

	public TransactionReferences.Json getTransaction() {
		return transaction;
	}

	public Optional<GasCostJson> getGasCost() {
		return Optional.ofNullable(gasCost);
	}

	public Optional<String> getErrorMessage() {
		return Optional.ofNullable(errorMessage);
	}
}