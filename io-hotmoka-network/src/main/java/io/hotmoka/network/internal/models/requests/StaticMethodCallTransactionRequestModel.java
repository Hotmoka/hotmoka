package io.hotmoka.network.internal.models.requests;

import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.network.internal.util.StorageResolver;
import io.hotmoka.network.json.JSONTransactionReference;

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
            JSONTransactionReference.fromJSON(getClasspath()),
            methodSignature,
            StorageResolver.resolveStorageValues(getValues()));
	}
}