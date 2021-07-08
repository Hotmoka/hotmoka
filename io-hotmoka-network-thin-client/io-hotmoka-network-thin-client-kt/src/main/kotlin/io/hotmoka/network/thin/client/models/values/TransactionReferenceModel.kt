package io.hotmoka.network.thin.client.models.values

/**
 * The model of a transaction reference.
 */
class TransactionReferenceModel(
        /**
         * The type of transaction.
         */
        val type: String,
        /**
         * Used at least for local transactions.
         */
        val hash: String
)