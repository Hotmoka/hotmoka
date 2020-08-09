package io.hotmoka.network.models.requests;

import java.math.BigInteger;

import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.network.models.values.StorageValueModel;

public class StaticMethodCallTransactionRequestModel extends MethodCallTransactionRequestModel {

    /**
     * Builds the model from the request.
     * 
     * @param request the request to copy
     */
    public StaticMethodCallTransactionRequestModel(StaticMethodCallTransactionRequest request) {
    	super(request);
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