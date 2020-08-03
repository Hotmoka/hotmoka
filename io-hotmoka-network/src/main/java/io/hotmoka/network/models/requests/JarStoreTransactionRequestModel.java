package io.hotmoka.network.models.requests;

import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.network.models.values.TransactionReferenceModel;

/**
 * The model of a jar store transaction request.
 */
@Immutable
public class JarStoreTransactionRequestModel extends NonInitialTransactionRequestModel {
    public final String jar;
    private final List<TransactionReferenceModel> dependencies;

    /**
     * Builds the model from the request.
     * 
     * @param request the request to copy
     */
    public JarStoreTransactionRequestModel(JarStoreTransactionRequest request) {
    	super(request);

    	this.jar = Base64.getEncoder().encodeToString(request.getJar());
    	this.dependencies = request.getDependencies().map(TransactionReferenceModel::new).collect(Collectors.toList());
    }

    public final Stream<TransactionReferenceModel> getDependencies() {
    	return dependencies.stream();
    }

    public JarStoreTransactionRequest toBean() {
    	return new JarStoreTransactionRequest(
        	decodeBase64(signature),
            caller.toBean(),
            nonce,
            chainId,
            gasLimit,
            gasPrice,
            classpath.toBean(),
            decodeBase64(jar),
            dependencies.stream().map(TransactionReferenceModel::toBean).toArray(TransactionReference[]::new));
    }
}