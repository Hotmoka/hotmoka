package io.hotmoka.network.thin.client.models.responses

import io.hotmoka.network.thin.client.exceptions.TransactionException
import io.hotmoka.network.thin.client.models.updates.UpdateModel
import io.hotmoka.network.thin.client.models.values.TransactionReferenceModel

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
) {
        override fun getOutcomeAt(transactionReference: TransactionReferenceModel): TransactionReferenceModel {
                val message = classNameOfCause + (if (messageOfCause.isEmpty()) "" else ": $messageOfCause")
                throw TransactionException(message)
        }
}