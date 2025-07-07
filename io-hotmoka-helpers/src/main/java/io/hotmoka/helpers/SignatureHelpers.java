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

import io.hotmoka.helpers.api.SignatureHelper;
import io.hotmoka.helpers.internal.SignatureHelperImpl;
import io.hotmoka.node.api.ClosedNodeException;
import io.hotmoka.node.api.Node;

/**
 * Providers of helpers to determine the signature algorithm to use for an externally owned account.
 */
public abstract class SignatureHelpers {
	private SignatureHelpers() {}

	/**
	 * Yields a signature helper for the given node.
	 * 
	 * @param node the node
	 * @return the signature helper
	 * @throws InterruptedException if the current thread has been interrupted
	 * @throws TimeoutException if the operation times out
	 * @throws ClosedNodeException if {@code node} is already closed
	 */
	public static SignatureHelper of(Node node) throws ClosedNodeException, TimeoutException, InterruptedException {
		return new SignatureHelperImpl(node);
	}
}