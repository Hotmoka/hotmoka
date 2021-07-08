package io.hotmoka.network.thin.client.models.requests

import io.hotmoka.network.thin.client.models.values.TransactionReferenceModel

class GameteCreationTransactionRequestModel(
    val initialAmount: String,
    val publicKey: String,
    val classpath: TransactionReferenceModel
): InitialTransactionRequestModel()