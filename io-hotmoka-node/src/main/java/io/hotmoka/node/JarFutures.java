/*
Copyright 2024 Fausto Spoto

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

package io.hotmoka.node;

import io.hotmoka.node.api.JarFuture;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.internal.nodes.JarFutureImpl;

/**
 * Provider of futures of jar installations in a Hotmoka node.
 */
public abstract class JarFutures {

	private JarFutures() {}

	/**
	 * Creates a future for the installation of a jar, whose request has the given reference.
	 * 
	 * @param reference the reference to the request of the installation of the jar
	 * @param node the node where the jar is installed
	 * @return the future
	 */
	public static JarFuture of(TransactionReference reference, Node node) {
		return new JarFutureImpl(reference, node);
	}
}