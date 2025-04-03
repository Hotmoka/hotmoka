/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.node.local.internal.builders;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import io.hotmoka.exceptions.ExceptionSupplier;
import io.hotmoka.instrumentation.api.InstrumentationFields;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.responses.JarStoreTransactionResponseWithInstrumentedJar;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.types.StorageType;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.local.api.ClassLoaderCreationException;
import io.hotmoka.node.local.api.EngineClassLoader;
import io.hotmoka.node.local.api.StoreException;
import io.hotmoka.node.local.internal.Reverification;
import io.hotmoka.verification.TakamakaClassLoaders;
import io.hotmoka.verification.api.IllegalJarException;
import io.hotmoka.verification.api.TakamakaClassLoader;
import io.hotmoka.whitelisting.api.UnsupportedVerificationVersionException;
import io.hotmoka.whitelisting.api.WhiteListingWizard;

/**
 * The implementation of a classloader used to access the definition of the classes
 * of Takamaka methods or constructors executed during a transaction.
 */
public final class EngineClassLoaderImpl implements EngineClassLoader {

	/**
	 * The parent of this class loader;
	 */
	private final TakamakaClassLoader parent;

	/**
	 * Method {@link io.takamaka.code.lang.Storage#fromContract(io.takamaka.code.lang.Contract)}.
	 */
	private final Method fromContract;

	/**
	 * Method {@link io.takamaka.code.lang.Contract#payableFromContract(io.takamaka.code.lang.Contract, int)}.
	 */
	private final Method payableFromContractInt;

	/**
	 * Method {@link io.takamaka.code.lang.Contract#payableFromContract(io.takamaka.code.lang.Contract, long)}.
	 */
	private final Method payableFromContractLong;

	/**
	 * Method {@link io.takamaka.code.lang.Contract#payableFromContract(io.takamaka.code.lang.Contract, BigInteger)}.
	 */
	private final Method payableFromContractBigInteger;

	/**
	 * Field {@link io.takamaka.code.lang.ExternallyOwnedAccount#nonce}.
	 */
	private final Field externallyOwnedAccountNonce;

	/**
	 * Field {@link io.takamaka.code.givernance.AbstractValidators#currentSupply}.
	 */
	private final Field abstractValidatorsCurrentSupply;

	/**
	 * Field {@link io.takamaka.code.lang.Storage#storageReference}.
	 */
	private final Field storageReference;

	/**
	 * Field {@link io.takamaka.code.lang.Storage#inStorage}.
	 */
	private final Field inStorage;

	/**
	 * Field {@link io.takamaka.code.lang.Contract#balance}.
	 */
	private final Field balanceField;

	/**
	 * The lengths (in bytes) of the instrumented jars of the classpath and its dependencies
	 * used to create this class loader.
	 */
	private final int[] lengthsOfJars;

	/**
	 * A map from each class name to the transaction that installed the jar it belongs to.
	 */
	private final ConcurrentMap<String, TransactionReference> transactionsThatInstalledJarForClasses = new ConcurrentHashMap<>();

	/**
	 * List of reverification that has been performed on the responses of the transactions that installed
	 * the jars in this class loader. This occurs if the verification version of the node changed
	 * w.r.t. its version when the response has been added into the store.
	 */
	private final Reverification reverification;

	private final static int CLASS_END_LENGTH = ".class".length();

