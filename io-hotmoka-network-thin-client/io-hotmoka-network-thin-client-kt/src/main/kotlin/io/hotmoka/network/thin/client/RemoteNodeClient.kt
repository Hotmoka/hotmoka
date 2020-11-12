package io.hotmoka.network.thin.client

import com.google.gson.Gson
import com.google.gson.JsonObject
import io.hotmoka.network.thin.client.exceptions.*
import io.hotmoka.network.thin.client.models.errors.ErrorModel
import io.hotmoka.network.thin.client.models.requests.*
import io.hotmoka.network.thin.client.models.responses.*
import io.hotmoka.network.thin.client.models.updates.ClassTagModel
import io.hotmoka.network.thin.client.models.updates.StateModel
import io.hotmoka.network.thin.client.models.values.StorageReferenceModel
import io.hotmoka.network.thin.client.models.values.StorageValueModel
import io.hotmoka.network.thin.client.models.values.TransactionReferenceModel
import io.hotmoka.network.thin.client.suppliers.CodeSupplier
import io.hotmoka.network.thin.client.suppliers.JarSupplier
import io.hotmoka.network.thin.client.webSockets.StompClient
import io.hotmoka.network.thin.client.webSockets.Subscription
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.NoSuchAlgorithmException
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.function.BiConsumer


class RemoteNodeClient(url: String): RemoteNode {
    private val mediaTypeJson = "application/json; charset=utf-8".toMediaType()
    private val gson = Gson()
    private val httpUrl = "http://$url"
    private val httpClient = OkHttpClient.Builder().readTimeout(30, TimeUnit.SECONDS).build()
    private val hotmokaExceptionPackage = "io.hotmoka.beans."
    private val stompClient = StompClient("$url/node")

