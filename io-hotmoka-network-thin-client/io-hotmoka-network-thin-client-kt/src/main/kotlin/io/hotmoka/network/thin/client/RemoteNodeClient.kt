package io.hotmoka.network.thin.client

import com.google.gson.Gson
import com.google.gson.JsonObject
import io.hotmoka.network.thin.client.exceptions.InternalFailureException
import io.hotmoka.network.thin.client.exceptions.NetworkException
import io.hotmoka.network.thin.client.exceptions.TransactionRejectedException
import io.hotmoka.network.thin.client.models.errors.ErrorModel
import io.hotmoka.network.thin.client.models.requests.*
import io.hotmoka.network.thin.client.models.responses.*
import io.hotmoka.network.thin.client.models.updates.ClassTagModel
import io.hotmoka.network.thin.client.models.updates.StateModel
import io.hotmoka.network.thin.client.models.values.StorageReferenceModel
import io.hotmoka.network.thin.client.models.values.StorageValueModel
import io.hotmoka.network.thin.client.models.values.TransactionReferenceModel
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.NoSuchAlgorithmException
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.TimeoutException


class RemoteNodeClient(url: String): RemoteNode {
    private val MEDIA_TYPE_JSON = "application/json; charset=utf-8".toMediaType()
    private val gson = Gson()
    private val httpUrl = "http://$url"
    private val websocketUrl = "ws://$url"
    private val httpClient = OkHttpClient()
    private val hotmokaExceptionPackage = "io.hotmoka.beans."

    override fun getTakamakaCode(): TransactionReferenceModel {
        return wrapNetworkExceptionForNoSuchElementException{ get("$httpUrl/get/takamakaCode") { jsonToModel(it, TransactionReferenceModel::class.java) } }
    }

    override fun getManifest(): StorageReferenceModel {
        return wrapNetworkExceptionForNoSuchElementException{ get("$httpUrl/get/manifest") { jsonToModel(it, StorageReferenceModel::class.java) } }
    }

    override fun getState(request: StorageReferenceModel): StateModel {
        return wrapNetworkExceptionForNoSuchElementException{ post("$httpUrl/get/state", request) { jsonToModel(it, StateModel::class.java) } }
    }

    override fun getClassTag(request: StorageReferenceModel): ClassTagModel {
        return wrapNetworkExceptionForNoSuchElementException{ post("$httpUrl/get/classTag", request) { jsonToModel(it, ClassTagModel::class.java) } }
    }

    override fun getRequest(reference: TransactionReferenceModel): TransactionRestRequestModel<*> {
        return wrapNetworkExceptionForNoSuchElementException{ post("$httpUrl/get/request", reference) { jsonToTransactionRequest(it) } }
    }

    override fun getSignatureAlgorithmForRequests(): SignatureAlgorithmResponseModel {
        return wrapNetworkExceptionForNoSuchAlgorithmException{ get("$httpUrl/get/signatureAlgorithmForRequests") { jsonToModel(it, SignatureAlgorithmResponseModel::class.java) } }
    }

    override fun getResponse(reference: TransactionReferenceModel): TransactionRestResponseModel<*> {
        return wrapNetworkExceptionForResponseAtException{ post("$httpUrl/get/response", reference) { jsonToTransactionResponse(it) } }
    }

    override fun getPolledResponse(reference: TransactionReferenceModel): TransactionRestResponseModel<*> {
        return wrapNetworkExceptionForPolledResponseException{ post("$httpUrl/get/polledResponse", reference) { jsonToTransactionResponse(it) } }
    }

    override fun addJarStoreInitialTransaction(request: JarStoreInitialTransactionRequestModel): TransactionReferenceModel {
        TODO("Not yet implemented")
    }

    override fun addGameteCreationTransaction(request: GameteCreationTransactionRequestModel): StorageReferenceModel {
        TODO("Not yet implemented")
    }

    override fun addRedGreenGameteCreationTransaction(request: RedGreenGameteCreationTransactionRequestModel): StorageReferenceModel {
        TODO("Not yet implemented")
    }

    override fun addInitializationTransaction(request: InitializationTransactionRequestModel): Void {
        TODO("Not yet implemented")
    }

    override fun addJarStoreTransaction(request: JarStoreTransactionRequestModel): TransactionReferenceModel {
        TODO("Not yet implemented")
    }

