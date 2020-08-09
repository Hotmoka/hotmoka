package io.hotmoka.network.models.requests;

import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.network.models.values.TransactionReferenceModel;

/**
 * The model of an initial jar store transaction request.
 */
public class JarStoreInitialTransactionRequestModel extends InitialTransactionRequestModel {
	public String jar;
    private List<TransactionReferenceModel> dependencies;

    public JarStoreInitialTransactionRequestModel() {}

    /**
     * Builds the model from the request.
     * 
     * @param request the request to copy
     */
    public JarStoreInitialTransactionRequestModel(JarStoreInitialTransactionRequest request) {
    	this.jar = Base64.getEncoder().encodeToString(request.getJar());
    	this.dependencies = request.getDependencies().map(TransactionReferenceModel::new).collect(Collectors.toList());
    }

    public final Stream<TransactionReferenceModel> getDependencies() {
    	return dependencies.stream();
    }

    /**
     * Yields the request having this model.
     * 
     * @return the request
     */
    public JarStoreInitialTransactionRequest toBean() {
    	 if (jar == null)
    		 throw new InternalFailureException("unexpected null jar");

         return new JarStoreInitialTransactionRequest(decodeBase64(jar),
       		 dependencies.stream().map(TransactionReferenceModel::toBean).toArray(TransactionReference[]::new));
    }
}