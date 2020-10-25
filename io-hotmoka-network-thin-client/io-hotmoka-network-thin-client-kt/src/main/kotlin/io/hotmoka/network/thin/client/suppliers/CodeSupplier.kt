package io.hotmoka.network.thin.client.suppliers

import io.hotmoka.network.thin.client.exceptions.CodeExecutionException
import io.hotmoka.network.thin.client.exceptions.TransactionException
import io.hotmoka.network.thin.client.exceptions.TransactionRejectedException
import io.hotmoka.network.thin.client.models.values.TransactionReferenceModel

interface CodeSupplier<T> {

    fun getReferenceOfRequest(): TransactionReferenceModel

    @Throws(TransactionRejectedException::class, TransactionException::class, CodeExecutionException::class)
    fun get(): T
}