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

package io.hotmoka.node.local.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import io.hotmoka.beans.api.responses.TransactionResponse;
import io.hotmoka.beans.api.responses.TransactionResponseWithInstrumentedJar;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.instrumentation.InstrumentationFields;
import io.hotmoka.node.api.ConsensusConfig;
import io.hotmoka.node.local.api.EngineClassLoader;
import io.hotmoka.node.local.api.UnsupportedVerificationVersionException;
import io.hotmoka.verification.TakamakaClassLoaders;
import io.hotmoka.verification.api.TakamakaClassLoader;
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
	 * Method {@link io.takamaka.code.lang.Contract#redPayable(io.takamaka.code.lang.RedGreenContract, int)}.
	 */
	private final Method redPayableInt;

	/**
	 * Method {@link io.takamaka.code.lang.Contract#redPayable(io.takamaka.code.lang.RedGreenContract, long)}.
	 */
	private final Method redPayableLong;

	/**
	 * Method {@link io.takamaka.code.lang.Contract#redPayable(io.takamaka.code.lang.RedGreenContract, BigInteger)}.
	 */
	private final Method redPayableBigInteger;

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
	 * Field {@link io.takamaka.code.lang.Contract#redBalance}.
	 */
	private final Field redBalanceField;

	/**
	 * The lengths (in bytes) of the instrumented jars of the classpath and its dependencies
	 * used to create this class loader.
	 */
	private final int[] lengthsOfJars;

	/**
	 * The transactions that installed the jars of the classpath and its dependencies
	 * used to create this class loader.
	 */
	private final TransactionReference[] transactionsOfJars;

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
	
	/**
	 * Builds the class loader for the given jar and its dependencies.
	 * 
	 * @param jar the jar; this might be null, in which case the class loader includes the dependencies only
	 * @param dependencies the dependencies
	 * @param node the node for which the class loader is created
	 * @param reverify true if and only if the class loader must reverify jars installed in the store of the node
	 *                 that, at the time of installation, were verified with a version of the verification module older than the current one
	 * @param consensus the consensus parameters to use for reverification, if that is required
	 * @throws ClassNotFoundException if some class of the Takamaka runtime cannot be loaded
	 * @throws UnsupportedVerificationVersionException if the verification version is not available
	 * @throws IOException if there was an I/O error while accessing some jar
	 */
	public EngineClassLoaderImpl(byte[] jar, Stream<TransactionReference> dependencies, NodeInternal node, boolean reverify, ConsensusConfig<?,?> consensus) throws ClassNotFoundException, UnsupportedVerificationVersionException, IOException {
		try {
			List<TransactionReference> dependenciesAsList = dependencies.collect(Collectors.toList());

			// consensus might be null just after restarting a node, during the recomputation of the same consensus
			// from the store (see AbstractLocalNode.getConsensusParams())
			this.reverification = reverify && consensus != null ?
				new Reverification(dependenciesAsList.stream(), node, consensus)
				:
				// if reverification is not required, we build an empty reverification object, for no dependencies
				new Reverification(Stream.empty(), node, consensus);

			List<byte[]> jars = new ArrayList<>();
			ArrayList<TransactionReference> transactionsOfJars = new ArrayList<>();
			this.parent = mkTakamakaClassLoader(dependenciesAsList.stream(), consensus, jar, node, jars, transactionsOfJars);

			this.lengthsOfJars = jars.stream().mapToInt(bytes -> bytes.length).toArray();
			this.transactionsOfJars = transactionsOfJars.toArray(TransactionReference[]::new);

			Class<?> contract = getContract(), storage = getStorage();
			this.fromContract = storage.getDeclaredMethod("fromContract", contract);
			this.fromContract.setAccessible(true); // it was private
			this.payableFromContractInt = contract.getDeclaredMethod("payableFromContract", contract, int.class);
			this.payableFromContractInt.setAccessible(true); // it was private
			this.payableFromContractLong = contract.getDeclaredMethod("payableFromContract", contract, long.class);
			this.payableFromContractLong.setAccessible(true); // it was private
			this.payableFromContractBigInteger = contract.getDeclaredMethod("payableFromContract", contract, BigInteger.class);
			this.payableFromContractBigInteger.setAccessible(true); // it was private
			this.redPayableInt = contract.getDeclaredMethod("redPayable", contract, int.class);
			this.redPayableInt.setAccessible(true); // it was private
			this.redPayableLong = contract.getDeclaredMethod("redPayable", contract, long.class);
			this.redPayableLong.setAccessible(true); // it was private
			this.redPayableBigInteger = contract.getDeclaredMethod("redPayable", contract, BigInteger.class);
			this.redPayableBigInteger.setAccessible(true); // it was private
			this.redBalanceField = contract.getDeclaredField("balanceRed");
			this.redBalanceField.setAccessible(true); // it was private
			this.externallyOwnedAccountNonce = getExternallyOwnedAccount().getDeclaredField("nonce");
			this.externallyOwnedAccountNonce.setAccessible(true); // it was private
			this.abstractValidatorsCurrentSupply= getAbstractValidators().getDeclaredField("currentSupply");
			this.abstractValidatorsCurrentSupply.setAccessible(true); // it was private
			this.storageReference = storage.getDeclaredField(InstrumentationFields.STORAGE_REFERENCE_FIELD_NAME);
			this.storageReference.setAccessible(true); // it was private
			this.inStorage = storage.getDeclaredField(InstrumentationFields.IN_STORAGE);
			this.inStorage.setAccessible(true); // it was private
			this.balanceField = contract.getDeclaredField("balance");
			this.balanceField.setAccessible(true); // it was private
		}
		catch (IllegalArgumentException e) {
			throw e;
		}
		catch (NoSuchMethodException | NoSuchFieldException e) {
			throw new RuntimeException("unexpected class change", e);
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
	 * @throws ClassNotFoundException if some class of the Takamaka runtime cannot be loaded
	 */
	private TakamakaClassLoader mkTakamakaClassLoader(Stream<TransactionReference> dependencies, ConsensusConfig<?,?> consensus, byte[] start, NodeInternal node, List<byte[]> jars, ArrayList<TransactionReference> transactionsOfJars) throws ClassNotFoundException {
		var counter = new AtomicInteger();

		if (start != null) {
			jars.add(start);
			transactionsOfJars.add(null);
			counter.incrementAndGet();
		}

		dependencies.forEachOrdered(dependency -> addJars(dependency, consensus, jars, transactionsOfJars, node, counter));
		processClassInJar(jars, transactionsOfJars);

		// consensus might be null if the node is restarting, during the recomputation of its consensus itself
		return TakamakaClassLoaders.of(jars.stream(), consensus != null ? consensus.getVerificationVersion() : 0);
	}

	private final static int CLASS_END_LENGTH = ".class".length();

	/**
	 * Checks that there are no split packages across jars and takes note of the transaction
	 * that installed each class in the jars.
	 * 
	 * @param jars the jars that form the classpath of this classloader
	 * @param transactionsOfJars the transactions that have installed the {@code jars}
	 */
	private void processClassInJar(List<byte[]> jars, List<TransactionReference> transactionsOfJars) {
		// a map from each package name to the jar that defines it
		Map<String, Integer> packages = new HashMap<>();

		int pos = 0;
		for (byte[] jar: jars) {
			try (ZipInputStream jis = new ZipInputStream(new ByteArrayInputStream(jar))) {
    			ZipEntry entry;
    			while ((entry = jis.getNextEntry()) != null) {
    				String className = entry.getName();
					if (className.endsWith(".class")) {
    					className = className.substring(0, className.length() - CLASS_END_LENGTH).replace('/', '.');
    					int lastDot = className.lastIndexOf('.');

    					if (lastDot == 0)
    						throw new IllegalArgumentException("package names cannot start with a dot");

    					String packageName = lastDot < 0 ? "" : className.substring(0, lastDot);
    					Integer previously = packages.get(packageName);
    					if (previously == null)
    						packages.put(packageName, pos);
    					else if (previously != pos)
    						if (packageName.isEmpty())
    							throw new IllegalArgumentException("the default package cannot be split across more jars");
    						else
    							throw new IllegalArgumentException("package " + packageName + " cannot be split across more jars");

    					// if the transaction reference is null, it means that the class comes from a jar that is being installed
    					// by the transaction that created this class loader. In that case, the storage reference of the class is not used
    					TransactionReference reference = transactionsOfJars.get(pos);
    					if (reference != null)
    						transactionsThatInstalledJarForClasses.put(className, reference);
					}
    			}
            }
    		catch (IOException e) {
    			throw new UncheckedIOException(e);
    		}

			pos++;
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
	 */
	private void addJars(TransactionReference classpath, ConsensusConfig<?,?> consensus, List<byte[]> jars, List<TransactionReference> jarTransactions, NodeInternal node, AtomicInteger counter) {
		// consensus might be null if the node is restarting, during the recomputation of its consensus itself
		if (consensus != null && counter.incrementAndGet() > consensus.getMaxDependencies())
			throw new IllegalArgumentException("too many dependencies in classpath: max is " + consensus.getMaxDependencies());

		TransactionResponseWithInstrumentedJar responseWithInstrumentedJar = getResponseWithInstrumentedJarAtUncommitted(classpath, node);

		// we consider its dependencies before as well, recursively
		responseWithInstrumentedJar.getDependencies().forEachOrdered(dependency -> addJars(dependency, consensus, jars, jarTransactions, node, counter));

		jars.add(responseWithInstrumentedJar.getInstrumentedJar());
		jarTransactions.add(classpath);

		// consensus might be null if the node is restarting, during the recomputation of its consensus itself
		if (consensus != null && jars.stream().mapToLong(bytes -> bytes.length).sum() > consensus.getMaxCumulativeSizeOfDependencies())
			throw new IllegalArgumentException("too large cumulative size of dependencies in classpath: max is " + consensus.getMaxCumulativeSizeOfDependencies() + " bytes");
	}

	/*
	 * Yields the response generated by the transaction with the given reference, even
	 * before the transaction gets committed. The transaction must be a transaction that installed
	 * a jar in the store of the node.
	 * 
	 * @param reference the reference of the transaction
	 * @param node the node for which the class loader is created
	 * @return the response
	 * @throws IllegalArgumentException if the transaction does not exist in the store, or did not generate a response with instrumented jar
	 */
	private TransactionResponseWithInstrumentedJar getResponseWithInstrumentedJarAtUncommitted(TransactionReference reference, NodeInternal node) {
		// first we check if the response has been reverified and we use the reverified version
		TransactionResponse response = reverification.getReverifiedResponse(reference)
			// otherwise the response has not been reverified
			.or(() -> node.getCaches().getResponseUncommitted(reference))
			.orElseThrow(() -> new IllegalArgumentException("unknown transaction reference " + reference));
		
		if (!(response instanceof TransactionResponseWithInstrumentedJar))
			throw new IllegalArgumentException("the transaction " + reference + " did not install a jar in store");
	
		return (TransactionResponseWithInstrumentedJar) response;
	}

	@Override
	public final IntStream getLengthsOfJars() {
		return IntStream.of(lengthsOfJars);
	}

	@Override
	public final Stream<TransactionReference> getTransactionsOfJars() {
		return Stream.of(transactionsOfJars);
	}

	@Override
	public final TransactionReference transactionThatInstalledJarFor(Class<?> clazz) {
		return transactionsThatInstalledJarForClasses.get(clazz.getName());
	}

	@Override
	public final StorageReference getStorageReferenceOf(Object object) {
		try {
			return (StorageReference) storageReference.get(object);
		}
		catch (IllegalArgumentException e) {
			throw e;
		}
		catch (IllegalAccessException e) {
			throw new IllegalArgumentException("cannot read the storage reference of a storage object of class " + object.getClass().getName(), e);
		}
	}

	@Override
	public final boolean getInStorageOf(Object object) {
		try {
			return (boolean) inStorage.get(object);
		}
		catch (IllegalArgumentException e) {
			throw e;
		}
		catch (IllegalAccessException e) {
			throw new IllegalArgumentException("cannot read the inStorage tag of a storage object of class " + object.getClass().getName(), e);
		}
	}

	@Override
	public final BigInteger getBalanceOf(Object object) {
		try {
			return (BigInteger) balanceField.get(object);
		}
		catch (IllegalArgumentException e) {
			throw e;
		}
		catch (IllegalAccessException e) {
			throw new IllegalArgumentException("cannot read the balance field of a contract object of class " + object.getClass().getName(), e);
		}
	}

	@Override
	public final BigInteger getRedBalanceOf(Object object) {
		try {
			return (BigInteger) redBalanceField.get(object);
		}
		catch (IllegalArgumentException e) {
			throw e;
		}
		catch (IllegalAccessException e) {
			throw new IllegalArgumentException("cannot read the red balance field of a contract object of class " + object.getClass().getName(), e);
		}
	}

	@Override
	public final void setBalanceOf(Object object, BigInteger value) {
		try {
			balanceField.set(object, value);
		}
		catch (IllegalArgumentException e) {
			throw e;
		}
		catch (IllegalAccessException e) {
			throw new IllegalArgumentException("cannot write the balance field of a contract object of class " + object.getClass().getName(), e);
		}
	}

	@Override
	public final void setNonceOf(Object object, BigInteger value) {
		Class<?> clazz = object.getClass();

		try {
			if (getExternallyOwnedAccount().isAssignableFrom(clazz))
				externallyOwnedAccountNonce.set(object, value);
			else
				throw new IllegalArgumentException("unknown account class " + clazz);
		}
		catch (IllegalArgumentException e) {
			throw e;
		}
		catch (IllegalAccessException e) {
			throw new IllegalArgumentException("cannot write the nonce field of an account object of class " + clazz.getName(), e);
		}
	}

	@Override
	public final void increaseCurrentSupply(Object validators, BigInteger amount) {
		Class<?> clazz = validators.getClass();

		try {
			if (getAbstractValidators().isAssignableFrom(clazz))
				abstractValidatorsCurrentSupply.set(validators, ((BigInteger) abstractValidatorsCurrentSupply.get(validators)).add(amount));
			else
				throw new IllegalArgumentException("unknown validators class " + clazz);
		}
		catch (IllegalArgumentException e) {
			throw e;
		}
		catch (IllegalAccessException e) {
			throw new IllegalArgumentException("cannot access the current supply field of a validators object of class " + clazz.getName(), e);
		}
	}

	@Override
	public final void setRedBalanceOf(Object object, BigInteger value) {
		try {
			redBalanceField.set(object, value);
		}
		catch (IllegalArgumentException e) {
			throw e;
		}
		catch (IllegalAccessException e) {
			throw new IllegalArgumentException("cannot write the red balance field of a contract object of class " + object.getClass().getName(), e);
		}
	}

	/**
	 * Called at the beginning of the instrumentation of a {@code @@FromContract} method or constructor
	 * of a storage object. It forwards the call to {@code io.takamaka.code.lang.Storage.fromContract()}.
	 * 
	 * @param callee the contract whose method or constructor is called
	 * @param caller the caller of the method or constructor
	 * @throws any possible exception thrown inside {@code io.takamaka.code.lang.Storage.fromContract()}
	 */
	public final void fromContract(Object callee, Object caller) throws Throwable {
		// we call the private method of contract
		try {
			fromContract.invoke(callee, caller);
		}
		catch (IllegalArgumentException e) {
			throw e;
		}
		catch (IllegalAccessException e) {
			throw new IllegalArgumentException("cannot call Storage.fromContract()", e);
		}
		catch (InvocationTargetException e) {
			// an exception inside Storage.fromContract() itself: we forward it
			throw e.getCause();
		}
	}

	/**
	 * Called at the beginning of the instrumentation of a payable {@code @@FromContract} method or constructor.
	 * It forwards the call to {@code io.takamaka.code.lang.Contract.payableFromContract()}.
	 * 
	 * @param callee the contract whose method or constructor is called
	 * @param payer the payer of the call
	 * @param amount the amount of coins
	 * @throws any possible exception thrown inside {@code io.takamaka.code.lang.Contract.payableFromContract()}
	 */
	public final void payableFromContract(Object callee, Object payer, BigInteger amount) throws Throwable {
		// we call the private method of contract
		try {
			payableFromContractBigInteger.invoke(callee, payer, amount);
		}
		catch (IllegalArgumentException e) {
			throw e;
		}
		catch (IllegalAccessException e) {
			throw new IllegalArgumentException("cannot call Contract.payableFromContract()", e);
		}
		catch (InvocationTargetException e) {
			// an exception inside Contract.payableFromContract() itself: we forward it
			throw e.getCause();
		}
	}

	/**
	 * Called at the beginning of the instrumentation of a red payable {@code @@FromContract} method or constructor.
	 * It forwards the call to {@code io.takamaka.code.lang.RedGreenContract.redPayable()}.
	 * 
	 * @param callee the contract whose method or constructor is called
	 * @param caller the caller of the method or constructor
	 * @param amount the amount of coins
	 * @throws any possible exception thrown inside {@code io.takamaka.code.lang.RedGreenContract.redPayable()}
	 */
	public final void redPayableFromContract(Object callee, Object caller, BigInteger amount) throws Throwable {
		try {
			redPayableBigInteger.invoke(callee, caller, amount);
		}
		catch (IllegalArgumentException e) {
			throw e;
		}
		catch (IllegalAccessException e) {
			throw new IllegalArgumentException("cannot call RedGreenContract.redPayableFromContract()", e);
		}
		catch (InvocationTargetException e) {
			// an exception inside RedGreenContract.redPayableFromContract() itself: we forward it
			throw e.getCause();
		}
	}

	/**
	 * Called at the beginning of the instrumentation of a payable {@code @@FromContract} method or constructor.
	 * It forwards the call to {@code io.takamaka.code.lang.Contract.payableFromContract()}.
	 * 
	 * @param callee the contract whose method or constructor is called
	 * @param caller the caller of the method or constructor
	 * @param amount the amount of coins
	 * @throws any possible exception thrown inside {@code io.takamaka.code.lang.Contract.payableFromContract()}
	 */
	public final void payableFromContract(Object callee, Object caller, int amount) throws Throwable {
		try {
			payableFromContractInt.invoke(callee, caller, amount);
		}
		catch (IllegalArgumentException e) {
			throw e;
		}
		catch (IllegalAccessException e) {
			throw new IllegalArgumentException("cannot call Contract.payableFromContract()", e);
		}
		catch (InvocationTargetException e) {
			// an exception inside Contract.payableFromContract() itself: we forward it
			throw e.getCause();
		}
	}

	/**
	 * Called at the beginning of the instrumentation of a red payable {@code @@FromContract} method or constructor.
	 * It forwards the call to {@code io.takamaka.code.lang.RedGreenContract.redPayable()}.
	 * 
	 * @param callee the contract whose entry is called
	 * @param caller the caller of the entry
	 * @param amount the amount of coins
	 * @throws any possible exception thrown inside {@code io.takamaka.code.lang.RedGreenContract.redPayable()}
	 */
	public final void redPayableFromContract(Object callee, Object caller, int amount) throws Throwable {
		try {
			redPayableInt.invoke(callee, caller, amount);
		}
		catch (IllegalArgumentException e) {
			throw e;
		}
		catch (IllegalAccessException e) {
			throw new IllegalArgumentException("cannot call RedGreenContract.redPayableEntry()", e);
		}
		catch (InvocationTargetException e) {
			// an exception inside RedGreenContract.redPayableEntry(): we forward it
			throw e.getCause();
		}
	}

	/**
	 * Called at the beginning of the instrumentation of a payable {@code @@FromContract} method or constructor.
	 * It forwards the call to {@code io.takamaka.code.lang.Contract.payableFromContract()}.
	 * 
	 * @param callee the contract whose method or constructor is called
	 * @param caller the caller of the method or constructor
	 * @param amount the amount of coins
	 * @throws any possible exception thrown inside {@code io.takamaka.code.lang.Contract.payableFromContract()}
	 */
	public final void payableFromContract(Object callee, Object caller, long amount) throws Throwable {
		try {
			payableFromContractLong.invoke(callee, caller, amount);
		}
		catch (IllegalArgumentException e) {
			throw e;
		}
		catch (IllegalAccessException e) {
			throw new IllegalArgumentException("cannot call Contract.payableFromContract()", e);
		}
		catch (InvocationTargetException e) {
			// an exception inside Contract.payableFromContract() itself: we forward it
			throw e.getCause();
		}
	}

	/**
	 * Called at the beginning of the instrumentation of a red payable {@code @@FromContract} method or constructor.
	 * It forwards the call to {@code io.takamaka.code.lang.RedGreenContract.redPayable()}.
	 * 
	 * @param callee the contract whose method or constructor is called
	 * @param caller the caller of the method or constructor
	 * @param amount the amount of coins
	 * @throws any possible exception thrown inside {@code io.takamaka.code.lang.RedGreenContract.redPayable()}
	 */
	public final void redPayableFromContract(Object callee, Object caller, long amount) throws Throwable {
		try {
			redPayableLong.invoke(callee, caller, amount);
		}
		catch (IllegalArgumentException e) {
			throw e;
		}
		catch (IllegalAccessException e) {
			throw new IllegalArgumentException("cannot call RedGreenContract.redPayable()", e);
		}
		catch (InvocationTargetException e) {
			// an exception inside RedGreenContract.redPayable() itself: we forward it
			throw e.getCause();
		}
	}

	/**
	 * Replaces all reverified responses into the store of the node for which
	 * the class loader has been built.
	 */
	public void replaceReverifiedResponses() {
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