    override fun addConstructorCallTransaction(request: ConstructorCallTransactionRequestModel): StorageReferenceModel {
        TODO("Not yet implemented")
    }

    override fun addInstanceMethodCallTransaction(request: InstanceMethodCallTransactionRequestModel): StorageValueModel {
        TODO("Not yet implemented")
    }

    override fun addStaticMethodCallTransaction(request: StaticMethodCallTransactionRequestModel): StorageValueModel {
        TODO("Not yet implemented")
    }

    override fun postJarStoreTransaction(request: JarStoreTransactionRequestModel): TransactionReferenceModel {
        TODO("Not yet implemented")
    }

    override fun postConstructorCallTransaction(request: ConstructorCallTransactionRequestModel): TransactionReferenceModel {
        TODO("Not yet implemented")
    }

    override fun postInstanceMethodCallTransaction(request: InstanceMethodCallTransactionRequestModel): TransactionReferenceModel {
        TODO("Not yet implemented")
    }

    override fun postStaticMethodCallTransaction(request: StaticMethodCallTransactionRequestModel): TransactionReferenceModel {
        TODO("Not yet implemented")
    }

    override fun runInstanceMethodCallTransaction(request: InstanceMethodCallTransactionRequestModel): StorageValueModel {
        TODO("Not yet implemented")
    }

