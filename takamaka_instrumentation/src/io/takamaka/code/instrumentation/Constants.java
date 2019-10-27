package io.takamaka.code.instrumentation;

/**
 * A collector of constants used in the instrumented code.
 */
public class Constants {

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.ExternallyOwnedAccount}.
	 */
	public final static String EOA_NAME = "io.takamaka.code.lang.ExternallyOwnedAccount";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.Contract}.
	 */
	public final static String CONTRACT_NAME = "io.takamaka.code.lang.Contract";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.Storage}.
	 */
	public final static String STORAGE_NAME = "io.takamaka.code.lang.Storage";

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
	 * The name of the class type for {@link io.takamaka.code.lang.Payable}.
	 */
	public final static String PAYABLE_NAME = "io.takamaka.code.lang.Payable";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.Entry}.
	 */
	public final static String ENTRY_NAME = "io.takamaka.code.lang.Entry";

	/**
	 * The name of the class type for {@link io.takamaka.code.lang.ThrowsException}.
	 */
	public final static String THROWS_EXCEPTIONS_NAME = "io.takamaka.code.lang.ThrowsExceptions";

	/**
	 * The name of the class type for {@link takamaka.blockchain.runtime.AbstractEvent}.
	 */
	public final static String ABSTRACT_EVENT_NAME = "takamaka.blockchain.runtime.AbstractEvent";

	/**
	 * The name of the class type for {@link takamaka.blockchain.runtime.AbstractStorage}.
	 */
	public final static String ABSTRACT_STORAGE_NAME = "takamaka.blockchain.runtime.AbstractStorage";

	/**
	 * The name of the class type for {@link takamaka.blockchain.runtime.AbstractTakamaka}.
	 */
	public final static String ABSTRACT_TAKAMAKA_NAME = "takamaka.blockchain.runtime.AbstractTakamaka";

	/**
	 * The name of the class type for {@link takamaka.blockchain.values.StorageReferenceAlreadyInBlockchain}.
	 */
	public final static String SRAIB_NAME = "takamaka.blockchain.values.StorageReferenceAlreadyInBlockchain";

	/**
	 * The maximal gas cost for which there is an optimized charge method.
	 */
	public final static int MAX_COMPACT_CHARGE = 20;
}