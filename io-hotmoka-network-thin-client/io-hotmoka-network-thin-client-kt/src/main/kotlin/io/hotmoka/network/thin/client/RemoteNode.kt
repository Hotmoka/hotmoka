package io.hotmoka.network.thin.client

import io.hotmoka.network.thin.client.exceptions.NetworkException
import io.hotmoka.network.thin.client.models.requests.*
import io.hotmoka.network.thin.client.models.responses.ResponseEntity
import io.hotmoka.network.thin.client.models.responses.SignatureAlgorithm
import io.hotmoka.network.thin.client.models.responses.TransactionRestResponse
import io.hotmoka.network.thin.client.models.updates.ClassTag
import io.hotmoka.network.thin.client.models.updates.State
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
    fun getState(request: StorageReference): State

    @Throws(NoSuchElementException::class)
    fun getClassTag(request: StorageReference): ClassTag

    @Throws(NoSuchElementException::class)
    fun getRequest(reference: TransactionReference): TransactionRequest<*>

    @Throws(NoSuchAlgorithmException::class)
    fun getSignatureAlgorithmForRequests(): SignatureAlgorithm

    @Throws(NetworkException::class)
    fun getResponse(reference: TransactionReference): TransactionRestResponse<*>

    @Throws(NetworkException::class)
    fun getPolledResponse(reference: TransactionReference): TransactionRestResponse<*>

    @Throws(NetworkException::class)
    fun addJarStoreInitialTransaction(request: JarStoreInitialTransaction): TransactionReference

    @Throws(NetworkException::class)
    fun addGameteCreationTransaction(request: GameteCreationTransaction): StorageReference

    @Throws(NetworkException::class)
    fun addRedGreenGameteCreationTransaction(request: RedGreenGameteCreationTransaction): StorageReference

    @Throws(NetworkException::class)
    fun addInitializationTransaction(request: InitializationTransaction): ResponseEntity<Void>

    @Throws(NetworkException::class)
    fun addJarStoreTransaction(request: JarStoreTransaction): TransactionReference

    @Throws(NetworkException::class)
    fun addConstructorCallTransaction(request: ConstructorCallTransaction): StorageReference

    @Throws(NetworkException::class)
    fun addInstanceMethodCallTransaction(request: InstanceMethodCallTransaction): StorageValue

    @Throws(NetworkException::class)
    fun addStaticMethodCallTransaction(request: StaticMethodCallTransaction): StorageValue

    @Throws(NetworkException::class)
    fun postJarStoreTransaction(request: JarStoreTransaction): TransactionReference

    @Throws(NetworkException::class)
    fun postConstructorCallTransaction(request: ConstructorCallTransaction): TransactionReference

    @Throws(NetworkException::class)
    fun postInstanceMethodCallTransaction(request: InstanceMethodCallTransaction): TransactionReference

    @Throws(NetworkException::class)
    fun postStaticMethodCallTransaction(request: StaticMethodCallTransaction): TransactionReference

    @Throws(NetworkException::class)
    fun runInstanceMethodCallTransaction(request: InstanceMethodCallTransaction): StorageValue

    @Throws(NetworkException::class)
    fun runStaticMethodCallTransaction(request: StaticMethodCallTransaction): StorageValue
}