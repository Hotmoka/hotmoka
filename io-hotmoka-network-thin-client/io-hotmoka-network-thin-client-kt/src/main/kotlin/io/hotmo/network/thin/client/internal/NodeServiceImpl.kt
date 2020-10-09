package io.hotmo.network.thin.client.internal

import io.hotmo.network.thin.client.NodeService
import io.hotmo.network.thin.client.internal.models.requests.*
import io.hotmo.network.thin.client.internal.models.responses.ResponseEntity
import io.hotmo.network.thin.client.internal.models.responses.SignatureAlgorithmResponse
import io.hotmo.network.thin.client.internal.models.responses.TransactionRestResponse
import io.hotmo.network.thin.client.internal.models.updates.ClassTag
import io.hotmo.network.thin.client.internal.models.updates.State
import io.hotmo.network.thin.client.internal.models.values.StorageReference
import io.hotmo.network.thin.client.internal.models.values.StorageValue

class NodeServiceImpl: NodeService {

    override fun getTakamakaCode(): TransactionReference? {
      //  TODO("Not yet implemented")
        return TransactionReference()
    }

    override fun getManifest(): StorageReference? {
        TODO("Not yet implemented")
    }

    override fun getState(request: StorageReference?): State? {
        TODO("Not yet implemented")
    }

    override fun getClassTag(request: StorageReference?): ClassTag? {
        TODO("Not yet implemented")
    }

    override fun getRequest(reference: TransactionReference?): TransactionRestRequest<*>? {
        TODO("Not yet implemented")
    }

    override fun getSignatureAlgorithmForRequests(): SignatureAlgorithmResponse? {
        TODO("Not yet implemented")
    }

    override fun getResponse(reference: TransactionReference?): TransactionRestResponse<*>? {
        TODO("Not yet implemented")
    }

    override fun getPolledResponse(reference: TransactionReference?): TransactionRestResponse<*>? {
        TODO("Not yet implemented")
    }

    override fun addJarStoreInitialTransaction(request: JarStoreInitialTransaction?): TransactionReference? {
        TODO("Not yet implemented")
    }

    override fun addGameteCreationTransaction(request: GameteCreationTransaction?): StorageReference? {
        TODO("Not yet implemented")
    }

    override fun addRedGreenGameteCreationTransaction(request: RedGreenGameteCreationTransaction?): StorageReference? {
        TODO("Not yet implemented")
    }

    override fun addInitializationTransaction(request: InitializationTransaction?): ResponseEntity<Void?>? {
        TODO("Not yet implemented")
    }

    override fun addJarStoreTransaction(request: JarStoreTransaction?): TransactionReference? {
        TODO("Not yet implemented")
    }

    override fun addConstructorCallTransaction(request: ConstructorCallTransaction?): StorageReference? {
        TODO("Not yet implemented")
    }

    override fun addInstanceMethodCallTransaction(request: InstanceMethodCallTransaction?): StorageValue? {
        TODO("Not yet implemented")
    }

    override fun addStaticMethodCallTransaction(request: StaticMethodCallTransaction?): StorageValue? {
        TODO("Not yet implemented")
    }

    override fun postJarStoreTransaction(request: JarStoreTransaction?): TransactionReference? {
        TODO("Not yet implemented")
    }

    override fun postConstructorCallTransaction(request: ConstructorCallTransaction?): TransactionReference? {
        TODO("Not yet implemented")
    }

    override fun postInstanceMethodCallTransaction(request: InstanceMethodCallTransaction?): TransactionReference? {
        TODO("Not yet implemented")
    }

    override fun postStaticMethodCallTransaction(request: StaticMethodCallTransaction?): TransactionReference? {
        TODO("Not yet implemented")
    }

    override fun runInstanceMethodCallTransaction(request: InstanceMethodCallTransaction?): StorageValue? {
        TODO("Not yet implemented")
    }

    override fun runStaticMethodCallTransaction(request: StaticMethodCallTransaction?): StorageValue? {
        TODO("Not yet implemented")
    }

}