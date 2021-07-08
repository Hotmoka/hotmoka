package io.hotmoka.network.thin.client.models.updates

import io.hotmoka.network.thin.client.models.values.TransactionReferenceModel

/**
 * The model of the class tag of an object.
 */
class ClassTagModel(
        /**
         * The name of the class of the object.
         */
        val className: String,
        /**
         * The transaction that installed the jar from where the class has been loaded.
         */
        val jar: TransactionReferenceModel
)