	/**
	 * Builds the class loader for the given jar and its dependencies.
	 * 
	 * @param jar the jar; this might be null, in which case the class loader includes the dependencies only
	 * @param dependencies the dependencies
	 * @param environment the execution environment for which the class loader must be built
	 * @param consensus the consensus parameters to use for reverification
	 * @throws ClassLoaderCreationException if the class loader cannot be created, for instance because the
	 *                                      {@code dependencies} refer to some failed transaction
	 * @throws StoreException if the store is misbehaving
	 */
	public EngineClassLoaderImpl(byte[] jar, Stream<TransactionReference> dependencies, ExecutionEnvironment environment, ConsensusConfig<?,?> consensus) throws StoreException, ClassLoaderCreationException {
		try {
			var dependenciesAsList = dependencies.collect(Collectors.toList());

			this.reverification = new Reverification(dependenciesAsList.stream(), environment, consensus);
			var jars = new ArrayList<byte[]>();
			var transactionsOfJars = new ArrayList<TransactionReference>();
			this.parent = mkTakamakaClassLoader(dependenciesAsList, consensus, jar, environment, jars, transactionsOfJars);
			this.lengthsOfJars = jars.stream().mapToInt(bytes -> bytes.length).toArray();
			Class<?> contract = getContract(), storage = getStorage();
			this.fromContract = storage.getDeclaredMethod("fromContract", contract);
			this.fromContract.setAccessible(true); // it was private
			this.payableFromContractInt = contract.getDeclaredMethod("payableFromContract", contract, int.class);
			this.payableFromContractInt.setAccessible(true); // it was private
			this.payableFromContractLong = contract.getDeclaredMethod("payableFromContract", contract, long.class);
			this.payableFromContractLong.setAccessible(true); // it was private
			this.payableFromContractBigInteger = contract.getDeclaredMethod("payableFromContract", contract, BigInteger.class);
			this.payableFromContractBigInteger.setAccessible(true); // it was private
			this.externallyOwnedAccountNonce = getExternallyOwnedAccount().getDeclaredField("nonce");
			this.externallyOwnedAccountNonce.setAccessible(true); // it was private
			this.abstractValidatorsCurrentSupply = getAbstractValidators().getDeclaredField("currentSupply");
			this.abstractValidatorsCurrentSupply.setAccessible(true); // it was private
			this.storageReference = storage.getDeclaredField(InstrumentationFields.STORAGE_REFERENCE_FIELD_NAME);
			this.storageReference.setAccessible(true); // it was private
			this.inStorage = storage.getDeclaredField(InstrumentationFields.IN_STORAGE);
			this.inStorage.setAccessible(true); // it was private
			this.balanceField = contract.getDeclaredField("balance");
			this.balanceField.setAccessible(true); // it was private
		}
		catch (NoSuchMethodException | NoSuchFieldException e) {
			throw new StoreException("Unexpected class change", e);
		}
	}

	/**
	 * Yields the Takamaka class loader for the given dependencies.
	 * 
	 * @param dependencies the dependencies
	 * @param consensus the consensus parameters of the node
	 * @param start an initial jar. This can be {@code null}
	 * @param node the node for which the class loader is created
	 * @return the class loader
	 * @throws StoreException 
	 * @throws ClassLoaderCreationException 
	 */
	private TakamakaClassLoader mkTakamakaClassLoader(List<TransactionReference> dependencies, ConsensusConfig<?,?> consensus, byte[] start, ExecutionEnvironment environment, List<byte[]> jars, ArrayList<TransactionReference> transactionsOfJars) throws StoreException, ClassLoaderCreationException {
		var counter = new AtomicInteger();

		if (start != null) {
			jars.add(start);
			transactionsOfJars.add(null);
			counter.incrementAndGet();
		}

		for (var dependency: dependencies)
			addJars(dependency, consensus, jars, transactionsOfJars, environment, counter);

		processClassesInJars(jars, transactionsOfJars, environment);

		try {
			return TakamakaClassLoaders.of(jars.stream(), consensus.getVerificationVersion());
		}
		catch (UnsupportedVerificationVersionException e) {
			throw new StoreException(e);
		}
		catch (IllegalJarException e) {
			throw new ClassLoaderCreationException(e);
		}
	}

