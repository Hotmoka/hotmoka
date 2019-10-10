package takamaka.lang;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import java.util.stream.Stream;

import takamaka.blockchain.AbstractBlockchain;
import takamaka.whitelisted.WhiteListed;
import takamaka.whitelisted.WhiteListingProofObligation;

/**
 * A class that acts as a global context for statements added
 * to the Takamaka language.
 */
public abstract class Takamaka {

	/**
	 * The blockchain used for the current transaction.
	 */
	private static AbstractBlockchain blockchain;

	/**
	 * The counter for the next storage object created during the current transaction.
	 */
	private static BigInteger nextProgressive;

	private Takamaka() {}

	/**
	 * Resets static data at the beginning of a transaction.
	 * 
	 * @param blockchain the blockchain used for the new transaction
	 */
	public static void init(AbstractBlockchain blockchain) {
		Takamaka.blockchain = blockchain;
		Takamaka.nextProgressive = BigInteger.ZERO;
	}

	/**
	 * Takes note of the given event. This method can only be called during
	 * a transaction.
	 * 
	 * @param event the event
	 */
	@WhiteListed
	public static void event(Event event) {
		blockchain.event(event);
	}

	/**
	 * Requires that the given condition holds.
	 * This is a synonym of {@link takamaka.lang.Takamaka#requireThat(boolean, String)}.
	 * 
	 * @param condition the condition that must hold
	 * @param message the message used in the exception raised if the
	 *                condition does not hold
	 * @throws RequirementViolationException if the condition does not hold
	 */
	@WhiteListed
	public static void require(boolean condition, String message) {
		if (!condition)
			throw new RequirementViolationException(message);
	}

	/**
	 * Requires that the given condition holds.
	 * This is a synonym of {@link takamaka.lang.Takamaka#require(boolean, String)}.
	 * 
	 * @param condition the condition that must hold
	 * @param message the message used in the exception raised if the
	 *                condition does not hold
	 * @throws RequirementViolationException if the condition does not hold
	 */
	@WhiteListed
	public static void requireThat(boolean condition, String message) {
		if (!condition)
			throw new RequirementViolationException(message);
	}

	/**
	 * Asserts that the given condition holds.
	 * 
	 * @param condition the condition that must hold
	 * @param message the message used in the exception raised if the
	 *                condition does not hold
	 * @throws AssertionViolationException if the condition does not hold
	 */
	@WhiteListed
	public static void assertThat(boolean condition, String message) {
		if (!condition)
			throw new AssertionViolationException(message);
	}

	/**
	 * Requires that the given condition holds.
	 * This is a synonym of {@link takamaka.lang.Takamaka#requireThat(boolean, Supplier)}.
	 * 
	 * @param condition the condition that must hold
	 * @param message the supplier of the message used in the exception raised if the
	 *                condition does not hold
	 * @throws RequirementViolationException if the condition does not hold
	 */
	@WhiteListed
	public static void require(boolean condition, Supplier<String> message) {
		if (!condition)
			throw new RequirementViolationException(message.get());
	}

	/**
	 * Requires that the given condition holds.
	 * This is a synonym of {@link takamaka.lang.Takamaka#require(boolean, Supplier)}.
	 * 
	 * @param condition the condition that must hold
	 * @param message the supplier of the message used in the exception raised if the
	 *                condition does not hold
	 * @throws RequirementViolationException if the condition does not hold
	 */
	@WhiteListed
	public static void requireThat(boolean condition, Supplier<String> message) {
		if (!condition)
			throw new RequirementViolationException(message.get());
	}

