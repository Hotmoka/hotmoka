package io.hotmoka.network.thin.client.suppliers

import io.hotmoka.network.thin.client.exceptions.TransactionException
import io.hotmoka.network.thin.client.exceptions.TransactionRejectedException
import io.hotmoka.network.thin.client.models.values.TransactionReferenceModel

interface JarSupplier {

    fun getReferenceOfRequest(): TransactionReferenceModel

    @Throws(TransactionRejectedException::class, TransactionException::class)
    fun get(): TransactionReferenceModel
}