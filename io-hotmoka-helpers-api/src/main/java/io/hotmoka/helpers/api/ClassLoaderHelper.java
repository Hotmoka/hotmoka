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

import java.util.concurrent.TimeoutException;

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.verification.api.TakamakaClassLoader;

/**
 * A helper object for building class loaders for the jar installed at a given
 * transaction reference inside a node.
 */
@ThreadSafe
public interface ClassLoaderHelper {

	/**
	 * Yields the class loader for the jar installed at the given reference (including its dependencies).
	 * 
	 * @param jar the reference inside the node
	 * @return the class loader
	 * @throws NodeException if the node is not able to perform the operation
	 * @throws TimeoutException if no answer arrives before a time window
	 * @throws InterruptedException if the current thread is interrupted while waiting for an answer to arrive
	 * @throws UnknownReferenceException if {@code jar} is not found in store or did not install a jar
	 */
	TakamakaClassLoader classloaderFor(TransactionReference jar) throws NodeException, TimeoutException, InterruptedException, UnknownReferenceException, ClassNotFoundException;
}