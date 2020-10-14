package io.hotmoka.network.thin.client

import com.google.gson.Gson
import com.google.gson.JsonObject
import io.hotmoka.network.thin.client.exceptions.InternalFailureException
import io.hotmoka.network.thin.client.exceptions.NetworkException
import io.hotmoka.network.thin.client.exceptions.TransactionRejectedException
import io.hotmoka.network.thin.client.models.errors.ErrorModel
import io.hotmoka.network.thin.client.models.requests.*
import io.hotmoka.network.thin.client.models.responses.SignatureAlgorithmModel
import io.hotmoka.network.thin.client.models.responses.TransactionRestResponseModel
import io.hotmoka.network.thin.client.models.updates.ClassTagModel
import io.hotmoka.network.thin.client.models.updates.StateModel
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

    override fun getState(request: StorageReference): StateModel {
        return wrapNetworkExceptionForNoSuchElementException{ post("$httpUrl/get/state", request) { jsonToModel(it, StateModel::class.java) } }
    }

    override fun getClassTag(request: StorageReference): ClassTagModel {
        return wrapNetworkExceptionForNoSuchElementException{ post("$httpUrl/get/classTag", request) { jsonToModel(it, ClassTagModel::class.java) } }
    }

    override fun getRequest(reference: TransactionReference): TransactionRequestModel<*> {
        return wrapNetworkExceptionForNoSuchElementException{ post("$httpUrl/get/request", reference) { jsonToTransactionRequest(it) } }
    }

    override fun getSignatureAlgorithmForRequests(): SignatureAlgorithmModel {
        return wrapNetworkExceptionForNoSuchAlgorithmException{ get("$httpUrl/get/signatureAlgorithmForRequests") { jsonToModel(it, SignatureAlgorithmModel::class.java) } }
    }

    override fun getResponse(reference: TransactionReference): TransactionRestResponseModel<*> {
        TODO("Not yet implemented")
    }

    override fun getPolledResponse(reference: TransactionReference): TransactionRestResponseModel<*> {
        TODO("Not yet implemented")
    }

    override fun addJarStoreInitialTransaction(request: JarStoreInitialTransactionRequestModel): TransactionReference {
        TODO("Not yet implemented")
    }

    override fun addGameteCreationTransaction(request: GameteCreationTransactionRequestModel): StorageReference {
        TODO("Not yet implemented")
    }

    override fun addRedGreenGameteCreationTransaction(request: RedGreenGameteCreationTransactionRequestModel): StorageReference {
        TODO("Not yet implemented")
    }

    override fun addInitializationTransaction(request: InitializationTransactionRequestModel): Void {
        TODO("Not yet implemented")
    }

    override fun addJarStoreTransaction(request: JarStoreTransactionRequestModel): TransactionReference {
        TODO("Not yet implemented")
    }

    override fun addConstructorCallTransaction(request: ConstructorCallTransactionRequestModel): StorageReference {
        TODO("Not yet implemented")
    }

    override fun addInstanceMethodCallTransaction(request: InstanceMethodCallTransactionRequestModel): StorageValue {
        TODO("Not yet implemented")
    }

    override fun addStaticMethodCallTransaction(request: StaticMethodCallTransactionRequestModel): StorageValue {
        TODO("Not yet implemented")
    }

    override fun postJarStoreTransaction(request: JarStoreTransactionRequestModel): TransactionReference {
        TODO("Not yet implemented")
    }

    override fun postConstructorCallTransaction(request: ConstructorCallTransactionRequestModel): TransactionReference {
        TODO("Not yet implemented")
    }

    override fun postInstanceMethodCallTransaction(request: InstanceMethodCallTransactionRequestModel): TransactionReference {
        TODO("Not yet implemented")
    }

    override fun postStaticMethodCallTransaction(request: StaticMethodCallTransactionRequestModel): TransactionReference {
        TODO("Not yet implemented")
    }

    override fun runInstanceMethodCallTransaction(request: InstanceMethodCallTransactionRequestModel): StorageValue {
        TODO("Not yet implemented")
    }

    override fun runStaticMethodCallTransaction(request: StaticMethodCallTransactionRequestModel): StorageValue {
        TODO("Not yet implemented")
    }

    /**
     * Invokes a GET request to an endpoint and returns the result.
     * @param url the url endpoint
     * @param deserialize the lambda function to deserialize the json into the model T
     * @return the result
     */
    private fun <T> get(url: String, deserialize: (String?) -> T): T {
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
    private fun <T, R> post(url: String, body: R, deserialize: (String?) -> T): T {
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
    private fun httpCall(request: Request): String? {
        this.httpClient.newCall(request).execute().use { response ->
            val json = response.body?.string()

            // handle error
            if (!response.isSuccessful) {
                if (response.code.toString().startsWith("4")) {
                    throw NetworkException(this.gson.fromJson(json, ErrorModel::class.java))
                } else {
                    throw NetworkException(ErrorModel("failed to process the request - response code ($response.code)", InternalFailureException::class.java.simpleName))
                }
            }

            return json
        }
    }


    @Throws(NoSuchElementException::class)
    private fun <T> wrapNetworkExceptionForNoSuchElementException(callable: Callable<T>): T {
        try {
            return callable.call()
        } catch (networkException: NetworkException) {
            if (networkException.errorModel.exceptionClassName.equals(NoSuchElementException::class.java.name))
                throw NoSuchElementException(networkException.errorModel.message)
            else
                throw InternalFailureException(networkException.errorModel.message)
        } catch (e: Exception) {
            if (e.message != null) throw InternalFailureException(e.message!!) else throw InternalFailureException("An error occured")
        }
    }

    @Throws(NoSuchAlgorithmException::class)
    private fun <T> wrapNetworkExceptionForNoSuchAlgorithmException(callable: Callable<T>): T {
        try {
            return callable.call()
        } catch (networkException: NetworkException) {
            if (networkException.errorModel.exceptionClassName == NoSuchAlgorithmException::class.java.name)
                throw NoSuchAlgorithmException(networkException.errorModel.message)
            else
                throw InternalFailureException(networkException.errorModel.message)
        } catch (e: Exception) {
            if (e.message != null) throw InternalFailureException(e.message!!) else throw InternalFailureException("An error occured")
        }
    }

    @Throws(NoSuchAlgorithmException::class, TransactionRejectedException::class)
    private fun <T> wrapNetworkExceptionForResponseAtException(callable: Callable<T>): T {
        try {
            return callable.call();
        } catch (networkException: NetworkException) {
            when (networkException.errorModel.exceptionClassName) {
                TransactionRejectedException::class.java.name -> throw TransactionRejectedException(networkException.errorModel.message)
                NoSuchElementException::class.java.name -> throw NoSuchElementException(networkException.errorModel.message)
                else -> throw InternalFailureException(networkException.errorModel.message)
            };
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
    private fun <T> jsonToModel(json: String?, type: Class<T>): T {
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
    private fun jsonToTransactionRequest(json: String?): TransactionRequestModel<*> {
        if (json == null)
            throw InternalFailureException("Unexpected null transaction request model")

        val jsonObject = this.gson.fromJson(json, JsonObject::class.java)
                ?: throw InternalFailureException("Unexpected null transaction request serialized object")

        val transactionRequestType = jsonObject.get("type")?.asString
                ?: throw InternalFailureException("Unexpected null type of transaction request")
        val transactionRequestModel = jsonObject.get("transactionRequestModel")?.asJsonObject
                ?: throw InternalFailureException("Unexpected null transactionRequestModel")
        val basePackage = "io.hotmoka.network.models.requests.";

        return when (transactionRequestType) {
            basePackage + ConstructorCallTransactionRequestModel::class.java -> TransactionRequestModel(transactionRequestType, gson.fromJson(transactionRequestModel, ConstructorCallTransactionRequestModel::class.java))
            basePackage + GameteCreationTransactionRequestModel::class.java -> TransactionRequestModel(transactionRequestType, gson.fromJson(transactionRequestModel, GameteCreationTransactionRequestModel::class.java))
            basePackage + InitializationTransactionRequestModel::class.java -> TransactionRequestModel(transactionRequestType, gson.fromJson(transactionRequestModel, InitializationTransactionRequestModel::class.java))
            basePackage + InstanceMethodCallTransactionRequestModel::class.java -> TransactionRequestModel(transactionRequestType, gson.fromJson(transactionRequestModel, InstanceMethodCallTransactionRequestModel::class.java))
            basePackage + JarStoreInitialTransactionRequestModel::class.java -> TransactionRequestModel(transactionRequestType, gson.fromJson(transactionRequestModel, JarStoreInitialTransactionRequestModel::class.java))
            basePackage + JarStoreTransactionRequestModel::class.java -> TransactionRequestModel(transactionRequestType, gson.fromJson(transactionRequestModel, JarStoreTransactionRequestModel::class.java))
            basePackage + RedGreenGameteCreationTransactionRequestModel::class.java -> TransactionRequestModel(transactionRequestType, gson.fromJson(transactionRequestModel, RedGreenGameteCreationTransactionRequestModel::class.java))
            basePackage + StaticMethodCallTransactionRequestModel::class.java -> TransactionRequestModel(transactionRequestType, gson.fromJson(transactionRequestModel, StaticMethodCallTransactionRequestModel::class.java))
            else -> throw InternalFailureException("Unexpected transaction request model of class $transactionRequestType")
        }
    }


}

