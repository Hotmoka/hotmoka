package io.hotmoka.network.thin.client.models.responses

import io.hotmoka.network.thin.client.models.updates.UpdateModel
import io.hotmoka.network.thin.client.models.values.StorageReferenceModel

class ConstructorCallTransactionSuccessfulResponseModel(
        updates: List<UpdateModel>,
        gasConsumedForCPU: String,
        gasConsumedForRAM: String,
        gasConsumedForStorage: String,
        val events: List<StorageReferenceModel>,
        val newObject: StorageReferenceModel
): ConstructorCallTransactionResponseModel(
        updates,
        gasConsumedForCPU,
        gasConsumedForRAM,
        gasConsumedForStorage
)