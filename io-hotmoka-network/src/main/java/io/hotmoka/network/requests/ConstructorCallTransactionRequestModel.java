package io.hotmoka.network.requests;

import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.network.signatures.ConstructorSignatureModel;
import io.hotmoka.network.values.StorageValueModel;

import java.math.BigInteger;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The model of a constructor call transaction.
 */
public class ConstructorCallTransactionRequestModel extends NonInitialTransactionRequestModel {
    public ConstructorSignatureModel constructor;
    public List<StorageValueModel> actuals;
    public String chainId;
    public String signature;

    public ConstructorCallTransactionRequestModel() {}


    /**
     * Builds the model from the request.
     * 
     * @param request the request to copy
     */
    public ConstructorCallTransactionRequestModel(ConstructorCallTransactionRequest request) {
    	super(request);

    	this.signature = Base64.getEncoder().encodeToString(request.getSignature());
    	this.chainId = request.chainId;
    	this.constructor = new ConstructorSignatureModel(request.constructor);
    	this.actuals = request.actuals().map(StorageValueModel::new).collect(Collectors.toList());
    }

    public ConstructorCallTransactionRequest toBean() {
    	return new ConstructorCallTransactionRequest(
        	decodeBase64(signature),
            caller.toBean(),
            new BigInteger(nonce),
            chainId,
            new BigInteger(gasLimit),
            new BigInteger(gasPrice),
            classpath.toBean(),
            constructor.toBean(),
            actuals.stream().map(StorageValueModel::toBean).toArray(StorageValue[]::new));
    }
}