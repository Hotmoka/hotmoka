package io.hotmoka.network.internal.models.transactions;

import java.math.BigInteger;
import java.util.List;

import io.hotmoka.network.internal.models.storage.StorageValueModel;

public class ConstructorCallTransactionRequestModel extends TransactionRequestModel {
    private String classType;
    private String chainId;
    private BigInteger gasLimit;
    private BigInteger gasPrice;
    private List<StorageValueModel> values;

    public String getClassType() {
        return classType;
    }

    public void setClassType(String classType) {
        this.classType = classType;
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

    public List<StorageValueModel> getValues() {
        return values;
    }

    public void setValues(List<StorageValueModel> values) {
        this.values = values;
    }
}
