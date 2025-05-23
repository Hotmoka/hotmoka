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

package io.hotmoka.moka.api.accounts;

import java.util.Optional;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.moka.api.GasCostOutput;
import io.hotmoka.node.api.values.StorageReference;

/**
 * The output of the {@code moka accounts send} command.
 */
@Immutable
public interface AccountsSendOutput extends GasCostOutput {

	/**
	 * Yields the account, from the accounts ledger, that has received the sent coins.
	 * 
	 * @return the account, from the accounts ledger, that has received the sent coins; this is missing if
	 *         the transaction has just been posted rather than added, or if the payment was not into a key
	 *         but was into a specific account
	 */
	Optional<StorageReference> getDestinationInAccountsLedger();
}