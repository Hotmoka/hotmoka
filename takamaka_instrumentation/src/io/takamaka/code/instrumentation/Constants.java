package io.takamaka.code.instrumentation;

import static io.takamaka.code.verification.Constants.FORBIDDEN_PREFIX;

/**
 * A collector of constants useful during code instrumentation.
 */
public interface Constants {

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.Takamaka}.
	 */
	public final static String TAKAMAKA_NAME = "io.takamaka.code.lang.Takamaka";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.Event}.
	 */
	public final static String EVENT_NAME = "io.takamaka.code.lang.Event";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.View}.
	 */
	public final static String VIEW_NAME = "io.takamaka.code.lang.View";

	/**
	 * The name of the class type for {@link io.takamaka.code.blockchain.runtime.Runtime}.
	 */
	public final static String RUNTIME_NAME = "io.takamaka.code.blockchain.runtime.Runtime";

	/**
	 * The name of the class type for {@link io.takamaka.code.blockchain.values.StorageReference}.
	 */
	public final static String STORAGE_REFERENCE_NAME = "io.takamaka.code.blockchain.values.StorageReference";

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
	 * The name of the method of {@code io.takamaka.code.blockchain.runtime.Runtime}
	 * used to retrieve the last update for a non-final lazy field.
	 */
	public final static String DESERIALIZE_LAST_UPDATE_FOR = "deserializeLastLazyUpdateFor";

	/**
	 * The name of the method of {@code io.takamaka.code.blockchain.runtime.Runtime}
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
	public final static String EXTRA_ALLOCATOR = FORBIDDEN_PREFIX + "multianewarray";

	/**
	 * The prefix of the name of extra methods used to check white-listing annotations at run time.
	 */
	public final static String EXTRA_VERIFIER = FORBIDDEN_PREFIX + "verifier";

	/**
	 * The name of the method of {@code io.takamaka.code.blockchain.runtime.Runtime}
	 * that sets the caller and transfers money at the beginning of a payable entry.
	 */
	public final static String PAYABLE_ENTRY = "payableEntry";

	/**
	 * The name of the method of {@code io.takamaka.code.blockchain.runtime.Runtime}
	 * that sets the caller and transfers money at the beginning of a red payable entry.
	 */
	public final static String RED_PAYABLE_ENTRY = "redPayableEntry";

	/**
	 * The name of the method of {@code io.takamaka.code.blockchain.runtime.Runtime}
	 * that sets the caller at the beginning of an entry.
	 */
	public final static String ENTRY = "entry";

	/**
	 * The number of optimized methods for gas charge in the
	 * {@code io.takamaka.code.blockchain.runtime.Runtime} class.
	 */
	public static final long MAX_OPTIMIZED_CHARGE = 20;
}