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

import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.beans.api.values.StorageValue;
import io.hotmoka.node.api.CodeSupplier;
import io.hotmoka.node.api.Node;
import io.hotmoka.node.internal.nodes.CodeSupplierConstructor;
import io.hotmoka.node.internal.nodes.CodeSupplierMethod;

/**
 * Provider of futures of code executions in a Hotmoka node.
 */
public abstract class CodeSuppliers {

	private CodeSuppliers() {}

	/**
	 * Creates a future for the execution of a constructor, whose request has the given reference.
	 * 
	 * @param reference the reference to the request of the execution of the constructor
	 * @param node the node where the constructor is executed
	 * @return the future
	 */
	public static CodeSupplier<StorageReference> ofConstructor(TransactionReference reference, Node node) {
		return new CodeSupplierConstructor(reference, node);
	}

	/**
	 * Creates a future for the execution of a method, whose request has the given reference.
	 * 
	 * @param reference the reference to the request of the execution of the method
	 * @param node the node where the constructor is executed
	 * @return the future
	 */
	public static CodeSupplier<StorageValue> ofMethod(TransactionReference reference, Node node) {
		return new CodeSupplierMethod(reference, node);
	}
}