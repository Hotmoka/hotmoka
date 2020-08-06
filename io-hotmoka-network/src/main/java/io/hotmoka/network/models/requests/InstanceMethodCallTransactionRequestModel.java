package io.hotmoka.network.models.requests;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.network.models.values.StorageReferenceModel;
import io.hotmoka.network.models.values.StorageValueModel;

import java.math.BigInteger;

@Immutable
public class InstanceMethodCallTransactionRequestModel extends MethodCallTransactionRequestModel {
	public final StorageReferenceModel receiver;

    /**
     * Builds the model from the request.
     * 
     * @param request the request to copy
     */
    public InstanceMethodCallTransactionRequestModel(InstanceMethodCallTransactionRequest request) {
    	super(request);

    	this.receiver = new StorageReferenceModel(request.receiver);
    }

    public InstanceMethodCallTransactionRequest toBean() {
    	return new InstanceMethodCallTransactionRequest(
        	decodeBase64(signature),
            caller.toBean(),
            new BigInteger(nonce),
            chainId,
            new BigInteger(gasLimit),
            new BigInteger(gasPrice),
            classpath.toBean(),
            method.toBean(),
            receiver.toBean(),
            getActuals().map(StorageValueModel::toBean).toArray(StorageValue[]::new));
    }
}