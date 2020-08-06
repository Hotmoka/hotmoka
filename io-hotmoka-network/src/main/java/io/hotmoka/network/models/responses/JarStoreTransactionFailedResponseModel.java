package io.hotmoka.network.models.responses;

import java.math.BigInteger;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.responses.JarStoreTransactionFailedResponse;
import io.hotmoka.network.models.updates.UpdateModel;

@Immutable
public class JarStoreTransactionFailedResponseModel extends JarStoreTransactionResponseModel {

    /**
     * The amount of gas consumed by the transaction as penalty for the failure.
     */
    public final String gasConsumedForPenalty;

    /**
     * The fully-qualified class name of the cause exception.
     */
    public final String classNameOfCause;

    /**
     * The message of the cause exception.
     */
    public final String messageOfCause;


    public JarStoreTransactionFailedResponseModel(JarStoreTransactionFailedResponse response) {
        super(response);

        this.gasConsumedForPenalty = response.gasConsumedForPenalty().toString();
        this.classNameOfCause = response.classNameOfCause;
        this.messageOfCause = response.messageOfCause;
    }

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
