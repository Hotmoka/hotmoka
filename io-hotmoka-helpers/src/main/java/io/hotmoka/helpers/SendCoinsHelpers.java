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

import io.hotmoka.helpers.api.SendCoinsHelper;
import io.hotmoka.helpers.internal.SendCoinsHelperImpl;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;

/**
 * Providers of objects that help with sending coins to accounts.
 */
public class SendCoinsHelpers {

	private SendCoinsHelpers() {}

	/**
	 * Yields an object that helps with sending coins to accounts.
	 * 
	 * @param node the node whose accounts are considered
	 * @return the helper object
	 * @throws CodeExecutionException if some transaction fails
	 * @throws TransactionException if some transaction fails
	 * @throws TransactionRejectedException if some transaction fails
	 */
	public static SendCoinsHelper of(Node node) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return new SendCoinsHelperImpl(node);
	}
}