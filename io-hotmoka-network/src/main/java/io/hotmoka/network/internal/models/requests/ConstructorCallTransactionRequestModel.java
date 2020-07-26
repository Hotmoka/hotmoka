package io.hotmoka.network.internal.models.requests;

import java.util.List;

import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.network.internal.models.storage.ValueModel;
import io.hotmoka.network.internal.util.StorageResolver;
import io.hotmoka.network.json.JSONTransactionReference;

public class ConstructorCallTransactionRequestModel extends NonInitialTransactionRequestModel {
    private String constructorType;
    private List<ValueModel> values;

    public void setConstructorType(String constructorType) {
        this.constructorType = constructorType;
    }

    public void setValues(List<ValueModel> values) {
        this.values = values;
    }

    public ConstructorCallTransactionRequest toBean() {
    	ConstructorSignature constructor = new ConstructorSignature(constructorType, StorageResolver.resolveStorageTypes(values));
        StorageValue[] actuals = StorageResolver.resolveStorageValues(values);

        return new ConstructorCallTransactionRequest(
        	decodeBase64(getSignature()),
            getCaller().toBean(),
            getNonce(),
            getChainId(),
            getGasLimit(),
            getGasPrice(),
            JSONTransactionReference.fromJSON(getClasspath()),
            constructor,
            actuals);
    }
}