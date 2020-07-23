package io.hotmoka.network.internal.models.updates;

import java.math.BigInteger;
import java.util.List;

public class StateModel {
    private String transaction;
    private BigInteger progressive;
    private List<UpdateModel> updates;

    public BigInteger getProgressive() {
        return progressive;
    }

    public void setProgressive(BigInteger progressive) {
        this.progressive = progressive;
    }

    public String getTransaction() {
        return transaction;
    }

    public void setTransaction(String transaction) {
        this.transaction = transaction;
    }

    public List<UpdateModel> getUpdates() {
        return updates;
    }

    public void setUpdates(List<UpdateModel> updates) {
        this.updates = updates;
    }
}