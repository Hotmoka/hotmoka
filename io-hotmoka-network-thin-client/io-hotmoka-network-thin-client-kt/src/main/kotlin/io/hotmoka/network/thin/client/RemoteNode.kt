package io.hotmoka.network.thin.client

import io.hotmoka.network.thin.client.exceptions.CodeExecutionException
import io.hotmoka.network.thin.client.exceptions.TransactionException
import io.hotmoka.network.thin.client.exceptions.TransactionRejectedException
import io.hotmoka.network.thin.client.models.requests.*
import io.hotmoka.network.thin.client.models.responses.SignatureAlgorithmResponseModel
import io.hotmoka.network.thin.client.models.responses.TransactionRestResponseModel
import io.hotmoka.network.thin.client.models.updates.ClassTagModel
import io.hotmoka.network.thin.client.models.updates.StateModel
import io.hotmoka.network.thin.client.models.values.StorageReferenceModel
import io.hotmoka.network.thin.client.models.values.StorageValueModel
import io.hotmoka.network.thin.client.models.values.TransactionReferenceModel
import io.hotmoka.network.thin.client.suppliers.CodeSupplier
import io.hotmoka.network.thin.client.suppliers.JarSupplier
import java.security.NoSuchAlgorithmException
import java.util.*
import java.util.concurrent.TimeoutException
import java.util.function.BiConsumer

/**
 * A proxy for a node of the Hotmoka network, that provides the storage
 * facilities for the execution of Takamaka code.
 * Calls to code in the node can be added, run or posted.
 * Posted calls are executed, eventually, and their value can be retrieved
 * through the future returned by the calls. Added calls are shorthand
 * for posting a call and waiting until the value of their future is
 * available. Run calls are only available for view methods, without side-effects.
 * They execute immediately and never modify the store of the node.
 */
interface RemoteNode {

    /**
     * Yields the reference, in the store of the node, where the base Takamaka base classes are installed.
     * If this node has some form of commit, then this method returns a reference
     * only if the installation of the jar with the Takamaka base classes has been
     * already committed.
     *
     * @throws NoSuchElementException if the node has not been initialized yet
     */
    @Throws(NoSuchElementException::class)
    fun getTakamakaCode(): TransactionReferenceModel

    /**
     * Yields the manifest installed in the store of the node. The manifest is an object of type
     * {@code io.takamaka.code.system.Manifest} that contains some information about the node,
     * useful for the users of the node.
     * If this node has some form of commit, then this method returns a reference
     * only if the installation of the manifest has been already committed.
     *
     * @return the reference to the node
     * @throws NoSuchElementException if no manifest has been set for this node
     */
    @Throws(NoSuchElementException::class)
    fun getManifest(): StorageReferenceModel

    /**
     * Yields the current state of the object at the given storage reference.
     * If this method succeeds and this node has some form of commit, then the transaction
     * of the storage reference has been definitely committed in this node.
     * A node is allowed to keep in store all, some or none of the objects.
     * Hence, this method might fail to find the state of the object although the object previously
     * existed in store.
     *
     * @param request the storage reference of the object
     * @return the last updates of all its instance fields; these updates include
     *         the class tag update for the object
     * @throws NoSuchElementException if there is no object with that reference
     */
    @Throws(NoSuchElementException::class)
    fun getState(request: StorageReferenceModel): StateModel

    /**
     * Yields the class tag of the object with the given storage reference.
     * If this method succeeds and this node has some form of commit, then the transaction
     * of the storage reference has been definitely committed in this node.
     * A node is allowed to keep in store all, some or none of the objects.
     * Hence, this method might fail to find the class tag although the object previously
     * existed in store.
     *
     * @param request the storage reference of the object
     * @return the class tag, if any
     * @throws NoSuchElementException if there is no object with that reference or
     *                                if the class tag could not be found
     */
    @Throws(NoSuchElementException::class)
    fun getClassTag(request: StorageReferenceModel): ClassTagModel

