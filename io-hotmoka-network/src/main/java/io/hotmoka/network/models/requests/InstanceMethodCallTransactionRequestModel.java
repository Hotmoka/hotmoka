package io.hotmoka.network.models.requests;

import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.types.StorageType;
import io.hotmoka.network.models.values.StorageReferenceModel;

public class InstanceMethodCallTransactionRequestModel extends MethodCallTransactionRequestModel {
	private StorageReferenceModel receiver;

	/**
     * For Spring.
     */
    protected InstanceMethodCallTransactionRequestModel() {}

    /**
     * Builds the model from the request.
     * 
     * @param request the request to copy
     */
    public InstanceMethodCallTransactionRequestModel(InstanceMethodCallTransactionRequest request) {
    	super(request);

    	this.receiver = new StorageReferenceModel(request.receiver);
    }

    public void setReceiver(StorageReferenceModel receiver) {
        this.receiver = receiver;
    }

    public InstanceMethodCallTransactionRequest toBean() {
    	MethodSignature methodAsBean = getMethod().toBean();

    	return new InstanceMethodCallTransactionRequest(
        	decodeBase64(getSignature()),
            getCaller().toBean(),
            getNonce(),
            getChainId(),
            getGasLimit(),
            getGasPrice(),
            getClasspath().toBean(),
            methodAsBean,
            receiver.toBean(),
            actualsToBeans(methodAsBean.formals().toArray(StorageType[]::new)));
    }
}