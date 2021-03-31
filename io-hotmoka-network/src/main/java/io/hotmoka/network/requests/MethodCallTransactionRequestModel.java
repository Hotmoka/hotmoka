package io.hotmoka.network.requests;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.beans.requests.MethodCallTransactionRequest;
import io.hotmoka.network.signatures.MethodSignatureModel;
import io.hotmoka.network.values.StorageValueModel;

/**
 * The model of a method call transaction request.
 */
public abstract class MethodCallTransactionRequestModel extends NonInitialTransactionRequestModel {
	public MethodSignatureModel method;
    private List<StorageValueModel> actuals;

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

    protected MethodCallTransactionRequestModel() {}

    public Stream<StorageValueModel> getActuals() {
    	return actuals.stream();
    }
}