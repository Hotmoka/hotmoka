package io.hotmoka.network.internal.models.transactions;

import java.util.Base64;

public abstract class TransactionModel {
    protected final byte[] decodeBase64(String what) {
    	return Base64.getDecoder().decode(what);
    }
}