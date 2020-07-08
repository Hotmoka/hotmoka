package io.hotmoka.network.internal.models.transactions;

import java.math.BigInteger;

public class GameteCreationTransactionRequestModel extends TransactionModel {
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
