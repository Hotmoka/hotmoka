package io.hotmoka.network.thin.client

import com.google.gson.Gson
import com.google.gson.JsonObject
import io.hotmoka.network.thin.client.exceptions.InternalFailureException
import io.hotmoka.network.thin.client.exceptions.NetworkException
import io.hotmoka.network.thin.client.models.errors.NodeError
import io.hotmoka.network.thin.client.models.requests.*
import io.hotmoka.network.thin.client.models.responses.ResponseEntity
import io.hotmoka.network.thin.client.models.responses.SignatureAlgorithm
import io.hotmoka.network.thin.client.models.responses.TransactionRestResponse
import io.hotmoka.network.thin.client.models.updates.ClassTag
import io.hotmoka.network.thin.client.models.updates.State
import io.hotmoka.network.thin.client.models.values.StorageReference
import io.hotmoka.network.thin.client.models.values.StorageValue
import io.hotmoka.network.thin.client.models.values.TransactionReference
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.NoSuchAlgorithmException
import java.util.*
import java.util.concurrent.Callable


class RemoteNodeClient(url: String): RemoteNode {
    private val MEDIA_TYPE_JSON = "application/json; charset=utf-8".toMediaType()
    private val gson = Gson()
    private val httpUrl = "http://$url"
    private val websocketUrl = "ws://$url"
    private val httpClient = OkHttpClient()

    override fun getTakamakaCode(): TransactionReference {
        return wrapNetworkExceptionForNoSuchElementException{ get("$httpUrl/get/takamakaCode") { jsonToModel(it, TransactionReference::class.java) } }
    }

    override fun getManifest(): StorageReference {
        return wrapNetworkExceptionForNoSuchElementException{ get("$httpUrl/get/manifest") { jsonToModel(it, StorageReference::class.java) } }
    }

    override fun getState(request: StorageReference): State {
        return wrapNetworkExceptionForNoSuchElementException{ post("$httpUrl/get/state", request) { jsonToModel(it, State::class.java) } }
    }

    override fun getClassTag(request: StorageReference): ClassTag {
        return wrapNetworkExceptionForNoSuchElementException{ post("$httpUrl/get/classTag", request) { jsonToModel(it, ClassTag::class.java) } }
    }

    override fun getRequest(reference: TransactionReference): TransactionRequest<*> {
        return wrapNetworkExceptionForNoSuchElementException{ post("$httpUrl/get/request", reference) { jsonToTransactionRequest(it) } }
    }

    override fun getSignatureAlgorithmForRequests(): SignatureAlgorithm {
        return wrapNetworkExceptionForNoSuchAlgorithmException{ get("$httpUrl/get/signatureAlgorithmForRequests") { jsonToModel(it, SignatureAlgorithm::class.java) } }
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

    /**
     * Invokes a GET request to an endpoint and returns the result.
     * @param url the url endpoint
     * @param deserialize the lambda function to deserialize the json into the model T
     * @return the result
     */
    private fun <T> get(url: String, deserialize: (String) -> T): T {
        val request = Request.Builder()
                .url(url)
                .build()

        return deserialize(httpCall(request))
    }

    /**
     * Invokes a POST request to an endpoint.
     * @param url the url endpoint
     * @param body the body of the POST
     * @param deserialize the lambda function to deserialize the json into the model T
     * @return the result
     */
    private fun <T, R> post(url: String, body: R, deserialize: (String) -> T): T {
        val bodyJson = this.gson.toJson(body)
        val request = Request.Builder()
                .url(url)
                .post(bodyJson.toRequestBody(MEDIA_TYPE_JSON))
                .build()

        return deserialize(httpCall(request))
    }

    /**
     * It performs an http call GET or POST and returns the result as json.
     * @param request the request
     * @return the result as json
     */
    private fun httpCall(request: Request): String {
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

            return json ?: throw NetworkException(NodeError("Cannot deserialize null body", InternalFailureException::class.java.simpleName))
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

    @Throws(NoSuchAlgorithmException::class)
    private fun <T> wrapNetworkExceptionForNoSuchAlgorithmException(callable: Callable<T>): T {
        try {
            return callable.call()
        } catch (networkException: NetworkException) {
            if (networkException.error.exceptionClassName.equals(NoSuchAlgorithmException::class.java.name))
                throw NoSuchAlgorithmException(networkException.error.message)
            else
                throw InternalFailureException(networkException.error.message)
        } catch (e: Exception) {
            if (e.message != null) throw InternalFailureException(e.message!!) else throw InternalFailureException("An error occured")
        }
    }

    /**
     * Deserializes a json into a model T.
     * @param json the json
     * @param type the type of the model
     * @return the model
     */
    private fun <T> jsonToModel(json: String, type: Class<T>): T {
        try {
            return this.gson.fromJson(json, type)
        } catch (e: Exception) {
            throw InternalFailureException("Cannot deserialize object")
        }
    }

    /**
     * Deserializes the json into a transaction request model.
     * @param json the json
     * @return the transaction request model
     */
    private fun jsonToTransactionRequest(json: String): TransactionRequest<*> {
        val jsonObject = this.gson.fromJson(json, JsonObject::class.java)
                ?: throw InternalFailureException("Unexpected null transaction request serialized object")

        val transactionRequestType = jsonObject.get("type")?.asString
                ?: throw InternalFailureException("Unexpected transaction request model type")
        val transactionRequestModel = jsonObject.get("transactionRequestModel").asJsonObject
        val basePackage = "io.hotmoka.network.models.requests.";

        if (transactionRequestType == basePackage + "ConstructorCallTransactionRequestModel")
            return TransactionRequest(transactionRequestType, gson.fromJson(transactionRequestModel, ConstructorCallTransaction::class.java))
        else if (transactionRequestType == basePackage + "GameteCreationTransactionRequestModel")
            return TransactionRequest(transactionRequestType, gson.fromJson(transactionRequestModel, GameteCreationTransaction::class.java))
        else if (transactionRequestType == basePackage + "InitializationTransactionRequestModel")
            return TransactionRequest(transactionRequestType, gson.fromJson(transactionRequestModel, InitializationTransaction::class.java))
        else if (transactionRequestType == basePackage + "InstanceMethodCallTransactionRequestModel")
            return TransactionRequest(transactionRequestType, gson.fromJson(transactionRequestModel, InstanceMethodCallTransaction::class.java))
        else if (transactionRequestType == basePackage + "JarStoreInitialTransactionRequestModel")
            return TransactionRequest(transactionRequestType, gson.fromJson(transactionRequestModel, JarStoreInitialTransaction::class.java))
        else if (transactionRequestType == basePackage + "JarStoreTransactionRequestModel")
            return TransactionRequest(transactionRequestType, gson.fromJson(transactionRequestModel, JarStoreTransaction::class.java))
        else if (transactionRequestType == basePackage + "RedGreenGameteCreationTransactionRequestModel")
            return TransactionRequest(transactionRequestType, gson.fromJson(transactionRequestModel, RedGreenGameteCreationTransaction::class.java))
        else if (transactionRequestType == basePackage + "StaticMethodCallTransactionRequestModel")
            return TransactionRequest(transactionRequestType, gson.fromJson(transactionRequestModel, StaticMethodCallTransaction::class.java))
        else
            throw InternalFailureException("Unexpected transaction request model of class " + transactionRequestType);
    }
}

