package io.hotmoka.network.thin.client.models.responses

import io.hotmoka.network.thin.client.models.updates.UpdateModel

abstract class CodeExecutionTransactionResponseModel(
        /**
         * The updates resulting from the execution of the transaction.
         */
        val updates: List<UpdateModel>,
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
): TransactionResponseModel()