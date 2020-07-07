package io.hotmoka.network.internal.models;

import java.math.BigInteger;
import java.util.List;

import io.hotmoka.network.internal.models.updates.Update;

public class State {
    private String transaction;
    private BigInteger progressive;
    private List<Update> updates;

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

    public List<Update> getUpdates() {
        return updates;
    }

    public void setUpdates(List<Update> updates) {
        this.updates = updates;
    }
}
