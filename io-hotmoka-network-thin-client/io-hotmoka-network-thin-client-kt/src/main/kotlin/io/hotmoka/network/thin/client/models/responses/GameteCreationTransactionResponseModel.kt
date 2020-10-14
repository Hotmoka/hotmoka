package io.hotmoka.network.thin.client.models.responses

import io.hotmoka.network.thin.client.models.values.StorageReferenceModel

class GameteCreationTransactionResponseModel(
        val events: List<StorageReferenceModel>,
        val newObject: StorageReferenceModel
)