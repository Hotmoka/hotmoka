package io.hotmoka.network.models.requests;

import java.math.BigInteger;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.requests.GameteCreationTransactionRequest;
import io.hotmoka.network.models.values.TransactionReferenceModel;

@Immutable
public class GameteCreationTransactionRequestModel extends InitialTransactionRequestModel {
    public final String initialAmount;
    public final String publicKey;
    public final TransactionReferenceModel classpath;

    /**
     * Builds the model from the request.
     * 
     * @param request the request to copy
     */
    public GameteCreationTransactionRequestModel(GameteCreationTransactionRequest request) {
    	this.initialAmount = request.initialAmount.toString();
    	this.publicKey = request.publicKey;
    	this.classpath = new TransactionReferenceModel(request.classpath);
    }

    public GameteCreationTransactionRequest toBean() {
    	return new GameteCreationTransactionRequest(classpath.toBean(), new BigInteger(initialAmount), publicKey);
    }
}