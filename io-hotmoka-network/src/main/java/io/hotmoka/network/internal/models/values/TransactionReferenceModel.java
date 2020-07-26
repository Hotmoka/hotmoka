package io.hotmoka.network.internal.models.values;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.references.LocalTransactionReference;
import io.hotmoka.beans.references.TransactionReference;

public class TransactionReferenceModel {
	private String type;
    private String hash;

    public TransactionReferenceModel() {}

    public TransactionReferenceModel(TransactionReference reference) {
    	if (reference instanceof LocalTransactionReference) {
    		this.type = "local";
    		this.hash = reference.getHash();
    	}
    	else
    		throw new InternalFailureException("unexpected transaction reference of type " + reference.getClass().getName());
    }

    public TransactionReference toBean() {
    	if (type == null)
    		throw new InternalFailureException("unexpected null transaction reference type");

    	switch (type) {
    	case "local": return new LocalTransactionReference(hash);
    	default:
    		throw new InternalFailureException("unexpected transaction reference type " + type);
    	}
    }
    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public void setType(String type) {
        this.type = type;
    }
}