package io.hotmoka.service.models.values;

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