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

import java.util.stream.Stream;

import io.hotmoka.moka.api.accounts.AccountsExportOutput;
import io.hotmoka.moka.internal.accounts.Export;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import io.hotmoka.websockets.beans.api.JsonRepresentation;

/**
 * The JSON representation of the output of the {@code moka keys export} command.
 */
public abstract class AccountsExportOutputJson implements JsonRepresentation<AccountsExportOutput> {
	private final String[] bip39Words;

	protected AccountsExportOutputJson(AccountsExportOutput output) {
		this.bip39Words = output.getBip39Words().toArray(String[]::new);
	}

	public Stream<String> getBip39Words() {
		return Stream.of(bip39Words);
	}

	@Override
	public AccountsExportOutput unmap() throws InconsistentJsonException {
		return new Export.Output(this);
	}
}