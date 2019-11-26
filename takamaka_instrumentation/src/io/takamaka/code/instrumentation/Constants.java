package io.takamaka.code.instrumentation;

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
	 * The name of the class type for {@link io.takamaka.code.blockchain.runtime.AbstractEvent}.
	 */
	public final static String ABSTRACT_EVENT_NAME = "io.takamaka.code.blockchain.runtime.AbstractEvent";

	/**
	 * The name of the class type for {@link io.takamaka.code.blockchain.runtime.AbstractStorage}.
	 */
	public final static String ABSTRACT_STORAGE_NAME = "io.takamaka.code.blockchain.runtime.AbstractStorage";

	/**
	 * The name of the class type for {@link io.takamaka.code.blockchain.runtime.Runtime}.
	 */
	public final static String RUNTIME_NAME = "io.takamaka.code.blockchain.runtime.Runtime";

	/**
	 * The name of the class type for {@link io.takamaka.code.blockchain.values.StorageReference}.
	 */
	public final static String STORAGE_REFERENCE_NAME = "io.takamaka.code.blockchain.values.StorageReference";

	/**
	 * The prefix of the name of the field used in instrumented storage classes
	 * to take note of the old value of the fields.
	 */
	public final static String OLD_PREFIX = io.takamaka.code.verification.Constants.FORBIDDEN_PREFIX + "old_";

	/**
	 * The prefix of the name of the field used in instrumented storage classes
	 * to determine if a lazy field has been assigned.
	 */	
	public final static String IF_ALREADY_LOADED_PREFIX = io.takamaka.code.verification.Constants.FORBIDDEN_PREFIX + "ifAlreadyLoaded_";

	/**
	 * The prefix of the name of the method used in instrumented storage classes
	 * to ensure that a lazy field has been loaded.
	 */
	public final static String ENSURE_LOADED_PREFIX = io.takamaka.code.verification.Constants.FORBIDDEN_PREFIX + "ensureLoaded_";

	/**
	 * The prefix of the name of the field used in instrumented storage classes
	 * to remember if the object is new or already serialized in blockchain.
	 * This does not need the forbidden character at its beginning, since
	 * it is a normal field of class {@code io.takamaka.code.blockchain.runtime.AbstractStorage}.
	 */
	public final static String IN_STORAGE_NAME = "inStorage";

	/**
	 * The prefix of the name of the method used in instrumented storage classes
	 * to read a lazy field.
	 */
	public final static String GETTER_PREFIX = io.takamaka.code.verification.Constants.FORBIDDEN_PREFIX + "get_";

	/**
	 * The prefix of the name of the method used in instrumented storage classes
	 * to set a lazy field.
	 */
	public final static String SETTER_PREFIX = io.takamaka.code.verification.Constants.FORBIDDEN_PREFIX + "set_";

	/**
	 * The name of the method of {@code io.takamaka.code.blockchain.runtime.AbstractStorage}
	 * used to retrieve the last update for a non-final lazy field.
	 */
	//TODO: forbidden prefix
	public final static String DESERIALIZE_LAST_UPDATE_FOR = "deserializeLastLazyUpdateFor";

	/**
	 * The name of the method of {@code io.takamaka.code.blockchain.runtime.AbstractStorage}
	 * used to retrieve the last update for a final lazy field.
	 */
	//TODO: forbidden prefix
	public final static String DESERIALIZE_LAST_UPDATE_FOR_FINAL = "deserializeLastLazyUpdateForFinal";

	/**
	 * The prefix of the name of extra lambdas added during instrumentation.
	 */
	public final static String EXTRA_LAMBDA_NAME = io.takamaka.code.verification.Constants.FORBIDDEN_PREFIX + "lambda";

	/**
	 * The prefix of the name of extra methods used to simulate multidimensional
	 * array creations and keep track of the gas consumed for RAM consumption.
	 */
	public final static String EXTRA_ALLOCATOR_NAME = io.takamaka.code.verification.Constants.FORBIDDEN_PREFIX + "multianewarray";

	/**
	 * The prefix of the name of extra methods used to check white-listing annotations at run time.
	 */
	public final static String EXTRA_VERIFIER_NAME = io.takamaka.code.verification.Constants.FORBIDDEN_PREFIX + "verifier";

	/**
	 * The name of the method of {@code io.takamaka.code.lang.Contract} that sets the caller and transfers
	 * money at the beginning of a payable entry.
	 */
	//TODO forbidden prefix
	public final static String PAYABLE_ENTRY = "payableEntry";

	/**
	 * The name of the method of {@code io.takamaka.code.lang.Contract} that sets caller
	 * at the beginning of a payable entry.
	 */
	public final static String ENTRY = "entry";
}