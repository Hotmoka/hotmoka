package io.takamaka.code.selfcharged;

import io.takamaka.code.lang.Event;
import io.takamaka.code.lang.Storage;

/**
 * An event stating that an account should be remove from the white-list for
 * {@code @@SelfCharged} methods, if any such white-list exists.
 */
public class BlackList extends Event {

	/**
	 * Creates the event.
	 * 
	 * @param key the key of the event
	 * @param id the identifier of the account
	 */
	public BlackList(Storage key, String id) {
		super(key);
	}
}