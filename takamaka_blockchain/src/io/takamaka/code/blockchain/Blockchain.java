package io.takamaka.code.blockchain;

import io.takamaka.code.blockchain.requests.ConstructorCallTransactionRequest;
import io.takamaka.code.blockchain.requests.GameteCreationTransactionRequest;
import io.takamaka.code.blockchain.requests.InstanceMethodCallTransactionRequest;
import io.takamaka.code.blockchain.requests.JarStoreInitialTransactionRequest;
import io.takamaka.code.blockchain.requests.JarStoreTransactionRequest;
import io.takamaka.code.blockchain.requests.RedGreenGameteCreationTransactionRequest;
import io.takamaka.code.blockchain.requests.StaticMethodCallTransactionRequest;
import io.takamaka.code.blockchain.responses.ConstructorCallTransactionResponse;
import io.takamaka.code.blockchain.responses.GameteCreationTransactionResponse;
import io.takamaka.code.blockchain.responses.JarStoreInitialTransactionResponse;
import io.takamaka.code.blockchain.responses.JarStoreTransactionResponse;
import io.takamaka.code.blockchain.responses.MethodCallTransactionResponse;

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
	 * @param current the reference to the transaction where this must be executed
	 * @return the response resulting from the execution of the request
	 * @throws TransactionException if the transaction could not be completed successfully
	 */
	public JarStoreInitialTransactionResponse runJarStoreInitialTransaction(JarStoreInitialTransactionRequest request, TransactionReference current) throws TransactionException;

	/**
	 * Runs a transaction that creates a gamete, that is, an externally owned contract with the given initial amount of coins.
	 * This transaction can only occur during initialization of the blockchain. It has
	 * no caller and requires no gas. This method runs the transaction
	 * specified by the request, after the given transaction reference, and yields the corresponding response.
	 * The blockchain does not get modified.
	 * 
	 * @param request the transaction request
	 * @param current the reference to the transaction where this must be executed
	 * @return the response resulting from the execution of the request
	 * @throws TransactionException if the transaction could not be completed successfully
	 */
	public abstract GameteCreationTransactionResponse runGameteCreationTransaction(GameteCreationTransactionRequest request, TransactionReference current) throws TransactionException;

	/**
	 * Runs a transaction that creates a red/green gamete, that is, a red/green externally owned contract with the given initial amount of coins.
	 * This transaction can only occur during initialization of the blockchain. It has
	 * no caller and requires no gas. This method runs the transaction
	 * specified by the request, after the given transaction reference, and yields the corresponding response.
	 * The blockchain does not get modified.
	 * 
	 * @param request the transaction request
	 * @param current the reference to the transaction where this must be executed
	 * @return the response resulting from the execution of the request
	 * @throws TransactionException if the transaction could not be completed successfully
	 */
	public abstract GameteCreationTransactionResponse runRedGreenGameteCreationTransaction(RedGreenGameteCreationTransactionRequest request, TransactionReference current) throws TransactionException;

	/**
	 * Runs a transaction that installs a jar in this blockchain. The goal is to install, in blockchain, a jar, with its dependencies.
	 * This method runs the transaction specified by the request, after the given transaction reference, and yields
	 * the corresponding response. The blockchain does not get modified.
	 * 
	 * @param request the transaction request
	 * @param current the reference to the transaction where this must be executed
	 * @return the response resulting from the execution of the request
	 * @throws TransactionException if the transaction could not be completed successfully
	 */
	public JarStoreTransactionResponse runJarStoreTransaction(JarStoreTransactionRequest request, TransactionReference current) throws TransactionException;

	/**
	 * Runs a transaction that calls a constructor of a class installed in blockchain.
	 * The goal is to run the constructor and compute a reference to the freshly created object.
	 * This method runs the transaction specified by the request, after the given transaction reference, and yields
	 * the corresponding response. The blockchain does not get modified.
	 * 
	 * @param request the transaction request
	 * @param current the reference to the transaction where this must be executed
	 * @return the response resulting from the execution of the request
	 * @throws TransactionException if the transaction could not be completed successfully
	 */
	public ConstructorCallTransactionResponse runConstructorCallTransaction(ConstructorCallTransactionRequest request, TransactionReference current) throws TransactionException;

	/**
	 * Runs a transaction that calls an instance method of an object in blockchain.
	 * The goal is to run the method and compute its returned value (if any).
	 * This method runs the transaction specified by the request, after the given transaction reference, and yields
	 * the corresponding response. The blockchain does not get modified.
	 * 
	 * @param request the transaction request
	 * @param current the reference to the transaction where this must be executed
	 * @return the response resulting from the execution of the request
	 * @throws TransactionException if the transaction could not be completed successfully
	 */
	public MethodCallTransactionResponse runInstanceMethodCallTransaction(InstanceMethodCallTransactionRequest request, TransactionReference current) throws TransactionException;

	/**
	 * Runs a transaction that calls a static method of a class in blockchain.
	 * The goal is to run the method and compute its returned value (if any).
	 * This method runs the transaction specified by the request, at the given transaction reference, and yields
	 * the corresponding response. The blockchain does not get modified.
	 * 
	 * @param request the transaction request
	 * @param current the reference to the transaction where this must be executed
	 * @return the response resulting from the execution of the request
	 * @throws TransactionException if the transaction could not be completed successfully
	 */
	public MethodCallTransactionResponse runStaticMethodCallTransaction(StaticMethodCallTransactionRequest request, TransactionReference current) throws TransactionException;
}