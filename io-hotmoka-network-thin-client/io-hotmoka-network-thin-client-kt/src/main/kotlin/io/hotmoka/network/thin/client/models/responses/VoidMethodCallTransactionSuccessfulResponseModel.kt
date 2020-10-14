package io.hotmoka.network.thin.client.models.responses

import io.hotmoka.network.thin.client.models.values.StorageReferenceModel

class VoidMethodCallTransactionSuccessfulResponseModel(
    val events: List<StorageReferenceModel>
)