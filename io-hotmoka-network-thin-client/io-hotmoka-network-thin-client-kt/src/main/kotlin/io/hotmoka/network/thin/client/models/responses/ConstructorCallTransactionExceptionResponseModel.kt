package io.hotmoka.network.thin.client.models.responses

import io.hotmoka.network.thin.client.models.values.StorageReferenceModel

class ConstructorCallTransactionExceptionResponseModel(
    val events: List<StorageReferenceModel>,
    val classNameOfCause: String,
    val messageOfCause: String,
    val where: String
)