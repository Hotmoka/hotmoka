package io.hotmoka.network.models.requests;

import java.math.BigInteger;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.requests.TransferTransactionRequest;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.LongValue;
import io.hotmoka.beans.values.StorageValue;

@Immutable
public class TransferTransactionRequestModel extends InstanceMethodCallTransactionRequestModel {

    /**
     * Builds the model from the request.
     * 
     * @param request the request to copy
     */
    public TransferTransactionRequestModel(TransferTransactionRequest request) {
    	super(request);
    }

    @Override
    public TransferTransactionRequest toBean() {
    	StorageValue arg = getActuals().findFirst().get().toBean();

    	if (arg instanceof IntValue)
    		return new TransferTransactionRequest(
    			decodeBase64(signature),
    			caller.toBean(),
    			new BigInteger(nonce),
    			chainId,
    			new BigInteger(gasPrice),
    			classpath.toBean(),
    			receiver.toBean(),
    			((IntValue) arg).value);
    	else if (arg instanceof LongValue)
    		return new TransferTransactionRequest(
    			decodeBase64(signature),
    			caller.toBean(),
    			new BigInteger(nonce),
    			chainId,
    			new BigInteger(gasPrice),
    			classpath.toBean(),
    			receiver.toBean(),
    			((LongValue) arg).value);
    	else if (arg instanceof BigIntegerValue)
    		return new TransferTransactionRequest(
    			decodeBase64(signature),
    			caller.toBean(),
    			new BigInteger(nonce),
    			chainId,
    			new BigInteger(gasPrice),
    			classpath.toBean(),
    			receiver.toBean(),
    			((BigIntegerValue) arg).value);
    	else
    		throw new InternalFailureException("unexpected value for transfer amount");
    }
}