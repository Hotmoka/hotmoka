package io.hotmoka.service.models.responses;

import java.math.BigInteger;

import io.hotmoka.beans.responses.MethodCallTransactionFailedResponse;
import io.hotmoka.service.models.updates.UpdateModel;

public class MethodCallTransactionFailedResponseModel extends MethodCallTransactionResponseModel {

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

    /**
     * The program point where the cause exception occurred.
     */
    public String where;

    public MethodCallTransactionFailedResponseModel(MethodCallTransactionFailedResponse response) {
        super(response);

        this.gasConsumedForPenalty = response.gasConsumedForPenalty().toString();
        this.classNameOfCause = response.classNameOfCause;
        this.messageOfCause = response.messageOfCause;
        this.where = response.where;
    }

    public MethodCallTransactionFailedResponseModel() {}

    public MethodCallTransactionFailedResponse toBean() {
        return new MethodCallTransactionFailedResponse(
        	classNameOfCause,
        	messageOfCause,
        	where,
        	selfCharged,
        	updates.stream().map(UpdateModel::toBean),
        	new BigInteger(gasConsumedForCPU),
        	new BigInteger(gasConsumedForRAM),
        	new BigInteger(gasConsumedForStorage),
        	new BigInteger(gasConsumedForPenalty)
        );
    }
}
