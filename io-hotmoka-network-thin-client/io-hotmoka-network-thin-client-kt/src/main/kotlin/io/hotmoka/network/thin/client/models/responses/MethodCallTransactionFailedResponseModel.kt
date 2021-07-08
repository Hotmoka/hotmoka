package io.hotmoka.network.thin.client.models.responses

import io.hotmoka.network.thin.client.exceptions.TransactionException
import io.hotmoka.network.thin.client.models.updates.UpdateModel
import io.hotmoka.network.thin.client.models.values.StorageValueModel

class MethodCallTransactionFailedResponseModel(
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
        val messageOfCause: String,
        /**
         * The program point where the cause exception occurred.
         */
        val where: String
) : MethodCallTransactionResponseModel(
        updates,
        gasConsumedForCPU,
        gasConsumedForRAM,
        gasConsumedForStorage
) {
        override fun getOutcome(): StorageValueModel {
                throw TransactionException(classNameOfCause, messageOfCause, where)
        }
}