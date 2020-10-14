package io.hotmoka.network.thin.client

import io.hotmoka.network.thin.client.exceptions.NetworkException
import io.hotmoka.network.thin.client.models.requests.*
import io.hotmoka.network.thin.client.models.responses.SignatureAlgorithmResponseModel
import io.hotmoka.network.thin.client.models.responses.TransactionRestResponseModel
import io.hotmoka.network.thin.client.models.updates.ClassTagModel
import io.hotmoka.network.thin.client.models.updates.StateModel
import io.hotmoka.network.thin.client.models.values.StorageReferenceModel
import io.hotmoka.network.thin.client.models.values.StorageValueModel
import io.hotmoka.network.thin.client.models.values.TransactionReferenceModel
import java.security.NoSuchAlgorithmException
import java.util.NoSuchElementException

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

    @Throws(NetworkException::class)
    fun getResponse(reference: TransactionReferenceModel): TransactionRestResponseModel<*>

    @Throws(NetworkException::class)
    fun getPolledResponse(reference: TransactionReferenceModel): TransactionRestResponseModel<*>

    @Throws(NetworkException::class)
    fun addJarStoreInitialTransaction(request: JarStoreInitialTransactionRequestModel): TransactionReferenceModel

    @Throws(NetworkException::class)
    fun addGameteCreationTransaction(request: GameteCreationTransactionRequestModel): StorageReferenceModel

    @Throws(NetworkException::class)
    fun addRedGreenGameteCreationTransaction(request: RedGreenGameteCreationTransactionRequestModel): StorageReferenceModel

    @Throws(NetworkException::class)
    fun addInitializationTransaction(request: InitializationTransactionRequestModel): Void

    @Throws(NetworkException::class)
    fun addJarStoreTransaction(request: JarStoreTransactionRequestModel): TransactionReferenceModel

    @Throws(NetworkException::class)
    fun addConstructorCallTransaction(request: ConstructorCallTransactionRequestModel): StorageReferenceModel

    @Throws(NetworkException::class)
    fun addInstanceMethodCallTransaction(request: InstanceMethodCallTransactionRequestModel): StorageValueModel

    @Throws(NetworkException::class)
    fun addStaticMethodCallTransaction(request: StaticMethodCallTransactionRequestModel): StorageValueModel

    @Throws(NetworkException::class)
    fun postJarStoreTransaction(request: JarStoreTransactionRequestModel): TransactionReferenceModel

    @Throws(NetworkException::class)
    fun postConstructorCallTransaction(request: ConstructorCallTransactionRequestModel): TransactionReferenceModel

    @Throws(NetworkException::class)
    fun postInstanceMethodCallTransaction(request: InstanceMethodCallTransactionRequestModel): TransactionReferenceModel

    @Throws(NetworkException::class)
    fun postStaticMethodCallTransaction(request: StaticMethodCallTransactionRequestModel): TransactionReferenceModel

    @Throws(NetworkException::class)
    fun runInstanceMethodCallTransaction(request: InstanceMethodCallTransactionRequestModel): StorageValueModel

    @Throws(NetworkException::class)
    fun runStaticMethodCallTransaction(request: StaticMethodCallTransactionRequestModel): StorageValueModel
}