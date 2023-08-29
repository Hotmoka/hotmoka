/*
Copyright 2021 Dinu Berinde and Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.network.responses;

import java.math.BigInteger;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import io.hotmoka.beans.responses.JarStoreTransactionSuccessfulResponse;
import io.hotmoka.network.updates.UpdateModel;
import io.hotmoka.network.values.TransactionReferenceModel;

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
	public long verificationToolVersion;

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