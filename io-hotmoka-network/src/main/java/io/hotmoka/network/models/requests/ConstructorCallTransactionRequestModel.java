package io.hotmoka.network.models.requests;

import java.util.List;
import java.util.stream.Collectors;

import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.types.StorageType;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.network.models.signatures.ConstructorSignatureModel;
import io.hotmoka.network.models.values.StorageValueModel;

public class ConstructorCallTransactionRequestModel extends NonInitialTransactionRequestModel {
    private ConstructorSignatureModel constructor;
    private List<StorageValueModel> actuals;

    /**
     * For Spring.
     */
    public void setConstructor(ConstructorSignatureModel constructor) {
        this.constructor = constructor;
    }

    public void setActuals(List<StorageValueModel> actuals) {
        this.actuals = actuals;
    }

    /**
     * For Spring.
     */
    public ConstructorCallTransactionRequestModel() {}

    public ConstructorCallTransactionRequestModel(ConstructorCallTransactionRequest request) {
    	super(request);

    	this.constructor = new ConstructorSignatureModel(request.constructor);
    	this.actuals = request.actuals().map(StorageValueModel::new).collect(Collectors.toList());
    }

    public ConstructorCallTransactionRequest toBean() {
    	ConstructorSignature constructorAsBean = constructor.toBean();

    	return new ConstructorCallTransactionRequest(
        	decodeBase64(getSignature()),
            getCaller().toBean(),
            getNonce(),
            getChainId(),
            getGasLimit(),
            getGasPrice(),
            getClasspath().toBean(),
            constructorAsBean,
            actualsToBeans(constructorAsBean.formals().toArray(StorageType[]::new)));
    }

    private StorageValue[] actualsToBeans(StorageType[] formals) {
    	StorageValue[] result = new StorageValue[formals.length];
    	int pos = 0;
    	for (StorageValueModel actual: actuals) {
    		result[pos] = actual.toBean(formals[pos]);
    		pos++;
    	}

    	return result;
    }
}