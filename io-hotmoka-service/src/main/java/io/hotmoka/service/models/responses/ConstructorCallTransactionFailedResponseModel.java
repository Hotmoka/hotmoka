package io.hotmoka.service.models.responses;

import java.math.BigInteger;

import io.hotmoka.beans.responses.ConstructorCallTransactionFailedResponse;
import io.hotmoka.service.models.updates.UpdateModel;

public class ConstructorCallTransactionFailedResponseModel extends ConstructorCallTransactionResponseModel {

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

    public ConstructorCallTransactionFailedResponseModel(ConstructorCallTransactionFailedResponse response) {
        super(response);

        this.gasConsumedForPenalty = response.gasConsumedForPenalty().toString();
        this.classNameOfCause = response.classNameOfCause;
        this.messageOfCause = response.messageOfCause;
        this.where = response.where;
    }

    public ConstructorCallTransactionFailedResponseModel() {}

    public ConstructorCallTransactionFailedResponse toBean() {
        return new ConstructorCallTransactionFailedResponse(
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
