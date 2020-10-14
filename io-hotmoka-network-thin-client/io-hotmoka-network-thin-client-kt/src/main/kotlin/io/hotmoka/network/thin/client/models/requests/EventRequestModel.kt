package io.hotmoka.network.thin.client.models.requests

import io.hotmoka.network.thin.client.models.values.StorageReferenceModel

class EventRequestModel(
    val key: StorageReferenceModel,
    val event: StorageReferenceModel
)