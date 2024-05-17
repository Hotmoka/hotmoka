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

import io.hotmoka.helpers.api.GasHelper;
import io.hotmoka.helpers.internal.GasHelperImpl;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.NodeException;

/**
 * Providers of objects that help with gas operations.
 */
public abstract class GasHelpers {
	
	private GasHelpers() {}

	/**
	 * Yields an object that helps with gas operations.
	 * 
	 * @param node the node whose gas is considered
	 * @return the gas helper
	 * @throws InterruptedException if the current thread is interrupted while performing the operation
	 * @throws TimeoutException if the operation does not complete within the expected time window
	 * @throws NodeException if the node is not able to complete the operation
	 */
	public static GasHelper of(Node node) throws NodeException, TimeoutException, InterruptedException {
		return new GasHelperImpl(node);
	}
}