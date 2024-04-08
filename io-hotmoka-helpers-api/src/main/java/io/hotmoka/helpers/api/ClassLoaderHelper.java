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

package io.hotmoka.helpers.api;

import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.verification.api.TakamakaClassLoader;

/**
 * A helper object for building class loaders for the jar installed at a given
 * transaction reference inside a node.
 */
@ThreadSafe
public interface ClassLoaderHelper {

	/**
	 * Yields the class loader for the jar installed at the given reference
	 * (including its dependencies).
	 * 
	 * @param jar the reference inside the node
	 * @return the class loader
	 * @throws ClassNotFoundException if some class of the Takamaka runtime cannot be loaded
	 * @throws TransactionRejectedException if some transaction was rejected
	 * @throws TransactionException if some transaction failed
	 * @throws CodeExecutionException if some transaction generated an exception
	 * @throws NodeException if the node is not able to perform the operation
	 * @throws TimeoutException if no answer arrives before a time window
	 * @throws InterruptedException if the current thread is interrupted while waiting for an answer to arrive
	 */
	TakamakaClassLoader classloaderFor(TransactionReference jar) throws TransactionRejectedException, TransactionException, CodeExecutionException, ClassNotFoundException, NodeException, TimeoutException, InterruptedException, NoSuchElementException;
}