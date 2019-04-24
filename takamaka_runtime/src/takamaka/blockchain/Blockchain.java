package takamaka.blockchain;

import takamaka.blockchain.request.ConstructorCallTransactionRequest;
import takamaka.blockchain.request.GameteCreationTransactionRequest;
import takamaka.blockchain.request.InstanceMethodCallTransactionRequest;
import takamaka.blockchain.request.JarStoreInitialTransactionRequest;
import takamaka.blockchain.request.JarStoreTransactionRequest;
import takamaka.blockchain.request.StaticMethodCallTransactionRequest;
import takamaka.blockchain.response.ConstructorCallTransactionResponse;
import takamaka.blockchain.response.GameteCreationTransactionResponse;
import takamaka.blockchain.response.JarStoreInitialTransactionResponse;
import takamaka.blockchain.response.JarStoreTransactionResponse;
import takamaka.blockchain.response.MethodCallTransactionResponse;
import takamaka.blockchain.values.StorageReference;
import takamaka.blockchain.values.StorageValue;

/**
 * The abstraction of a Takamaka blockchain. It defines methods for the execution of transactions.
 */
public interface Blockchain {

	/**
	 * Runs a transaction that installs a jar in this blockchain. This transaction can only occur during initialization
	 * of the blockchain. It has no caller and requires no gas. The goal is to install, in the
	 * blockchain, some basic jars that are likely needed as dependencies by future jars.
	 * For instance, the jar containing the basic contract classes. This method runs the transaction
	 * specified by the request, after the given transaction reference, and yields the corresponding response.
	 * The blockchain does not get modified.
	 * 
	 * @param request the transaction request
	 * @param previous the reference to the transaction after which this must be executed. This might be {@code null}
	 *                 if this is the first transaction in a blockchain
	 * @return the response resulting from the execution of the request
	 * @throws TransactionException if the transaction could not be completed successfully
	 */
	public JarStoreInitialTransactionResponse runJarStoreInitialTransaction(JarStoreInitialTransactionRequest request, TransactionReference previous) throws TransactionException;

	/**
	 * Expands this blockchain with a transaction that
	 * installs a jar in this blockchain. This transaction can only occur during initialization
	 * of the blockchain. It has no caller and requires no gas. The goal is to install, in the
	 * blockchain, some basic jars that are likely needed as dependencies by future jars.
	 * For instance, the jar containing the basic contract classes.
	 * 
	 * @param request the transaction request
	 * @return the reference to the transaction that can be used to refer to this jar in a class path or as future dependency of other jars
	 * @throws TransactionException if the transaction could not be completed successfully. In this case, the blockchain is not expanded
	 */
	public TransactionReference addJarStoreInitialTransaction(JarStoreInitialTransactionRequest request) throws TransactionException;

	/**
	 * Runs a transaction that creates a gamete, that is, an externally owned contract with the given initial amount of coins.
	 * This transaction can only occur during initialization of the blockchain. It has
	 * no caller and requires no gas. This method runs the transaction
	 * specified by the request, after the given transaction reference, and yields the corresponding response.
	 * The blockchain does not get modified.
	 * 
	 * @param request the transaction request
	 * @param previous the reference to the transaction after which this must be executed
	 * @return the response resulting from the execution of the request
	 * @throws TransactionException if the transaction could not be completed successfully
	 */
	public abstract GameteCreationTransactionResponse runGameteCreationTransaction(GameteCreationTransactionRequest request, TransactionReference previous) throws TransactionException;

	/**
	 * Expands this blockchain with a transaction that creates a gamete, that is,
	 * an externally owned contract with the given initial amount of coins.
	 * This transaction can only occur during initialization of the blockchain. It has
	 * no caller and requires no gas.
	 * 
	 * @param request the transaction request
	 * @return the reference to the freshly created gamete
	 * @throws TransactionException if the transaction could not be completed successfully. In this case, the blockchain is not expanded
	 */
	public abstract StorageReference addGameteCreationTransaction(GameteCreationTransactionRequest request) throws TransactionException;

	/**
	 * Runs a transaction that installs a jar in this blockchain. The goal is to install, in blockchain, a jar, with its dependencies.
	 * This method runs the transaction specified by the request, after the given transaction reference, and yields
	 * the corresponding response. The blockchain does not get modified.
	 * 
	 * @param request the transaction request
	 * @param previous the transaction reference after which the request must be executed
	 * @return the response resulting from the execution of the request
	 * @throws TransactionException if the transaction could not be completed successfully
	 */
	public JarStoreTransactionResponse runJarStoreTransaction(JarStoreTransactionRequest request, TransactionReference previous) throws TransactionException;

	/**
	 * Expands this blockchain with a transaction that installs a jar in it.
	 * 
	 * @param request the transaction request
	 * @return the reference to the transaction, that can be used to refer to this jar in a class path or as future dependency of other jars
	 * @throws TransactionException if the transaction could not be completed successfully. If this occurs and the caller
	 *                              has been identified, the blockchain will still be expanded
	 *                              with a transaction that charges all gas to the caller, but no jar will be installed.
	 *                              Otherwise, the transaction will be rejected and not added to this blockchain
	 */
	public TransactionReference addJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionException;

