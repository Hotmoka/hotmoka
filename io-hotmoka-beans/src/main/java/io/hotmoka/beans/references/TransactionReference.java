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

package io.hotmoka.beans.references;

import java.io.IOException;
import java.io.Serializable;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.MarshallableBean;
import io.hotmoka.beans.UnmarshallingContext;

/**
 * A unique identifier for a transaction.
 */
@Immutable
public abstract class TransactionReference extends MarshallableBean implements Comparable<TransactionReference>, Serializable {
	private static final long serialVersionUID = 3206541167819020375L;

	/**
	 * Yields the hash of the request that generated the transaction.
	 * 
	 * @return the hash
	 */
	public abstract String getHash();

	/**
	 * Yields the hash of the request, as an array of bytes.
	 * 
	 * @return the hash
	 */
	public abstract byte[] getHashAsBytes();

	/**
	 * Factory method that unmarshals a transaction reference from the given stream.
	 * 
	 * @param context the unmarshalling context
	 * @return the transaction reference
	 * @throws IOException if the transaction reference could not be unmarshalled
     */
	public static TransactionReference from(UnmarshallingContext context) throws IOException {
		return context.readObject(TransactionReference.class);
	}
}