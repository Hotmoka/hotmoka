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

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.helpers.api.ClassLoaderHelper;
import io.hotmoka.helpers.internal.ClassLoaderHelperImpl;
import io.hotmoka.node.api.Node;

/**
 * Providers of helpers for building class loaders for the jar installed at a given
 * transaction reference inside a node.
 */
public class ClassLoaderHelpers {

	private ClassLoaderHelpers() {}

	/**
	 * Yields a helper object for building class loaders for jars installed in the given node.
	 * 
	 * @param node the node
	 * @return the helper object
	 * @throws TransactionRejectedException if some transaction was rejected
	 * @throws TransactionException if some transaction failed
	 * @throws CodeExecutionException if some transaction generated an exception
	 */
	public static ClassLoaderHelper of(Node node) throws TransactionRejectedException, TransactionException, CodeExecutionException {
		return new ClassLoaderHelperImpl(node);
	}
}