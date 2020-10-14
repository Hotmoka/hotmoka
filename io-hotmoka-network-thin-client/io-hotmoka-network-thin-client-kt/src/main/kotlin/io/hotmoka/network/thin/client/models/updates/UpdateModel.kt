package io.hotmoka.network.thin.client.models.updates

import io.hotmoka.network.thin.client.models.signatures.FieldSignatureModel
import io.hotmoka.network.thin.client.models.values.StorageReference
import io.hotmoka.network.thin.client.models.values.StorageValue
import io.hotmoka.network.thin.client.models.values.TransactionReference

class UpdateModel(
        val field: FieldSignatureModel,
        val value: StorageValue,
        val className: String,
        val jar: TransactionReference,
        val updatedObject: StorageReference
)