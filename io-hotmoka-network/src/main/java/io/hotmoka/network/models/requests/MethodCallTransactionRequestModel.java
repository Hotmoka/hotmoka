package io.hotmoka.network.models.requests;

import java.util.List;
import java.util.stream.Collectors;

import io.hotmoka.beans.requests.MethodCallTransactionRequest;
import io.hotmoka.beans.types.StorageType;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.network.models.signatures.MethodSignatureModel;
import io.hotmoka.network.models.values.StorageValueModel;

public abstract class MethodCallTransactionRequestModel extends NonInitialTransactionRequestModel {
	private MethodSignatureModel method;
    private List<StorageValueModel> actuals;

    /**
     * For Spring.
     */
    protected MethodCallTransactionRequestModel() {}

    /**
     * Builds the model from the request.
     * 
     * @param request the request to copy
     */
    public MethodCallTransactionRequestModel(MethodCallTransactionRequest request) {
    	super(request);

    	this.method = new MethodSignatureModel(request.method);
    	this.actuals = request.actuals().map(StorageValueModel::new).collect(Collectors.toList());
    }

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