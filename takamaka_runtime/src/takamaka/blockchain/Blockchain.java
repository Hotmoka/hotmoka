package takamaka.blockchain;

import java.math.BigInteger;

import takamaka.blockchain.request.ConstructorCallTransactionRequest;
import takamaka.blockchain.request.GameteCreationTransactionRequest;
import takamaka.blockchain.request.JarStoreInitialTransactionRequest;
import takamaka.blockchain.request.JarStoreTransactionRequest;
import takamaka.blockchain.response.ConstructorCallTransactionResponse;
import takamaka.blockchain.response.GameteCreationTransactionResponse;
import takamaka.blockchain.response.JarStoreInitialTransactionResponse;
import takamaka.blockchain.response.JarStoreTransactionResponse;
import takamaka.blockchain.values.StorageReference;
import takamaka.blockchain.values.StorageValue;

/**
 * The abstraction of a Takamaka blockchain. It defines methods for the execution of transactions.
 * Some transactions can only occur during initialization.
 */
public interface Blockchain {

	/**
	 * Runs a transaction that installs a jar in this blockchain. This transaction can only occur during initialization
	 * of the blockchain. It has no caller and requires no gas. The goal is to install, in the
	 * blockchain, some basic jars that are likely needed as dependencies by future jars.
	 * For instance, the jar containing the basic contract classes. This method runs the transaction
	 * specified by the request, at the given transaction reference, and yields the corresponding response.
	 * The blockchain does not get modified.
	 * 
	 * @param request the transaction request
	 * @param where the transaction reference where the request must be executed
	 * @return the response resulting from the execution of the request
	 * @throws TransactionException if the transaction could not be completed successfully
	 */
	public JarStoreInitialTransactionResponse runJarStoreInitialTransaction(JarStoreInitialTransactionRequest request, TransactionReference where) throws TransactionException;

	/**
	 * Installs a jar in this blockchain. This transaction can only occur during initialization
	 * of the blockchain. It has no caller and requires no gas. The goal is to install, in the
	 * blockchain, some basic jars that are likely needed as dependencies by future jars.
	 * For instance, the jar containing the basic contract classes.
	 * 
	 * @param request the transaction request
	 * @return the reference to the transaction that can be used to refer to this jar in a class path or as future dependency of other jars
	 * @throws TransactionException if the transaction could not be completed successfully. It will not
	 *                              be added to this blockchain
	 */
	public TransactionReference addJarStoreInitialTransaction(JarStoreInitialTransactionRequest request) throws TransactionException;

	/**
	 * Runs a transaction that creates a gamete, that is, an externally owned contract with the given initial amount of coins.
	 * This transaction can only occur during initialization of the blockchain. It has
	 * no caller and requires no gas. After this transaction, no more transactions can be executed for an
	 * uninitialized blockchain. It sets the blockchain as initialized.
	 * Hence, this is the last transaction of the initialization of the blockchain.
	 * This method runs the transaction
	 * specified by the request, at the given transaction reference, and yields the corresponding response.
	 * The blockchain does not get modified.
	 * 
	 * @param request the transaction request
	 * @param where the transaction reference where the request must be executed
	 * @return the response resulting from the execution of the request
	 * @throws TransactionException if the transaction could not be completed successfully
	 */
	public abstract GameteCreationTransactionResponse runGameteCreationTransaction(GameteCreationTransactionRequest request, TransactionReference where) throws TransactionException;

	/**
	 * Creates a gamete, that is, an externally owned contract with the given initial amount of coins.
	 * This transaction can only occur during initialization of the blockchain. It has
	 * no caller and requires no gas. After this transaction, no more transactions can be executed for an
	 * uninitialized blockchain. It sets the blockchain as initialized.
	 * Hence, this is the last transaction of the initialization of the blockchain.
	 * 
	 * @param request the transaction request
	 * @return the reference to the freshly created gamete
	 * @throws TransactionException if the transaction could not be completed successfully. It will not be added to this blockchain
	 */
	public abstract StorageReference addGameteCreationTransaction(GameteCreationTransactionRequest request) throws TransactionException;

	/**
	 * Runs a transaction that installs a jar in this blockchain. This transaction cannot only occur during initialization
	 * of the blockchain. The goal is to install, in blockchain, a jar, with its dependencies.
	 * This method runs the transaction specified by the request, at the given transaction reference, and yields
	 * the corresponding response. The blockchain does not get modified.
	 * 
	 * @param request the transaction request
	 * @param where the transaction reference where the request must be executed
	 * @return the response resulting from the execution of the request
	 * @throws TransactionException if the transaction could not be completed successfully
	 */
	public JarStoreTransactionResponse runJarStoreTransaction(JarStoreTransactionRequest request, TransactionReference where) throws TransactionException;

	/**
	 * Installs a jar in this blockchain. This method can only be used after the blockchain has been initialized.
	 * 
	 * @param request the transaction request
	 * @return the reference to the transaction, that can be used to refer to this jar in a class path or as future dependency of other jars
	 * @throws TransactionException if the transaction could not be completed successfully. If this occurs and the caller
	 *                              has been identified, the blockchain will still be expanded
	 *                              with a transaction that charges all gas to the caller, but no jar will be installed.
	 *                              Otherwise, the transaction will not be added to this blockchain
	 */
	public TransactionReference addJarStoreTransaction(JarStoreTransactionRequest request) throws TransactionException;

