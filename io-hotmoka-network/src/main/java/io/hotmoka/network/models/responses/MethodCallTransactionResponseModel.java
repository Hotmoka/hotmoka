package io.hotmoka.network.models.responses;

import io.hotmoka.beans.responses.MethodCallTransactionResponse;

public abstract class MethodCallTransactionResponseModel extends CodeExecutionTransactionResponseModel {

	protected MethodCallTransactionResponseModel(MethodCallTransactionResponse response) {
        super(response);
    }

	protected MethodCallTransactionResponseModel() {}
}