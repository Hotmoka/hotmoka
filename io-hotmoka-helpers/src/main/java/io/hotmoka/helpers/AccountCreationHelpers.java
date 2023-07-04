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
import io.hotmoka.helpers.api.AccountCreationHelper;
import io.hotmoka.helpers.internal.AccountCreationHelperImpl;
import io.hotmoka.nodes.Node;

/**
 * Providers of objects that help with the creation of new accounts.
 */
public class AccountCreationHelpers {
	private AccountCreationHelpers() {}

	/**
	 * Yields an object that helps with the creation of new accounts.
	 * 
	 * @param node the node whose accounts are considered
	 * @return the helper object
	 * @throws CodeExecutionException if some transaction fails
	 * @throws TransactionException if some transaction fails
	 * @throws TransactionRejectedException if some transaction fails
	 */
	public static AccountCreationHelper of(Node node) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return new AccountCreationHelperImpl(node);
	}
}