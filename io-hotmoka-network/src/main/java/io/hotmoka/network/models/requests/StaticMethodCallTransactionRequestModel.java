package io.hotmoka.network.models.requests;

import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.types.StorageType;

public class StaticMethodCallTransactionRequestModel extends MethodCallTransactionRequestModel {

	/**
     * For Spring.
     */
    protected StaticMethodCallTransactionRequestModel() {}

    /**
     * Builds the model from the request.
     * 
     * @param request the request to copy
     */
    public StaticMethodCallTransactionRequestModel(InstanceMethodCallTransactionRequest request) {
    	super(request);
    }

    public StaticMethodCallTransactionRequest toBean() {
		MethodSignature methodAsBean = getMethod().toBean();

		return new StaticMethodCallTransactionRequest(
        	decodeBase64(getSignature()),
            getCaller().toBean(),
            getNonce(),
            getChainId(),
            getGasLimit(),
            getGasPrice(),
            getClasspath().toBean(),
            methodAsBean,
            actualsToBeans(methodAsBean.formals().toArray(StorageType[]::new)));
	}
}