package io.hotmoka.network.thin.client.models.responses

import io.hotmoka.network.thin.client.exceptions.TransactionException
import io.hotmoka.network.thin.client.models.updates.UpdateModel
import io.hotmoka.network.thin.client.models.values.TransactionReferenceModel
import kotlin.jvm.Throws

abstract class JarStoreTransactionResponseModel(
        /**
         * The updates resulting from the execution of the transaction.
         */
        val update: List<UpdateModel>,
        /**
         * The amount of gas consumed by the transaction for CPU execution.
         */
        val gasConsumedForCPU: String,
        /**
         * The amount of gas consumed by the transaction for RAM allocation.
         */
        val gasConsumedForRAM: String,
        /**
         * The amount of gas consumed by the transaction for storage consumption.
         */
        val gasConsumedForStorage: String
) : TransactionResponseModel() {


        /**
         * Yields the outcome of the execution having this response, performed
         * at the given transaction reference.
         *
         * @param transactionReference the transaction reference
         * @return the outcome
         * @throws TransactionException if the outcome of the transaction is this exception
         */
        @Throws(TransactionException::class)
        abstract fun getOutcomeAt(transactionReference: TransactionReferenceModel): TransactionReferenceModel
}