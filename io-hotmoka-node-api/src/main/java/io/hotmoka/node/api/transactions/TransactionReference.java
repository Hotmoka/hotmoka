/*
Copyright 2021 Fausto Spoto

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

package io.hotmoka.node.api.transactions;

import java.io.Serializable;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.marshalling.api.Marshallable;

/**
 * A unique identifier for a transaction.
 */
@Immutable
public interface TransactionReference extends Marshallable, Serializable, Comparable<TransactionReference> {

	/**
	 * The length of the hash of a transaction reference.
	 */
	public final static int REQUEST_HASH_LENGTH = 32;

	/**
	 * Yields the hash of the request.
	 * 
	 * @return the hash
	 */
	byte[] getHash();

	@Override
	boolean equals(Object other);

	@Override
	int hashCode();

	@Override
	String toString();
}