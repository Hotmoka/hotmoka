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

import io.hotmoka.helpers.api.NonceHelper;
import io.hotmoka.helpers.internal.NonceHelperImpl;
import io.hotmoka.nodes.api.Node;

/**
 * Providers of objects that help with nonce operations.
 */
public class NonceHelpers {
	private NonceHelpers() {}

	/**
	 * Yields an object that helps with nonce operations.
	 * 
	 * @param node the node whose accounts are considered
	 * @return the nonce helper
	 */
	public static NonceHelper of(Node node) {
		return new NonceHelperImpl(node);
	}
}