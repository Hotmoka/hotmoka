/*
Copyright 2023 Fausto Spoto

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

package io.hotmoka.helpers;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.helpers.api.MintBurnHelper;
import io.hotmoka.helpers.internal.MintBurnHelperImpl;
import io.hotmoka.nodes.Node;

/**
 * Providers of helpers for minting and burning coins of an account in the accounts ledger
 * of the node. Only the gamete can do that and only if the node allows mint and burn from the gamete.
 */
public class MintBurnHelpers {

	private MintBurnHelpers() {}

	/**
	 * Yields a helper for minting and burning coins of an account.
	 * 
	 * @param node the node whose accounts are considered
	 * @return the helper
	 * @throws CodeExecutionException if some transaction fails
	 * @throws TransactionException if some transaction fails
	 * @throws TransactionRejectedException if some transaction fails
	 */
	public static MintBurnHelper of(Node node) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return new MintBurnHelperImpl(node);
	}
}