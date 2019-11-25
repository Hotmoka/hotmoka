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
	 * The name of the class type for {@link io.takamaka.code.blockchain.runtime.AbstractTakamaka}.
	 */
	public final static String ABSTRACT_TAKAMAKA_NAME = "io.takamaka.code.blockchain.runtime.AbstractTakamaka";

	/**
	 * The name of the class type for {@link io.takamaka.code.blockchain.values.StorageReference}.
	 */
	public final static String STORAGE_REFERENCE_NAME = "io.takamaka.code.blockchain.values.StorageReference";
}