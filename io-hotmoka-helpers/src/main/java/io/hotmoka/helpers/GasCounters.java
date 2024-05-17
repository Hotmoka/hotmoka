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

import java.util.concurrent.TimeoutException;

import io.hotmoka.helpers.api.GasCounter;
import io.hotmoka.helpers.internal.GasCounterImpl;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.requests.TransactionRequest;

/**
 * Providers of counters of the gas consumed for the execution of a set of requests.
 */
public abstract class GasCounters {

	private GasCounters() {}

	/**
	 * Yields a counter of the gas consumed for the execution of a set of requests.
	 * 
	 * @param node the node that executed the requests
	 * @param requests the requests
	 * @return the gas counter
	 * @throws InterruptedException if the execution gets interrupted
	 * @throws TimeoutException if no answer arrives within the expected time window
	 * @throws UnknownReferenceException if some request has not been processed by the node
	 * @throws TransactionRejectedException if some request has been rejected by the node
	 * @throws NodeException if the node is not able to complete the operation correctly
	 */
	public static GasCounter of(Node node, TransactionRequest<?>... requests) throws NodeException, TimeoutException, InterruptedException, TransactionRejectedException, UnknownReferenceException {
		return new GasCounterImpl(node, requests);
	}
}