    init {
        stompClient.connect()
    }


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
        return wrapNetworkExceptionSimple{ post("$httpUrl/add/jarStoreInitialTransaction", request) { jsonToModel(it, TransactionReferenceModel::class.java) } }
    }

    override fun addGameteCreationTransaction(request: GameteCreationTransactionRequestModel): StorageReferenceModel {
        return wrapNetworkExceptionSimple{ post("$httpUrl/add/gameteCreationTransaction", request) { jsonToModel(it, StorageReferenceModel::class.java) } }
    }

    override fun addRedGreenGameteCreationTransaction(request: RedGreenGameteCreationTransactionRequestModel): StorageReferenceModel {
        return wrapNetworkExceptionSimple{ post("$httpUrl/add/redGreenGameteCreationTransaction", request) { jsonToModel(it, StorageReferenceModel::class.java) } }
    }

    override fun addInitializationTransaction(request: InitializationTransactionRequestModel) {
        return wrapNetworkExceptionSimple{ post("$httpUrl/add/initializationTransaction", request) {} }
    }

    override fun addJarStoreTransaction(request: JarStoreTransactionRequestModel): TransactionReferenceModel {
        return wrapNetworkExceptionMedium{ post("$httpUrl/add/jarStoreTransaction", request) { jsonToModel(it, TransactionReferenceModel::class.java) } }
    }

    override fun addConstructorCallTransaction(request: ConstructorCallTransactionRequestModel): StorageReferenceModel {
        return wrapNetworkExceptionFull{ post("$httpUrl/add/constructorCallTransaction", request) { jsonToModel(it, StorageReferenceModel::class.java) } }
    }

    override fun addInstanceMethodCallTransaction(request: InstanceMethodCallTransactionRequestModel): StorageValueModel? {
        return wrapNetworkExceptionFull{ post("$httpUrl/add/instanceMethodCallTransaction", request) { dealWithReturnVoid(request, it) } }
    }

    override fun addStaticMethodCallTransaction(request: StaticMethodCallTransactionRequestModel): StorageValueModel? {
        return wrapNetworkExceptionFull{ post("$httpUrl/add/staticMethodCallTransaction", request) { dealWithReturnVoid(request, it) } }
    }

    override fun postJarStoreTransaction(request: JarStoreTransactionRequestModel): JarSupplier {
        val transactionReference = wrapNetworkExceptionSimple{ post("$httpUrl/post/jarStoreTransaction", request) { jsonToModel(it, TransactionReferenceModel::class.java) } }
        return wrapNetworkExceptionSimple{ jarSupplierFor(transactionReference) }
    }

    override fun postConstructorCallTransaction(request: ConstructorCallTransactionRequestModel): CodeSupplier<StorageReferenceModel> {
        val transactionReference = wrapNetworkExceptionSimple{ post("$httpUrl/post/constructorCallTransaction", request) { jsonToModel(it, TransactionReferenceModel::class.java) } }
        return wrapNetworkExceptionSimple{ constructorSupplierOf(transactionReference) }
    }

    override fun postInstanceMethodCallTransaction(request: InstanceMethodCallTransactionRequestModel): CodeSupplier<StorageValueModel> {
        val transactionReference = wrapNetworkExceptionSimple{ post("$httpUrl/post/instanceMethodCallTransaction", request) { jsonToModel(it, TransactionReferenceModel::class.java) } }
        return wrapNetworkExceptionSimple{ methodSupplierOf(transactionReference) }
    }

    override fun postStaticMethodCallTransaction(request: StaticMethodCallTransactionRequestModel): CodeSupplier<StorageValueModel> {
        val transactionReference = wrapNetworkExceptionSimple{ post("$httpUrl/post/staticMethodCallTransaction", request) { jsonToModel(it, TransactionReferenceModel::class.java) } }
        return wrapNetworkExceptionSimple{ methodSupplierOf(transactionReference) }
    }

    override fun runInstanceMethodCallTransaction(request: InstanceMethodCallTransactionRequestModel): StorageValueModel? {
        return wrapNetworkExceptionFull{ post("$httpUrl/run/instanceMethodCallTransaction", request) { dealWithReturnVoid(request, it) } }
    }

    override fun runStaticMethodCallTransaction(request: StaticMethodCallTransactionRequestModel): StorageValueModel? {
        return wrapNetworkExceptionFull{ post("$httpUrl/run/staticMethodCallTransaction", request) { dealWithReturnVoid(request, it) } }
    }

    override fun subscribeToEvents(creator: StorageReferenceModel?, handler: BiConsumer<StorageReferenceModel, StorageReferenceModel>) : Subscription {
        return stompClient.subscribeTo("/topic/events", EventRequestModel::class.java, { result, error ->
            when {
                error != null -> {
                    println("handling error")
                }
                result != null -> {
                   handler.accept(result.event, result.creator)
                }
                else -> {
                    println("unexpected payload")
                }
            }
        })
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
                .post(bodyJson.toRequestBody(mediaTypeJson))
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
            return callable.call()
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
            return callable.call()
        } catch (networkException: NetworkException) {
            when (networkException.errorModel.exceptionClassName) {
                hotmokaExceptionPackage + TransactionRejectedException::class.java.simpleName -> throw TransactionRejectedException(networkException.errorModel.message)
                TimeoutException::class.java.name -> throw TimeoutException(networkException.errorModel.message)
                InterruptedException::class.java.name -> throw InterruptedException(networkException.errorModel.message)
                else -> throw InternalFailureException(networkException.errorModel.message)
            }
        } catch (e: Exception) {
            if (e.message != null) throw InternalFailureException(e.message!!) else throw InternalFailureException("An error occured")
        }
    }

    @Throws(TransactionRejectedException::class)
    private fun <T> wrapNetworkExceptionSimple(callable: Callable<T>): T {
        try {
            return callable.call()
        } catch (networkException: NetworkException) {
            if (networkException.errorModel.exceptionClassName == hotmokaExceptionPackage + TransactionRejectedException::class.java.simpleName)
                throw TransactionRejectedException(networkException.errorModel.message)
            else
                throw InternalFailureException(networkException.errorModel.message)
        } catch (e: Exception) {
            if (e.message != null) throw InternalFailureException(e.message!!) else throw InternalFailureException("An error occured")
        }
    }

    @Throws(TransactionRejectedException::class, TransactionException::class)
    private fun <T> wrapNetworkExceptionMedium(callable: Callable<T>): T {
        try {
            return callable.call()
        } catch (networkException: NetworkException) {
            when (networkException.errorModel.exceptionClassName) {
                hotmokaExceptionPackage + TransactionRejectedException::class.java.simpleName -> throw TransactionRejectedException(networkException.errorModel.message)
                hotmokaExceptionPackage + TransactionException::class.java.simpleName -> throw TransactionException(networkException.errorModel.message)
                else -> throw InternalFailureException(networkException.errorModel.message)
            }
        } catch (e: Exception) {
            if (e.message != null) throw InternalFailureException(e.message!!) else throw InternalFailureException("An error occured")
        }
    }

    @Throws(TransactionRejectedException::class, TransactionException::class)
    private fun <T> wrapNetworkExceptionMediumForSupplier(callable: Callable<T>): T {
        try {
            return callable.call()
        } catch (networkException: NetworkException) {
            when (networkException.errorModel.exceptionClassName) {
                hotmokaExceptionPackage + TransactionRejectedException::class.java.simpleName -> throw TransactionRejectedException(networkException.errorModel.message)
                hotmokaExceptionPackage + TransactionException::class.java.simpleName -> throw TransactionException(networkException.errorModel.message)
                else -> throw InternalFailureException(networkException.errorModel.message)
            }
        } catch (e: TransactionRejectedException) {
            throw e
        } catch (e: TransactionException) {
            throw e
        } catch (e: Exception) {
            if (e.message != null) throw TransactionRejectedException(e.message!!) else throw TransactionRejectedException("Transaction rejected")
        }
    }

    @Throws(TransactionRejectedException::class, TransactionException::class, CodeExecutionException::class)
    private fun <T> wrapNetworkExceptionFull(callable: Callable<T>): T {
        try {
            return callable.call()
        } catch (networkException: NetworkException) {
            when (networkException.errorModel.exceptionClassName) {
                hotmokaExceptionPackage + TransactionRejectedException::class.java.simpleName -> throw TransactionRejectedException(networkException.errorModel.message)
                hotmokaExceptionPackage + TransactionException::class.java.simpleName -> throw TransactionException(networkException.errorModel.message)
                hotmokaExceptionPackage + CodeExecutionException::class.java.simpleName -> throw CodeExecutionException(networkException.errorModel.message)
                else -> throw InternalFailureException(networkException.errorModel.message)
            }
        } catch (e: Exception) {
            if (e.message != null) throw InternalFailureException(e.message!!) else throw InternalFailureException("An error occured")
        }
    }

    @Throws(TransactionRejectedException::class, TransactionException::class, CodeExecutionException::class)
    private fun <T> wrapNetworkExceptionFullForSupplier(callable: Callable<T>): T {
        try {
            return callable.call()
        } catch (networkException: NetworkException) {
            when (networkException.errorModel.exceptionClassName) {
                hotmokaExceptionPackage + TransactionRejectedException::class.java.simpleName -> throw TransactionRejectedException(networkException.errorModel.message)
                hotmokaExceptionPackage + TransactionException::class.java.simpleName -> throw TransactionException(networkException.errorModel.message)
                hotmokaExceptionPackage + CodeExecutionException::class.java.simpleName -> throw CodeExecutionException(networkException.errorModel.message)
                else -> throw InternalFailureException(networkException.errorModel.message)
            }
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
    private fun jsonToTransactionRequest(json: String?): TransactionRestRequestModel<*> {
        if (json == null)
            throw InternalFailureException("Unexpected null transaction request model")

        val jsonObject = this.gson.fromJson(json, JsonObject::class.java)
                ?: throw InternalFailureException("Unexpected null transaction request serialized object")

        val transactionRequestType = jsonObject.get("type")?.asString
                ?: throw InternalFailureException("Unexpected null type of transaction request")
        val transactionRequestModel = jsonObject.get("transactionRequestModel")?.asJsonObject
                ?: throw InternalFailureException("Unexpected null transactionRequestModel")
        val basePackage = "io.hotmoka.network.models.requests."

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
        val basePackage = "io.hotmoka.network.models.responses."

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

    /**
     * Deals with methods that return void: the API of the node
     * requires to return null, always, when such methods are called.
     *
     * @param request the request that calls the method
     * @param json the json model of the return value of the method
     * @return the resulting value, using {@code null} if the method returned void
     */
    private fun dealWithReturnVoid(request: MethodCallTransactionRequestModel, json: String?): StorageValueModel? {
        return if (request.method.returnType == null) null else jsonToModel(json, StorageValueModel::class.java)
    }

    /**
     * Yields a jar supplier that polls for the outcome of a transaction that installed
     * a jar in the store of the node.
     *
     * @param reference the reference of the request of the transaction
     * @return the jar supplier
     */
    private fun jarSupplierFor(reference: TransactionReferenceModel): JarSupplier {
       return object : JarSupplier {

            override fun getReferenceOfRequest(): TransactionReferenceModel {
                return reference
            }

            override fun get(): TransactionReferenceModel {
                return wrapNetworkExceptionMediumForSupplier {
                    val transaction = getPolledResponse(reference).transactionResponseModel as JarStoreTransactionResponseModel
                    transaction.getOutcomeAt(reference)
                }
            }
        }
    }

    /**
     * Yields a code supplier that polls for the outcome of a transaction that ran a constructor.
     *
     * @param reference the reference of the request of the transaction
     * @return the code supplier
     */
    private fun constructorSupplierOf(reference: TransactionReferenceModel): CodeSupplier<StorageReferenceModel> {
        return object : CodeSupplier<StorageReferenceModel> {

            override fun getReferenceOfRequest(): TransactionReferenceModel {
                return reference
            }

            override fun get(): StorageReferenceModel {
                return wrapNetworkExceptionFullForSupplier {
                    val transaction = getPolledResponse(reference).transactionResponseModel as ConstructorCallTransactionResponseModel
                    transaction.getOutcome()
                }
            }
        }
    }

    /**
     * Yields a code supplier that polls for the outcome of a transaction that ran a method.
     *
     * @param reference the reference of the request of the transaction
     * @return the code supplier
     */
    private fun methodSupplierOf(reference: TransactionReferenceModel): CodeSupplier<StorageValueModel> {
        return object : CodeSupplier<StorageValueModel> {

            override fun getReferenceOfRequest(): TransactionReferenceModel {
                return reference
            }

            override fun get(): StorageValueModel? {
                return wrapNetworkExceptionFullForSupplier {
                    val transaction = getPolledResponse(reference).transactionResponseModel as MethodCallTransactionResponseModel
                    transaction.getOutcome()
                }
            }
        }
    }

    override fun close() {
        stompClient.close()
    }

}

