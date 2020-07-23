package io.hotmoka.network.internal.models.transactions;

import java.math.BigInteger;

import io.hotmoka.beans.requests.RedGreenGameteCreationTransactionRequest;
import io.hotmoka.network.json.JSONTransactionReference;

public class RGGameteCreationTransactionRequestModel extends InitialTransactionRequestModel {
	private BigInteger amount;
    private BigInteger redAmount;
	private String publicKey;
	private String classpath;

    public String getClasspath() {
        return classpath;
    }

    public void setClasspath(String classpath) {
        this.classpath = classpath;
    }

    public BigInteger getAmount() {
        return amount;
    }

    public void setAmount(BigInteger amount) {
        this.amount = amount;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public BigInteger getRedAmount() {
        return redAmount;
    }

    public void setRedAmount(BigInteger redAmount) {
        this.redAmount = redAmount;
    }

    public RedGreenGameteCreationTransactionRequest toBean() {
    	return new RedGreenGameteCreationTransactionRequest(
    		JSONTransactionReference.fromJSON(getClasspath()),
    		getAmount(),
            redAmount,
            getPublicKey());
    }
}