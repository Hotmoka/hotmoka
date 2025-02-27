/*
Copyright 2025 Fausto Spoto

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

package io.hotmoka.verification;

import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;

import io.hotmoka.verification.api.IllegalJarException;
import io.hotmoka.verification.api.PushersIterator;
import io.hotmoka.verification.internal.PushersImpl;

/**
 * A provider of the pushers of a stack value.
 */
public final class Pushers {

	private Pushers() {}

	/**
	 * Yields an iterator over the pushers of a value on the stack.
	 *
	 * @param ih the start instruction of the look-up
	 * @param slots the difference in stack height
	 * @param method the method where {@code ih} occurs
	 * @return the iterator
	 * @throws IllegalJarException if the jar where pushers are considered is illegal or too complex
	 */
	public static PushersIterator iterator(InstructionHandle ih, int slots, MethodGen method) throws IllegalJarException {
		return new PushersImpl(ih, slots, method).iterator();
	}
}