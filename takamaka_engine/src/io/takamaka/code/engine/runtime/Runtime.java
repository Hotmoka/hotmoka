package io.takamaka.code.engine.runtime;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.StorageReference;
import io.takamaka.code.engine.AbstractBlockchain;
import io.takamaka.code.engine.NonWhiteListedCallException;
import io.takamaka.code.engine.OutOfGasError;

/**
 * A class that contains utility methods called by instrumented
 * Takamaka code stored in blockchain. This class is not installed in
 * blockchain, hence it is not visible to Takamaka programmers
 * and needn't obey to Takamaka code constraints.
 */
public abstract class Runtime {

	/**
	 * The blockchain used for the current transaction.
	 */
	private static AbstractBlockchain blockchain;

	/**
	 * The counter for the next storage object created during the current transaction.
	 */
	private static BigInteger nextProgressive;

	/**
	 * Resets static data at the beginning of a transaction.
	 * 
	 * @param blockchain the blockchain used for the new transaction
	 */
	public static void init(AbstractBlockchain blockchain) {
		Runtime.blockchain = blockchain;
		Runtime.nextProgressive = BigInteger.ZERO;
	}

	/**
	 * Yields the last value assigned to the given lazy, non-{@code final} field of this storage object.
	 * 
	 * @param object the container of the field
	 * @param definingClass the class of the field. This can only be the class of this storage object
	 *                      of one of its superclasses
	 * @param name the name of the field
	 * @param fieldClassName the name of the type of the field
	 * @return the value of the field
	 * @throws Exception if the value could not be found
	 */
	public static Object deserializeLastLazyUpdateFor(Object object, String definingClass, String name, String fieldClassName) throws Exception {
		return blockchain.deserializeLastLazyUpdateFor(blockchain.getStorageReferenceOf(object), FieldSignature.mk(definingClass, name, ClassType.mk(fieldClassName)));
	}

	/**
	 * Yields the last value assigned to the given lazy, {@code final} field of this storage object.
	 * 
	 * @param object the container of the field
	 * @param definingClass the class of the field. This can only be the class of this storage object
	 *                      of one of its superclasses
	 * @param name the name of the field
	 * @param fieldClassName the name of the type of the field
	 * @return the value of the field
	 * @throws Exception if the value could not be found
	 */
	public static Object deserializeLastLazyUpdateForFinal(Object object, String definingClass, String name, String fieldClassName) throws Exception {
		return blockchain.deserializeLastLazyUpdateForFinal(blockchain.getStorageReferenceOf(object), FieldSignature.mk(definingClass, name, ClassType.mk(fieldClassName)));
	}

