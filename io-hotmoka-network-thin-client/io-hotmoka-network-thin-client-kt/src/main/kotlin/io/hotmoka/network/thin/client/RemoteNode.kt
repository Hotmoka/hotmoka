package io.hotmoka.network.thin.client

import io.hotmoka.network.thin.client.exceptions.NetworkException
import io.hotmoka.network.thin.client.models.requests.*
import io.hotmoka.network.thin.client.models.responses.SignatureAlgorithmModel
import io.hotmoka.network.thin.client.models.responses.TransactionRestResponseModel
import io.hotmoka.network.thin.client.models.updates.ClassTagModel
import io.hotmoka.network.thin.client.models.updates.StateModel
import io.hotmoka.network.thin.client.models.values.StorageReference
import io.hotmoka.network.thin.client.models.values.StorageValue
import io.hotmoka.network.thin.client.models.values.TransactionReference
import java.security.NoSuchAlgorithmException
import java.util.NoSuchElementException

interface RemoteNode {

    @Throws(NoSuchElementException::class)
    fun getTakamakaCode(): TransactionReference

    @Throws(NoSuchElementException::class)
    fun getManifest(): StorageReference

    @Throws(NoSuchElementException::class)
    fun getState(request: StorageReference): StateModel

    @Throws(NoSuchElementException::class)
    fun getClassTag(request: StorageReference): ClassTagModel

    @Throws(NoSuchElementException::class)
    fun getRequest(reference: TransactionReference): TransactionRequestModel<*>

    @Throws(NoSuchAlgorithmException::class)
    fun getSignatureAlgorithmForRequests(): SignatureAlgorithmModel

    @Throws(NetworkException::class)
    fun getResponse(reference: TransactionReference): TransactionRestResponseModel<*>

    @Throws(NetworkException::class)
    fun getPolledResponse(reference: TransactionReference): TransactionRestResponseModel<*>

    @Throws(NetworkException::class)
    fun addJarStoreInitialTransaction(request: JarStoreInitialTransactionRequestModel): TransactionReference

    @Throws(NetworkException::class)
    fun addGameteCreationTransaction(request: GameteCreationTransactionRequestModel): StorageReference

    @Throws(NetworkException::class)
    fun addRedGreenGameteCreationTransaction(request: RedGreenGameteCreationTransactionRequestModel): StorageReference

    @Throws(NetworkException::class)
    fun addInitializationTransaction(request: InitializationTransactionRequestModel): Void

    @Throws(NetworkException::class)
    fun addJarStoreTransaction(request: JarStoreTransactionRequestModel): TransactionReference

    @Throws(NetworkException::class)
    fun addConstructorCallTransaction(request: ConstructorCallTransactionRequestModel): StorageReference

    @Throws(NetworkException::class)
    fun addInstanceMethodCallTransaction(request: InstanceMethodCallTransactionRequestModel): StorageValue

    @Throws(NetworkException::class)
    fun addStaticMethodCallTransaction(request: StaticMethodCallTransactionRequestModel): StorageValue

    @Throws(NetworkException::class)
    fun postJarStoreTransaction(request: JarStoreTransactionRequestModel): TransactionReference

    @Throws(NetworkException::class)
    fun postConstructorCallTransaction(request: ConstructorCallTransactionRequestModel): TransactionReference

    @Throws(NetworkException::class)
    fun postInstanceMethodCallTransaction(request: InstanceMethodCallTransactionRequestModel): TransactionReference

    @Throws(NetworkException::class)
    fun postStaticMethodCallTransaction(request: StaticMethodCallTransactionRequestModel): TransactionReference

    @Throws(NetworkException::class)
    fun runInstanceMethodCallTransaction(request: InstanceMethodCallTransactionRequestModel): StorageValue

    @Throws(NetworkException::class)
    fun runStaticMethodCallTransaction(request: StaticMethodCallTransactionRequestModel): StorageValue
}