    /**
     * Yields the request that generated the transaction with the given reference.
     * If this node has some form of commit, then this method can only succeed
     * when the transaction has been definitely committed in this node.
     * Nodes are allowed to keep in store all, some or none of the requests
     * that they received during their lifetime.
     *
     * @param reference the reference of the transaction
     * @return the request
     * @throws NoSuchElementException if there is no request with that reference
     */
    @Throws(NoSuchElementException::class)
    fun getRequest(reference: TransactionReferenceModel): TransactionRestRequestModel<*>

    /**
     * Yields the algorithm used to sign non-initial requests with this node.
     *
     * @return the algorithm
     * @throws NoSuchAlgorithmException if the required signature algorithm is not available in the Java installation
     */
    @Throws(NoSuchAlgorithmException::class)
    fun getSignatureAlgorithmForRequests(): SignatureAlgorithmResponseModel

    /**
     * Yields the response generated for the request for the given transaction.
     * If this node has some form of commit, then this method can only succeed
     * or yield a {@linkplain TransactionRejectedException} only
     * when the transaction has been definitely committed in this node.
     * Nodes are allowed to keep in store all, some or none of the responses
     * that they computed during their lifetime.
     *
     * @param reference the reference of the transaction
     * @return the response
     * @throws TransactionRejectedException if there is a request for that transaction but it failed with this exception
     * @throws NoSuchElementException if there is no request, and hence no response, with that reference
     */
    @Throws(TransactionRejectedException::class, NoSuchAlgorithmException::class)
    fun getResponse(reference: TransactionReferenceModel): TransactionRestResponseModel<*>

    /**
     * Waits until a transaction has been committed, or until its delivering fails.
     * If this method succeeds and this node has some form of commit, then the
     * transaction has been definitely committed.
     * Nodes are allowed to keep in store all, some or none of the responses
     * computed during their lifetime. Hence, this method might time out also
     * when a response has been computed in the past for the transaction of {@code reference},
     * but it has not been kept in store.
     *
     * @param reference the reference of the transaction
     * @return the response computed for {@code request}
     * @throws TransactionRejectedException if the request failed to be committed, because of this exception
     * @throws TimeoutException if the polling delay has expired but the request did not get committed
     * @throws InterruptedException if the current thread has been interrupted while waiting for the response
     */
    @Throws(TransactionRejectedException::class, TimeoutException::class, InterruptedException::class)
    fun getPolledResponse(reference: TransactionReferenceModel): TransactionRestResponseModel<*>

    /**
     * Expands the store of this node with a transaction that
     * installs a jar in it. It has no caller and requires no gas. The goal is to install, in the
     * node, some basic jars that are likely needed as dependencies by future jars.
     * For instance, the jar containing the basic contract classes.
     * This installation have special privileges, such as that of installing
     * packages in {@code io.takamaka.code.lang.*}.
     *
     * @param request the transaction request
     * @return the reference to the transaction, that can be used to refer to the jar in a class path or as future dependency of other jars
     * @throws TransactionRejectedException if the transaction could not be executed and the store of the node remained unchanged
     */
    @Throws(TransactionRejectedException::class)
    fun addJarStoreInitialTransaction(request: JarStoreInitialTransactionRequestModel): TransactionReferenceModel

    /**
     * Expands the store of this node with a transaction that creates a gamete, that is,
     * an externally owned contract with the given initial amount of coins.
     * This transaction has no caller and requires no gas.
     *
     * @param request the transaction request
     * @return the reference to the freshly created gamete
     * @throws TransactionRejectedException if the transaction could not be executed and the store of the node remained unchanged
     */
    @Throws(TransactionRejectedException::class)
    fun addGameteCreationTransaction(request: GameteCreationTransactionRequestModel): StorageReferenceModel

