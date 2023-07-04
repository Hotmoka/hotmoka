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
import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.helpers.api.ManifestHelper;
import io.hotmoka.helpers.internal.ManifestHelperImpl;
import io.hotmoka.nodes.Node;

/**
 * Providers of helpers for accessing the manifest of a node.
 */
@ThreadSafe
public class ManifestHelpers {

	private ManifestHelpers() {}

	/**
	 * Yields an object that helps with the access to the manifest of a node.
	 * 
	 * @param node the node whose manifest is considered
	 * @return the helper
	 * @throws TransactionRejectedException if some transaction that installs the jars is rejected
	 * @throws TransactionException if some transaction that installs the jars fails
	 * @throws CodeExecutionException if some transaction that installs the jars throws an exception
	 */
	public static ManifestHelper of(Node node) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return new ManifestHelperImpl(node);
	}
}