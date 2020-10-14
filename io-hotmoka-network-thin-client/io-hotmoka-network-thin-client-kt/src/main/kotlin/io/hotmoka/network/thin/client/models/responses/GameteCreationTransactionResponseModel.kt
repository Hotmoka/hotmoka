package io.hotmoka.network.thin.client.models.responses

import io.hotmoka.network.thin.client.models.updates.UpdateModel
import io.hotmoka.network.thin.client.models.values.StorageReferenceModel

class GameteCreationTransactionResponseModel(
        /**
         * The updates resulting from the execution of the transaction.
         */
        val updates: List<UpdateModel>,
        /**
         * The created gamete.
         */
        val gamete: StorageReferenceModel
): TransactionResponseModel()