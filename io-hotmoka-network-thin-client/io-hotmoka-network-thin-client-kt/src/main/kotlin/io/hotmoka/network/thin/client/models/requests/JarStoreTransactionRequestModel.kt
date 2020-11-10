package io.hotmoka.network.thin.client.models.requests

import io.hotmoka.network.thin.client.models.values.StorageReferenceModel
import io.hotmoka.network.thin.client.models.values.TransactionReferenceModel

/**
 * The model of a jar store transaction request.
 */
class JarStoreTransactionRequestModel(
        signature: String,
        caller: StorageReferenceModel,
        nonce: String,
        classpath: TransactionReferenceModel,
        chainId: String,
        gasLimit: String,
        gasPrice: String,
        val jar: String,
        val dependencies: List<TransactionReferenceModel>
) : NonInitialTransactionRequestModel(
        signature,
        caller,
        nonce,
        classpath,
        chainId,
        gasLimit,
        gasPrice
)