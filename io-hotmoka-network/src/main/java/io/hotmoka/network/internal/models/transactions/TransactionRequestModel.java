package io.hotmoka.network.internal.models.transactions;

import io.hotmoka.network.internal.models.storage.StorageModel;

import java.math.BigInteger;

public class TransactionRequestModel extends TransactionModel {
    private String signature;
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

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}
