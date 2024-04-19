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

import io.hotmoka.annotations.ThreadSafe;
import io.hotmoka.helpers.api.GasCounter;
import io.hotmoka.helpers.internal.GasCounterImpl;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.requests.TransactionRequest;

/**
 * Providers of counters of the gas consumed for the execution of a set of requests.
 */
@ThreadSafe
public class GasCounters {

	private GasCounters() {}

	/**
	 * Yields a counter of the gas consumed for the execution of a set of requests.
	 * 
	 * @param node the node that executed the requests
	 * @param requests the requests
	 * @return the gas counter
	 */
	public static GasCounter of(Node node, TransactionRequest<?>... requests) {
		return new GasCounterImpl(node, requests);
	}
}