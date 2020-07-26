package io.hotmoka.network.internal.models.requests;

import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.network.internal.models.storage.StorageValueModel;
import io.hotmoka.network.internal.util.StorageResolver;

public class StaticMethodCallTransactionRequestModel extends MethodCallTransactionRequestModel {

	public StaticMethodCallTransactionRequest toBean() {
		MethodSignature methodSignature = StorageResolver.resolveMethodSignature(this);

		return new StaticMethodCallTransactionRequest(
        	decodeBase64(getSignature()),
            getCaller().toBean(),
            getNonce(),
            getChainId(),
            getGasLimit(),
            getGasPrice(),
            getClasspath().toBean(),
            methodSignature,
            getActuals().stream().map(StorageValueModel::toBean).toArray(StorageValue[]::new));
	}
}