    override fun runStaticMethodCallTransaction(request: StaticMethodCallTransactionRequestModel): StorageValueModel {
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
            if (networkException.errorModel.exceptionClassName == NoSuchElementException::class.java.name)
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
                hotmokaExceptionPackage + TransactionRejectedException::class.java.simpleName -> throw TransactionRejectedException(networkException.errorModel.message)
                NoSuchElementException::class.java.name -> throw NoSuchElementException(networkException.errorModel.message)
                else -> throw InternalFailureException(networkException.errorModel.message)
            }
        } catch (e: Exception) {
            if (e.message != null) throw InternalFailureException(e.message!!) else throw InternalFailureException("An error occured")
        }
    }

    @Throws(TransactionRejectedException::class, TimeoutException::class, InterruptedException::class)
    private fun <T> wrapNetworkExceptionForPolledResponseException(callable: Callable<T>): T {
        try {
            return callable.call();
        } catch (networkException: NetworkException) {
            when (networkException.errorModel.exceptionClassName) {
                hotmokaExceptionPackage + TransactionRejectedException::class.java.simpleName -> throw TransactionRejectedException(networkException.errorModel.message)
                TimeoutException::class.java.name -> throw NoSuchElementException(networkException.errorModel.message)
                InterruptedException::class.java.name -> throw NoSuchElementException(networkException.errorModel.message)
                else -> throw InternalFailureException(networkException.errorModel.message)
            }
        }
        catch (e: Exception) {
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
    private fun jsonToTransactionRequest(json: String?): TransactionRestRequestModel<*> {
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
            basePackage + ConstructorCallTransactionRequestModel::class.simpleName -> TransactionRestRequestModel(transactionRequestType, gson.fromJson(transactionRequestModel, ConstructorCallTransactionRequestModel::class.java))
            basePackage + GameteCreationTransactionRequestModel::class.simpleName -> TransactionRestRequestModel(transactionRequestType, gson.fromJson(transactionRequestModel, GameteCreationTransactionRequestModel::class.java))
            basePackage + InitializationTransactionRequestModel::class.simpleName -> TransactionRestRequestModel(transactionRequestType, gson.fromJson(transactionRequestModel, InitializationTransactionRequestModel::class.java))
            basePackage + InstanceMethodCallTransactionRequestModel::class.simpleName -> TransactionRestRequestModel(transactionRequestType, gson.fromJson(transactionRequestModel, InstanceMethodCallTransactionRequestModel::class.java))
            basePackage + JarStoreInitialTransactionRequestModel::class.simpleName -> TransactionRestRequestModel(transactionRequestType, gson.fromJson(transactionRequestModel, JarStoreInitialTransactionRequestModel::class.java))
            basePackage + JarStoreTransactionRequestModel::class.simpleName -> TransactionRestRequestModel(transactionRequestType, gson.fromJson(transactionRequestModel, JarStoreTransactionRequestModel::class.java))
            basePackage + RedGreenGameteCreationTransactionRequestModel::class.simpleName -> TransactionRestRequestModel(transactionRequestType, gson.fromJson(transactionRequestModel, RedGreenGameteCreationTransactionRequestModel::class.java))
            basePackage + StaticMethodCallTransactionRequestModel::class.simpleName -> TransactionRestRequestModel(transactionRequestType, gson.fromJson(transactionRequestModel, StaticMethodCallTransactionRequestModel::class.java))
            else -> throw InternalFailureException("Unexpected transaction request model of class $transactionRequestType")
        }
    }


    /**
     * Deserializes the json into a transaction response model.
     * @param json the json
     * @return the transaction request model
     */
    private fun jsonToTransactionResponse(json: String?): TransactionRestResponseModel<*> {
        if (json == null)
            throw InternalFailureException("Unexpected null transaction response model")

        val jsonObject = this.gson.fromJson(json, JsonObject::class.java)
                ?: throw InternalFailureException("Unexpected null transaction response serialized object")

        val transactionResponseType = jsonObject.get("type")?.asString
                ?: throw InternalFailureException("Unexpected null type of transaction request")
        val transactionResponseModel = jsonObject.get("transactionResponseModel")?.asJsonObject
                ?: throw InternalFailureException("Unexpected null transactionResponseModel")
        val basePackage = "io.hotmoka.network.models.responses.";

        return when (transactionResponseType) {
            basePackage + JarStoreInitialTransactionResponseModel::class.simpleName -> TransactionRestResponseModel(transactionResponseType, gson.fromJson(transactionResponseModel, JarStoreInitialTransactionResponseModel::class.java))
            basePackage + JarStoreTransactionFailedResponseModel::class.simpleName -> TransactionRestResponseModel(transactionResponseType, gson.fromJson(transactionResponseModel, JarStoreTransactionFailedResponseModel::class.java))
            basePackage + JarStoreTransactionSuccessfulResponseModel::class.simpleName -> TransactionRestResponseModel(transactionResponseType, gson.fromJson(transactionResponseModel, JarStoreTransactionSuccessfulResponseModel::class.java))
            basePackage + GameteCreationTransactionResponseModel::class.simpleName -> TransactionRestResponseModel(transactionResponseType, gson.fromJson(transactionResponseModel, GameteCreationTransactionResponseModel::class.java))
            basePackage + InitializationTransactionResponseModel::class.simpleName -> TransactionRestResponseModel(transactionResponseType, gson.fromJson(transactionResponseModel, InitializationTransactionResponseModel::class.java))
            basePackage + ConstructorCallTransactionFailedResponseModel::class.simpleName -> TransactionRestResponseModel(transactionResponseType, gson.fromJson(transactionResponseModel, ConstructorCallTransactionFailedResponseModel::class.java))
            basePackage + ConstructorCallTransactionSuccessfulResponseModel::class.simpleName -> TransactionRestResponseModel(transactionResponseType, gson.fromJson(transactionResponseModel, ConstructorCallTransactionSuccessfulResponseModel::class.java))
            basePackage + ConstructorCallTransactionExceptionResponseModel::class.simpleName -> TransactionRestResponseModel(transactionResponseType, gson.fromJson(transactionResponseModel, ConstructorCallTransactionExceptionResponseModel::class.java))
            basePackage + MethodCallTransactionFailedResponseModel::class.simpleName -> TransactionRestResponseModel(transactionResponseType, gson.fromJson(transactionResponseModel, MethodCallTransactionFailedResponseModel::class.java))
            basePackage + MethodCallTransactionSuccessfulResponseModel::class.simpleName -> TransactionRestResponseModel(transactionResponseType, gson.fromJson(transactionResponseModel, MethodCallTransactionSuccessfulResponseModel::class.java))
            basePackage + MethodCallTransactionExceptionResponseModel::class.simpleName -> TransactionRestResponseModel(transactionResponseType, gson.fromJson(transactionResponseModel, MethodCallTransactionExceptionResponseModel::class.java))
            basePackage + VoidMethodCallTransactionSuccessfulResponseModel::class.simpleName -> TransactionRestResponseModel(transactionResponseType, gson.fromJson(transactionResponseModel, VoidMethodCallTransactionSuccessfulResponseModel::class.java))
            else -> throw InternalFailureException("Unexpected transaction response model of class $transactionResponseType")
        }
    }


}

