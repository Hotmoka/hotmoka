package io.hotmoka.network.internal.models.transactions;

import java.math.BigInteger;

public class RGGameteCreationTransactionRequestModel extends GameteCreationTransactionRequestModel {
    private BigInteger redAmount;

    public BigInteger getRedAmount() {
        return redAmount;
    }

    public void setRedAmount(BigInteger redAmount) {
        this.redAmount = redAmount;
    }
}
