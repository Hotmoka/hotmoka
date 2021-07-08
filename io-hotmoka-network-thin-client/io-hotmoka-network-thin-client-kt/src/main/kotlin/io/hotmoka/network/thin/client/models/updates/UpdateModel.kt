package io.hotmoka.network.thin.client.models.updates

import io.hotmoka.network.thin.client.models.signatures.FieldSignatureModel
import io.hotmoka.network.thin.client.models.values.StorageReferenceModel
import io.hotmoka.network.thin.client.models.values.StorageValueModel
import io.hotmoka.network.thin.client.models.values.TransactionReferenceModel

/**
 * The model of an update of an object.
 */
class UpdateModel(
        /**
         * The field that is updated. This is {@code null} for class tags.
         */
        val field: FieldSignatureModel,
        /**
         * The value assigned to the updated field. This is {@code null} for class tags.
         */
        val value: StorageValueModel,
        /**
         * The name of the class of the object. This is non-{@code null} for class tags only.
         */
        val className: String,
        /**
         * The transaction that installed the jar from where the class has been loaded.
         * This is non-{@code null} for class tags only.
         */
        val jar: TransactionReferenceModel,
        /**
         * The object whose field is modified.
         */
        val updatedObject: StorageReferenceModel
)