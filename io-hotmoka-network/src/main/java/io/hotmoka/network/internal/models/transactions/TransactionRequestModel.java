package io.hotmoka.network.internal.models.transactions;

import io.hotmoka.network.internal.models.storage.StorageReferenceModel;

import java.math.BigInteger;

public class TransactionRequestModel extends TransactionModel {
    private String signature;
    private StorageReferenceModel caller;
    private BigInteger nonce;

    public BigInteger getNonce() {
        return nonce;
    }

    public void setNonce(BigInteger nonce) {
        this.nonce = nonce;
    }

    public StorageReferenceModel getCaller() {
        return caller;
    }

    public void setCaller(StorageReferenceModel caller) {
        this.caller = caller;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}
