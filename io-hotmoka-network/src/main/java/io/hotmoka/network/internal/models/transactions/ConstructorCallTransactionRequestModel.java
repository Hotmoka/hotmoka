package io.hotmoka.network.internal.models.transactions;

import java.math.BigInteger;
import java.util.List;

import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.network.internal.models.storage.ValueModel;
import io.hotmoka.network.internal.util.StorageResolver;
import io.hotmoka.network.json.JSONTransactionReference;

public class ConstructorCallTransactionRequestModel extends TransactionRequestModel {
    private String constructorType;
    private String chainId;
    private BigInteger gasLimit;
    private BigInteger gasPrice;
    private List<ValueModel> values;

    public String getConstructorType() {
        return constructorType;
    }

    public void setConstructorType(String constructorType) {
        this.constructorType = constructorType;
    }

    public String getChainId() {
        return chainId;
    }

    public void setChainId(String chainId) {
        this.chainId = chainId;
    }

    public BigInteger getGasLimit() {
        return gasLimit;
    }

    public void setGasLimit(BigInteger gasLimit) {
        this.gasLimit = gasLimit;
    }

    public BigInteger getGasPrice() {
        return gasPrice;
    }

    public void setGasPrice(BigInteger gasPrice) {
        this.gasPrice = gasPrice;
    }

    public List<ValueModel> getValues() {
        return values;
    }

    public void setValues(List<ValueModel> values) {
        this.values = values;
    }

    public ConstructorCallTransactionRequest toBean() {
    	byte[] signature = StorageResolver.decodeBase64(getSignature());
        StorageReference caller = StorageResolver.resolveStorageReference(getCaller());
        ConstructorSignature constructor = new ConstructorSignature(getConstructorType(), StorageResolver.resolveStorageTypes(getValues()));
        StorageValue[] actuals = StorageResolver.resolveStorageValues(getValues());
        TransactionReference classpath = JSONTransactionReference.fromJSON(getClasspath());

        return new ConstructorCallTransactionRequest(
        	signature,
            caller,
            getNonce(),
            getChainId(),
            getGasLimit(),
            getGasPrice(),
            classpath,
            constructor,
            actuals);
    }
}