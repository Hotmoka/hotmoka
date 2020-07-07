package io.hotmoka.network.internal.models.transactions;

import java.math.BigInteger;

public class TransactionRequestModel {
    private String caller;
    private BigInteger callerProgressive;
    private BigInteger nonce;

    public BigInteger getNonce() {
        return nonce;
    }

    public void setNonce(BigInteger nonce) {
        this.nonce = nonce;
    }

    public String getCaller() {
        return caller;
    }

    public void setCaller(String caller) {
        this.caller = caller;
    }

    public BigInteger getCallerProgressive() {
        return callerProgressive;
    }

    public void setCallerProgressive(BigInteger callerProgressive) {
        this.callerProgressive = callerProgressive;
    }
}
