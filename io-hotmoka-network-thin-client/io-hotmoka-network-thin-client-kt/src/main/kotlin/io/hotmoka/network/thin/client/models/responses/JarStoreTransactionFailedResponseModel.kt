package io.hotmoka.network.thin.client.models.responses

import io.hotmoka.network.thin.client.models.updates.UpdateModel

class JarStoreTransactionFailedResponseModel(
        updates: List<UpdateModel>,
        gasConsumedForCPU: String,
        gasConsumedForRAM: String,
        gasConsumedForStorage: String,
        /**
         * The amount of gas consumed by the transaction as penalty for the failure.
         */
        val gasConsumedForPenalty: String,
        /**
         * The fully-qualified class name of the cause exception.
         */
        val classNameOfCause: String,
        /**
         * The message of the cause exception.
         */
        val messageOfCause: String
) : JarStoreTransactionResponseModel(
        updates,
        gasConsumedForCPU,
        gasConsumedForRAM,
        gasConsumedForStorage
)