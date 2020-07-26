package io.hotmoka.network.internal.models.requests;

import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.network.internal.models.values.StorageValueModel;

public class StaticMethodCallTransactionRequestModel extends MethodCallTransactionRequestModel {

	public StaticMethodCallTransactionRequest toBean() {
		return new StaticMethodCallTransactionRequest(
        	decodeBase64(getSignature()),
            getCaller().toBean(),
            getNonce(),
            getChainId(),
            getGasLimit(),
            getGasPrice(),
            getClasspath().toBean(),
            resolveMethodSignature(),
            getActuals().stream().map(StorageValueModel::toBean).toArray(StorageValue[]::new));
	}
}