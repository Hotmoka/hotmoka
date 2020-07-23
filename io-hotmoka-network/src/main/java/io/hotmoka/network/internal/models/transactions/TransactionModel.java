package io.hotmoka.network.internal.models.transactions;

import java.util.Base64;

public class TransactionModel {
    private String classpath;

    public String getClasspath() {
        return classpath;
    }

    public void setClasspath(String classpath) {
        this.classpath = classpath;
    }

    protected final byte[] decodeBase64(String what) {
    	return Base64.getDecoder().decode(what);
    }
}