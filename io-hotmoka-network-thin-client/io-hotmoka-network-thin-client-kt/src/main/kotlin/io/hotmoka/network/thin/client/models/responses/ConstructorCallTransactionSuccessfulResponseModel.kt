package io.hotmoka.network.thin.client.models.responses

import io.hotmoka.network.thin.client.models.updates.UpdateModel
import io.hotmoka.network.thin.client.models.values.StorageReferenceModel

class ConstructorCallTransactionSuccessfulResponseModel(
        val updates: List<UpdateModel>,
        val gamete: StorageReferenceModel
)