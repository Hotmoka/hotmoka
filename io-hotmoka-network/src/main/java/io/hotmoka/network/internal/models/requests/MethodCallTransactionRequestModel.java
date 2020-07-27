package io.hotmoka.network.internal.models.requests;

import java.util.List;

import io.hotmoka.beans.types.StorageType;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.network.internal.models.signatures.MethodSignatureModel;
import io.hotmoka.network.internal.models.values.StorageValueModel;

public abstract class MethodCallTransactionRequestModel extends NonInitialTransactionRequestModel {
	private MethodSignatureModel method;
    private List<StorageValueModel> actuals;

    public void setMethod(MethodSignatureModel method) {
    	this.method = method;
    }

    protected List<StorageValueModel> getActuals() {
        return actuals;
    }

    public void setActuals(List<StorageValueModel> actuals) {
        this.actuals = actuals;
    }

    protected MethodSignatureModel getMethod() {
    	return method;
    }

    protected final StorageValue[] actualsToBeans(StorageType[] formals) {
    	StorageValue[] result = new StorageValue[formals.length];
    	int pos = 0;
    	for (StorageValueModel actual: actuals) {
    		result[pos] = actual.toBean(formals[pos]);
    		pos++;
    	}

    	return result;
    }
}