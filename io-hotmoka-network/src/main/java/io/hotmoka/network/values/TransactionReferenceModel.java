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

package io.hotmoka.network.values;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.references.LocalTransactionReference;
import io.hotmoka.beans.references.TransactionReference;

/**
 * The model of a transaction reference.
 */
public class TransactionReferenceModel {

	/**
	 * The type of transaction.
	 */
	public String type;

	/**
	 * Used at least for local transactions.
	 */
	public String hash;

    /**
     * Builds the model of a transaction reference.
     * 
     * @param reference the transaction reference to copy
     */
    public TransactionReferenceModel(TransactionReference reference) {
    	if (reference instanceof LocalTransactionReference) {
    		this.type = "local";
    		this.hash = reference.getHash();
    	}
    	else
    		throw new InternalFailureException("unexpected transaction reference of type " + reference.getClass().getName());
    }

    public TransactionReferenceModel() {}

    /**
     * Yields the transaction reference having this model.
     * 
     * @return the transaction reference
     */
    public TransactionReference toBean() {
    	if (type == null)
    		throw new InternalFailureException("unexpected null transaction reference type");

    	switch (type) {
    	case "local": return new LocalTransactionReference(hash);
    	default:
    		throw new InternalFailureException("unexpected transaction reference type " + type);
    	}
    }
}