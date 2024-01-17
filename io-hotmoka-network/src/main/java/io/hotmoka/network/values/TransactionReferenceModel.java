/*
Copyright 2021 Dinu Berinde and Fausto Spoto

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

import io.hotmoka.beans.TransactionReferences;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.crypto.Hex;
import io.hotmoka.crypto.HexConversionException;

/**
 * The model of a transaction reference.
 */
public class TransactionReferenceModel {

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
    	this.hash = Hex.toHexString(reference.getHash());
    }

    public TransactionReferenceModel() {}

    /**
     * Yields the transaction reference having this model.
     * 
     * @return the transaction reference
     */
    public TransactionReference toBean() {
    	try {
    		return TransactionReferences.of(Hex.fromHexString(hash));
    	}
    	catch (HexConversionException e) {
    		throw new IllegalArgumentException(e);
    	}
    }
}