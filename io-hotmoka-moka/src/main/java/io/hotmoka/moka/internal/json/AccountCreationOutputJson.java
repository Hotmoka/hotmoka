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

import io.hotmoka.moka.api.AccountCreationOutput;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionReferences;

/**
 * The JSON representation of the output of the account creation commands.
 */
public abstract class AccountCreationOutputJson extends GasCostOutputJson {
	private final TransactionReferences.Json transaction; // TODO: remove, it's the transaction of account
	private final StorageValues.Json account;
	private final String file;

	protected AccountCreationOutputJson(AccountCreationOutput output) {
		super(output);

		this.transaction = new TransactionReferences.Json(output.getTransaction());
		this.account = new StorageValues.Json(output.getAccount());
		this.file = output.getFile().map(Object::toString).orElse(null);
	}

	public TransactionReferences.Json getTransaction() {
		return transaction;
	}

	public StorageValues.Json getAccount() {
		return account;
	}

	public String getFile() {
		return file;
	}
}