    /**
     * Expands the store of this node with a transaction that creates a red/green gamete, that is,
     * a red/green externally owned contract with the given initial amount of coins.
     * This transaction has no caller and requires no gas.
     *
     * @param request the transaction request
     * @return the reference to the freshly created gamete
     * @throws TransactionRejectedException if the transaction could not be executed and the store of the node remained unchanged
     */
    @Throws(TransactionRejectedException::class)
    fun addRedGreenGameteCreationTransaction(request: RedGreenGameteCreationTransactionRequestModel): StorageReferenceModel

    /**
     * Expands the store of this node with a transaction that marks the node as
     * initialized and installs its manifest. After this transaction, no more initial transactions
     * can be executed on the node.
     *
     * @param request the transaction request
     * @throws TransactionRejectedException if the transaction could not be executed and the store of the node remained unchanged
     */
    @Throws(TransactionRejectedException::class)
    fun addInitializationTransaction(request: InitializationTransactionRequestModel)

    /**
     * Expands the store of this node with a transaction that installs a jar in it.
     *
     * @param request the transaction request
     * @return the reference to the transaction, that can be used to refer to the jar in a class path or as future dependency of other jars
     * @throws TransactionRejectedException if the transaction could not be executed and the store of the node remained unchanged
     * @throws TransactionException if the transaction could be executed and the store of the node has been expanded with a failed transaction
     */
    @Throws(TransactionRejectedException::class, TransactionException::class)
    fun addJarStoreTransaction(request: JarStoreTransactionRequestModel): TransactionReferenceModel

    /**
     * Expands this node's store with a transaction that runs a constructor of a class.
     *
     * @param request the request of the transaction
     * @return the created object, if the constructor was successfully executed, without exception
     * @throws TransactionRejectedException if the transaction could not be executed and the store of the node remained unchanged
     * @throws CodeExecutionException if the transaction could be executed and the node has been expanded with a failed transaction,
     *                                because of an exception in the user code in blockchain, that is allowed to be thrown by the constructor
     * @throws TransactionException if the transaction could be executed and the node has been expanded with a failed transaction,
     *                              because of an exception outside the user code in blockchain, or not allowed to be thrown by the constructor
     */
    @Throws(TransactionRejectedException::class, TransactionException::class, CodeExecutionException::class)
    fun addConstructorCallTransaction(request: ConstructorCallTransactionRequestModel): StorageReferenceModel

    /**
     * Expands this node's store with a transaction that runs an instance method of an object already in this node's store.
     *
     * @param request the transaction request
     * @return the result of the call, if the method was successfully executed, without exception. If the method is
     *         declared to return {@code void}, this result will be {@code null}
     * @throws TransactionRejectedException if the transaction could not be executed and the store of the node remained unchanged
     * @throws CodeExecutionException if the transaction could be executed and the node has been expanded with a failed transaction,
     *                                because of an exception in the user code in blockchain, that is allowed to be thrown by the method
     * @throws TransactionException if the transaction could be executed and the node has been expanded with a failed transaction,
     *                              because of an exception outside the user code in blockchain, or not allowed to be thrown by the method
     */
    @Throws(TransactionRejectedException::class, TransactionException::class, CodeExecutionException::class)
    fun addInstanceMethodCallTransaction(request: InstanceMethodCallTransactionRequestModel): StorageValueModel?

    /**
     * Expands this node's store with a transaction that runs a static method of a class in this node.
     *
     * @param request the transaction request
     * @return the result of the call, if the method was successfully executed, without exception. If the method is
     *         declared to return {@code void}, this result will be {@code null}
     * @throws TransactionRejectedException if the transaction could not be executed and the store of the node remained unchanged
     * @throws CodeExecutionException if the transaction could be executed and the node has been expanded with a failed transaction,
     *                                because of an exception in the user code in blockchain, that is allowed to be thrown by the method
     * @throws TransactionException if the transaction could be executed and the node has been expanded with a failed transaction,
     *                              because of an exception outside the user code in blockchain, or not allowed to be thrown by the method
     */
    @Throws(TransactionRejectedException::class, TransactionException::class, CodeExecutionException::class)
    fun addStaticMethodCallTransaction(request: StaticMethodCallTransactionRequestModel): StorageValueModel?

