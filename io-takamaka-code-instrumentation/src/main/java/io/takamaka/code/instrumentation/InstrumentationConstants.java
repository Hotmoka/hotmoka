package io.takamaka.code.instrumentation;

import static io.takamaka.code.verification.VerifiedClass.FORBIDDEN_PREFIX;

/**
 * A collector of constants useful during code instrumentation.
 */
public interface InstrumentationConstants {

	/**
	 * The name of the instrumented field of storage objects that holds their storage reference.
	 * Since it is private, it does not need any forbidden character at its beginning.
	 */
	public final static String STORAGE_REFERENCE_FIELD_NAME = "storageReference";

	/**
	 * The prefix of the name of the field used in instrumented storage classes
	 * to take note of the old value of the fields.
	 */
	public final static String OLD_PREFIX = FORBIDDEN_PREFIX + "old_";

	/**
	 * The prefix of the name of the field used in instrumented storage classes
	 * to determine if a lazy field has been assigned.
	 */	
	public final static String IF_ALREADY_LOADED_PREFIX = FORBIDDEN_PREFIX + "ifAlreadyLoaded_";

	/**
	 * The prefix of the name of the method used in instrumented storage classes
	 * to ensure that a lazy field has been loaded.
	 */
	public final static String ENSURE_LOADED_PREFIX = FORBIDDEN_PREFIX + "ensureLoaded_";

	/**
	 * The prefix of the name of the method used in instrumented storage classes
	 * to read a lazy field.
	 */
	public final static String GETTER_PREFIX = FORBIDDEN_PREFIX + "get_";

	/**
	 * The prefix of the name of the method used in instrumented storage classes
	 * to set a lazy field.
	 */
	public final static String SETTER_PREFIX = FORBIDDEN_PREFIX + "set_";

	/**
	 * The name of the field used in instrumented storage classes
	 * to remember if the object is new or already serialized in blockchain.
	 * Since it is private, it does not need any forbidden character at its beginning.
	 */
	public final static String IN_STORAGE = "inStorage";

	/**
	 * The name of the method of {@code io.takamaka.code.lang.Storage} used to access the caller of entries.
	 */
	public final static String CALLER = "caller";

	/**
	 * The name of the method of {@code io.hotmoka.local.internal.runtime.Runtime}
	 * used to retrieve the last update for a non-final lazy field.
	 */
	public final static String DESERIALIZE_LAST_UPDATE_FOR = "deserializeLastLazyUpdateFor";

	/**
	 * The name of the method of {@code io.hotmoka.local.internal.runtime.Runtime}
	 * used to retrieve the last update for a final lazy field.
	 */
	public final static String DESERIALIZE_LAST_UPDATE_FOR_FINAL = "deserializeLastLazyUpdateForFinal";

	/**
	 * The prefix of the name of extra lambdas added during instrumentation.
	 */
	public final static String EXTRA_LAMBDA = FORBIDDEN_PREFIX + "lambda";

	/**
	 * The prefix of the name of extra methods used to simulate multidimensional
	 * array creations and keep track of the gas consumed for RAM consumption.
	 */
	public final static String EXTRA_ALLOCATOR = FORBIDDEN_PREFIX + "newarray";

	/**
	 * The prefix of the name of extra methods used to check white-listing annotations at run time.
	 */
	public final static String EXTRA_VERIFIER = FORBIDDEN_PREFIX + "verifier";

	/**
	 * The name of the method of {@code io.hotmoka.local.internal.runtime.Runtime}
	 * that sets the caller and transfers money at the beginning of a payable {@@code @FromContract}.
	 */
	public final static String PAYABLE_FROM_CONTRACT = "payableFromContract";

	/**
	 * The name of the method of {@code io.hotmoka.local.internal.runtime.Runtime}
	 * that sets the caller and transfers money at the beginning of a red payable {@@code @FromContract}.
	 */
	public final static String RED_PAYABLE_FROM_CONTRACT = "redPayableFromContract";

	/**
	 * The name of the method of {@code io.hotmoka.local.internal.runtime.Runtime}
	 * that sets the caller at the beginning of a {@@code @FromContract}.
	 */
	public final static String FROM_CONTRACT = "fromContract";

	/**
	 * The number of optimized methods for gas charge in the
	 * {@code io.hotmoka.local.internal.runtime.Runtime} class.
	 */
	public static final long MAX_OPTIMIZED_CHARGE = 20;
}