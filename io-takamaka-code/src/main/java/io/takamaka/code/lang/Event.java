package io.takamaka.code.lang;

/**
 * An event is a storage object that remains in the blockchain at the
 * end of a successful execution of a blockchain transaction.
 * Events have a key that can be used for subscribing to events by key.
 */
public abstract class Event extends Storage {
	
	/**
	 * The key of the event. It is possible to subscribe to events by key.
	 */
	public final Storage key;

	/**
	 * Creates the event.
	 * 
	 * @param key the key that can be used subsequently to subscribe to events by key;
	 *            this cannot be {@code null}
	 */
	protected Event(Storage key) {
		Takamaka.require(key != null, "The key of an event cannot be null");

		this.key = key;
	}

	/**
	 * Yields the key of this event.
	 * 
	 * @return the key. It is possible to subscribe to events by key
	 */
	public Storage key() {
		return key;
	}
}