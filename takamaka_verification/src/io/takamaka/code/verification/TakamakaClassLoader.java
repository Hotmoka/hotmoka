package io.takamaka.code.verification;

import java.net.URL;

import io.takamaka.code.verification.internal.TakamakaClassLoaderImpl;
import io.takamaka.code.whitelisting.ResolvingClassLoader;

/**
 * A class loader used to access the definition of the classes of a Takamaka program.
 */
public interface TakamakaClassLoader extends ResolvingClassLoader {

	/**
	 * Builds a class loader with the given URLs.
	 */
	static TakamakaClassLoader of(URL[] urls) {
		return new TakamakaClassLoaderImpl(urls);
	}

	/**
	 * Determines if a class is an instance of the storage class.
	 * 
	 * @param className the name of the class
	 * @return true if and only if that class extends {@link takamaka.blockchain.runtime.AbstractStorage}
	 */
	boolean isStorage(String className);

	/**
	 * Checks if a class is an instance of the contract class.
	 * 
	 * @param className the name of the class
	 * @return true if and only if that condition holds
	 */
	boolean isContract(String className);

	/**
	 * Checks if a class is an instance of the red/green contract class.
	 * 
	 * @param className the name of the class
	 * @return true if and only if that condition holds
	 */
	boolean isRedGreenContract(String className);

	/**
	 * Checks if a class is actually an interface.
	 * 
	 * @param className the name of the class
	 * @return true if and only if that condition holds
	 */
	boolean isInterface(String className);

	/**
	 * Determines if a field of a storage class, having the given field, is lazily loaded.
	 * 
	 * @param type the type
	 * @return true if and only if that condition holds
	 */
	boolean isLazilyLoaded(Class<?> type);

	/**
	 * Determines if a field of a storage class, having the given field, is eagerly loaded.
	 * 
	 * @param type the type
	 * @return true if and only if that condition holds
	 */
	boolean isEagerlyLoaded(Class<?> type);

	/**
	 * Yields the class token of the contract class.
	 * 
	 * @return the class token
	 */
	Class<?> getContract();

	/**
	 * Yields the class token of the red/green contract class.
	 * 
	 * @return the class token
	 */
	Class<?> getRedGreenContract();

	/**
	 * Yields the class token of the storage class.
	 * 
	 * @return the class token
	 */
	Class<?> getStorage();

	/**
	 * Yields the class token of the externally owned account class.
	 * 
	 * @return the class token
	 */
	Class<?> getExternallyOwnedAccount();

	/**
	 * Yields the class token of the red/green externally owned account class.
	 * 
	 * @return the class token
	 */
	Class<?> getRedGreenExternallyOwnedAccount();
}