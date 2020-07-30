package io.hotmoka.network.models.responses;

import java.math.BigInteger;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.responses.ConstructorCallTransactionFailedResponse;
import io.hotmoka.network.models.updates.UpdateModel;

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
        	classNameOfCause,
            messageOfCause,
            where,
            updates.stream().map(UpdateModel::toBean),
            gasConsumedForCPU,
            gasConsumedForRAM,
            gasConsumedForStorage,
            gasConsumedForPenalty
        );
    }
}
