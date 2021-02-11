package io.hotmoka.service.models.requests;

import java.math.BigInteger;

import io.hotmoka.beans.requests.RedGreenGameteCreationTransactionRequest;
import io.hotmoka.service.models.values.TransactionReferenceModel;

public class RedGreenGameteCreationTransactionRequestModel extends InitialTransactionRequestModel {
	public String initialAmount;
    public String redInitialAmount;
	public String publicKey;
	public TransactionReferenceModel classpath;

    /**
     * Builds the model from the request.
     * 
     * @param request the request to copy
     */
    public RedGreenGameteCreationTransactionRequestModel(RedGreenGameteCreationTransactionRequest request) {
    	this.initialAmount = request.initialAmount.toString();
    	this.redInitialAmount = request.redInitialAmount.toString();
    	this.publicKey = request.publicKey;
    	this.classpath = new TransactionReferenceModel(request.classpath);
    }

    public RedGreenGameteCreationTransactionRequestModel() {}

    public RedGreenGameteCreationTransactionRequest toBean() {
    	return new RedGreenGameteCreationTransactionRequest(classpath.toBean(), new BigInteger(initialAmount), new BigInteger(redInitialAmount), publicKey);
    }
}