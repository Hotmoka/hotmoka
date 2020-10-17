package io.hotmoka.network.thin.client.models.responses

/**
 * Class which wraps a type response model
 * @param <T> the type response model
 */
class TransactionRestResponseModel<T>(
        /**
         * The runtime type of the response model
         */
        val type: String,
        /**
         * The response model which should be an instance of TransactionResponseModel.
         */
        val transactionResponseModel: T,
)