	/**
	 * Expands the given list of jars with the components of the given classpath.
	 * 
	 * @param classpath the classpath
	 * @param consensus the consensus parameters of the node
	 * @param jars the list where the jars will be added
	 * @param jarTransactions the list of transactions where the {@code jars} have been installed
	 * @param node the node for which the class loader is created
	 * @param counter the number of jars that have been encountered up to now, during the recursive descent
	 * @throws ClassLoaderCreationException 
	 */
	private void addJars(TransactionReference classpath, ConsensusConfig<?,?> consensus, List<byte[]> jars, List<TransactionReference> jarTransactions, ExecutionEnvironment environment, AtomicInteger counter) throws StoreException, ClassLoaderCreationException {
		// consensus might be null if the node is restarting, during the recomputation of its consensus itself
		if (counter.incrementAndGet() > consensus.getMaxDependencies())
			throw new ClassLoaderCreationException("Too many dependencies in classpath: max is " + consensus.getMaxDependencies());

		JarStoreTransactionResponseWithInstrumentedJar responseWithInstrumentedJar = getResponseWithInstrumentedJarAt(classpath, environment);

		// we consider its dependencies before as well, recursively
		for (var dependency: responseWithInstrumentedJar.getDependencies().toArray(TransactionReference[]::new))
			addJars(dependency, consensus, jars, jarTransactions, environment, counter);

		jars.add(responseWithInstrumentedJar.getInstrumentedJar());
		jarTransactions.add(classpath);

		if (jars.stream().mapToLong(bytes -> bytes.length).sum() > consensus.getMaxCumulativeSizeOfDependencies())
			throw new ClassLoaderCreationException("Too large cumulative size of dependencies in classpath: max is " + consensus.getMaxCumulativeSizeOfDependencies() + " bytes");
	}

	/**
	 * Checks that there are no split packages across jars and takes note of the transaction
	 * that installed each class in the jars.
	 * 
	 * @param jars the jars that form the classpath of this classloader
	 * @param transactionsOfJars the transactions that have installed the {@code jars}
	 * @throws ClassLoaderCreationException if the jars are corrupted or they contain a split package 
	 */
	private void processClassesInJars(List<byte[]> jars, List<TransactionReference> transactionsOfJars, ExecutionEnvironment environment) throws ClassLoaderCreationException {
		// a map from each package name to the jar that defines it
		var packages = new HashMap<String, Integer>();
	
		int pos = 0;
		for (byte[] jar: jars) {
			TransactionReference reference = transactionsOfJars.get(pos);

			try (var jis = new ZipInputStream(new ByteArrayInputStream(jar))) {
				ZipEntry entry;
				while ((entry = jis.getNextEntry()) != null) {
					String className = entry.getName();
					if (className.endsWith(".class")) {
						className = className.substring(0, className.length() - CLASS_END_LENGTH).replace('/', '.');
						int lastDot = className.lastIndexOf('.');
						if (lastDot == 0)
							throw new ClassLoaderCreationException("Package names cannot start with a dot");
	
						String packageName = lastDot < 0 ? "" : className.substring(0, lastDot);
						Integer previously = packages.get(packageName);
						if (previously == null)
							packages.put(packageName, pos);
						else if (previously != pos)
							if (packageName.isEmpty())
								throw new ClassLoaderCreationException("The default package cannot be split across more jars");
							else
								throw new ClassLoaderCreationException("Package " + packageName + " cannot be split across more jars");
	
						// if the transaction reference is null, it means that the class comes from a jar that is being installed
						// by the transaction that created this class loader. In that case, the storage reference of the class is not used
						if (reference != null)
							transactionsThatInstalledJarForClasses.put(className, reference);
					}
				}
	        }
			catch (IOException e) {
				// some jar seems corrupted
				throw new ClassLoaderCreationException("Corrupted jar", e);
			}
	
			pos++;
		}		
	}

	/*
	 * Yields the response generated by the transaction with the given reference, even
	 * before the transaction gets committed. The transaction must be a transaction that installed
	 * a jar in the store of the node.
	 * 
	 * @param reference the reference of the transaction
	 * @param environment the execution environment for which the class loader is created
	 * @return the response
	 * @throws ClassLoaderCreationException if the transaction does not exist in the store, or did not generate a response with instrumented jar
	 */
	private JarStoreTransactionResponseWithInstrumentedJar getResponseWithInstrumentedJarAt(TransactionReference reference, ExecutionEnvironment environment) throws StoreException, ClassLoaderCreationException {
		// first we check if the response has been reverified and we use the reverified version
		Optional<TransactionResponse> maybeResponse = reverification.getReverifiedResponse(reference);
		TransactionResponse response;
		if (maybeResponse.isPresent())
			response = maybeResponse.get();
		else {
			// otherwise the response has not been reverified
			try {
				response = environment.getResponse(reference);
			}
			catch (UnknownReferenceException e) {
				throw new ClassLoaderCreationException("Unknown transaction reference " + reference);
			}
		}

		if (response instanceof JarStoreTransactionResponseWithInstrumentedJar trwij)
			return trwij;
		else
			throw new ClassLoaderCreationException("The transaction " + reference + " did not install a jar in store");
	}

