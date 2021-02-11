package io.hotmoka.service.models.responses;

import io.hotmoka.beans.responses.MethodCallTransactionResponse;

public abstract class MethodCallTransactionResponseModel extends CodeExecutionTransactionResponseModel {

	/**
	 * True if and only if the call was charged to the receiver of the target method
	 * rather than to the caller of the transaction.
	 */
	public boolean selfCharged;

	protected MethodCallTransactionResponseModel(MethodCallTransactionResponse response) {
        super(response);

        this.selfCharged = response.selfCharged;
	}

	protected MethodCallTransactionResponseModel() {}
}