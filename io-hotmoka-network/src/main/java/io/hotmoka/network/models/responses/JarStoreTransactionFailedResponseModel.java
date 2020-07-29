package io.hotmoka.network.models.responses;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.responses.JarStoreTransactionFailedResponse;
import io.hotmoka.network.models.updates.UpdateModel;

import java.math.BigInteger;
import java.util.stream.Collectors;

@Immutable
public class JarStoreTransactionFailedResponseModel extends JarStoreTransactionResponseModel {

    /**
     * The amount of gas consumed by the transaction as penalty for the failure.
     */
    public final BigInteger gasConsumedForPenalty;

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

        this.gasConsumedForPenalty = response.gasConsumedForPenalty();
        this.classNameOfCause = response.classNameOfCause;
        this.messageOfCause = response.messageOfCause;
    }

    public JarStoreTransactionFailedResponse toBean() {
        return new JarStoreTransactionFailedResponse(
                this.classNameOfCause,
                this.messageOfCause,
                updates.stream().map(UpdateModel::toBean).collect(Collectors.toSet()).stream(),
                gasConsumedForCPU,
                gasConsumedForRAM,
                gasConsumedForStorage,
                gasConsumedForPenalty
        );
    }
}
