package io.hotmoka.network.model.transaction;

import java.math.BigInteger;

public class MethodCallTransactionRequestModel extends ConstructorCallTransactionRequestModel {
    private String receiver;
    private BigInteger receiverProgressive;

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
}
