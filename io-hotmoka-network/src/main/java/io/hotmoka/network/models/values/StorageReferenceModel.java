package io.hotmoka.network.models.values;

import java.math.BigInteger;

import io.hotmoka.beans.values.StorageReference;

public class StorageReferenceModel {
    private TransactionReferenceModel transaction;
    private BigInteger progressive;

    public StorageReferenceModel() {}

    public StorageReferenceModel(StorageReference input) {
    	transaction = new TransactionReferenceModel(input.transaction);
    	progressive = input.progressive;
    }

    public BigInteger getProgressive() {
        return progressive;
    }

    public void setProgressive(BigInteger progressive) {
        this.progressive = progressive;
    }

    public TransactionReferenceModel getTransaction() {
    	return transaction;
    }

    public void setTransaction(TransactionReferenceModel transaction) {
        this.transaction = transaction;
    }

    public StorageReference toBean() {
    	return new StorageReference(transaction.toBean(), progressive);
    }
}