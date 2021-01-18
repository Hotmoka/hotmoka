package io.takamaka.code.engine;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.responses.TransactionResponseWithInstrumentedJar;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.ConsensusParams;
import io.takamaka.code.engine.internal.Reverification;
import io.takamaka.code.instrumentation.InstrumentationConstants;
import io.takamaka.code.verification.TakamakaClassLoader;
import io.takamaka.code.whitelisting.WhiteListingWizard;

/**
 * A class loader used to access the definition of the classes
 * of Takamaka methods or constructors executed during a transaction.
 */
public class EngineClassLoader implements TakamakaClassLoader {

	/**
	 * The maximal number of dependencies in the classpath used to create an engine class loader.
	 */
	public final static int MAX_DEPENDENCIES = 20;

	/**
	 * The maximal cumulative size (in bytes) of the instrumented jars of the dependencies
	 * in the classpath used to create an engine class loader.
	 */
	public final static int MAX_SIZE_OF_DEPENDENCIES = 1_000_000;

	/**
	 * The parent of this class loader;
	 */
	private final TakamakaClassLoader parent;

	/**
	 * Method {@link io.takamaka.code.lang.Storage#entry(io.takamaka.code.lang.Contract)}.
	 */
	private final Method entry;

	/**
	 * Method {@link io.takamaka.code.lang.Contract#payableEntry(io.takamaka.code.lang.Contract, int)}.
	 */
	private final Method payableEntryInt;

	/**
	 * Method {@link io.takamaka.code.lang.Contract#payableEntry(io.takamaka.code.lang.Contract, long)}.
	 */
	private final Method payableEntryLong;

	/**
	 * Method {@link io.takamaka.code.lang.Contract#payableEntry(io.takamaka.code.lang.Contract, BigInteger)}.
	 */
	private final Method payableEntryBigInteger;

	/**
	 * Method {@link io.takamaka.code.lang.RedGreenContract#redPayable(io.takamaka.code.lang.RedGreenContract, int)}.
	 */
	private final Method redPayableInt;

	/**
	 * Method {@link io.takamaka.code.lang.RedGreenContract#redPayable(io.takamaka.code.lang.RedGreenContract, long)}.
	 */
	private final Method redPayableLong;

	/**
	 * Method {@link io.takamaka.code.lang.RedGreenContract#redPayable(io.takamaka.code.lang.RedGreenContract, BigInteger)}.
	 */
	private final Method redPayableBigInteger;

	/**
	 * Field {@link io.takamaka.code.lang.ExternallyOwnedAccount#nonce}.
	 */
	private final Field externallyOwnedAccountNonce;

	/**
	 * Field {@link io.takamaka.code.lang.RedGreenExternallyOwnedAccount#nonce}.
	 */
	private final Field redGreenExternallyOwnedAccountNonce;

	/**
	 * Field {@link io.takamaka.code.lang.ExternallyOwnedAccount#publicKey}.
	 */
	private final Field externallyOwnedAccountPublicKey;

