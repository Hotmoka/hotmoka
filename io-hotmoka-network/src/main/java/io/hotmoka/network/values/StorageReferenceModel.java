package io.hotmoka.network.values;

import java.math.BigInteger;

import io.hotmoka.beans.values.StorageReference;

public class StorageReferenceModel {
	public TransactionReferenceModel transaction;
    public String progressive;

    public StorageReferenceModel(StorageReference input) {
    	transaction = new TransactionReferenceModel(input.transaction);
    	progressive = input.progressive.toString();
    }

    public StorageReferenceModel() {}

    public StorageReference toBean() {
    	return new StorageReference(transaction.toBean(), new BigInteger(progressive));
    }
}