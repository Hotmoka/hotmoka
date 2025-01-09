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

package io.hotmoka.examples.errors.loop2;

import java.util.LinkedList;
import java.util.List;

/**
 * An example of a method that runs into an infinite loop. This
 * must be rejected by the verification layer of Takamaka.
 */
public class Loop {

	public static void loop() {
		List<Object> l = new LinkedList<>();
		List<Object> ll = new LinkedList<>();
		ll.add(l);
		l.add(ll);
		l.equals(ll); // this diverges
	}
}