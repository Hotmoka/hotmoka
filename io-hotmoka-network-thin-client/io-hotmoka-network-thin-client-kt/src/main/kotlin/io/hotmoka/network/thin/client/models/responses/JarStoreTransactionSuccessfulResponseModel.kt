package io.hotmoka.network.thin.client.models.responses

import io.hotmoka.network.thin.client.models.updates.UpdateModel
import io.hotmoka.network.thin.client.models.values.TransactionReferenceModel

class JarStoreTransactionSuccessfulResponseModel(
        updates: List<UpdateModel>,
        gasConsumedForCPU: String,
        gasConsumedForRAM: String,
        gasConsumedForStorage: String,
        /**
         * The jar to install, instrumented.
         */
        val instrumentedJar: String,
        /**
         * The dependencies of the jar, previously installed in blockchain.
         * This is a copy of the same information contained in the request.
         */
        val dependencies: List<TransactionReferenceModel>
) : JarStoreTransactionResponseModel(
        updates,
        gasConsumedForCPU,
        gasConsumedForRAM,
        gasConsumedForStorage
) {
        override fun getOutcomeAt(transactionReference: TransactionReferenceModel): TransactionReferenceModel {
                // the outcome is the reference to the transaction where this response has been executed
                return transactionReference
        }
}