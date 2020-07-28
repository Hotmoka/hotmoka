package io.hotmoka.network.models.requests;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.network.models.values.StorageReferenceModel;

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
    	MethodSignature methodAsBean = method.toBean();

    	return new InstanceMethodCallTransactionRequest(
        	decodeBase64(signature),
            caller.toBean(),
            nonce,
            chainId,
            gasLimit,
            gasPrice,
            classpath.toBean(),
            methodAsBean,
            receiver.toBean(),
            actualsToBeans(methodAsBean));
    }
}