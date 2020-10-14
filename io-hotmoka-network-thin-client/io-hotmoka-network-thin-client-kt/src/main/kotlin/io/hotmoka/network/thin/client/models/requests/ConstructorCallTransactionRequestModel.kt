package io.hotmoka.network.thin.client.models.requests

import io.hotmoka.network.thin.client.models.signatures.ConstructorSignatureModel
import io.hotmoka.network.thin.client.models.values.StorageReferenceModel
import io.hotmoka.network.thin.client.models.values.StorageValueModel
import io.hotmoka.network.thin.client.models.values.TransactionReferenceModel

/**
 * The model of a constructor call transaction.
 */
class ConstructorCallTransactionRequestModel(
        signature: String,
        caller: StorageReferenceModel,
        nonce: String,
        classPath: TransactionReferenceModel,
        chainId: String,
        gasLimit: String,
        gasPrice: String,
        val constructor: ConstructorSignatureModel,
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