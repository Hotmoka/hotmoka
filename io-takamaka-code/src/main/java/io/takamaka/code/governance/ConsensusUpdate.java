package io.takamaka.code.governance;

import static io.takamaka.code.lang.Takamaka.require;

import io.takamaka.code.lang.Event;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.View;

/**
 * An event generated when some consensus parameters might have changed.
 * Clients might monitor these events, for instance, to update their consensus cache.
 */
public class ConsensusUpdate extends Event {
	public final String message;

	/**
	 * Builds the event.
	 * 
	 * @param message a message that describes what has changed
	 */
	@FromContract ConsensusUpdate(String message) {
		require(message != null, "the message cannot be null");
		this.message = message;
	}

	/**
	 * Yields a message that describes what has changed.
	 * 
	 * @return the message
	 */
	public @View String getMessage() {
		return message;
	}
}