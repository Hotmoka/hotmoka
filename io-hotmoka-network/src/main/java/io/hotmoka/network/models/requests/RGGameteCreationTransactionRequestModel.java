package io.hotmoka.network.models.requests;

import java.math.BigInteger;

import io.hotmoka.beans.requests.RedGreenGameteCreationTransactionRequest;
import io.hotmoka.network.models.values.TransactionReferenceModel;

public class RGGameteCreationTransactionRequestModel extends InitialTransactionRequestModel {
	private BigInteger amount;
    private BigInteger redAmount;
	private String publicKey;
	private TransactionReferenceModel classpath;

    public void setClasspath(TransactionReferenceModel classpath) {
        this.classpath = classpath;
    }

    public void setAmount(BigInteger amount) {
        this.amount = amount;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public void setRedAmount(BigInteger redAmount) {
        this.redAmount = redAmount;
    }

    public RedGreenGameteCreationTransactionRequest toBean() {
    	return new RedGreenGameteCreationTransactionRequest(classpath.toBean(), amount, redAmount, publicKey);
    }
}