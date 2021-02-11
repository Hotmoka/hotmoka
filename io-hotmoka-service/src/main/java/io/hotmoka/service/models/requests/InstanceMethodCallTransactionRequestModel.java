package io.hotmoka.service.models.requests;

import java.math.BigInteger;
import java.util.Base64;

import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.service.models.values.StorageReferenceModel;
import io.hotmoka.service.models.values.StorageValueModel;

public class InstanceMethodCallTransactionRequestModel extends MethodCallTransactionRequestModel {
	public StorageReferenceModel receiver;
    public String chainId;
	public String signature;

    /**
     * Builds the model from the request.
     * 
     * @param request the request to copy
     */
    public InstanceMethodCallTransactionRequestModel(InstanceMethodCallTransactionRequest request) {
    	super(request);

    	this.chainId = request.chainId;
    	this.signature = Base64.getEncoder().encodeToString(request.getSignature());
    	this.receiver = new StorageReferenceModel(request.receiver);
    }

    public InstanceMethodCallTransactionRequestModel() {}

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