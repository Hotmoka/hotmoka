package io.hotmoka.network.models.requests;

import java.util.Base64;

public abstract class TransactionRequestModel {
    protected final byte[] decodeBase64(String what) {
    	return Base64.getDecoder().decode(what);
    }
}