package takamaka.lang;

import takamaka.whitelisted.WhiteListed;

/**
 * An event is a storage object that remains in the blockchain at the
 * end of a successful execution of a blockchain transaction.
 */
public abstract class Event extends Storage {

	@WhiteListed
	protected Event() {}
}