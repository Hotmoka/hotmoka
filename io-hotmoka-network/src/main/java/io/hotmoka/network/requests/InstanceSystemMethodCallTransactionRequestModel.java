package io.hotmoka.network.requests;

import java.math.BigInteger;

import io.hotmoka.beans.requests.InstanceSystemMethodCallTransactionRequest;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.network.values.StorageReferenceModel;
import io.hotmoka.network.values.StorageValueModel;

public class InstanceSystemMethodCallTransactionRequestModel extends MethodCallTransactionRequestModel {
	public StorageReferenceModel receiver;

    /**
     * Builds the model from the request.
     * 
     * @param request the request to copy
     */
    public InstanceSystemMethodCallTransactionRequestModel(InstanceSystemMethodCallTransactionRequest request) {
    	super(request);

    	this.receiver = new StorageReferenceModel(request.receiver);
    }

    public InstanceSystemMethodCallTransactionRequestModel() {}

    public InstanceSystemMethodCallTransactionRequest toBean() {
    	return new InstanceSystemMethodCallTransactionRequest(
            caller.toBean(),
            new BigInteger(nonce),
            new BigInteger(gasLimit),
            classpath.toBean(),
            method.toBean(),
            receiver.toBean(),
            getActuals().map(StorageValueModel::toBean).toArray(StorageValue[]::new));
    }
}