	/**
	 * Field {@link io.takamaka.code.lang.RedGreenExternallyOwnedAccount#publicKey}.
	 */
	private final Field redGreenExternallyOwnedAccountPublicKey;

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
	 * Field {@link io.takamaka.code.lang.RedGreenContract#redBalance}.
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
	 * @throws Exception if an error occurs
	 */
	public EngineClassLoader(byte[] jar, Stream<TransactionReference> dependencies, AbstractLocalNode<?,?> node, boolean reverify, ConsensusParams consensus) throws Exception {
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
		this.parent = mkTakamakaClassLoader(dependenciesAsList.stream(), jar, node, jars, transactionsOfJars);

		this.lengthsOfJars = jars.stream().mapToInt(bytes -> bytes.length).toArray();
		this.transactionsOfJars = transactionsOfJars.toArray(TransactionReference[]::new);

		Class<?> contract = getContract(), redGreenContract = getRedGreenContract(), storage = getStorage();
		this.entry = storage.getDeclaredMethod("entry", contract);
		this.entry.setAccessible(true); // it was private
		this.payableEntryInt = contract.getDeclaredMethod("payableEntry", contract, int.class);
		this.payableEntryInt.setAccessible(true); // it was private
		this.payableEntryLong = contract.getDeclaredMethod("payableEntry", contract, long.class);
		this.payableEntryLong.setAccessible(true); // it was private
		this.payableEntryBigInteger = contract.getDeclaredMethod("payableEntry", contract, BigInteger.class);
		this.payableEntryBigInteger.setAccessible(true); // it was private
		this.redPayableInt = redGreenContract.getDeclaredMethod("redPayable", redGreenContract, int.class);
		this.redPayableInt.setAccessible(true); // it was private
		this.redPayableLong = redGreenContract.getDeclaredMethod("redPayable", redGreenContract, long.class);
		this.redPayableLong.setAccessible(true); // it was private
		this.redPayableBigInteger = redGreenContract.getDeclaredMethod("redPayable", redGreenContract, BigInteger.class);
		this.redPayableBigInteger.setAccessible(true); // it was private
		this.redBalanceField = redGreenContract.getDeclaredField("balanceRed");
		this.redBalanceField.setAccessible(true); // it was private
		this.externallyOwnedAccountNonce = getExternallyOwnedAccount().getDeclaredField("nonce");
		this.externallyOwnedAccountNonce.setAccessible(true); // it was private
		this.redGreenExternallyOwnedAccountNonce = getRedGreenExternallyOwnedAccount().getDeclaredField("nonce");
		this.redGreenExternallyOwnedAccountNonce.setAccessible(true); // it was private
		this.externallyOwnedAccountPublicKey = getExternallyOwnedAccount().getDeclaredField("publicKey");
		this.externallyOwnedAccountPublicKey.setAccessible(true); // it was private
		this.redGreenExternallyOwnedAccountPublicKey = getRedGreenExternallyOwnedAccount().getDeclaredField("publicKey");
		this.redGreenExternallyOwnedAccountPublicKey.setAccessible(true); // it was private
		this.storageReference = storage.getDeclaredField(InstrumentationConstants.STORAGE_REFERENCE_FIELD_NAME);
		this.storageReference.setAccessible(true); // it was private
		this.inStorage = storage.getDeclaredField(InstrumentationConstants.IN_STORAGE);
		this.inStorage.setAccessible(true); // it was private
		this.balanceField = contract.getDeclaredField("balance");
		this.balanceField.setAccessible(true); // it was private
	}

	/**
	 * Yields the Takamaka class loader for the components of the given classpaths.
	 * 
	 * @param classpaths the classpaths
	 * @param start an initial jar. This can be {@code null}
	 * @param node the node for which the class loader is created
	 * @return the class loader
	 */
	private TakamakaClassLoader mkTakamakaClassLoader(Stream<TransactionReference> classpaths, byte[] start, AbstractLocalNode<?,?> node, List<byte[]> jars, ArrayList<TransactionReference> transactionsOfJars) {
		if (start != null) {
			jars.add(start);
			transactionsOfJars.add(null);
		}

		classpaths.forEachOrdered(classpath -> addJars(classpath, jars, transactionsOfJars, node));

		return TakamakaClassLoader.of(jars.stream(), (name, pos) -> takeNoteOfTransactionThatInstalledJarFor(name, transactionsOfJars.get(pos)));
	}

	private void takeNoteOfTransactionThatInstalledJarFor(String className, TransactionReference transactionReference) {
		// if the transaction reference is null, it means that the class comes from a jar that is being installed
		// by the transaction that created this class loader. In that case, the storage reference of the class is not used
		if (transactionReference != null)
			transactionsThatInstalledJarForClasses.put(className, transactionReference);
	}

	/**
	 * Expands the given list of jars with the components of the given classpath.
	 * 
	 * @param classpath the classpath
	 * @param jars the list where the jars will be added
	 * @param jarTransactions the list of transactions where the {@code jars} have been installed
	 * @param node the node for which the class loader is created
	 */
	private void addJars(TransactionReference classpath, List<byte[]> jars, List<TransactionReference> jarTransactions, AbstractLocalNode<?,?> node) {
		if (jars.size() > MAX_DEPENDENCIES)
			throw new IllegalArgumentException("too many dependencies in classpath: max is " + MAX_DEPENDENCIES);

		if (jars.stream().mapToLong(bytes -> bytes.length).sum() > MAX_SIZE_OF_DEPENDENCIES)
			throw new IllegalArgumentException("too large cumulative size of dependencies in classpath: max is " + MAX_SIZE_OF_DEPENDENCIES + " bytes");

		TransactionResponseWithInstrumentedJar responseWithInstrumentedJar = getResponseWithInstrumentedJarAtUncommitted(classpath, node);

		// we consider its dependencies as well, recursively
		responseWithInstrumentedJar.getDependencies().forEachOrdered(dependency -> addJars(dependency, jars, jarTransactions, node));

		jars.add(responseWithInstrumentedJar.getInstrumentedJar());
		jarTransactions.add(classpath);
	}
	/*
	 * Yields the response generated by the transaction with the given reference, even
	 * before the transaction gets committed. The transaction must be a transaction that installed
	 * a jar in the store of the node.
	 * 
	 * @param reference the reference of the transaction
	 * @param node the node for which the class loader is created
	 * @return the response
	 * @throws NoSuchElementException if the transaction does not exist in the store, or
	 *                                did not generate a response with instrumented jar
	 */
	private TransactionResponseWithInstrumentedJar getResponseWithInstrumentedJarAtUncommitted(TransactionReference reference, AbstractLocalNode<?,?> node) throws NoSuchElementException {
		// first we check if the response has been reverified and we use the reverified version
		TransactionResponse response = reverification.getReverifiedResponse(reference)
			// otherwise the response has not been reverified
			.or(() -> node.getStore().getResponseUncommitted(reference))
			.orElseThrow(() -> new InternalFailureException("unknown transaction reference " + reference));
		
		if (!(response instanceof TransactionResponseWithInstrumentedJar))
			throw new NoSuchElementException("the transaction " + reference + " did not install a jar in store");
	
		return (TransactionResponseWithInstrumentedJar) response;
	}
	 
	/**
	 * Yields the lengths (in bytes) of the instrumented jars of the classpath and its dependencies
	 * used to create this class loader.
	 * 
	 * @return the lengths
	 */
	public final IntStream getLengthsOfJars() {
		return IntStream.of(lengthsOfJars);
	}

	/**
	 * Yields the transactions that installed the jars of the classpath and its dependencies
	 * used to create this class loader.
	 * 
	 * @return the transactions
	 */
	public final Stream<TransactionReference> getTransactionsOfJars() {
		return Stream.of(transactionsOfJars);
	}

	/**
	 * Yields the transaction reference that installed the jar where the given class is defined.
	 * The class must belong to the cla)ss path used at creation time of this engine class loader
	 * (hence not to the extra jar provided in the second constructor).
	 * 
	 * @param clazz the class
	 * @return the transaction reference
	 */
	public final TransactionReference transactionThatInstalledJarFor(Class<?> clazz) {
		return transactionsThatInstalledJarForClasses.get(clazz.getName().replace('.', '/') + ".class");
	}

	/**
	 * Yields the value of the {@code storageReference} field of the given storage object in RAM.
	 * 
	 * @param object the object
	 * @return the value of the field
	 */
	public final StorageReference getStorageReferenceOf(Object object) {
		try {
			return (StorageReference) storageReference.get(object);
		}
		catch (IllegalArgumentException | IllegalAccessException e) {
			throw new IllegalStateException("cannot read the storage reference of a storage object of class " + object.getClass().getName());
		}
	}

	/**
	 * Yields the value of the boolean {@code inStorage} field of the given storage object in RAM.
	 * 
	 * @param object the object
	 * @return the value of the field
	 */
	public final boolean getInStorageOf(Object object) {
		try {
			return (boolean) inStorage.get(object);
		}
		catch (IllegalArgumentException | IllegalAccessException e) {
			throw new IllegalStateException("cannot read the inStorage tag of a storage object of class " + object.getClass().getName());
		}
	}

	/**
	 * Yields the value of the {@code nonce} field of the given account in RAM.
	 * 
	 * @param object the account
	 * @return the value of the field
	 */
	public final BigInteger getNonceOf(Object object) {
		Class<? extends Object> clazz = object.getClass();

		try {
			if (getExternallyOwnedAccount().isAssignableFrom(clazz))
				return (BigInteger) externallyOwnedAccountNonce.get(object);
			else if (getRedGreenExternallyOwnedAccount().isAssignableFrom(clazz))
				return (BigInteger) redGreenExternallyOwnedAccountNonce.get(object);
			else
				throw new IllegalArgumentException("unknown account class " + clazz);
		}
		catch (IllegalArgumentException | IllegalAccessException e) {
			throw new IllegalStateException("cannot read the nonce of an account of class " + clazz.getName());
		}
	}

	/**
	 * Yields the value of the {@code publicKey} field of the given account in RAM.
	 * 
	 * @param object the account
	 * @return the value of the field
	 */
	public final String getPublicKeyOf(Object object) {
		Class<? extends Object> clazz = object.getClass();

		try {
			if (getExternallyOwnedAccount().isAssignableFrom(clazz))
				return (String) externallyOwnedAccountPublicKey.get(object);
			else if (getRedGreenExternallyOwnedAccount().isAssignableFrom(clazz))
				return (String) redGreenExternallyOwnedAccountPublicKey.get(object);
			else
				throw new IllegalArgumentException("unknown account class " + clazz);
		}
		catch (IllegalArgumentException | IllegalAccessException e) {
			throw new IllegalStateException("cannot read the public key of an account of class " + clazz.getName());
		}
	}

	/**
	 * Yields the value of the {@code balance} field of the given contract in RAM.
	 * 
	 * @param object the contract
	 * @return the value of the field
	 */
	public final BigInteger getBalanceOf(Object object) {
		try {
			return (BigInteger) balanceField.get(object);
		}
		catch (IllegalArgumentException | IllegalAccessException e) {
			throw new IllegalStateException("cannot read the balance field of a contract object of class " + object.getClass().getName());
		}
	}

	/**
	 * Yields the value of the {@code balanceRed} field of the given red/green contract in RAM.
	 * 
	 * @param object the contract
	 * @return the value of the field
	 */
	public final BigInteger getRedBalanceOf(Object object) {
		try {
			return (BigInteger) redBalanceField.get(object);
		}
		catch (IllegalArgumentException | IllegalAccessException e) {
			throw new IllegalStateException("cannot read the red balance field of a contract object of class " + object.getClass().getName());
		}
	}

	/**
	 * Sets the value of the {@code balance} field of the given contract in RAM.
	 * 
	 * @param object the contract
	 * @param value to value to set for the balance of the contract
	 */
	public final void setBalanceOf(Object object, BigInteger value) {
		try {
			balanceField.set(object, value);
		}
		catch (IllegalArgumentException | IllegalAccessException e) {
			throw new IllegalStateException("cannot write the balance field of a contract object of class " + object.getClass().getName());
		}
	}

	/**
	 * Sets the value of the {@code nonce} field of the given account in RAM.
	 * 
	 * @param object the account
	 * @param value to value to set for the nonce of the account
	 */
	public final void setNonceOf(Object object, BigInteger value) {
		Class<?> clazz = object.getClass();

		try {
			if (getExternallyOwnedAccount().isAssignableFrom(clazz))
				externallyOwnedAccountNonce.set(object, value);
			else if (getRedGreenExternallyOwnedAccount().isAssignableFrom(clazz))
				redGreenExternallyOwnedAccountNonce.set(object, value);
			else
				throw new IllegalArgumentException("unknown account class " + clazz);
		}
		catch (IllegalArgumentException | IllegalAccessException e) {
			throw new IllegalStateException("cannot write the nonce field of an account object of class " + clazz.getName());
		}
	}

	/**
	 * Sets the val)ue of the {@code balanceRed} field of the given red/green contract in RAM.
	 * 
	 * @param object the contract
	 * @param value to value to set for the red balance of the contract
	 */
	public final void setRedBalanceOf(Object object, BigInteger value) {
		try {
			redBalanceField.set(object, value);
		}
		catch (IllegalArgumentException | IllegalAccessException e) {
			throw new IllegalStateException("cannot write the red balance field of a contract object of class " + object.getClass().getName());
		}
	}

	/**
	 * Called at the beginning of the instrumentation of an entry method or constructor
	 * of a contract. It forwards the call to {@code io.takamaka.code.lang.Storage.entry()}.
	 * 
	 * @param callee the contract whose entry is called
	 * @param caller the caller of the entry
	 * @throws any possible exception thrown inside {@code io.takamaka.code.lang.Storage.entry()}
	 */
	public final void entry(Object callee, Object caller) throws Throwable {
		// we call the private method of contract
		try {
			entry.invoke(callee, caller);
		}
		catch (IllegalAccessException | IllegalArgumentException e) {
			throw new IllegalStateException("cannot call Storage.entry()", e);
		}
		catch (InvocationTargetException e) {
			// an exception inside Contract.entry() itself: we forward it
			throw e.getCause();
		}
	}

	/**
	 * Called at the beginning of the instrumentation of a payable entry method or constructor.
	 * It forwards the call to {@code io.takamaka.code.lang.Contract.payableEntry()}.
	 * 
	 * @param callee the contract whose entry is called
	 * @param caller the caller of the entry
	 * @param amount the amount of coins
	 * @throws any possible exception thrown inside {@code io.takamaka.code.lang.Contract.payableEntry()}
	 */
	public final void payableEntry(Object callee, Object caller, BigInteger amount) throws Throwable {
		// we call the private method of contract
		try {
			payableEntryBigInteger.invoke(callee, caller, amount);
		}
		catch (IllegalAccessException | IllegalArgumentException e) {
			throw new IllegalStateException("cannot call Contract.payableEntry()", e);
		}
		catch (InvocationTargetException e) {
			// an exception inside Contract.payableEntry() itself: we forward it
			throw e.getCause();
		}
	}

	/**
	 * Called at the beginning of the instrumentation of a red payable entry method or constructor.
	 * It forwards the call to {@code io.takamaka.code.lang.RedGreenContract.redPayable()}.
	 * 
	 * @param callee the contract whose entry is called
	 * @param caller the caller of the entry
	 * @param amount the amount of coins
	 * @throws any possible exception thrown inside {@code io.takamaka.code.lang.RedGreenContract.redPayable()}
	 */
	public final void redPayableEntry(Object callee, Object caller, BigInteger amount) throws Throwable {
		try {
			redPayableBigInteger.invoke(callee, caller, amount);
		}
		catch (IllegalAccessException | IllegalArgumentException e) {
			throw new IllegalStateException("cannot call RedGreenContract.redPayableEntry()", e);
		}
		catch (InvocationTargetException e) {
			// an exception inside RedGreenContract.redPayableEntry() itself: we forward it
			throw e.getCause();
		}
	}

	/**
	 * Called at the beginning of the instrumentation of a payable entry method or constructor.
	 * It forwards the call to {@code io.takamaka.code.lang.Contract.payableEntry()}.
	 * 
	 * @param callee the contract whose entry is called
	 * @param caller the caller of the entry
	 * @param amount the amount of coins
	 * @throws any possible exception thrown inside {@code io.takamaka.code.lang.Contract.payableEntry()}
	 */
	public final void payableEntry(Object callee, Object caller, int amount) throws Throwable {
		// we call the private method of contract
		try {
			payableEntryInt.invoke(callee, caller, amount);
		}
		catch (IllegalAccessException | IllegalArgumentException e) {
			throw new IllegalStateException("cannot call Contract.payableEntry()", e);
		}
		catch (InvocationTargetException e) {
			// an exception inside Contract.payableEntry() itself: we forward it
			throw e.getCause();
		}
	}

	/**
	 * Called at the beginning of the instrumentation of a red payable entry method or constructor.
	 * It forwards the call to {@code io.takamaka.code.lang.RedGreenContract.redPayable()}.
	 * 
	 * @param callee the contract whose entry is called
	 * @param caller the caller of the entry
	 * @param amount the amount of coins
	 * @throws any possible exception thrown inside {@code io.takamaka.code.lang.RedGreenContract.redPayable()}
	 */
	public final void redPayableEntry(Object callee, Object caller, int amount) throws Throwable {
		try {
			redPayableInt.invoke(callee, caller, amount);
		}
		catch (IllegalAccessException | IllegalArgumentException e) {
			throw new IllegalStateException("cannot call RedGreenContract.redPayableEntry()", e);
		}
		catch (InvocationTargetException e) {
			// an exception inside RedGreenContract.redPayableEntry(): we forward it
			throw e.getCause();
		}
	}

	/**
	 * Called at the beginning of the instrumentation of a payable entry method or constructor.
	 * It forwards the call to {@code io.takamaka.code.lang.Contract.payableEntry()}.
	 * 
	 * @param callee the contract whose entry is called
	 * @param caller the caller of the entry
	 * @param amount the amount of coins
	 * @throws any possible exception thrown inside {@code io.takamaka.code.lang.Contract.payableEntry()}
	 */
	public final void payableEntry(Object callee, Object caller, long amount) throws Throwable {
		// we call the private method of contract
		try {
			payableEntryLong.invoke(callee, caller, amount);
		}
		catch (IllegalAccessException | IllegalArgumentException e) {
			throw new IllegalStateException("cannot call Contract.payableEntry()", e);
		}
		catch (InvocationTargetException e) {
			// an exception inside Contract.payableEntry() itself: we forward it
			throw e.getCause();
		}
	}

	/**
	 * Called at the beginning of the instrumentation of a red payable entry method or constructor.
	 * It forwards the call to {@code io.takamaka.code.lang.RedGreenContract.redPayable()}.
	 * 
	 * @param callee the contract whose entry is called
	 * @param caller the caller of the entry
	 * @param amount the amount of coins
	 * @throws any possible exception thrown inside {@code io.takamaka.code.lang.RedGreenContract.redPayable()}
	 */
	public final void redPayableEntry(Object callee, Object caller, long amount) throws Throwable {
		try {
			redPayableLong.invoke(callee, caller, amount);
		}
		catch (IllegalAccessException | IllegalArgumentException e) {
			throw new IllegalStateException("cannot call RedGreenContract.redPayableEntry()", e);
		}
		catch (InvocationTargetException e) {
			// an exception inside RedGreenContract.redPayableEntry() itself: we forward it
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
	public final Class<?> loadClass(String className) throws ClassNotFoundException {
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
	public boolean isStorage(String className) {
		return parent.isStorage(className);
	}

	@Override
	public boolean isContract(String className) {
		return parent.isContract(className);
	}

	@Override
	public boolean isRedGreenContract(String className) {
		return parent.isRedGreenContract(className);
	}

	@Override
	public boolean isConsensusUpdateEvent(String className) {
		return parent.isConsensusUpdateEvent(className);
	}

	@Override
	public boolean isGasPriceUpdateEvent(String className) {
		return parent.isGasPriceUpdateEvent(className);
	}

	@Override
	public boolean isInterface(String className) {
		return parent.isInterface(className);
	}

	@Override
	public boolean isExported(String className) {
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
	public Class<?> getRedGreenContract() {
		return parent.getRedGreenContract();
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
	public Class<?> getRedGreenExternallyOwnedAccount() {
		return parent.getRedGreenExternallyOwnedAccount();
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
}