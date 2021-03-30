package io.hotmoka.network.responses;

import java.math.BigInteger;

import io.hotmoka.beans.responses.JarStoreTransactionFailedResponse;
import io.hotmoka.network.updates.UpdateModel;

public class JarStoreTransactionFailedResponseModel extends JarStoreTransactionResponseModel {

    /**
     * The amount of gas consumed by the transaction as penalty for the failure.
     */
    public String gasConsumedForPenalty;

    /**
     * The fully-qualified class name of the cause exception.
     */
    public String classNameOfCause;

    /**
     * The message of the cause exception.
     */
    public String messageOfCause;

    public JarStoreTransactionFailedResponseModel(JarStoreTransactionFailedResponse response) {
        super(response);

        this.gasConsumedForPenalty = response.gasConsumedForPenalty().toString();
        this.classNameOfCause = response.classNameOfCause;
        this.messageOfCause = response.messageOfCause;
    }

    public JarStoreTransactionFailedResponseModel() {}

    public JarStoreTransactionFailedResponse toBean() {
        return new JarStoreTransactionFailedResponse(
        	classNameOfCause,
        	messageOfCause,
        	updates.stream().map(UpdateModel::toBean),
        	new BigInteger(gasConsumedForCPU),
        	new BigInteger(gasConsumedForRAM),
        	new BigInteger(gasConsumedForStorage),
        	new BigInteger(gasConsumedForPenalty)
        );
    }
}
