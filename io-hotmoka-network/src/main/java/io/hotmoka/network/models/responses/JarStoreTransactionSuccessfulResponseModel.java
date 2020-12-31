package io.hotmoka.network.models.responses;

import java.math.BigInteger;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import io.hotmoka.beans.responses.JarStoreTransactionSuccessfulResponse;
import io.hotmoka.network.models.updates.UpdateModel;
import io.hotmoka.network.models.values.TransactionReferenceModel;

public class JarStoreTransactionSuccessfulResponseModel extends JarStoreTransactionResponseModel {

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

    public JarStoreTransactionSuccessfulResponseModel(JarStoreTransactionSuccessfulResponse response) {
        super(response);

        this.instrumentedJar = Base64.getEncoder().encodeToString(response.getInstrumentedJar());
        this.dependencies = response.getDependencies().map(TransactionReferenceModel::new).collect(Collectors.toList());
        this.verificationToolVersion = response.getVerificationVersion();
    }

    public JarStoreTransactionSuccessfulResponseModel() {}

    public JarStoreTransactionSuccessfulResponse toBean() {
        return new JarStoreTransactionSuccessfulResponse(
        	Base64.getDecoder().decode(instrumentedJar),
        	dependencies.stream().map(TransactionReferenceModel::toBean),
        	verificationToolVersion,
        	updates.stream().map(UpdateModel::toBean),
        	new BigInteger(gasConsumedForCPU),
        	new BigInteger(gasConsumedForRAM),
        	new BigInteger(gasConsumedForStorage)
        );
    }
}