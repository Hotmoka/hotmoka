package io.hotmoka.network.models.values;

import java.math.BigInteger;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.values.StorageReference;

@Immutable
public class StorageReferenceModel {
	public final TransactionReferenceModel transaction;
    public final String progressive;

    public StorageReferenceModel(StorageReference input) {
    	transaction = new TransactionReferenceModel(input.transaction);
    	progressive = input.progressive.toString();
    }

    public StorageReference toBean() {
    	return new StorageReference(transaction.toBean(), new BigInteger(progressive));
    }
}