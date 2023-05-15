/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

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

	/**
	 * A message that describes what has changed.
	 */
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