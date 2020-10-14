package io.hotmoka.network.thin.client.models.responses

import io.hotmoka.network.thin.client.models.values.TransactionReferenceModel

class JarStoreTransactionSuccessfulResponseModel(
    val instrumentedJar: String,
    val dependencies: List<TransactionReferenceModel>
)