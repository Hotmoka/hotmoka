package io.hotmoka.network.thin.client.models.requests

import io.hotmoka.network.thin.client.models.values.StorageReferenceModel

class EventRequestModel(
    val creator: StorageReferenceModel,
    val event: StorageReferenceModel
)