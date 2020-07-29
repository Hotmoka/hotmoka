package io.hotmoka.network.models.responses;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.responses.ConstructorCallTransactionFailedResponse;
import io.hotmoka.network.models.updates.UpdateModel;

import java.math.BigInteger;
import java.util.stream.Collectors;

@Immutable
public class ConstructorCallTransactionFailedResponseModel extends ConstructorCallTransactionResponseModel {
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

    /**
     * The program point where the cause exception occurred.
     */
    public final String where;


    public ConstructorCallTransactionFailedResponseModel(ConstructorCallTransactionFailedResponse response) {
        super(response);

        this.gasConsumedForPenalty = response.gasConsumedForPenalty();
        this.classNameOfCause = response.classNameOfCause;
        this.messageOfCause = response.messageOfCause;
        this.where = response.where;
    }

    public ConstructorCallTransactionFailedResponse toBean() {
        return new ConstructorCallTransactionFailedResponse(
                this.classNameOfCause,
                this.messageOfCause,
                this.where,
                updates.stream().map(UpdateModel::toBean).collect(Collectors.toSet()).stream(),
                this.gasConsumedForCPU,
                this.gasConsumedForRAM,
                this.gasConsumedForStorage,
                this.gasConsumedForPenalty
        );
    }
}
