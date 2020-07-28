package io.hotmoka.network.models.requests;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.signatures.MethodSignature;

@Immutable
public class StaticMethodCallTransactionRequestModel extends MethodCallTransactionRequestModel {

    /**
     * Builds the model from the request.
     * 
     * @param request the request to copy
     */
    public StaticMethodCallTransactionRequestModel(InstanceMethodCallTransactionRequest request) {
    	super(request);
    }

    public StaticMethodCallTransactionRequest toBean() {
		MethodSignature methodAsBean = method.toBean();

		return new StaticMethodCallTransactionRequest(
        	decodeBase64(signature),
            caller.toBean(),
            nonce,
            chainId,
            gasLimit,
            gasPrice,
            classpath.toBean(),
            methodAsBean,
            actualsToBeans(methodAsBean));
	}
}