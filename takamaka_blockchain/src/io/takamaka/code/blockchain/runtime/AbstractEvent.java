package io.takamaka.code.blockchain.runtime;

/**
 * This class will be set by instrumentation as superclass of {@link io.takamaka.code.lang.Event}.
 * Moreover, this class can be used in this module without importing the runtime of Takamaka.
 */
public abstract class AbstractEvent extends AbstractStorage {
	protected AbstractEvent() {}
}