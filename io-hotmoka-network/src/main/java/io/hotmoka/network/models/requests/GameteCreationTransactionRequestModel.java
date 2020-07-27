package io.hotmoka.network.models.requests;

import java.math.BigInteger;

import io.hotmoka.beans.requests.GameteCreationTransactionRequest;
import io.hotmoka.network.models.values.TransactionReferenceModel;

public class GameteCreationTransactionRequestModel extends InitialTransactionRequestModel {
    private BigInteger initialAmount;
    private String publicKey;
    private TransactionReferenceModel classpath;

    /**
     * For Spring.
     */
    public GameteCreationTransactionRequestModel() {}

    /**
     * Builds the model from the request.
     * 
     * @param request the request to copy
     */
    public GameteCreationTransactionRequestModel(GameteCreationTransactionRequest request) {
    	this.initialAmount = request.initialAmount;
    	this.publicKey = request.publicKey;
    	this.classpath = new TransactionReferenceModel(request.classpath);
    }

    public void setClasspath(TransactionReferenceModel classpath) {
        this.classpath = classpath;
    }

    public void setInitialAmount(BigInteger initialAmount) {
        this.initialAmount = initialAmount;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public GameteCreationTransactionRequest toBean() {
    	return new GameteCreationTransactionRequest(classpath.toBean(), initialAmount, publicKey);
    }
}