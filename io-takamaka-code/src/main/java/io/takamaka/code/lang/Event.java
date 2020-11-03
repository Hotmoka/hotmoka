package io.takamaka.code.lang;

/**
 * An event is a storage object that remains in store at the
 * end of a successful execution of a request.
 * Events keep note of their creator. It is possible to subscribe
 * to events by creator.
 */
public abstract class Event extends Storage {
	
	/**
	 * The creator of the event. It is possible to subscribe to events by creator.
	 */
	public final Contract creator;

	/**
	 * Creates the event.
	 */
	protected @Entry Event() {
		this.creator = caller();
	}

	/**
	 * Yields the creator of this event.
	 * 
	 * @return the creator. It is possible to subscribe to events by creator
	 */
	public @View Storage creator() {
		return creator;
	}
}