    /**
     * Posts a transaction that expands the store of this node with a transaction that installs a jar in it.
     *
     * @param request the transaction request
     * @return the future holding the reference to the transaction where the jar has been installed
     * @throws TransactionRejectedException if the transaction could not be posted
     */
    @Throws(TransactionRejectedException::class)
    fun postJarStoreTransaction(request: JarStoreTransactionRequestModel): JarSupplier

    /**
     * Posts a transaction that runs a constructor of a class in this node.
     *
     * @param request the request of the transaction
     * @return the future holding the result of the computation
     * @throws TransactionRejectedException if the transaction could not be posted
     */
    @Throws(TransactionRejectedException::class)
    fun postConstructorCallTransaction(request: ConstructorCallTransactionRequestModel): CodeSupplier<StorageReferenceModel>

    /**
     * Posts a transaction that runs an instance method of an object already in this node's store.
     *
     * @param request the transaction request
     * @return the future holding the result of the transaction
     * @throws TransactionRejectedException if the transaction could not be posted
     */
    @Throws(TransactionRejectedException::class)
    fun postInstanceMethodCallTransaction(request: InstanceMethodCallTransactionRequestModel): CodeSupplier<StorageValueModel>

    /**
     * Posts a request that runs a static method of a class in this node.
     *
     * @param request the transaction request
     * @return the future holding the result of the transaction
     * @throws TransactionRejectedException if the transaction could not be posted
     */
    @Throws(TransactionRejectedException::class)
    fun postStaticMethodCallTransaction(request: StaticMethodCallTransactionRequestModel): CodeSupplier<StorageValueModel>

    /**
     * Runs an instance {@code @@View} method of an object already in this node's store.
     * The node's store is not expanded, since the execution of the method has no side-effects.
     *
     * @param request the transaction request
     * @return the result of the call, if the method was successfully executed, without exception
     * @throws TransactionRejectedException if the transaction could not be executed
     * @throws CodeExecutionException if the transaction could be executed but led to an exception in the user code in blockchain,
     *                                that is allowed to be thrown by the method
     * @throws TransactionException if the transaction could be executed but led to an exception outside the user code in blockchain,
     *                              or that is not allowed to be thrown by the method
     */
    @Throws(TransactionRejectedException::class, TransactionException::class, CodeExecutionException::class)
    fun runInstanceMethodCallTransaction(request: InstanceMethodCallTransactionRequestModel): StorageValueModel?

    /**
     * Runs a static {@code @@View} method of a class in this node.
     * The node's store is not expanded, since the execution of the method has no side-effects.
     *
     * @param request the transaction request
     * @return the result of the call, if the method was successfully executed, without exception
     * @throws TransactionRejectedException if the transaction could not be executed
     * @throws CodeExecutionException if the transaction could be executed but led to an exception in the user code in blockchain,
     *                                that is allowed to be thrown by the method
     * @throws TransactionException if the transaction could be executed but led to an exception outside the user code in blockchain,
     *                              or that is not allowed to be thrown by the method
     */
    @Throws(TransactionRejectedException::class, TransactionException::class, CodeExecutionException::class)
    fun runStaticMethodCallTransaction(request: StaticMethodCallTransactionRequestModel): StorageValueModel?

    /**
     * Subscribes the given handler for events with the given key.
     *
     * @param key the key of the events that will be forwarded to the handler; if this is `null`,
     * all events will be forwarded to the handler
     * @param handler a handler that gets executed when an event with the given key occurs; a handler can be
     * subscribed to more keys; for each event, it receives its key and the event itself
     * @return the subscription, that can be used later to stop event handling with `handler`
     */
    fun subscribeToEvents(key: StorageReferenceModel?, handler: BiConsumer<StorageReferenceModel, StorageReferenceModel>)
}