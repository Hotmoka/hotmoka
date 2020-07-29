package io.hotmoka.network.models.responses;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.responses.ConstructorCallTransactionResponse;

@Immutable
public abstract class ConstructorCallTransactionResponseModel extends CodeExecutionTransactionResponseModel {

    public ConstructorCallTransactionResponseModel(ConstructorCallTransactionResponse response) {
        super(response);
    }
}
