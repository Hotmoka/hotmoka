package io.hotmoka.network.thin.client

import io.hotmoka.network.thin.client.internal.exceptions.NetworkException
import io.hotmoka.network.thin.client.internal.models.requests.*
import io.hotmoka.network.thin.client.internal.models.responses.ResponseEntity
import io.hotmoka.network.thin.client.internal.models.responses.SignatureAlgorithmResponse
import io.hotmoka.network.thin.client.internal.models.responses.TransactionRestResponse
import io.hotmoka.network.thin.client.internal.models.updates.ClassTag
import io.hotmoka.network.thin.client.internal.models.updates.State
import io.hotmoka.network.thin.client.internal.models.values.StorageReference
import io.hotmoka.network.thin.client.internal.models.values.StorageValue

interface RemoteNode {

    @Throws(NetworkException::class)
    fun getTakamakaCode(): TransactionReference

    @Throws(NetworkException::class)
    fun getManifest(): StorageReference

    @Throws(NetworkException::class)
    fun getState(request: StorageReference?): State

    @Throws(NetworkException::class)
    fun getClassTag(request: StorageReference?): ClassTag

    @Throws(NetworkException::class)
    fun getRequest(reference: TransactionReference?): TransactionRestRequest<*>

    @Throws(NetworkException::class)
    fun getSignatureAlgorithmForRequests(): SignatureAlgorithmResponse

    @Throws(NetworkException::class)
    fun getResponse(reference: TransactionReference?): TransactionRestResponse<*>

    @Throws(NetworkException::class)
    fun getPolledResponse(reference: TransactionReference?): TransactionRestResponse<*>

    @Throws(NetworkException::class)
    fun addJarStoreInitialTransaction(request: JarStoreInitialTransaction?): TransactionReference

    @Throws(NetworkException::class)
    fun addGameteCreationTransaction(request: GameteCreationTransaction?): StorageReference

    @Throws(NetworkException::class)
    fun addRedGreenGameteCreationTransaction(request: RedGreenGameteCreationTransaction?): StorageReference

    @Throws(NetworkException::class)
    fun addInitializationTransaction(request: InitializationTransaction?): ResponseEntity<Void?>

    @Throws(NetworkException::class)
    fun addJarStoreTransaction(request: JarStoreTransaction?): TransactionReference

    @Throws(NetworkException::class)
    fun addConstructorCallTransaction(request: ConstructorCallTransaction?): StorageReference

    @Throws(NetworkException::class)
    fun addInstanceMethodCallTransaction(request: InstanceMethodCallTransaction?): StorageValue

    @Throws(NetworkException::class)
    fun addStaticMethodCallTransaction(request: StaticMethodCallTransaction?): StorageValue

    @Throws(NetworkException::class)
    fun postJarStoreTransaction(request: JarStoreTransaction?): TransactionReference

    @Throws(NetworkException::class)
    fun postConstructorCallTransaction(request: ConstructorCallTransaction?): TransactionReference

    @Throws(NetworkException::class)
    fun postInstanceMethodCallTransaction(request: InstanceMethodCallTransaction?): TransactionReference

    @Throws(NetworkException::class)
    fun postStaticMethodCallTransaction(request: StaticMethodCallTransaction?): TransactionReference

    @Throws(NetworkException::class)
    fun runInstanceMethodCallTransaction(request: InstanceMethodCallTransaction?): StorageValue

    @Throws(NetworkException::class)
    fun runStaticMethodCallTransaction(request: StaticMethodCallTransaction?): StorageValue
}