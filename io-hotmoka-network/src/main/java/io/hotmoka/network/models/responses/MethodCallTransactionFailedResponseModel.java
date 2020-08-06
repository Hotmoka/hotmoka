package io.hotmoka.network.models.responses;

import java.math.BigInteger;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.responses.MethodCallTransactionFailedResponse;
import io.hotmoka.network.models.updates.UpdateModel;

@Immutable
public class MethodCallTransactionFailedResponseModel extends MethodCallTransactionResponseModel {

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

    /**
     * The program point where the cause exception occurred.
     */
    public final String where;


    public MethodCallTransactionFailedResponseModel(MethodCallTransactionFailedResponse response) {
        super(response);

        this.gasConsumedForPenalty = response.gasConsumedForPenalty().toString();
        this.classNameOfCause = response.classNameOfCause;
        this.messageOfCause = response.messageOfCause;
        this.where = response.where;
    }

    public MethodCallTransactionFailedResponse toBean() {
        return new MethodCallTransactionFailedResponse(
        	classNameOfCause,
        	messageOfCause,
        	where,
        	updates.stream().map(UpdateModel::toBean),
        	new BigInteger(gasConsumedForCPU),
        	new BigInteger(gasConsumedForRAM),
        	new BigInteger(gasConsumedForStorage),
        	new BigInteger(gasConsumedForPenalty)
        );
    }
}
