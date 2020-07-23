package io.hotmoka.network.internal.models.storage;

import java.math.BigInteger;

import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.network.json.JSONTransactionReference;

public class StorageReferenceModel {
    private String transaction;
    private BigInteger progressive;

    public StorageReferenceModel() {}

    public StorageReferenceModel(StorageReference input) {
    	transaction = input.transaction.getHash();
    	progressive = input.progressive;
    }

    public BigInteger getProgressive() {
        return progressive;
    }

    public void setProgressive(BigInteger progressive) {
        this.progressive = progressive;
    }

    public String getTransaction() {
        return transaction;
    }

    public void setTransaction(String hash) {
        this.transaction = hash;
    }

    public StorageReference toBean() {
    	return new StorageReference(JSONTransactionReference.fromJSON(transaction), progressive);
    }
}