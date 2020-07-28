package io.hotmoka.network.models.requests;

import java.math.BigInteger;

import io.hotmoka.beans.requests.RedGreenGameteCreationTransactionRequest;
import io.hotmoka.network.models.values.TransactionReferenceModel;

public class RedGreenGameteCreationTransactionRequestModel extends InitialTransactionRequestModel {
	public final BigInteger initialAmount;
    public final BigInteger redInitialAmount;
	public final String publicKey;
	public final TransactionReferenceModel classpath;

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

    public RedGreenGameteCreationTransactionRequest toBean() {
    	return new RedGreenGameteCreationTransactionRequest(classpath.toBean(), initialAmount, redInitialAmount, publicKey);
    }
}