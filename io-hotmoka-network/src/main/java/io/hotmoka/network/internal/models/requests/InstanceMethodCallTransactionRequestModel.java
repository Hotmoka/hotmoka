package io.hotmoka.network.internal.models.requests;

import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.types.StorageType;
import io.hotmoka.network.internal.models.values.StorageReferenceModel;

public class InstanceMethodCallTransactionRequestModel extends MethodCallTransactionRequestModel {
	private StorageReferenceModel receiver;

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