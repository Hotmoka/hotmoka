package io.hotmoka.service.models.responses;

import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import io.hotmoka.beans.responses.JarStoreInitialTransactionResponse;
import io.hotmoka.service.models.values.TransactionReferenceModel;

public class JarStoreInitialTransactionResponseModel extends TransactionResponseModel {

    /**
     * The jar to install, instrumented.
     */
    public String instrumentedJar;

    /**
     * The dependencies of the jar, previously installed in blockchain.
     * This is a copy of the same information contained in the request.
     */
    public List<TransactionReferenceModel> dependencies;
    
	/**
	 * The version of the verification tool involved in the verification process.
	 */
	public int verificationToolVersion;

    public JarStoreInitialTransactionResponseModel(JarStoreInitialTransactionResponse response) {
        this.instrumentedJar = Base64.getEncoder().encodeToString(response.getInstrumentedJar());
        this.dependencies = response.getDependencies().map(TransactionReferenceModel::new).collect(Collectors.toList());
        this.verificationToolVersion = response.getVerificationVersion();
    }

    public JarStoreInitialTransactionResponseModel() {}

    public JarStoreInitialTransactionResponse toBean() {
        return new JarStoreInitialTransactionResponse(Base64.getDecoder().decode(instrumentedJar), dependencies.stream().map(TransactionReferenceModel::toBean), verificationToolVersion);
    }
}
