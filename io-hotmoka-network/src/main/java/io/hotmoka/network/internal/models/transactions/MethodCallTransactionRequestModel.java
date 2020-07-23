package io.hotmoka.network.internal.models.transactions;

import java.util.List;

import io.hotmoka.network.internal.models.storage.StorageReferenceModel;
import io.hotmoka.network.internal.models.storage.ValueModel;

public class MethodCallTransactionRequestModel extends NonInitialTransactionRequestModel {
	private String constructorType;
	private StorageReferenceModel receiver;
    private boolean voidReturnType;
    private String methodName;
    private String returnType;
    private List<ValueModel> values;

    public String getConstructorType() {
        return constructorType;
    }

    public void setConstructorType(String constructorType) {
        this.constructorType = constructorType;
    }

    public boolean isVoidReturnType() {
        return voidReturnType;
    }

    public void setVoidReturnType(boolean voidReturnType) {
        this.voidReturnType = voidReturnType;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public StorageReferenceModel getReceiver() {
        return receiver;
    }

    public void setReceiver(StorageReferenceModel receiver) {
        this.receiver = receiver;
    }

    public List<ValueModel> getValues() {
        return values;
    }

    public void setValues(List<ValueModel> values) {
        this.values = values;
    }
}