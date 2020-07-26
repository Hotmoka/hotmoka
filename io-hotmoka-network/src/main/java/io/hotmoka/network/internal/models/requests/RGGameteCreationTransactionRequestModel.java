package io.hotmoka.network.internal.models.requests;

import java.math.BigInteger;

import io.hotmoka.beans.requests.RedGreenGameteCreationTransactionRequest;
import io.hotmoka.network.json.JSONTransactionReference;

public class RGGameteCreationTransactionRequestModel extends InitialTransactionRequestModel {
	private BigInteger amount;
    private BigInteger redAmount;
	private String publicKey;
	private String classpath;

    public void setClasspath(String classpath) {
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
    	return new RedGreenGameteCreationTransactionRequest(
    		JSONTransactionReference.fromJSON(classpath), amount, redAmount, publicKey);
    }
}