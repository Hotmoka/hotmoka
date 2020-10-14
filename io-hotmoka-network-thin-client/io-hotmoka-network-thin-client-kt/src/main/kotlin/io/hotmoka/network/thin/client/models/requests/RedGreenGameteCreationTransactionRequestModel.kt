package io.hotmoka.network.thin.client.models.requests

import io.hotmoka.network.thin.client.models.values.TransactionReferenceModel

class RedGreenGameteCreationTransactionRequestModel(
        val initialAmount: String,
        val redInitialAmount: String,
        val publicKey: String,
        val classpath: TransactionReferenceModel
): InitialTransactionRequestModel()