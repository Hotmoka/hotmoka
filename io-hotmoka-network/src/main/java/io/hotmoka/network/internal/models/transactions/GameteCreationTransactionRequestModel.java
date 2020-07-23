package io.hotmoka.network.internal.models.transactions;

import java.math.BigInteger;

import io.hotmoka.beans.requests.GameteCreationTransactionRequest;
import io.hotmoka.network.json.JSONTransactionReference;

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

    public GameteCreationTransactionRequest toBean() {
    	return new GameteCreationTransactionRequest(JSONTransactionReference.fromJSON(getClasspath()), amount, publicKey);
    }
}