	/**
	 * Runs a transaction that calls a constructor of a class installed in blockchain.
	 * The goal is to run the constructor and compute a reference to the freshly created object.
	 * This method runs the transaction specified by the request, after the given transaction reference, and yields
	 * the corresponding response. The blockchain does not get modified.
	 * 
	 * @param request the transaction request
	 * @param previous the transaction reference after which the request must be executed
	 * @return the response resulting from the execution of the request
	 * @throws TransactionException if the transaction could not be completed successfully
	 */
	public ConstructorCallTransactionResponse runConstructorCallTransaction(ConstructorCallTransactionRequest request, TransactionReference previous) throws TransactionException;

	/**
	 * Expands this blockchain with a transaction that runs a constructor of a class.
	 * 
	 * @param request the request of the transaction
	 * @return the created object, if the constructor was successfully executed, without exception
	 * @throws TransactionException if the transaction could not be completed successfully. This includes
	 *                              {@link takamaka.lang.OutOfGasError}s and {@link takamaka.lang.InsufficientFundsError}s.
	 *                              If this occurs and the caller
	 *                              has been identified, the blockchain will still be expanded
	 *                              with a transaction that charges all gas to the caller, but no constructor will be executed.
	 *                              Otherwise, the transaction will be rejected and not added to this blockchain
	 * @throws CodeExecutionException if the constructor is annotated as {@link takamaka.lang.ThrowsExceptions} and its execution
	 *                                failed with an exception that is an instance of
	 *                                {@link java.lang.Exception} (that exception is available as
	 *                                {@link java.lang.Throwable#getCause()}). Note that, in this case, from the point of view of Takamaka,
	 *                                the transaction was successful, it has been added to this blockchain and the consumed gas gets charged to the caller.
	 *                                In all other cases, a {@link takamaka.blockchain.TransactionException} is thrown
	 */
	public StorageReference addConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionException, CodeExecutionException;

	/**
	 * Runs a transaction that calls an instance method of an object in blockchain.
	 * The goal is to run the method and compute its returned value (if any).
	 * This method runs the transaction specified by the request, after the given transaction reference, and yields
	 * the corresponding response. The blockchain does not get modified.
	 * 
	 * @param request the transaction request
	 * @param previous the transaction reference after which the request must be executed
	 * @return the response resulting from the execution of the request
	 * @throws TransactionException if the transaction could not be completed successfully
	 */
	public MethodCallTransactionResponse runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request, TransactionReference previous) throws TransactionException;

	/**
	 * Runs an instance method of an object in blockchain.
	 * 
	 * @param request the transaction request
	 * @return the result of the call, if the method was successfully executed, without exception. If the method is
	 *         declared to return {@code void}, this result will be {@code null}
	 * @throws TransactionException if the transaction could not be completed successfully. This includes
	 *                              {@link takamaka.lang.OutOfGasError}s and {@link takamaka.lang.InsufficientFundsError}s.
	 *                              If this occurs and the caller
	 *                              has been identified, the blockchain will still be expanded
	 *                              with a transaction that charges all gas to the caller, but no method will be executed.
	 *                              Otherwise, the transaction will be rejected and not added to this blockchain
	 * @throws CodeExecutionException if the method is annotated as {@link takamaka.lang.ThrowsExceptions} and its execution
	 *                                failed with an exception that is an instance of
	 *                                {@link java.lang.Exception} (that exception is available as
	 *                                {@link java.lang.Throwable#getCause()}). Note that, in this case, from the point of view of Takamaka,
	 *                                the transaction was successful, it has been added to this blockchain and the consumed gas gets charged to the caller.
	 *                                In all other cases, a {@link takamaka.blockchain.TransactionException} is thrown
	 */
	public StorageValue addInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request) throws TransactionException, CodeExecutionException;

	/**
	 * Runs a transaction that calls a static method of a class in blockchain.
	 * The goal is to run the method and compute its returned value (if any).
	 * This method runs the transaction specified by the request, at the given transaction reference, and yields
	 * the corresponding response. The blockchain does not get modified.
	 * 
	 * @param request the transaction request
	 * @param previous the transaction reference after which the request must be executed
	 * @return the response resulting from the execution of the request
	 * @throws TransactionException if the transaction could not be completed successfully
	 */
	public MethodCallTransactionResponse runStaticMethodCallTransaction(StaticMethodCallTransactionRequest request, TransactionReference previous) throws TransactionException;

	/**
	 * Expands this blockchain with a transaction that runs a static method of a class in blockchain.
	 * 
	 * @param request the transaction request
	 * @return the result of the call, if the method was successfully executed, without exception. If the method is
	 *         declared to return {@code void}, this result will be {@code null}
	 * @throws TransactionException if the transaction could not be completed successfully. This includes
	 *                              {@link takamaka.lang.OutOfGasError}s and {@link takamaka.lang.InsufficientFundsError}s.
	 *                              If this occurs and the caller
	 *                              has been identified, the blockchain will still be expanded
	 *                              with a transaction that charges all gas to the caller, but no method will be executed.
	 *                              Otherwise, the transaction will be rejected and not added to this blockchain
	 * @throws CodeExecutionException if the method is annotated as {@link takamaka.lang.ThrowsExceptions} and its execution
	 *                                failed with an exception that is an instance of
	 *                                {@link java.lang.Exception} (that exception is available as
	 *                                {@link java.lang.Throwable#getCause()}). Note that, in this case, from the point of view of Takamaka,
	 *                                the transaction was successful, it has been added to this blockchain and the consumed gas gets charged to the caller.
	 *                                In all other cases, a {@link takamaka.blockchain.TransactionException} is thrown
	 */
	public StorageValue addStaticMethodCallTransaction(StaticMethodCallTransactionRequest request) throws TransactionException, CodeExecutionException;
}