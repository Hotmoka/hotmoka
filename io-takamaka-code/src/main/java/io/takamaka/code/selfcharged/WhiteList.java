package io.takamaka.code.selfcharged;

import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Event;

/**
 * An event stating that an account should be added to the white-list for
 * {@code @@SelfCharged} methods, if any such white-list exists.
 */
public class WhiteList extends Event {

	/**
	 * Creates the event.
	 * 
	 * @param id the identifier of the account
	 */
	public @FromContract WhiteList(String id) {
	}
}