	@Override
	public Class<?> loadClass(StorageType type) throws ClassNotFoundException {
		if (type == StorageTypes.BOOLEAN)
			return boolean.class;
		else if (type == StorageTypes.BYTE)
			return byte.class;
		else if (type == StorageTypes.CHAR)
			return char.class;
		else if (type == StorageTypes.SHORT)
			return short.class;
		else if (type == StorageTypes.INT)
			return int.class;
		else if (type == StorageTypes.LONG)
			return long.class;
		else if (type == StorageTypes.FLOAT)
			return float.class;
		else if (type == StorageTypes.DOUBLE)
			return double.class;
		else if (type instanceof ClassType ct)
			return loadClass(ct.getName());
		else if (type == null)
			throw new RuntimeException("Unexpected null storage type");
		else
			throw new RuntimeException("Unexpected storage type of class " + type.getClass().getName());
	}

	@Override
	public IntStream getLengthsOfJars() {
		return IntStream.of(lengthsOfJars);
	}

	@Override
	public Optional<TransactionReference> transactionThatInstalledJarFor(Class<?> clazz) {
		return Optional.ofNullable(transactionsThatInstalledJarForClasses.get(clazz.getName()));
	}

	@Override
	public <E extends Exception> StorageReference getStorageReferenceOf(Object object, ExceptionSupplier<? extends E> onIllegalAccess) throws E {
		try {
			return (StorageReference) storageReference.get(object);
		}
		catch (RuntimeException | IllegalAccessException e) {
			throw onIllegalAccess.apply("Cannot read the storage reference: " + e.getMessage());
		}
	}

	@Override
	public <E extends Exception> boolean getInStorageOf(Object object, ExceptionSupplier<? extends E> onIllegalAccess) throws E {
		try {
			return (boolean) inStorage.get(object);
		}
		catch (RuntimeException | IllegalAccessException e) {
			throw onIllegalAccess.apply("Cannot read the inStorage field: " + e.getMessage());
		}
	}

	@Override
	public <E extends Exception> BigInteger getBalanceOf(Object object, ExceptionSupplier<? extends E> onIllegalAccess) throws E {
		try {
			return (BigInteger) balanceField.get(object);
		}
		catch (RuntimeException | IllegalAccessException e) {
			throw onIllegalAccess.apply("Cannot read the balance field: " + e.getMessage());
		}
	}

	@Override
	public <E extends Exception> void setBalanceOf(Object object, BigInteger value, ExceptionSupplier<? extends E> onIllegalAccess) throws E {
		try {
			balanceField.set(object, value);
		}
		catch (RuntimeException | IllegalAccessException e) {
			throw onIllegalAccess.apply("Cannot write the balance field: " + e.getMessage());
		}
	}

	@Override
	public <E extends Exception> void setNonceOf(Object object, BigInteger value, ExceptionSupplier<? extends E> onIllegalAccess) throws E {
		try {
			externallyOwnedAccountNonce.set(object, value);
		}
		catch (RuntimeException | IllegalAccessException e) {
			throw onIllegalAccess.apply("Cannot write the nonce field: " + e.getMessage());
		}
	}

