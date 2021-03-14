package io.takamaka.code.verification;

import java.util.function.BiConsumer;
import java.util.stream.Stream;

import io.takamaka.code.verification.internal.TakamakaClassLoaderImpl;
import io.takamaka.code.whitelisting.ResolvingClassLoader;

/**
 * A class loader used to access the definition of the classes of a Takamaka program.
 */
public interface TakamakaClassLoader extends ResolvingClassLoader {

	/**
	 * Builds a class loader with the given jars, given as byte arrays.
	 * 
	 * @param jars the jars
	 * @param verificationVersion the version of the verification module that must b e used; this affects the
	 *                            set of white-listing annotations used by the class loader
	 */
	static TakamakaClassLoader of(Stream<byte[]> jars, int verificationVersion) {
		return new TakamakaClassLoaderImpl(jars, verificationVersion, (name, pos) -> {});
	}

	/**
	 * Builds a class loader with the given jars, given as byte arrays.
	 * 
	 * @param jars the jars
	 * @param verificationVersion the version of the verification module that must b e used; this affects the
	 *                            set of white-listing annotations used by the class loader
	 * @param classNameProcessor a processor called whenever a new class is loaded with this class loader;
	 *                           it can be used to take note that a class with a given name comes from the
	 *                           n-th jar in {@code jars}
	 */
	static TakamakaClassLoader of(Stream<byte[]> jars, int verificationVersion, BiConsumer<String, Integer> classNameProcessor) {
		return new TakamakaClassLoaderImpl(jars, verificationVersion, classNameProcessor);
	}

	/**
	 * Determines if a class is an instance of the storage class.
	 * 
	 * @param className the name of the class
	 * @return true if and only if that class extends {@link io.takamaka.code.lang.Storage}
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
	 * Checks if a class is an instance of the consensus update event class.
	 * 
	 * @param className the name of the class
	 * @return true if and only if that condition holds
	 */
	boolean isConsensusUpdateEvent(String className);

	/**
	 * Checks if a class is an instance of the gas price update event class.
	 * 
	 * @param className the name of the class
	 * @return true if and only if that condition holds
	 */
	boolean isGasPriceUpdateEvent(String className);

	/**
	 * Checks if a class is an instance of the validators update event class.
	 * 
	 * @param className the name of the class
	 * @return true if and only if that condition holds
	 */
	boolean isValidatorsUpdateEvent(String className);

	/**
	 * Checks if a class is an instance of another class.
	 * 
	 * @param className the class
	 * @param superclassName the potential superclass of {@code className}
	 * @return true if and only if {@code className} is a subclass of {@code superclassName}
	 */
	boolean isa(String className, String superclassName);

	/**
	 * Checks if a class is annotated as {@code @@Exported}.
	 * 
	 * @param className the name of the class
	 * @return true if and only if that condition holds
	 */
	boolean isExported(String className);

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
	 * Yields the class token of the storage class.
	 * 
	 * @return the class token
	 */
	Class<?> getStorage();

	/**
	 * Yields the class token of the account interface.
	 * 
	 * @return the class token
	 */
	Class<?> getAccount();

	/**
	 * Yields the class token of the interface for accounts
	 * that use the ed25519 algorithm for signing transactions.
	 * 
	 * @return the class token
	 */
	Class<?> getAccountED25519();

	/**
	 * Yields the class token of the interface for accounts
	 * that use the qtesla-p-I algorithm for signing transactions.
	 * 
	 * @return the class token
	 */
	Class<?> getAccountQTESLA1();

	/**
	 * Yields the class token of the interface for accounts
	 * that use the qtesla-p-III algorithm for signing transactions.
	 * 
	 * @return the class token
	 */
	Class<?> getAccountQTESLA3();

	/**
	 * Yields the class token of the interface for accounts
	 * that use the sha256dsa algorithm for signing transactions.
	 * 
	 * @return the class token
	 */
	Class<?> getAccountSHA256DSA();

	/**
	 * Yields the class token of the externally owned account class.
	 * 
	 * @return the class token
	 */
	Class<?> getExternallyOwnedAccount();

	/**
	 * Yields the class token of gamete class.
	 * 
	 * @return the class token
	 */
	Class<?> getGamete();
}