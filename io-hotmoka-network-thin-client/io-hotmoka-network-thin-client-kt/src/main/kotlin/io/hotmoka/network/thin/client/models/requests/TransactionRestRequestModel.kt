package io.hotmoka.network.thin.client.models.requests

/**
 * Class which wraps a type request model
 * @param <T> the type request model
 */
class TransactionRestRequestModel<T>(
        /**
         * The runtime type of the request model
         */
        val type: String,
        /**
         * The request model which should be an instance of TransactionRequestModel.
         */
        val transactionResponseModel: T
)