	@Override
	public <E extends Exception> void fromContract(Object callee, Object caller, ExceptionSupplier<? extends E> onIllegalAccess) throws E {
		try {
			fromContract.invoke(callee, caller);
		}
		catch (RuntimeException | IllegalAccessException e) {
			throw onIllegalAccess.apply("Cannot call Storage.fromContract(): " + e.getMessage());
		}
		catch (InvocationTargetException e) {
			Throwable cause = e.getCause();

			// if the called code throws an unchecked exception, we forward it since some of its requirements might have been violated
			if (cause instanceof RuntimeException re)
				throw re;
			else if (cause instanceof Error error)
				throw error;
			else if (cause != null)
				throw onIllegalAccess.apply("Storage.fromContract() threw an unexpected exception of class " + cause.getClass().getName());
			else
				throw onIllegalAccess.apply("Cannot call Storage.fromContract()");
		}
	}

	@Override
	public <E extends Exception> void payableFromContract(Object callee, Object payer, BigInteger amount, ExceptionSupplier<? extends E> onIllegalAccess) throws E {
		try {
			payableFromContractBigInteger.invoke(callee, payer, amount);
		}
		catch (RuntimeException | IllegalAccessException e) {
			throw onIllegalAccess.apply("Cannot call Contract.payableFromContract(): " + e.getMessage());
		}
		catch (InvocationTargetException e) {
			Throwable cause = e.getCause();

			// if the called code throws an unchecked exception, we forward it since some of its requirements might have been violated
			if (cause instanceof RuntimeException re)
				throw re;
			else if (cause instanceof Error error)
				throw error;
			else if (cause != null)
				throw onIllegalAccess.apply("Storage.payableFromContract() threw an unexpected exception of class " + cause.getClass().getName());
			else
				throw onIllegalAccess.apply("Cannot call Contract.payableFromContract()");
		}
	}

	@Override
	public <E extends Exception> void payableFromContract(Object callee, Object caller, int amount, ExceptionSupplier<? extends E> onIllegalAccess) throws E {
		try {
			payableFromContractInt.invoke(callee, caller, amount);
		}
		catch (RuntimeException | IllegalAccessException e) {
			throw onIllegalAccess.apply("Cannot call Contract.payableFromContract(): " + e.getMessage());
		}
		catch (InvocationTargetException e) {
			Throwable cause = e.getCause();

			// if the called code throws an unchecked exception, we forward it since some of its requirements might have been violated
			if (cause instanceof RuntimeException re)
				throw re;
			else if (cause instanceof Error error)
				throw error;
			else if (cause != null)
				throw onIllegalAccess.apply("Storage.payableFromContract() threw an unexpected exception of class " + cause.getClass().getName());
			else
				throw onIllegalAccess.apply("Cannot call Contract.payableFromContract()");
		}
	}

	@Override
	public <E extends Exception> void payableFromContract(Object callee, Object caller, long amount, ExceptionSupplier<? extends E> onIllegalAccess) throws E {
		try {
			payableFromContractLong.invoke(callee, caller, amount);
		}
		catch (RuntimeException | IllegalAccessException e) {
			throw onIllegalAccess.apply("Cannot call Contract.payableFromContract(): " + e.getMessage());
		}
		catch (InvocationTargetException e) {
			Throwable cause = e.getCause();

			// if the called code throws an unchecked exception, we forward it since some of its requirements might have been violated
			if (cause instanceof RuntimeException re)
				throw re;
			else if (cause instanceof Error error)
				throw error;
			else if (cause != null)
				throw onIllegalAccess.apply("Storage.payableFromContract() threw an unexpected exception of class " + cause.getClass().getName());
			else
				throw onIllegalAccess.apply("Cannot call Contract.payableFromContract()");
		}
	}

	@Override
	public void replaceReverifiedResponses() throws StoreException {
		reverification.replace();
	}

	@Override
	public Class<?> loadClass(String className) throws ClassNotFoundException {
		return parent.loadClass(className);
	}

	@Override
	public WhiteListingWizard getWhiteListingWizard() {
		return parent.getWhiteListingWizard();
	}

	@Override
	public Optional<Field> resolveField(String className, String name, Class<?> type) throws ClassNotFoundException {
		return parent.resolveField(className, name, type);
	}

	@Override
	public Optional<Field> resolveField(Class<?> clazz, String name, Class<?> type) {
		return parent.resolveField(clazz, name, type);
	}

