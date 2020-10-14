package io.hotmoka.network.thin.client.models.requests

import io.hotmoka.network.thin.client.models.values.StorageReferenceModel
import io.hotmoka.network.thin.client.models.values.TransactionReferenceModel

class InitializationTransactionRequestModel(
    val manifest: StorageReferenceModel,
    val classpath: TransactionReferenceModel
): InitialTransactionRequestModel()