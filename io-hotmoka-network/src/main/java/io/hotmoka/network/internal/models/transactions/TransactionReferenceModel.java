package io.hotmoka.network.internal.models.transactions;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.references.LocalTransactionReference;
import io.hotmoka.beans.references.TransactionReference;

public class TransactionReferenceModel {
    private String hash;

    public TransactionReferenceModel() {}

    public TransactionReferenceModel(TransactionReference reference) {
    	if (reference instanceof LocalTransactionReference)
    		this.hash = reference.getHash();
    	else
    		throw new InternalFailureException("unexpected transaction reference of type " + reference.getClass().getName());
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }
}