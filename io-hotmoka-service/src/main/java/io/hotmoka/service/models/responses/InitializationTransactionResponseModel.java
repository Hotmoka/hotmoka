package io.hotmoka.service.models.responses;

import io.hotmoka.beans.responses.InitializationTransactionResponse;

/**
 * The model of a response for a transaction that initializes a node.
 * After that, no more initial transactions can be executed.
 */
public class InitializationTransactionResponseModel extends TransactionResponseModel {

    public InitializationTransactionResponseModel() {}

    public InitializationTransactionResponse toBean() {
        return new InitializationTransactionResponse();
    }
}