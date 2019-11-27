package io.takamaka.code.blockchain.runtime;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import io.takamaka.code.blockchain.AbstractBlockchain;
import io.takamaka.code.blockchain.DeserializationError;
import io.takamaka.code.blockchain.FieldSignature;
import io.takamaka.code.blockchain.GasCosts;
import io.takamaka.code.blockchain.NonWhiteListedCallException;
import io.takamaka.code.blockchain.OutOfGasError;
import io.takamaka.code.blockchain.Update;
import io.takamaka.code.blockchain.UpdateOfBalance;
import io.takamaka.code.blockchain.UpdateOfBigInteger;
import io.takamaka.code.blockchain.UpdateOfBoolean;
import io.takamaka.code.blockchain.UpdateOfByte;
import io.takamaka.code.blockchain.UpdateOfChar;
import io.takamaka.code.blockchain.UpdateOfDouble;
import io.takamaka.code.blockchain.UpdateOfEnumEager;
import io.takamaka.code.blockchain.UpdateOfEnumLazy;
import io.takamaka.code.blockchain.UpdateOfFloat;
import io.takamaka.code.blockchain.UpdateOfInt;
import io.takamaka.code.blockchain.UpdateOfLong;
import io.takamaka.code.blockchain.UpdateOfShort;
import io.takamaka.code.blockchain.UpdateOfStorage;
import io.takamaka.code.blockchain.UpdateOfString;
import io.takamaka.code.blockchain.UpdateToNullEager;
import io.takamaka.code.blockchain.UpdateToNullLazy;
import io.takamaka.code.blockchain.types.BasicTypes;
import io.takamaka.code.blockchain.types.ClassType;
import io.takamaka.code.blockchain.values.StorageReference;

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
	 * Utility method called in the instrumentation of storage classes to implement redefinitions of
	 * {@link io.takamaka.code.blockchain.runtime.AbstractStorage#extractUpdates(Set, Set, List)}
	 * to recur on the old value of fields of reference type.
	 * 
	 * @param s the storage objects whose fields are considered
	 * @param updates the set where updates are added
	 * @param seen the set of storage references already scanned
	 * @param workingSet the set of storage objects that still need to be processed
	 */
	public static void recursiveExtract(Object s, Set<Update> updates, Set<StorageReference> seen, List<AbstractStorage> workingSet) {
		if (s instanceof AbstractStorage) {
			if (seen.add(((AbstractStorage) s).storageReference))
				workingSet.add((AbstractStorage) s);
		}
		else if (s instanceof String || s instanceof BigInteger || s instanceof Enum<?>) {} // these types are not recursively followed
		else if (s != null)
			throw new DeserializationError("a field of a storage object cannot hold a " + s.getClass().getName());
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
	public static Object deserializeLastLazyUpdateFor(AbstractStorage object, String definingClass, String name, String fieldClassName) throws Exception {
		return Runtime.getBlockchain().deserializeLastLazyUpdateFor(object.storageReference, FieldSignature.mk(definingClass, name, ClassType.mk(fieldClassName)));
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
	public static Object deserializeLastLazyUpdateForFinal(AbstractStorage object, String definingClass, String name, String fieldClassName) throws Exception {
		return Runtime.getBlockchain().deserializeLastLazyUpdateForFinal(object.storageReference, FieldSignature.mk(definingClass, name, ClassType.mk(fieldClassName)));
	}

	/**
	 * Takes note that a field of reference type has changed its value and consequently adds it to the set of updates.
	 * 
	 * @param object the container of the field
	 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object of one of its superclasses
	 * @param fieldName the name of the field
	 * @param updates the set where the update will be added
	 * @param seen the set of storage references already processed
	 * @param workingSet the set of storage objects that still need to be processed
	 * @param fieldClassName the name of the type of the field
	 * @param s the value set to the field
	 */
	@SuppressWarnings("unchecked")
	public static void addUpdateFor(AbstractStorage object, String fieldDefiningClass, String fieldName, Set<Update> updates, Set<StorageReference> seen, List<AbstractStorage> workingSet, String fieldClassName, Object s) {
		// these values are not recursively followed
		FieldSignature field = FieldSignature.mk(fieldDefiningClass, fieldName, ClassType.mk(fieldClassName));

		if (s == null)
			//the field has been set to null
			updates.add(new UpdateToNullLazy(object.storageReference, field));
		else if (s instanceof AbstractStorage) {
			// the field has been set to a storage object
			AbstractStorage storage = (AbstractStorage) s;
			updates.add(new UpdateOfStorage(object.storageReference, field, storage.storageReference));

			// if the new value has not yet been considered, we put in the list of object still to be processed
			if (seen.add(storage.storageReference))
				workingSet.add(storage);
		}
		// the following cases occur if the declared type of the field is Object but it is updated
		// to an object whose type is allowed in storage
		else if (s instanceof String)
			updates.add(new UpdateOfString(object.storageReference, field, (String) s));
		else if (s instanceof BigInteger)
			updates.add(new UpdateOfBigInteger(object.storageReference, field, (BigInteger) s));
		else if (s instanceof Enum<?>) {
			if (hasInstanceFields((Class<? extends Enum<?>>) s.getClass()))
				throw new DeserializationError("field " + field + " of a storage object cannot hold an enumeration of class " + s.getClass().getName() + ": it has instance non-transient fields");

			updates.add(new UpdateOfEnumLazy(object.storageReference, field, s.getClass().getName(), ((Enum<?>) s).name()));
		}
		else
			throw new DeserializationError("field " + field + " of a storage object cannot hold a " + s.getClass().getName());
	}

	/**
	 * Determines if the given enumeration type has at least an instance, non-transient field.
	 * 
	 * @param clazz the class
	 * @return true only if that condition holds
	 */
	private static boolean hasInstanceFields(Class<? extends Enum<?>> clazz) {
		return Stream.of(clazz.getDeclaredFields())
			.map(Field::getModifiers)
			.anyMatch(modifiers -> !Modifier.isStatic(modifiers) && !Modifier.isTransient(modifiers));
	}

	/**
	 * Takes note that a field of {@code boolean} type has changed its value and consequently adds it to the set of updates.
	 * 
	 * @param object the container of the field
	 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object or one of its superclasses
	 * @param fieldName the name of the field
	 * @param updates the set where the update will be added
	 * @param s the value set to the field
	 */
	public static void addUpdateFor(AbstractStorage object, String fieldDefiningClass, String fieldName, Set<Update> updates, boolean s) {
		updates.add(new UpdateOfBoolean(object.storageReference, FieldSignature.mk(fieldDefiningClass, fieldName, BasicTypes.BOOLEAN), s));
	}

	/**
	 * Takes note that a field of {@code byte} type has changed its value and consequently adds it to the set of updates.
	 * 
	 * @param object the container of the field
	 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object or one of its superclasses
	 * @param fieldName the name of the field
	 * @param updates the set where the update will be added
	 * @param s the value set to the field
	 */
	public static void addUpdateFor(AbstractStorage object, String fieldDefiningClass, String fieldName, Set<Update> updates, byte s) {
		updates.add(new UpdateOfByte(object.storageReference, FieldSignature.mk(fieldDefiningClass, fieldName, BasicTypes.BYTE), s));
	}

	/**
	 * Takes note that a field of {@code char} type has changed its value and consequently adds it to the set of updates.
	 * 
	 * @param object the container of the field
	 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object or one of its superclasses
	 * @param fieldName the name of the field
	 * @param updates the set where the update will be added
	 * @param s the value set to the field
	 */
	public static void addUpdateFor(AbstractStorage object, String fieldDefiningClass, String fieldName, Set<Update> updates, char s) {
		updates.add(new UpdateOfChar(object.storageReference, FieldSignature.mk(fieldDefiningClass, fieldName, BasicTypes.CHAR), s));
	}

	/**
	 * Takes note that a field of {@code double} type has changed its value and consequently adds it to the set of updates.
	 * 
	 * @param object the container of the field
	 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object or one of its superclasses
	 * @param fieldName the name of the field
	 * @param updates the set where the update will be added
	 * @param s the value set to the field
	 */
	public static void addUpdateFor(AbstractStorage object, String fieldDefiningClass, String fieldName, Set<Update> updates, double s) {
		updates.add(new UpdateOfDouble(object.storageReference, FieldSignature.mk(fieldDefiningClass, fieldName, BasicTypes.DOUBLE), s));
	}

	/**
	 * Takes note that a field of {@code float} type has changed its value and consequently adds it to the set of updates.
	 * 
	 * @param object the container of the field
	 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object or one of its superclasses
	 * @param fieldName the name of the field
	 * @param updates the set where the update will be added
	 * @param s the value set to the field
	 */
	public static void addUpdateFor(AbstractStorage object, String fieldDefiningClass, String fieldName, Set<Update> updates, float s) {
		updates.add(new UpdateOfFloat(object.storageReference, FieldSignature.mk(fieldDefiningClass, fieldName, BasicTypes.FLOAT), s));
	}

	/**
	 * Takes note that a field of {@code int} type has changed its value and consequently adds it to the set of updates.
	 * 
	 * @param object the container of the field
	 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object or one of its superclasses
	 * @param fieldName the name of the field
	 * @param updates the set where the update will be added
	 * @param s the value set to the field
	 */
	public static void addUpdateFor(AbstractStorage object, String fieldDefiningClass, String fieldName, Set<Update> updates, int s) {
		updates.add(new UpdateOfInt(object.storageReference, FieldSignature.mk(fieldDefiningClass, fieldName, BasicTypes.INT), s));
	}

	/**
	 * Takes note that a field of {@code long} type has changed its value and consequently adds it to the set of updates.
	 * 
	 * @param object the container of the field
	 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object or one of its superclasses
	 * @param fieldName the name of the field
	 * @param updates the set where the update will be added
	 * @param s the value set to the field
	 */
	public static void addUpdateFor(AbstractStorage object, String fieldDefiningClass, String fieldName, Set<Update> updates, long s) {
		updates.add(new UpdateOfLong(object.storageReference, FieldSignature.mk(fieldDefiningClass, fieldName, BasicTypes.LONG), s));
	}

	/**
	 * Takes note that a field of {@code short} type has changed its value and consequently adds it to the set of updates.
	 * 
	 * @param object the container of the field
	 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object or one of its superclasses
	 * @param fieldName the name of the field
	 * @param updates the set where the update will be added
	 * @param s the value set to the field
	 */
	public static void addUpdateFor(AbstractStorage object, String fieldDefiningClass, String fieldName, Set<Update> updates, short s) {
		updates.add(new UpdateOfShort(object.storageReference, FieldSignature.mk(fieldDefiningClass, fieldName, BasicTypes.SHORT), s));
	}

	/**
	 * Takes note that a field of {@link java.lang.String} type has changed its value and consequently adds it to the set of updates.
	 * 
	 * @param object the container of the field
	 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object or one of its superclasses
	 * @param fieldName the name of the field
	 * @param updates the set where the update will be added
	 * @param s the value set to the field
	 */
	public static void addUpdateFor(AbstractStorage object, String fieldDefiningClass, String fieldName, Set<Update> updates, String s) {
		if (s == null)
			updates.add(new UpdateToNullEager(object.storageReference, FieldSignature.mk(fieldDefiningClass, fieldName, ClassType.STRING)));
		else
			updates.add(new UpdateOfString(object.storageReference, FieldSignature.mk(fieldDefiningClass, fieldName, ClassType.STRING), s));
	}

	/**
	 * Takes note that a field of {@link java.math.BigInteger} type has changed its value and consequently adds it to the set of updates.
	 * 
	 * @param object the container of the field
	 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object or one of its superclasses
	 * @param fieldName the name of the field
	 * @param updates the set where the update will be added
	 * @param bi the value set to the field
	 */
	public static void addUpdateFor(AbstractStorage object, String fieldDefiningClass, String fieldName, Set<Update> updates, BigInteger bi) {
		FieldSignature field = FieldSignature.mk(fieldDefiningClass, fieldName, ClassType.BIG_INTEGER);
		if (bi == null)
			updates.add(new UpdateToNullEager(object.storageReference, field));
		else if (field.equals(FieldSignature.BALANCE_FIELD))
			updates.add(new UpdateOfBalance(object.storageReference, bi));
		else
			updates.add(new UpdateOfBigInteger(object.storageReference, field, bi));
	}

	/**
	 * Takes note that a field of {@code enum} type has changed its value and consequently adds it to the set of updates.
	 * 
	 * @param object the container of the field
	 * @param fieldDefiningClass the class of the field. This can only be the class of this storage object or one of its superclasses
	 * @param fieldName the name of the field
	 * @param updates the set where the update will be added
	 * @param fieldClassName the name of the type of the field
	 * @param element the value set to the field
	 */
	public static void addUpdateFor(AbstractStorage object, String fieldDefiningClass, String fieldName, Set<Update> updates, String fieldClassName, Enum<?> element) {
		FieldSignature field = FieldSignature.mk(fieldDefiningClass, fieldName, ClassType.mk(fieldClassName));
		if (element == null)
			updates.add(new UpdateToNullEager(object.storageReference, field));
		else
			updates.add(new UpdateOfEnumEager(object.storageReference, field, element.getClass().getName(), element.name()));
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
			getBlockchain().getEntry().invoke(callee, caller);
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
			getBlockchain().getPayableEntryBigInteger().invoke(callee, caller, amount);
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
			getBlockchain().getPayableEntryInt().invoke(callee, caller, amount);
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
			getBlockchain().getPayableEntryLong().invoke(callee, caller, amount);
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
	 * Takes note of the given event.
	 *
	 * @param event the event
	 */
	public static void event(AbstractStorage event) {
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
	public static BigInteger generateNextProgressive() {
		BigInteger result = nextProgressive;
		nextProgressive = nextProgressive.add(BigInteger.ONE);
		return result;
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
	 * Charges the amount of gas for RAM usage for the current blockchain,
	 * needed to allocate an array of the given length.
	 * 
	 * @param length the length of the array
	 */
	public static void chargeForRAMForArrayOfLength(int length) {
		// if the array has negative length, its creation will fail
		if (length >= 0)
			// we convert into long to avoid overflow
			chargeForRAM(GasCosts.RAM_COST_PER_ARRAY + length * (long) GasCosts.RAM_COST_PER_ARRAY_SLOT);
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