	@Override
	public Optional<Constructor<?>> resolveConstructor(String className, Class<?>[] args) throws ClassNotFoundException {
		return parent.resolveConstructor(className, args);
	}

	@Override
	public Optional<Constructor<?>> resolveConstructor(Class<?> clazz, Class<?>[] args) {
		return parent.resolveConstructor(clazz, args);
	}

	@Override
	public Optional<Method> resolveMethod(String className, String methodName, Class<?>[] args, Class<?> returnType) throws ClassNotFoundException {
		return parent.resolveMethod(className, methodName, args, returnType);
	}

	@Override
	public Optional<Method> resolveMethod(Class<?> clazz, String methodName, Class<?>[] args, Class<?> returnType) {
		return parent.resolveMethod(clazz, methodName, args, returnType);
	}

	@Override
	public Optional<Method> resolveInterfaceMethod(String className, String methodName, Class<?>[] args, Class<?> returnType) throws ClassNotFoundException {
		return parent.resolveInterfaceMethod(className, methodName, args, returnType);
	}

	@Override
	public Optional<Method> resolveInterfaceMethod(Class<?> clazz, String methodName, Class<?>[] args, Class<?> returnType) {
		return parent.resolveInterfaceMethod(clazz, methodName, args, returnType);
	}

	@Override
	public boolean isStorage(String className) throws ClassNotFoundException {
		return parent.isStorage(className);
	}

	@Override
	public boolean isContract(String className) throws ClassNotFoundException {
		return parent.isContract(className);
	}

	@Override
	public boolean isConsensusUpdateEvent(String className) throws ClassNotFoundException {
		return parent.isConsensusUpdateEvent(className);
	}

	@Override
	public boolean isGasPriceUpdateEvent(String className) throws ClassNotFoundException {
		return parent.isGasPriceUpdateEvent(className);
	}

	@Override
	public boolean isInflationUpdateEvent(String className) throws ClassNotFoundException {
		return parent.isInflationUpdateEvent(className);
	}

	@Override
	public boolean isValidatorsUpdateEvent(String className) throws ClassNotFoundException {
		return parent.isValidatorsUpdateEvent(className);
	}

	@Override
	public boolean isa(String className, String superclassName) throws ClassNotFoundException {
		return parent.isa(className, superclassName);
	}

	@Override
	public boolean isInterface(String className) throws ClassNotFoundException {
		return parent.isInterface(className);
	}

	@Override
	public boolean isExported(String className) throws ClassNotFoundException {
		return parent.isExported(className);
	}

	@Override
	public boolean isLazilyLoaded(Class<?> type) {
		return parent.isLazilyLoaded(type);
	}

	@Override
	public boolean isEagerlyLoaded(Class<?> type) {
		return parent.isEagerlyLoaded(type);
	}

	@Override
	public Class<?> getContract() {
		return parent.getContract();
	}

	@Override
	public Class<?> getManifest() {
		return parent.getManifest();
	}

	@Override
	public Class<?> getStorage() {
		return parent.getStorage();
	}

	@Override
	public Class<?> getExternallyOwnedAccount() {
		return parent.getExternallyOwnedAccount();
	}

	@Override
	public Class<?> getAbstractValidators() {
		return parent.getAbstractValidators();
	}

	@Override
	public Class<?> getGamete() {
		return parent.getGamete();
	}

	@Override
	public Class<?> getAccount() {
		return parent.getAccount();
	}

	@Override
	public Class<?> getAccountED25519() {
		return parent.getAccountED25519();
	}

	@Override
	public Class<?> getAccountQTESLA1() {
		return parent.getAccountQTESLA1();
	}

	@Override
	public Class<?> getAccountQTESLA3() {
		return parent.getAccountQTESLA3();
	}

	@Override
	public Class<?> getAccountSHA256DSA() {
		return parent.getAccountSHA256DSA();
	}

	@Override
	public ClassLoader getJavaClassLoader() {
		return parent.getJavaClassLoader();
	}

	@Override
	public long getVerificationVersion() {
		return parent.getVerificationVersion();
	}
}