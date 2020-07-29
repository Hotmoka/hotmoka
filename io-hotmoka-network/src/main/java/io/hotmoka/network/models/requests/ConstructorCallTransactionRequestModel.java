package io.hotmoka.network.models.requests;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.types.StorageType;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.network.models.signatures.ConstructorSignatureModel;
import io.hotmoka.network.models.values.StorageValueModel;

/**
 * The model of a constructor call transaction.
 */
@Immutable
public class ConstructorCallTransactionRequestModel extends NonInitialTransactionRequestModel {
    public final ConstructorSignatureModel constructor;
    private final List<StorageValueModel> actuals;

    /**
     * Builds the model from the request.
     * 
     * @param request the request to copy
     */
    public ConstructorCallTransactionRequestModel(ConstructorCallTransactionRequest request) {
    	super(request);

    	this.constructor = new ConstructorSignatureModel(request.constructor);
    	this.actuals = request.actuals().map(StorageValueModel::new).collect(Collectors.toList());
    }

    public Stream<StorageValueModel> getActuals() {
    	return actuals.stream();
    }

    public ConstructorCallTransactionRequest toBean() {
    	ConstructorSignature constructorAsBean = constructor.toBean();

    	return new ConstructorCallTransactionRequest(
        	decodeBase64(signature),
            caller.toBean(),
            nonce,
            chainId,
            gasLimit,
            gasPrice,
            classpath.toBean(),
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