	/**
	 * Runs a transaction that calls a constructor of a class installed in blockchain.
	 * The goal is to run the constructor and compute a reference to the freshly created object.
	 * This method runs the transaction specified by the request, at the given transaction reference, and yields
	 * the corresponding response. The blockchain does not get modified.
	 * 
	 * @param request the transaction request
	 * @param where the transaction reference where the request must be executed
	 * @return the response resulting from the execution of the request
	 * @throws TransactionException if the transaction could not be completed successfully
	 */
	public ConstructorCallTransactionResponse runConstructorCallTransaction(ConstructorCallTransactionRequest request, TransactionReference where) throws TransactionException;

	/**
	 * Runs a constructor of a class.
	 * 
	 * @param request the request of the transaction
	 * @return the created object, if the constructor was successfully executed, without exception
	 * @throws TransactionException if the transaction could not be completed because of an internal error.
	 *                              Note that internal errors include
	 *                              {@link takamaka.lang.OutOfGasError} and {@link takamaka.lang.InsufficientFundsError}.
	 *                              If this occurs and the caller
	 *                              has been identified, the blockchain will still be expanded
	 *                              with a transaction that charges all gas to the caller, but no constructor will be executed.
	 *                              Otherwise, the transaction will not be added to this blockchain
	 * @throws CodeExecutionException if the execution of the constructor failed with an exception that is not an instance of
	 *                                {@link java.lang.Error} (that exception is available as
	 *                                {@link java.lang.Throwable#getCause()}). Note that, in this case, from the point of view of Takamaka,
	 *                                the transaction was successful, it has been added to this blockchain and gas gets charged to the caller.
	 *                                This can only occur if the constructor is annotated as {@link takamaka.lang.ThrowsExceptions}. In all other cases,
	 *                                a {@link takamaka.blockchain.TransactionException} is thrown
	 */
	public StorageReference addConstructorCallTransaction(ConstructorCallTransactionRequest request) throws TransactionException, CodeExecutionException;

	/**
	 * Runs an instance method of an object stored in the blockchain.
	 * 
	 * @param caller the externally owned caller contract that pays for the transaction
	 * @param gas the maximal amount of gas that can be consumed by the transaction
	 * @param classpath the class path where caller, receiver and actual parameters can be interpreted and the code must be executed
	 * @param method the method that must be called. It will be resolved from the {@code receiver}
	 * @param receiver the object whose method is called
	 * @param actuals the actual arguments passed to the method
	 * @return the result of the call, if the method was successfully executed, without exception. If the method is
	 *         declared to return {@code void}, this result will be {@code null}
	 * @throws TransactionException if the transaction could not be completed because of an internal error.
	 *                              Note that internal errors include
	 *                              {@link takamaka.lang.OutOfGasError} and {@link takamaka.lang.InsufficientFundsError}.
	 *                              If this occurs, the blockchain will be expanded
	 *                              with a transaction that charges all gas to the caller, but no method will be executed
	 * @throws CodeExecutionException if the execution of the method failed with an exception that is not an instance of
	 *                                {@link java.lang.Error} (that exception is available as
	 *                                {@link java.lang.Throwable#getCause()}). Note that, in this case, from the point of view of Takamaka,
	 *                                the transaction was successful, it has been added to this blockchain and it is a programmer's problem.
	 *                                This can only occur if the method is annotated as {@link takamaka.lang.ThrowsExceptions}. In all other cases,
	 *                                a {@link takamaka.blockchain.TransactionException} is thrown
	 */
	public StorageValue addInstanceMethodCallTransaction(StorageReference caller, BigInteger gas, Classpath classpath, MethodSignature method, StorageReference receiver, StorageValue... actuals) throws TransactionException, CodeExecutionException;

	/**
	 * Runs a static method of a class.
	 * 
	 * @param caller the externally owned caller contract that pays for the transaction
	 * @param gas the maximal amount of gas that can be consumed by the transaction
	 * @param classpath the class path where the {@code caller} and actual parameters can be interpreted and the code must be executed
	 * @param method the method that must be called. It specifies the class from where it must be looked up
	 * @param actuals the actual arguments passed to the method
	 * @return the result of the call, if the method was successfully executed, without exception. If the method is
	 *         declared to return {@code void}, this result will be {@code null}
	 * @throws TransactionException if the transaction could not be completed because of an internal error.
	 *                              Note that internal errors include
	 *                              {@link takamaka.lang.OutOfGasError} and {@link takamaka.lang.InsufficientFundsError}.
	 *                              If this occurs, the blockchain will be expanded
	 *                              with a transaction that charges all gas to the caller, but no method will be executed
	 * @throws CodeExecutionException if the execution of the method failed with an exception that is not an instance of
	 *                                {@link java.lang.Error} (that exception is available as
	 *                                {@link java.lang.Throwable#getCause()}). Note that, in this case, from the point of view of Takamaka,
	 *                                the transaction was successful, it has been added to this blockchain and it is a programmer's problem.
	 *                                This can only occur if the method is annotated as {@link takamaka.lang.ThrowsExceptions}. In all other cases,
	 *                                a {@link takamaka.blockchain.TransactionException} is thrown
	 */
	public StorageValue addStaticMethodCallTransaction(StorageReference caller, BigInteger gas, Classpath classpath, MethodSignature method, StorageValue... actuals) throws TransactionException, CodeExecutionException;
}