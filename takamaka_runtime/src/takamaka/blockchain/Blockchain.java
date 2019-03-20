package takamaka.blockchain;

import java.nio.file.Path;

import takamaka.blockchain.values.StorageReference;
import takamaka.blockchain.values.StorageValue;

public interface Blockchain {
	public TransactionReference addJarStoreTransaction(Path jar, Classpath... dependencies) throws TransactionException;

	/**
	 * Runs a constructor of a class.
	 * 
	 * @param classpath the class path where the code must be executed
	 * @param constructor the constructor that must be called
	 * @param actuals the actual arguments passed to the constructor
	 * @return the created object, if the constructor was successfully executed, without exception
	 * @throws TransactionException if the transaction could not be completed because of an internal error
	 * @throws CodeExecutionException if the execution of the constructor failed with an exception (available as
	 *                                {@code getCause()}. Note that, in this case, from the point of view of Takamaka
	 *                                the transaction was successful and the exception is a programmer's problem
	 */
	public StorageReference addConstructorCallTransaction(Classpath classpath, ConstructorReference constructor, StorageValue... actuals) throws TransactionException, CodeExecutionException;

	/**
	 * Runs an {@code @@Entry} constructor of a class.
	 * 
	 * @param classpath the class path where the code must be executed
	 * @param constructor the constructor that must be called
	 * @param actuals the actual arguments passed to the constructor
	 * @return the created object, if the constructor was successfully executed, without exception
	 * @throws TransactionException if the transaction could not be completed because of an internal error
	 * @throws CodeExecutionException if the execution of the constructor failed with an exception (available as
	 *                                {@code getCause()}. Note that, in this case, from the point of view of Takamaka
	 *                                the transaction was successful and the exception is a programmer's problem
	 */
	public StorageReference addEntryConstructorCallTransaction(Classpath classpath, ConstructorReference constructor, StorageReference caller, StorageValue... actuals) throws TransactionException, CodeExecutionException;

	/**
	 * Runs an instance method of an object stored in the blockchain.
	 * 
	 * @param classpath the class path where the code must be executed
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
	public StorageValue addInstanceMethodCallTransaction(Classpath classpath, MethodReference method, StorageReference receiver, StorageValue... actuals) throws TransactionException, CodeExecutionException;

	/**
	 * Runs a static method of a class.
	 * 
	 * @param classpath the class path where the code must be executed
	 * @param method the method that must be called. It specifies the class from where it must be looked up
	 * @param actuals the actual arguments passed to the constructor
	 * @return the result of the call, if the method was successfully executed, without exception. If the method is
	 *         declared to return {@code void}, this result will be {@code null}
	 * @throws TransactionException if the transaction could not be completed because of an internal error
	 * @throws CodeExecutionException if the execution of the method failed with an exception (available as
	 *                                {@code getCause()}. Note that, in this case, from the point of view of Takamaka
	 *                                the transaction was successful and the exception is a programmer's problem
	 */
	public StorageValue addStaticMethodCallTransaction(Classpath classpath, MethodReference method, StorageValue... actuals) throws TransactionException, CodeExecutionException;
}