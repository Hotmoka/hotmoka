package io.hotmoka.network.models.responses;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.responses.InitializationTransactionResponse;

@Immutable
public class InitializationTransactionResponseModel extends InitialTransactionResponseModel {

    /**
     * A response for a transaction that initializes a node.
     * After that, no more initial transactions can be executed.
     */
    public final boolean initialized;

    public InitializationTransactionResponseModel() {
        this.initialized = true;
    }

    public InitializationTransactionResponse toBean() {
        return new InitializationTransactionResponse();
    }
}
