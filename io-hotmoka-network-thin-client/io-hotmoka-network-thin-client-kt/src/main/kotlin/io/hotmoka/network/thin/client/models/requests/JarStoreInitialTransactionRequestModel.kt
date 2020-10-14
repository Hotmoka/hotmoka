package io.hotmoka.network.thin.client.models.requests

import io.hotmoka.network.thin.client.models.values.TransactionReferenceModel

/**
 * The model of an initial jar store transaction request.
 */
class JarStoreInitialTransactionRequestModel(
    val jar: String,
    val dependencies: List<TransactionReferenceModel>
): InitialTransactionRequestModel()