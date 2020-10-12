package io.hotmoka.network.thin.client.internal

import com.google.gson.Gson
import io.hotmoka.network.thin.client.RemoteNode
import io.hotmoka.network.thin.client.internal.exceptions.InternalFailureException
import io.hotmoka.network.thin.client.internal.exceptions.NetworkException
import io.hotmoka.network.thin.client.internal.models.errors.NodeError
import io.hotmoka.network.thin.client.internal.models.requests.*
import io.hotmoka.network.thin.client.internal.models.responses.ResponseEntity
import io.hotmoka.network.thin.client.internal.models.responses.SignatureAlgorithmResponse
import io.hotmoka.network.thin.client.internal.models.responses.TransactionRestResponse
import io.hotmoka.network.thin.client.internal.models.updates.ClassTag
import io.hotmoka.network.thin.client.internal.models.updates.State
import io.hotmoka.network.thin.client.internal.models.values.StorageReference
import io.hotmoka.network.thin.client.internal.models.values.StorageValue
import io.hotmoka.network.thin.client.internal.models.values.TransactionReference
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.*
import java.util.concurrent.Callable


class RemoteNodeClient(url: String): RemoteNode {
    private val MEDIA_TYPE_JSON = "application/json; charset=utf-8".toMediaType()
    private val gson = Gson()
    private val httpUrl = "http://$url"
    private val websocketUrl = "ws://$url"
    private val httpClient = OkHttpClient()

    override fun getTakamakaCode(): TransactionReference {
        return wrapNetworkExceptionForNoSuchElementException{ get("$httpUrl/get/takamakaCode", TransactionReference::class.java) }
    }

    override fun getManifest(): StorageReference {
        return wrapNetworkExceptionForNoSuchElementException{ get("$httpUrl/get/manifest", StorageReference::class.java) }
    }

    override fun getState(request: StorageReference): State {
        return wrapNetworkExceptionForNoSuchElementException{ post("$httpUrl/get/state", request, State::class.java) }
    }

    override fun getClassTag(request: StorageReference): ClassTag {
        return wrapNetworkExceptionForNoSuchElementException{ post("$httpUrl/get/classTag", request, ClassTag::class.java) }
    }

    override fun getRequest(reference: TransactionReference): TransactionRestRequest<*> {
        TODO("Not yet implemented")
    }

    override fun getSignatureAlgorithmForRequests(): SignatureAlgorithmResponse {
        TODO("Not yet implemented")
    }

    override fun getResponse(reference: TransactionReference): TransactionRestResponse<*> {
        TODO("Not yet implemented")
    }

    override fun getPolledResponse(reference: TransactionReference): TransactionRestResponse<*> {
        TODO("Not yet implemented")
    }

    override fun addJarStoreInitialTransaction(request: JarStoreInitialTransaction): TransactionReference {
        TODO("Not yet implemented")
    }

    override fun addGameteCreationTransaction(request: GameteCreationTransaction): StorageReference {
        TODO("Not yet implemented")
    }

    override fun addRedGreenGameteCreationTransaction(request: RedGreenGameteCreationTransaction): StorageReference {
        TODO("Not yet implemented")
    }

    override fun addInitializationTransaction(request: InitializationTransaction): ResponseEntity<Void> {
        TODO("Not yet implemented")
    }

    override fun addJarStoreTransaction(request: JarStoreTransaction): TransactionReference {
        TODO("Not yet implemented")
    }

    override fun addConstructorCallTransaction(request: ConstructorCallTransaction): StorageReference {
        TODO("Not yet implemented")
    }

    override fun addInstanceMethodCallTransaction(request: InstanceMethodCallTransaction): StorageValue {
        TODO("Not yet implemented")
    }

    override fun addStaticMethodCallTransaction(request: StaticMethodCallTransaction): StorageValue {
        TODO("Not yet implemented")
    }

    override fun postJarStoreTransaction(request: JarStoreTransaction): TransactionReference {
        TODO("Not yet implemented")
    }

    override fun postConstructorCallTransaction(request: ConstructorCallTransaction): TransactionReference {
        TODO("Not yet implemented")
    }

    override fun postInstanceMethodCallTransaction(request: InstanceMethodCallTransaction): TransactionReference {
        TODO("Not yet implemented")
    }

    override fun postStaticMethodCallTransaction(request: StaticMethodCallTransaction): TransactionReference {
        TODO("Not yet implemented")
    }

    override fun runInstanceMethodCallTransaction(request: InstanceMethodCallTransaction): StorageValue {
        TODO("Not yet implemented")
    }

    override fun runStaticMethodCallTransaction(request: StaticMethodCallTransaction): StorageValue {
        TODO("Not yet implemented")
    }


    private fun <T> get(url: String, resultType: Class<T>): T {
        val request = Request.Builder()
                .url(url)
                .build()

        return call(request, resultType)
    }

    private fun <T, R> post(url: String, body: R, resultType: Class<T>): T {
        val bodyJson = this.gson.toJson(body)
        val request = Request.Builder()
                .url(url)
                .post(bodyJson.toRequestBody(MEDIA_TYPE_JSON))
                .build()

        return call(request, resultType)
    }

    private fun <T> call(request: Request, resultType: Class<T>): T {
        this.httpClient.newCall(request).execute().use { response ->
            val json = response.body?.string()

            // handle error
            if (!response.isSuccessful) {
                if (response.code.toString().startsWith("4")) {
                    throw NetworkException(this.gson.fromJson(json, NodeError::class.java))
                } else {
                    throw NetworkException(NodeError("failed to process the request - response code ($response.code)", InternalFailureException::class.java.simpleName))
                }
            }

            return if (json != null) this.gson.fromJson(json, resultType) else throw NetworkException(NodeError("Cannot deserialize result", InternalFailureException::class.java.simpleName))
        }
    }

    @Throws(NoSuchElementException::class)
    private fun <T> wrapNetworkExceptionForNoSuchElementException(callable: Callable<T>): T {
        try {
            return callable.call()
        } catch (networkException: NetworkException) {
            if (networkException.error.exceptionClassName.equals(NoSuchElementException::class.java.name))
                throw NoSuchElementException(networkException.error.message)
            else
                throw InternalFailureException(networkException.error.message)
        } catch (e: Exception) {
            if (e.message != null) throw InternalFailureException(e.message!!) else throw InternalFailureException("An error occured")
        }
    }
}