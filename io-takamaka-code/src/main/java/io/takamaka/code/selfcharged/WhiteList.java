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