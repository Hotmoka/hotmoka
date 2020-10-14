package io.hotmoka.network.thin.client.models.requests

import io.hotmoka.network.thin.client.models.signatures.MethodSignatureModel
import io.hotmoka.network.thin.client.models.values.StorageReferenceModel
import io.hotmoka.network.thin.client.models.values.StorageValueModel
import io.hotmoka.network.thin.client.models.values.TransactionReferenceModel

/**
 * The model of a method call transaction request.
 */
abstract class MethodCallTransactionRequestModel(
        signature: String,
        caller: StorageReferenceModel,
        nonce: String,
        classPath: TransactionReferenceModel,
        chainId: String,
        gasLimit: String,
        gasPrice: String,
        val method: MethodSignatureModel,
        val actuals: List<StorageValueModel>
) : NonInitialTransactionRequestModel(
        signature,
        caller,
        nonce,
        classPath,
        chainId,
        gasLimit,
        gasPrice
)