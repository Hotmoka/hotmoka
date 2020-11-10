package io.hotmoka.network.thin.client.models.requests

import io.hotmoka.network.thin.client.models.values.StorageReferenceModel
import io.hotmoka.network.thin.client.models.values.TransactionReferenceModel

abstract class NonInitialTransactionRequestModel(
    val signature: String,
    val caller: StorageReferenceModel,
    val nonce: String,
    val classpath: TransactionReferenceModel,
    val chainId: String,
    val gasLimit: String,
    val gasPrice: String
): TransactionRequestModel()