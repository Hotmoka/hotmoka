package io.hotmoka.network.thin.client

import io.hotmoka.network.thin.client.exceptions.CodeExecutionException
import io.hotmoka.network.thin.client.exceptions.TransactionException
import io.hotmoka.network.thin.client.exceptions.TransactionRejectedException
import io.hotmoka.network.thin.client.models.requests.*
import io.hotmoka.network.thin.client.models.responses.SignatureAlgorithmResponseModel
import io.hotmoka.network.thin.client.models.responses.TransactionRestResponseModel
import io.hotmoka.network.thin.client.models.updates.ClassTagModel
import io.hotmoka.network.thin.client.models.updates.StateModel
import io.hotmoka.network.thin.client.models.values.StorageReferenceModel
import io.hotmoka.network.thin.client.models.values.StorageValueModel
import io.hotmoka.network.thin.client.models.values.TransactionReferenceModel
import io.hotmoka.network.thin.client.suppliers.CodeSupplier
import io.hotmoka.network.thin.client.suppliers.JarSupplier
import java.security.NoSuchAlgorithmException
import java.util.*
import java.util.concurrent.TimeoutException

interface RemoteNode {

    @Throws(NoSuchElementException::class)
    fun getTakamakaCode(): TransactionReferenceModel

    @Throws(NoSuchElementException::class)
    fun getManifest(): StorageReferenceModel

    @Throws(NoSuchElementException::class)
    fun getState(request: StorageReferenceModel): StateModel

    @Throws(NoSuchElementException::class)
    fun getClassTag(request: StorageReferenceModel): ClassTagModel

    @Throws(NoSuchElementException::class)
    fun getRequest(reference: TransactionReferenceModel): TransactionRestRequestModel<*>

    @Throws(NoSuchAlgorithmException::class)
    fun getSignatureAlgorithmForRequests(): SignatureAlgorithmResponseModel

    @Throws(TransactionRejectedException::class, NoSuchAlgorithmException::class)
    fun getResponse(reference: TransactionReferenceModel): TransactionRestResponseModel<*>

    @Throws(TransactionRejectedException::class, TimeoutException::class, InterruptedException::class)
    fun getPolledResponse(reference: TransactionReferenceModel): TransactionRestResponseModel<*>

    @Throws(TransactionRejectedException::class)
    fun addJarStoreInitialTransaction(request: JarStoreInitialTransactionRequestModel): TransactionReferenceModel

    @Throws(TransactionRejectedException::class)
    fun addGameteCreationTransaction(request: GameteCreationTransactionRequestModel): StorageReferenceModel

    @Throws(TransactionRejectedException::class)
    fun addRedGreenGameteCreationTransaction(request: RedGreenGameteCreationTransactionRequestModel): StorageReferenceModel

    @Throws(TransactionRejectedException::class)
    fun addInitializationTransaction(request: InitializationTransactionRequestModel)

    @Throws(TransactionRejectedException::class, TransactionException::class)
    fun addJarStoreTransaction(request: JarStoreTransactionRequestModel): TransactionReferenceModel

    @Throws(TransactionRejectedException::class, TransactionException::class, CodeExecutionException::class)
    fun addConstructorCallTransaction(request: ConstructorCallTransactionRequestModel): StorageReferenceModel

    @Throws(TransactionRejectedException::class, TransactionException::class, CodeExecutionException::class)
    fun addInstanceMethodCallTransaction(request: InstanceMethodCallTransactionRequestModel): StorageValueModel?

    @Throws(TransactionRejectedException::class, TransactionException::class, CodeExecutionException::class)
    fun addStaticMethodCallTransaction(request: StaticMethodCallTransactionRequestModel): StorageValueModel?

    @Throws(TransactionRejectedException::class)
    fun postJarStoreTransaction(request: JarStoreTransactionRequestModel): JarSupplier

    @Throws(TransactionRejectedException::class)
    fun postConstructorCallTransaction(request: ConstructorCallTransactionRequestModel): CodeSupplier<StorageReferenceModel>

    @Throws(TransactionRejectedException::class)
    fun postInstanceMethodCallTransaction(request: InstanceMethodCallTransactionRequestModel): CodeSupplier<StorageValueModel>

    @Throws(TransactionRejectedException::class)
    fun postStaticMethodCallTransaction(request: StaticMethodCallTransactionRequestModel): CodeSupplier<StorageValueModel>

    @Throws(TransactionRejectedException::class, TransactionException::class, CodeExecutionException::class)
    fun runInstanceMethodCallTransaction(request: InstanceMethodCallTransactionRequestModel): StorageValueModel?

    @Throws(TransactionRejectedException::class, TransactionException::class, CodeExecutionException::class)
    fun runStaticMethodCallTransaction(request: StaticMethodCallTransactionRequestModel): StorageValueModel?
}