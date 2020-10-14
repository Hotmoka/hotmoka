package io.hotmoka.network.thin.client.models.responses

import io.hotmoka.network.thin.client.models.values.StorageReferenceModel
import io.hotmoka.network.thin.client.models.values.StorageValueModel

class MethodCallTransactionSuccessfulResponseModel(
    val result: StorageValueModel,
    val events: List<StorageReferenceModel>
)