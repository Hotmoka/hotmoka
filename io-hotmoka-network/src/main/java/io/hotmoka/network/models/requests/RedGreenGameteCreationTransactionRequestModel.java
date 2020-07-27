package io.hotmoka.network.models.requests;

import java.math.BigInteger;

import io.hotmoka.beans.requests.RedGreenGameteCreationTransactionRequest;
import io.hotmoka.network.models.values.TransactionReferenceModel;

public class RedGreenGameteCreationTransactionRequestModel extends InitialTransactionRequestModel {
	private BigInteger initialAmount;
    private BigInteger redInitialAmount;
	private String publicKey;
	private TransactionReferenceModel classpath;

	/**
     * For Spring.
     */
    public RedGreenGameteCreationTransactionRequestModel() {}

    /**
     * Builds the model from the request.
     * 
     * @param request the request to copy
     */
    public RedGreenGameteCreationTransactionRequestModel(RedGreenGameteCreationTransactionRequest request) {
    	this.initialAmount = request.initialAmount;
    	this.redInitialAmount = request.redInitialAmount;
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

    public void setRedInitialAmount(BigInteger redInitialAmount) {
        this.redInitialAmount = redInitialAmount;
    }

    public RedGreenGameteCreationTransactionRequest toBean() {
    	return new RedGreenGameteCreationTransactionRequest(classpath.toBean(), initialAmount, redInitialAmount, publicKey);
    }
}