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

import io.hotmoka.moka.api.accounts.AccountsSendOutput;
import io.hotmoka.moka.internal.accounts.Send;
import io.hotmoka.node.StorageValues;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import io.hotmoka.websockets.beans.api.JsonRepresentation;

/**
 * The JSON representation of the output of the {@code moka accounts send} command.
 */
public abstract class AccountsSendOutputJson extends GasCostOutputJson implements JsonRepresentation<AccountsSendOutput> {
	private final StorageValues.Json destinationInAccountsLedger;

	protected AccountsSendOutputJson(AccountsSendOutput output) {
		super(output);

		this.destinationInAccountsLedger = output.getDestinationInAccountsLedger().map(StorageValues.Json::new).orElse(null);
	}

	public Optional<StorageValues.Json> getDestinationInAccountsLedger() {
		return Optional.ofNullable(destinationInAccountsLedger);
	}

	@Override
	public AccountsSendOutput unmap() throws InconsistentJsonException {
		return new Send.Output(this);
	}
}