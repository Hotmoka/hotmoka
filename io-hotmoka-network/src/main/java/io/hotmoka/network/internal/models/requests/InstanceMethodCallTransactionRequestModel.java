package io.hotmoka.network.internal.models.requests;

import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.network.internal.models.values.StorageReferenceModel;
import io.hotmoka.network.internal.models.values.StorageValueModel;

public class InstanceMethodCallTransactionRequestModel extends MethodCallTransactionRequestModel {
	private StorageReferenceModel receiver;

    public void setReceiver(StorageReferenceModel receiver) {
        this.receiver = receiver;
    }

    public InstanceMethodCallTransactionRequest toBean() {
    	return new InstanceMethodCallTransactionRequest(
        	decodeBase64(getSignature()),
            getCaller().toBean(),
            getNonce(),
            getChainId(),
            getGasLimit(),
            getGasPrice(),
            getClasspath().toBean(),
            resolveMethodSignature(),
            receiver.toBean(),
            getActuals().stream().map(StorageValueModel::toBean).toArray(StorageValue[]::new));
    }
}