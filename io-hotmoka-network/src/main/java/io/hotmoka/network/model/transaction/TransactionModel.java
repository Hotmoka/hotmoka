package io.hotmoka.network.model.transaction;

import java.math.BigInteger;

public class TransactionModel {
    private String hash;
    private BigInteger progressive;

    public BigInteger getProgressive() {
        return progressive;
    }

    public void setProgressive(BigInteger progressive) {
        this.progressive = progressive;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }
}
