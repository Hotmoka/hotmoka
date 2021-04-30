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
	protected @FromContract Event() {
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