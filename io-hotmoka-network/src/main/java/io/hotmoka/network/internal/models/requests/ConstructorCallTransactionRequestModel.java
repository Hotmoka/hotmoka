package io.hotmoka.network.internal.models.requests;

import java.util.List;

import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.network.internal.models.storage.StorageValueModel;
import io.hotmoka.network.internal.util.StorageResolver;

public class ConstructorCallTransactionRequestModel extends NonInitialTransactionRequestModel {
    private String constructorType;
    private List<StorageValueModel> actuals;

    public ConstructorCallTransactionRequestModel() {
    	System.out.println("building ConstructorCallTransactionRequestModel");
	}

    public void setConstructorType(String constructorType) {
    	System.out.println("setting constructor type to " + constructorType);
        this.constructorType = constructorType;
    }

    public void setActuals(List<StorageValueModel> actuals) {
    	System.out.println("setting actuals to " + actuals);
        this.actuals = actuals;
    }

    public ConstructorCallTransactionRequest toBean() {
    	ConstructorSignature constructor = new ConstructorSignature(constructorType, StorageResolver.resolveStorageTypes(actuals));
        return new ConstructorCallTransactionRequest(
        	decodeBase64(getSignature()),
            getCaller().toBean(),
            getNonce(),
            getChainId(),
            getGasLimit(),
            getGasPrice(),
            getClasspath().toBean(),
            constructor,
            actuals.stream().map(StorageValueModel::toBean).toArray(StorageValue[]::new));
    }
}