	/**
	 * Asserts that the given condition holds.
	 * 
	 * @param condition the condition that must hold
	 * @param message the supplier of the message used in the exception raised if the
	 *                condition does not hold
	 * @throws AssertionViolationException if the condition does not hold
	 */
	@WhiteListed
	public static void assertThat(boolean condition, Supplier<String> message) {
		if (!condition)
			throw new AssertionViolationException(message.get());
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
	@WhiteListed
	public static <T> T withGas(BigInteger amount, Callable<T> what) throws Exception {
		return blockchain.withGas(amount, what);
	}

	@WhiteListed
	public static long now() {
		return blockchain.getNow();
	}

	public static Optional<Method> getWhiteListingCheckFor(Annotation annotation) {
		return getWhiteListingCheckFor(annotation.annotationType());
	}

	public static Optional<Method> getWhiteListingCheckFor(Class<? extends Annotation> annotationType) {
		if (annotationType.isAnnotationPresent(WhiteListingProofObligation.class)) {
			String checkName = lowerInitial(annotationType.getSimpleName());
			Optional<Method> checkMethod = Stream.of(Takamaka.class.getDeclaredMethods())
				.filter(method -> method.getName().equals(checkName)).findFirst();

			if (!checkMethod.isPresent())
				throw new IllegalStateException("unexpected white-list annotation " + annotationType.getSimpleName());

			return checkMethod;
		}

		return Optional.empty();
	}

	private static String lowerInitial(String name) {
		return Character.toLowerCase(name.charAt(0)) + name.substring(1);
	}

	public static void mustBeFalse(boolean value, String methodName) {
		if (value)
			throw new NonWhiteListedCallException("the actual parameter of " + methodName + " must be false");
	}

	public static void mustRedefineHashCode(Object value, String methodName) {
		if (value != null)
			if (Stream.of(value.getClass().getMethods())
				.filter(method -> !Modifier.isAbstract(method.getModifiers()) && Modifier.isPublic(method.getModifiers()) && method.getDeclaringClass() != Object.class)
				.map(Method::getName)
				.noneMatch("hashCode"::equals))
				throw new NonWhiteListedCallException("the actual parameter of " + methodName + " must redefine Object.hashCode()");
	}

	public static void mustRedefineHashCodeOrToString(Object value, String methodName) {
		if (value != null && !redefinesHashCodeOrToString(value.getClass()))
			throw new NonWhiteListedCallException("the actual parameter of " + methodName + " must redefine Object.hashCode() or Object.toString()");
	}

	public static void mustBeOrdered(Object value, String methodName) {
		if (value != null && !isOrdered(value.getClass()))
			throw new NonWhiteListedCallException("the actual parameter of " + methodName + " must be an ordered collection");
	}

	public static boolean redefinesHashCodeOrToString(Class<?> clazz) {
		return Stream.of(clazz.getMethods())
			.filter(method -> !Modifier.isAbstract(method.getModifiers()) && Modifier.isPublic(method.getModifiers()) && method.getDeclaringClass() != Object.class)
			.map(Method::getName)
			.anyMatch(name -> "hashCode".equals(name) || "toString".equals(name));
	}

	public static boolean isOrdered(Class<?> clazz) {
		return List.class.isAssignableFrom(clazz) || SortedSet.class.isAssignableFrom(clazz);
	}

	/**
	 * Yields the blockchain used by the current transaction.
	 * This method can only be called during a transaction.
	 * 
	 * @return the blockchain
	 */
	static AbstractBlockchain getBlockchain() {
		return blockchain;
	}

	/**
	 * Yields the next identifier that can be used for a new storage object
	 * created during the execution of the current transaction. This identifier is unique
	 * inside the transaction. This method will return distinct identifiers at each call.
	 * 
	 * @return the identifier
	 */
	static BigInteger generateNextProgressive() {
		BigInteger result = nextProgressive;
		nextProgressive = nextProgressive.add(BigInteger.ONE);
		return result;
	}

	/**
	 * Decreases the available gas for the current blockchain, by the given amount.
	 * This method is used by the instrumented bytecode.
	 * 
	 * @param amount the amount of gas to consume
	 */
	public static void charge(long amount) {
		blockchain.chargeForCPU(BigInteger.valueOf(amount));
	}

	/**
	 * Decreases the available gas for the current blockchain, by the given amount.
	 * This method is used by the instrumented bytecode.
	 * 
	 * @param amount the amount of gas to consume
	 */
	public static void charge(int amount) {
		blockchain.chargeForCPU(BigInteger.valueOf(amount));
	}

	/**
	 * Decreases the available gas for the current blockchain, by a unit of gas.
	 * This method is used by the instrumented bytecode.
	 */
	public static void charge1() {
		blockchain.chargeForCPU(BigInteger.ONE);
	}

	/**
	 * Decreases the available gas for the current blockchain, by two units of gas.
	 * This method is used by the instrumented bytecode.
	 */
	public static void charge2() {
		blockchain.chargeForCPU(BigInteger.valueOf(2L));
	}

	/**
	 * Decreases the available gas for the current blockchain, by three units of gas.
	 * This method is used by the instrumented bytecode.
	 */
	public static void charge3() {
		blockchain.chargeForCPU(BigInteger.valueOf(3L));
	}

	/**
	 * Decreases the available gas for the current blockchain, by four units of gas.
	 * This method is used by the instrumented bytecode.
	 */
	public static void charge4() {
		blockchain.chargeForCPU(BigInteger.valueOf(4L));
	}

	/**
	 * Decreases the available gas for the current blockchain, by five units of gas.
	 * This method is used by the instrumented bytecode.
	 */
	public static void charge5() {
		blockchain.chargeForCPU(BigInteger.valueOf(5L));
	}

	/**
	 * Decreases the available gas for the current blockchain, by six units of gas.
	 * This method is used by the instrumented bytecode.
	 */
	public static void charge6() {
		blockchain.chargeForCPU(BigInteger.valueOf(6L));
	}

	/**
	 * Decreases the available gas for the current blockchain, by seven units of gas.
	 * This method is used by the instrumented bytecode.
	 */
	public static void charge7() {
		blockchain.chargeForCPU(BigInteger.valueOf(7L));
	}

	/**
	 * Decreases the available gas for the current blockchain, by eight units of gas.
	 * This method is used by the instrumented bytecode.
	 */
	public static void charge8() {
		blockchain.chargeForCPU(BigInteger.valueOf(8L));
	}

	/**
	 * Decreases the available gas for the current blockchain, by nine units of gas.
	 * This method is used by the instrumented bytecode.
	 */
	public static void charge9() {
		blockchain.chargeForCPU(BigInteger.valueOf(9L));
	}

	/**
	 * Decreases the available gas for the current blockchain, by ten units of gas.
	 * This method is used by the instrumented bytecode.
	 */
	public static void charge10() {
		blockchain.chargeForCPU(BigInteger.valueOf(10L));
	}

	/**
	 * Decreases the available gas for the current blockchain, by eleven units of gas.
	 * This method is used by the instrumented bytecode.
	 */
	public static void charge11() {
		blockchain.chargeForCPU(BigInteger.valueOf(11L));
	}

	/**
	 * Decreases the available gas for the current blockchain, by twelve units of gas.
	 * This method is used by the instrumented bytecode.
	 */
	public static void charge12() {
		blockchain.chargeForCPU(BigInteger.valueOf(12L));
	}

	/**
	 * Decreases the available gas for the current blockchain, by thirteen units of gas.
	 * This method is used by the instrumented bytecode.
	 */
	public static void charge13() {
		blockchain.chargeForCPU(BigInteger.valueOf(13L));
	}

	/**
	 * Decreases the available gas for the current blockchain, by fourteen units of gas.
	 * This method is used by the instrumented bytecode.
	 */
	public static void charge14() {
		blockchain.chargeForCPU(BigInteger.valueOf(14L));
	}

	/**
	 * Decreases the available gas for the current blockchain, by fifteen units of gas.
	 * This method is used by the instrumented bytecode.
	 */
	public static void charge15() {
		blockchain.chargeForCPU(BigInteger.valueOf(15L));
	}

	/**
	 * Decreases the available gas for the current blockchain, by sixteen units of gas.
	 * This method is used by the instrumented bytecode.
	 */
	public static void charge16() {
		blockchain.chargeForCPU(BigInteger.valueOf(16L));
	}

	/**
	 * Decreases the available gas for the current blockchain, by seventeen units of gas.
	 * This method is used by the instrumented bytecode.
	 */
	public static void charge17() {
		blockchain.chargeForCPU(BigInteger.valueOf(17L));
	}

	/**
	 * Decreases the available gas for the current blockchain, by eighteen units of gas.
	 * This method is used by the instrumented bytecode.
	 */
	public static void charge18() {
		blockchain.chargeForCPU(BigInteger.valueOf(18L));
	}

	/**
	 * Decreases the available gas for the current blockchain, by nineteen units of gas.
	 * This method is used by the instrumented bytecode.
	 */
	public static void charge19() {
		blockchain.chargeForCPU(BigInteger.valueOf(19L));
	}

	/**
	 * Decreases the available gas for the current blockchain, by twenty units of gas.
	 * This method is used by the instrumented bytecode.
	 */
	public static void charge20() {
		blockchain.chargeForCPU(BigInteger.valueOf(20L));
	}

	/**
	 * The maximal gas cost for which there is an optimized charge method.
	 */
	public final static int MAX_COMPACT = 20;
}