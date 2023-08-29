/*
Copyright 2021 Fausto Spoto

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

package io.hotmoka.node.api;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;

/**
 * The future of a transaction that stores a jar in a node.
 */
public interface JarSupplier {

	/**
	 * Yields the reference of the request of the transaction.
	 * 
	 * @return the reference
	 */
	TransactionReference getReferenceOfRequest();

	/**
	 * Waits if necessary for the transaction to complete, and then retrieves its result.
	 *
	 * @return the reference to the transaction, that can be used to refer to the jar in a class path or as future dependency of other jars
	 * @throws TransactionRejectedException if the transaction could not be executed and the store of the node remained unchanged
	 * @throws TransactionException if the transaction could be executed and the store of the node has been expanded with a failed transaction
	 */
	TransactionReference get() throws TransactionRejectedException, TransactionException;
}