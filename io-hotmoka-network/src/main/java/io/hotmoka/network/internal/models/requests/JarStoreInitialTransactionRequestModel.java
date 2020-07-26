package io.hotmoka.network.internal.models.requests;

import java.util.List;

import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.network.exception.GenericException;
import io.hotmoka.network.internal.models.values.TransactionReferenceModel;

public class JarStoreInitialTransactionRequestModel extends InitialTransactionRequestModel {
    private String jar;
    private List<TransactionReferenceModel> dependencies;

    public void setJar(String jar) {
        this.jar = jar;
    }

    public void setDependencies(List<TransactionReferenceModel> dependencies) {
        this.dependencies = dependencies;
    }

    public JarStoreInitialTransactionRequest toBean() {
    	 if (jar == null)
    		 throw new GenericException("Transaction rejected: Jar missing");

         return new JarStoreInitialTransactionRequest(decodeBase64(jar),
       		 dependencies.stream().map(TransactionReferenceModel::toBean).toArray(TransactionReference[]::new));
    }
}