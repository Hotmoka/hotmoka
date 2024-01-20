/*
Copyright 2021 Dinu Berinde and Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.network.requests;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.beans.api.requests.MethodCallTransactionRequest;
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

    	this.method = new MethodSignatureModel(request.getStaticTarget());
    	this.actuals = request.actuals().map(StorageValueModel::new).collect(Collectors.toList());
    }

    protected MethodCallTransactionRequestModel() {}

    public Stream<StorageValueModel> getActuals() {
    	return actuals.stream();
    }
}