package io.hotmoka.network.internal.models.transactions;

import io.hotmoka.network.internal.models.storage.StorageReferenceModel;

public class MethodCallTransactionRequestModel extends ConstructorCallTransactionRequestModel {
    private StorageReferenceModel receiver;
    private boolean voidReturnType;
    private String methodName;
    private String returnType;


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
}