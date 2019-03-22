package takamaka.blockchain;

import java.math.BigInteger;
import java.nio.file.Path;

import takamaka.blockchain.values.StorageReference;
import takamaka.blockchain.values.StorageValue;

public interface Blockchain {

	/**
	 * Sets this blockchain as initialized
	 */
	public abstract StorageReference setAsInitialized(Classpath takamakaBase, BigInteger initialAmount) throws TransactionException;

	/**
	 * Installs a jar in this blockchain. This transaction can only occur during initialization
	 * of the blockchain, before the {@code setAsInitialzied()} method has been called.
	 * 
	 * @param jar the jar to install
	 * @param dependencies the dependencies of the jar, already installed in the blockchain
	 * @return the reference to the transaction that can be used to refer to this jar in a class path or as future dependency of other jars
	 * @throws TransactionException if the transaction could not be completed successfully
	 */
	public TransactionReference addJarStoreInitialTransaction(Path jar, Classpath... dependencies) throws TransactionException;

	/**
	 * Installs a jar in this blockchain.
	 * 
	 * @param caller the externally owned caller contract
	 * @param classpath the class path where the {@code caller} can be interpreted
	 * @param jar the jar to install
	 * @param dependencies the dependencies of the jar, already installed in the blockchain
	 * @return the reference to the transaction that can be used to refer to this jar in a class path or as future dependency of other jars
	 * @throws TransactionException if the transaction could not be completed successfully
	 */
	public TransactionReference addJarStoreTransaction(StorageReference caller, Classpath classpath, Path jar, Classpath... dependencies) throws TransactionException;

	/**
	 * Runs a constructor of a class.
	 * 
	 * @param the caller, externally owned contract
	 * @param classpath the class path where the {@code caller} can be interpreted and the code must be executed
	 * @param constructor the constructor that must be called
	 * @param actuals the actual arguments passed to the constructor
	 * @return the created object, if the constructor was successfully executed, without exception
	 * @throws TransactionException if the transaction could not be completed because of an internal error
	 * @throws CodeExecutionException if the execution of the constructor failed with an exception (available as
	 *                                {@code getCause()}. Note that, in this case, from the point of view of Takamaka
	 *                                the transaction was successful and the exception is a programmer's problem
	 */
	public StorageReference addConstructorCallTransaction(StorageReference caller, Classpath classpath, ConstructorReference constructor, StorageValue... actuals) throws TransactionException, CodeExecutionException;

	/**
	 * Runs an {@code @@Entry} constructor of a class.
	 * 
	 * @param caller the externally owned caller contract
	 * @param classpath the class path where the {@code caller} can be interpreted and the code must be executed
	 * @param constructor the constructor that must be called
	 * @param actuals the actual arguments passed to the constructor
	 * @return the created object, if the constructor was successfully executed, without exception
	 * @throws TransactionException if the transaction could not be completed because of an internal error
	 * @throws CodeExecutionException if the execution of the constructor failed with an exception (available as
	 *                                {@code getCause()}. Note that, in this case, from the point of view of Takamaka
	 *                                the transaction was successful and the exception is a programmer's problem
	 */
	public StorageReference addEntryConstructorCallTransaction(StorageReference caller, Classpath classpath, ConstructorReference constructor, StorageValue... actuals) throws TransactionException, CodeExecutionException;

	/**
	 * Runs an instance method of an object stored in the blockchain.
	 * 
	 * @param caller the externally owned caller contract
	 * @param classpath the class path where the {@code caller} can be interpreted and the code must be executed
	 * @param method the method that must be called
	 * @param receiver the object whose method is called
	 * @param actuals the actual arguments passed to the constructor
	 * @return the result of the call, if the method was successfully executed, without exception. If the method is
	 *         declared to return {@code void}, this result will be {@code null}
	 * @throws TransactionException if the transaction could not be completed because of an internal error
	 * @throws CodeExecutionException if the execution of the method failed with an exception (available as
	 *                                {@code getCause()}. Note that, in this case, from the point of view of Takamaka
	 *                                the transaction was successful and the exception is a programmer's problem
	 */
	public StorageValue addInstanceMethodCallTransaction(StorageReference caller, Classpath classpath, MethodReference method, StorageReference receiver, StorageValue... actuals) throws TransactionException, CodeExecutionException;

	/**
	 * Runs a static method of a class.
	 * 
	 * @param caller the externally owned caller contract
	 * @param classpath the class path where the {@code caller} can be interpreted and the code must be executed
	 * @param method the method that must be called. It specifies the class from where it must be looked up
	 * @param actuals the actual arguments passed to the constructor
	 * @return the result of the call, if the method was successfully executed, without exception. If the method is
	 *         declared to return {@code void}, this result will be {@code null}
	 * @throws TransactionException if the transaction could not be completed because of an internal error
	 * @throws CodeExecutionException if the execution of the method failed with an exception (available as
	 *                                {@code getCause()}. Note that, in this case, from the point of view of Takamaka
	 *                                the transaction was successful and the exception is a programmer's problem
	 */
	public StorageValue addStaticMethodCallTransaction(StorageReference caller, Classpath classpath, MethodReference method, StorageValue... actuals) throws TransactionException, CodeExecutionException;

	/**
	 * Runs an {@code @@Entry} instance method of an object stored in the blockchain.
	 * 
	 * @param caller the externally owned caller contract
	 * @param classpath the class path where the {@code caller} can be interpreted and the code must be executed
	 * @param method the method that must be called
	 * @param receiver the object whose method is called
	 * @param actuals the actual arguments passed to the constructor
	 * @return the result of the call, if the method was successfully executed, without exception. If the method is
	 *         declared to return {@code void}, this result will be {@code null}
	 * @throws TransactionException if the transaction could not be completed because of an internal error
	 * @throws CodeExecutionException if the execution of the method failed with an exception (available as
	 *                                {@code getCause()}. Note that, in this case, from the point of view of Takamaka
	 *                                the transaction was successful and the exception is a programmer's problem
	 */
	public StorageValue addEntryInstanceMethodCallTransaction(StorageReference caller, Classpath classpath, MethodReference method, StorageReference receiver, StorageValue... actuals) throws TransactionException, CodeExecutionException;
}