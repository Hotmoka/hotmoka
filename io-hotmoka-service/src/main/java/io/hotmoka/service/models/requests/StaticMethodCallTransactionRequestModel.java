package io.hotmoka.service.models.requests;

import java.math.BigInteger;
import java.util.Base64;

import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.service.models.values.StorageValueModel;

public class StaticMethodCallTransactionRequestModel extends MethodCallTransactionRequestModel {
    public String chainId;
	public String signature;

	/**
     * Builds the model from the request.
     * 
     * @param request the request to copy
     */
    public StaticMethodCallTransactionRequestModel(StaticMethodCallTransactionRequest request) {
    	super(request);

    	this.chainId = request.chainId;
    	this.signature = Base64.getEncoder().encodeToString(request.getSignature());
    }

    public StaticMethodCallTransactionRequestModel() {}

    public StaticMethodCallTransactionRequest toBean() {
		return new StaticMethodCallTransactionRequest(
        	decodeBase64(signature),
            caller.toBean(),
            new BigInteger(nonce),
            chainId,
            new BigInteger(gasLimit),
            new BigInteger(gasPrice),
            classpath.toBean(),
            method.toBean(),
            getActuals().map(StorageValueModel::toBean).toArray(StorageValue[]::new));
	}
}