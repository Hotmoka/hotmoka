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
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.NodeException;

/**
 * A node that provides access to a set of previously installed jars,
 */
@ThreadSafe
public interface JarsNode extends Node {

	/**
	 * Yields the references, in the store of the node, where the {@code it}th jar has been installed.
	 * 
	 * @param i the jar number
	 * @return the reference to the jar, in the store of the node
	 * @throws NoSuchElementException if the {@code i}th installed jar does not exist
	 * @throws NodeException if the node is not able to perform the operation
	 * @throws TimeoutException if no answer arrives before a time window
	 * @throws InterruptedException if the current thread is interrupted while waiting for an answer to arrive
	 */
	TransactionReference jar(int i) throws NoSuchElementException, NodeException, TimeoutException, InterruptedException;
}