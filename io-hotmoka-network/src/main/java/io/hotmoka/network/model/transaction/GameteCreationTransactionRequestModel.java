package io.hotmoka.network.model.transaction;

import java.math.BigInteger;

public class GameteCreationTransactionRequestModel {
    private BigInteger amount;
    private String publicKey;


    public BigInteger getAmount() {
        return amount;
    }

    public void setAmount(BigInteger amount) {
        this.amount = amount;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }
}
