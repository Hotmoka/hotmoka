package io.hotmoka.network.internal.models.transactions;

import io.hotmoka.network.internal.models.storage.StorageModel;

import java.math.BigInteger;

public class TransactionRequestModel extends TransactionModel {
    private StorageModel caller;
    private BigInteger nonce;

    public BigInteger getNonce() {
        return nonce;
    }

    public void setNonce(BigInteger nonce) {
        this.nonce = nonce;
    }

    public StorageModel getCaller() {
        return caller;
    }

    public void setCaller(StorageModel caller) {
        this.caller = caller;
    }
}
