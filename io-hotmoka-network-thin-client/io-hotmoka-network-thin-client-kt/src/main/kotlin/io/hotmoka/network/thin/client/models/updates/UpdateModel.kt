package io.hotmoka.network.thin.client.models.updates

import io.hotmoka.network.thin.client.models.signatures.FieldSignatureModel
import io.hotmoka.network.thin.client.models.values.StorageReferenceModel
import io.hotmoka.network.thin.client.models.values.StorageValueModel
import io.hotmoka.network.thin.client.models.values.TransactionReferenceModel

class UpdateModel(
        val field: FieldSignatureModel,
        val value: StorageValueModel,
        val className: String,
        val jar: TransactionReferenceModel,
        val updatedObject: StorageReferenceModel
)