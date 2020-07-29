package io.hotmoka.network.models.responses;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.responses.MethodCallTransactionResponse;

@Immutable
public abstract class MethodCallTransactionResponseModel extends CodeExecutionTransactionResponseModel {

    public MethodCallTransactionResponseModel(MethodCallTransactionResponse response) {
        super(response);
    }
}
