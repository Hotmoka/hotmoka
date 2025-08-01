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

package io.hotmoka.node.api;

import java.util.Objects;

import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;

/**
 * An exception stating that a storage reference cannot be found in a Hotmoka node.
 */
@SuppressWarnings("serial")
public class UnknownReferenceException extends Exception {

	/**
	 * Creates a new exception.
	 * 
	 * @param reference the reference that cannot be found
	 */
	public UnknownReferenceException(StorageReference reference) {
		super("Unknown storage reference " + reference);
	}

	/**
	 * Creates a new exception.
	 * 
	 * @param reference the reference that cannot be found
	 */
	public UnknownReferenceException(TransactionReference reference) {
		super("Unknown transaction reference " + reference);
	}

	/**
	 * Creates a new exception with the given message.
	 * 
	 * @param message the message
	 */
	public UnknownReferenceException(String message) { // called by reflection, do not remove
		super(Objects.requireNonNull(message, "message cannot be null"));
	}
}