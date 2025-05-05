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

import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;

import io.hotmoka.helpers.api.GasCost;
import io.hotmoka.helpers.internal.GasCostImpl;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.transactions.TransactionReference;

/**
 * Providers of gas costs incurred for the already occurred execution of requests.
 */
public abstract class GasCosts {

	private GasCosts() {}

	/**
	 * Yields the gas cost incurred for the already occurred execution of a set of requests.
	 * 
	 * @param node the node that executed the requests
	 * @param requests the requests
	 * @return the gas costs
	 * @throws InterruptedException if the execution gets interrupted
	 * @throws TimeoutException if no answer arrives within the expected time window
	 * @throws UnknownReferenceException if some request has not been processed by the node
	 * @throws NodeException if the node is not able to complete the operation correctly
	 * @throws NoSuchAlgorithmException if some cryptographic algorithm is not available
	 */
	public static GasCost of(Node node, TransactionRequest<?>... requests) throws NodeException, TimeoutException, InterruptedException, UnknownReferenceException, NoSuchAlgorithmException {
		return new GasCostImpl(node, requests);
	}

	/**
	 * Yields the gas cost incurred for the already occurred execution of a set of requests, identified by their references.
	 * 
	 * @param node the node that executed the requests
	 * @param references the references of the requests whose consumed gas must be computed
	 * @return the gas cost
	 * @throws InterruptedException if the execution gets interrupted
	 * @throws TimeoutException if no answer arrives within the expected time window
	 * @throws UnknownReferenceException if some request cannot be found in the store of the node
	 * @throws NodeException if the node is not able to complete the operation correctly
	 */
	public static GasCost of(Node node, TransactionReference... references) throws NodeException, TimeoutException, InterruptedException, UnknownReferenceException {
		return new GasCostImpl(node, references);
	}
}