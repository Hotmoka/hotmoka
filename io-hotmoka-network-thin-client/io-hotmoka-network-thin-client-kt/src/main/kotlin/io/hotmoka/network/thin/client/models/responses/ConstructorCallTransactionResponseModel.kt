package io.hotmoka.network.thin.client.models.responses

import io.hotmoka.network.thin.client.exceptions.CodeExecutionException
import io.hotmoka.network.thin.client.exceptions.TransactionException
import io.hotmoka.network.thin.client.models.updates.UpdateModel
import io.hotmoka.network.thin.client.models.values.StorageReferenceModel

abstract class ConstructorCallTransactionResponseModel(
        updates: List<UpdateModel>,
        gasConsumedForCPU: String,
        gasConsumedForRAM: String,
        gasConsumedForStorage: String
) : CodeExecutionTransactionResponseModel(
        updates,
        gasConsumedForCPU,
        gasConsumedForRAM,
        gasConsumedForStorage
) {
        /**
         * Yields the outcome of the execution having this response.
         *
         * @return the outcome
         * @throws CodeExecutionException if the transaction failed with an exception inside the user code in store,
         * allowed to be thrown outside the store
         * @throws TransactionException if the transaction failed with an exception outside the user code in store,
         * or not allowed to be thrown outside the store
         */
        @Throws(TransactionException::class, CodeExecutionException::class)
        abstract fun getOutcome(): StorageReferenceModel
}