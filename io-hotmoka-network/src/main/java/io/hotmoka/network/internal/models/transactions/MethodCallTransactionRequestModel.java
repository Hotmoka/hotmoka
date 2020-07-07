package io.hotmoka.network.internal.models.transactions;

import java.math.BigInteger;

public class MethodCallTransactionRequestModel extends ConstructorCallTransactionRequestModel {
    private String receiver;
    private BigInteger receiverProgressive;
    private boolean voidReturnType;
    private String methodName;
    private String returnType;

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public BigInteger getReceiverProgressive() {
        return receiverProgressive;
    }

    public void setReceiverProgressive(BigInteger receiverProgressive) {
        this.receiverProgressive = receiverProgressive;
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
}
