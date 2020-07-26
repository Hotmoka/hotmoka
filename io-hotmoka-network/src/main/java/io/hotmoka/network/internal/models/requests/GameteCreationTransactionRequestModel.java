package io.hotmoka.network.internal.models.requests;

import java.math.BigInteger;

import io.hotmoka.beans.requests.GameteCreationTransactionRequest;
import io.hotmoka.network.internal.models.values.TransactionReferenceModel;

public class GameteCreationTransactionRequestModel extends InitialTransactionRequestModel {
    private BigInteger amount;
    private String publicKey;
    private TransactionReferenceModel classpath;

    public void setClasspath(TransactionReferenceModel classpath) {
        this.classpath = classpath;
    }

    public void setAmount(BigInteger amount) {
        this.amount = amount;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public GameteCreationTransactionRequest toBean() {
    	return new GameteCreationTransactionRequest(classpath.toBean(), amount, publicKey);
    }
}