	/**
	 * Called at the beginning of the instrumentation of an entry method or constructor
	 * of a contract. It forwards the call to {@code io.takamaka.code.lang.Contract.entry()}.
	 * 
	 * @param callee the contract whose entry is called
	 * @param caller the caller of the entry
	 * @throws any possible exception thrown inside {@code io.takamaka.code.lang.Contract.entry()}
	 */
	public static void entry(Object callee, Object caller) throws Throwable {
		// we call the private method of contract
		try {
			blockchain.getEntry().invoke(callee, caller);
		}
		catch (IllegalAccessException | IllegalArgumentException e) {
			throw new IllegalStateException("cannot call Contract.entry()", e);
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
	public static void payableEntry(Object callee, Object caller, BigInteger amount) throws Throwable {
		// we call the private method of contract
		try {
			blockchain.getPayableEntryBigInteger().invoke(callee, caller, amount);
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
	 * It forwards the call to {@code io.takamaka.code.lang.Contract.entry()} and then to
	 * {@code io.takamaka.code.lang.RedGreenContract.redPayable()}.
	 * 
	 * @param callee the contract whose entry is called
	 * @param caller the caller of the entry
	 * @param amount the amount of coins
	 * @throws any possible exception thrown inside or {@code io.takamaka.code.lang.Contract.entry()}
	 *         or {@code io.takamaka.code.lang.RedGreenContract.redPayable()}
	 */
	public static void redPayableEntry(Object callee, Object caller, BigInteger amount) throws Throwable {
		// we call the private methods of contract
		try {
			blockchain.getEntry().invoke(callee, caller);
		}
		catch (IllegalAccessException | IllegalArgumentException e) {
			throw new IllegalStateException("cannot call Contract.entry()", e);
		}
		catch (InvocationTargetException e) {
			// an exception inside Contract.entry() itself: we forward it
			throw e.getCause();
		}

		try {
			blockchain.getRedPayableBigInteger().invoke(callee, caller, amount);
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
	 * @throws any possible exception thrown inside {@code io.takamaka.code.lang.Contract.entry()}
	 */
	public static void payableEntry(Object callee, Object caller, int amount) throws Throwable {
		// we call the private method of contract
		try {
			blockchain.getPayableEntryInt().invoke(callee, caller, amount);
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
	 * It forwards the call to {@code io.takamaka.code.lang.Contract.entry()} and then to
	 * {@code io.takamaka.code.lang.RedGreenContract.redPayable()}.
	 * 
	 * @param callee the contract whose entry is called
	 * @param caller the caller of the entry
	 * @param amount the amount of coins
	 * @throws any possible exception thrown inside or {@code io.takamaka.code.lang.Contract.entry()}
	 *         or {@code io.takamaka.code.lang.RedGreenContract.redPayable()}
	 */
	public static void redPayableEntry(Object callee, Object caller, int amount) throws Throwable {
		// we call the private methods of contract
		try {
			blockchain.getEntry().invoke(callee, caller);
		}
		catch (IllegalAccessException | IllegalArgumentException e) {
			throw new IllegalStateException("cannot call Contract.entry()", e);
		}
		catch (InvocationTargetException e) {
			// an exception inside Contract.entry() itself: we forward it
			throw e.getCause();
		}

		try {
			blockchain.getRedPayableInt().invoke(callee, caller, amount);
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
	 * @throws any possible exception thrown inside {@code io.takamaka.code.lang.Contract.entry()}
	 */
	public static void payableEntry(Object callee, Object caller, long amount) throws Throwable {
		// we call the private method of contract
		try {
			blockchain.getPayableEntryLong().invoke(callee, caller, amount);
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
	 * It forwards the call to {@code io.takamaka.code.lang.Contract.entry()} and then to
	 * {@code io.takamaka.code.lang.RedGreenContract.redPayable()}.
	 * 
	 * @param callee the contract whose entry is called
	 * @param caller the caller of the entry
	 * @param amount the amount of coins
	 * @throws any possible exception thrown inside or {@code io.takamaka.code.lang.Contract.entry()}
	 *         or {@code io.takamaka.code.lang.RedGreenContract.redPayable()}
	 */
	public static void redPayableEntry(Object callee, Object caller, long amount) throws Throwable {
		// we call the private methods of contract
		try {
			blockchain.getEntry().invoke(callee, caller);
		}
		catch (IllegalAccessException | IllegalArgumentException e) {
			throw new IllegalStateException("cannot call Contract.entry()", e);
		}
		catch (InvocationTargetException e) {
			// an exception inside Contract.entry() itself: we forward it
			throw e.getCause();
		}
	
		try {
			blockchain.getRedPayableLong().invoke(callee, caller, amount);
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
	 * Takes note of the given event.
	 *
	 * @param event the event
	 */
	public static void event(Object event) {
		blockchain.event(event);
	}

	/**
	 * Runs a given piece of code with a subset of the available gas.
	 * It first charges the given amount of gas. Then runs the code
	 * with the charged gas only. At its end, the remaining gas is added
	 * to the available gas to continue the computation.
	 * 
	 * @param amount the amount of gas provided to the code
	 * @param what the code to run
	 * @return the result of the execution of the code
	 * @throws OutOfGasError if there is not enough gas
	 * @throws Exception if the code runs into this exception
	 */
	public static <T> T withGas(BigInteger amount, Callable<T> what) throws Exception {
		return blockchain.withGas(amount, what);
	}

	/**
	 * Yields the current time of the transaction.
	 * 
	 * @return the time
	 */
	public static long now() {
		return blockchain.getNow();
	}

	/**
	 * Yields the value of field {@code inStorage} of the given storage object}.
	 * 
	 * @param object the storage object
	 * @return the value of the field
	 */
	public static boolean inStorageOf(Object object) {
		return blockchain.getInStorageOf(object);
	}

	public static int compareStorageReferencesOf(Object o1, Object o2) {
		if (o1 == o2)
			return 0;
		else if (o1 == null)
			return -1;
		else if (o2 == null)
			return 1;
		else
			return blockchain.getStorageReferenceOf(o1).compareTo(blockchain.getStorageReferenceOf(o2));
	}

	/**
	 * Called during verification and by instrumented code to check
	 * the {@link io.takamaka.code.whitelisting.MustBeFalse} annotation. 
	 * Its name must be the uncapitalized simple name of the annotation.
	 * 
	 * @param value the value
	 * @param methodName the name of the method
	 */
	public static void mustBeFalse(boolean value, String methodName) {
		if (value)
			throw new NonWhiteListedCallException("the actual parameter of " + methodName + " must be false");
	}

	/**
	 * Called during verification and by instrumented code to check
	 * the {@link io.takamaka.code.whitelisting.MustRedefineHashCode} annotation. 
	 * Its name must be the uncapitalized simple name of the annotation.
	 * 
	 * @param value the value
	 * @param methodName the name of the method
	 */
	public static void mustRedefineHashCode(Object value, String methodName) {
		if (value != null)
			if (Stream.of(value.getClass().getMethods())
				.filter(method -> !Modifier.isAbstract(method.getModifiers()) && Modifier.isPublic(method.getModifiers()) && method.getDeclaringClass() != Object.class)
				.map(Method::getName)
				.noneMatch("hashCode"::equals))
				throw new NonWhiteListedCallException("the actual parameter of " + methodName + " must redefine Object.hashCode()");
	}

	/**
	 * Called during verification and by instrumented code to check
	 * the {@link io.takamaka.code.whitelisting.MustRedefineHashCodeOrToString} annotation. 
	 * Its name must be the uncapitalized simple name of the annotation.
	 * 
	 * @param value the value
	 * @param methodName the name of the method
	 */
	public static void mustRedefineHashCodeOrToString(Object value, String methodName) {
		if (value != null && !redefinesHashCodeOrToString(value.getClass()))
			throw new NonWhiteListedCallException("the actual parameter of " + methodName + " must redefine Object.hashCode() or Object.toString()");
	}

	private static boolean redefinesHashCodeOrToString(Class<?> clazz) {
		return Stream.of(clazz.getMethods())
			.filter(method -> !Modifier.isAbstract(method.getModifiers()) && Modifier.isPublic(method.getModifiers()) && method.getDeclaringClass() != Object.class)
			.map(Method::getName)
			.anyMatch(name -> "hashCode".equals(name) || "toString".equals(name));
	}

	/**
	 * Yields the blockchain used by the current transaction.
	 * This method can only be called during a transaction.
	 * 
	 * @return the blockchain
	 */
	public static AbstractBlockchain getBlockchain() {
		return blockchain;
	}

	/**
	 * Yields the next identifier that can be used for a new storage object
	 * created during the execution of the current transaction. This identifier is unique
	 * inside the transaction. This method will return distinct identifiers at each call.
	 * 
	 * @return the identifier
	 */
	private static BigInteger generateNextProgressive() {
		BigInteger result = nextProgressive;
		nextProgressive = nextProgressive.add(BigInteger.ONE);
		return result;
	}

	/**
	 * Yields the next storage reference for the current transaction.
	 * 
	 * @return the next storage reference
	 */
	public static Object getNextStorageReference() {
		return StorageReference.mk(blockchain.getCurrentTransaction(), generateNextProgressive());
	}

	/**
	 * Charges the given amount of gas for RAM usage for the current blockchain.
	 * 
	 * @param ram the amount of gas to consume for RAM consumption
	 */
	public static void chargeForRAM(BigInteger ram) {
		blockchain.chargeForRAM(ram);
	}

	/**
	 * Charges the given amount of gas for RAM usage for the current blockchain.
	 * 
	 * @param ram the amount of gas to consume for RAM consumption
	 */
	public static void chargeForRAM(long ram) {
		blockchain.chargeForRAM(BigInteger.valueOf(ram));
	}

	/**
	 * Charges the given amount of gas for RAM usage for the current blockchain.
	 * 
	 * @param ram the amount of gas to consume for RAM consumption
	 */
	public static void chargeForRAM(int ram) {
		blockchain.chargeForRAM(BigInteger.valueOf(ram));
	}

	/**
	 * Charges the given amount of gas for CPU usage for the current blockchain.
	 * 
	 * @param cpu the amount of gas to consume
	 */
	public static void charge(long cpu) {
		blockchain.chargeForCPU(BigInteger.valueOf(cpu));
	}

	/**
	 * Charges the given amount of gas for CPU usage for the current blockchain.
	 * 
	 * @param cpu the amount of gas to consume
	 */
	public static void charge(int cpu) {
		blockchain.chargeForCPU(BigInteger.valueOf(cpu));
	}

	/**
	 * Charges one unit of gas for CPU usage for the current blockchain.
	 */
	public static void charge1() {
		blockchain.chargeForCPU(BigInteger.ONE);
	}

	/**
	 * Charges two units of gas for CPU usage for the current blockchain.
	 */
	public static void charge2() {
		blockchain.chargeForCPU(BigInteger.valueOf(2L));
	}

	/**
	 * Charges three units of gas for CPU usage for the current blockchain.
	 */
	public static void charge3() {
		blockchain.chargeForCPU(BigInteger.valueOf(3L));
	}

	/**
	 * Charges four units of gas for CPU usage for the current blockchain.
	 */
	public static void charge4() {
		blockchain.chargeForCPU(BigInteger.valueOf(4L));
	}

	/**
	 * Charges five units of gas for CPU usage for the current blockchain.
	 */
	public static void charge5() {
		blockchain.chargeForCPU(BigInteger.valueOf(5L));
	}

	/**
	 * Charges six units of gas for CPU usage for the current blockchain.
	 */
	public static void charge6() {
		blockchain.chargeForCPU(BigInteger.valueOf(6L));
	}

	/**
	 * Charges seven units of gas for CPU usage for the current blockchain.
	 */
	public static void charge7() {
		blockchain.chargeForCPU(BigInteger.valueOf(7L));
	}

	/**
	 * Charges eight units of gas for CPU usage for the current blockchain.
	 */
	public static void charge8() {
		blockchain.chargeForCPU(BigInteger.valueOf(8L));
	}

	/**
	 * Charges nine units of gas for CPU usage for the current blockchain.
	 */
	public static void charge9() {
		blockchain.chargeForCPU(BigInteger.valueOf(9L));
	}

	/**
	 * Charges ten units of gas for CPU usage for the current blockchain.
	 */
	public static void charge10() {
		blockchain.chargeForCPU(BigInteger.valueOf(10L));
	}

	/**
	 * Charges eleven units of gas for CPU usage for the current blockchain.
	 */
	public static void charge11() {
		blockchain.chargeForCPU(BigInteger.valueOf(11L));
	}

	/**
	 * Charges twelve units of gas for CPU usage for the current blockchain.
	 */
	public static void charge12() {
		blockchain.chargeForCPU(BigInteger.valueOf(12L));
	}

	/**
	 * Charges 13 units of gas for CPU usage for the current blockchain.
	 */
	public static void charge13() {
		blockchain.chargeForCPU(BigInteger.valueOf(13L));
	}

	/**
	 * Charges 14 units of gas for CPU usage for the current blockchain.
	 */
	public static void charge14() {
		blockchain.chargeForCPU(BigInteger.valueOf(14L));
	}

	/**
	 * Charges 15 units of gas for CPU usage for the current blockchain.
	 */
	public static void charge15() {
		blockchain.chargeForCPU(BigInteger.valueOf(15L));
	}

	/**
	 * Charges 16 units of gas for CPU usage for the current blockchain.
	 */
	public static void charge16() {
		blockchain.chargeForCPU(BigInteger.valueOf(16L));
	}

	/**
	 * Charges 17 units of gas for CPU usage for the current blockchain.
	 */
	public static void charge17() {
		blockchain.chargeForCPU(BigInteger.valueOf(17L));
	}

	/**
	 * Charges 18 units of gas for CPU usage for the current blockchain.
	 */
	public static void charge18() {
		blockchain.chargeForCPU(BigInteger.valueOf(18L));
	}

	/**
	 * Charges 19 units of gas for CPU usage for the current blockchain.
	 */
	public static void charge19() {
		blockchain.chargeForCPU(BigInteger.valueOf(19L));
	}

	/**
	 * Charges 20 units of gas for CPU usage for the current blockchain.
	 */
	public static void charge20() {
		blockchain.chargeForCPU(BigInteger.valueOf(20L));
	}
}