/*
Copyright 2019 Fausto Spoto (fausto.spoto@univr.it)

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

package it.univr.bcel;

import java.util.stream.Stream;

import org.apache.bcel.generic.Type;

/**
 * The types at an instruction. Instances of this class specify the type
 * of each single stack element and local variable at the instruction.
 * Instances of this class are immutable.
 * 
 * @author Fausto Spoto <fausto.spoto@univr.it>
 */
public interface Types {

	/**
	 * Yields the number of slots used for the local variables.
	 * Types usually occupy one slot each, but for {@code long} and {@code double},
	 * that occupy two slots each.
	 * 
	 * @return the number of slots used for the local variables
	 */
	int localsCount();

	/**
	 * Yields the number of slots used for the stack elements.
	 * Types usually occupy one slot each, but for {@code long} and {@code double},
	 * that occupy two slots each.
	 * 
	 * @return the number of slots used for the stack elements
	 */
	int stackHeight();

	/**
	 * Yields the type of the {@code pos}th slot of the stack (from the base, towards its top).
	 * 
	 * @param pos the slot number, between 0 (inclusive) and {@link it.univr.bcel.Types#stackHeight()} (exclusive)
	 * @return the type
	 * @throws java.lang.ArrayIndexOutOfBoundsException if {@code pos} is out of its bounds
	 */
	Type getStack(int pos);

	/**
	 * Yields the type of the {@code pos}th slot of the local variables (from local 0, upwards).
	 * 
	 * @param pos the slot number, between 0 (inclusive) and {@link it.univr.bcel.Types#localsCount()} (exclusive)
	 * @return the type
	 * @throws java.lang.ArrayIndexOutOfBoundsException if {@code pos} is out of its bounds
	 */
	Type getLocal(int pos);

	/**
	 * Yields the ordered stream of the types of the stack slots, from the base of the stack
	 * towards its top. For types that span two slots, their two parts are returned in sequence.
	 * 
	 * @return the ordered stream
	 */
	Stream<Type> getStack();

	/**
	 * Yields the ordered stream of the types of the slots for the local variables, from local 0 upwards.
	 * For types that span two slots, their two parts are returned in sequence.
	 * 
	 * @return the ordered stream
	 */
	Stream<Type> getLocals();

	/**
	 * Yields the ordered stream of the types of the stack slots, from the base of the stack
	 * towards its top. For types that span two slots, their first part only is returned. That is,
	 * {@code double} and {@code long} are reported as a single type.
	 * 
	 * @return the ordered stream
	 */
	Stream<Type> getStackOnlyFirstSlots();

	/**
	 * Yields the ordered stream of the types of the slots for the local variables, from local 0 upwards.
	 * For types that span two slots, their first part only is returned. That is,
	 * {@code double} and {@code long} are reported as a single type.
	 * 
	 * @return the ordered stream
	 */
	Stream<Type> getLocalsOnlyFirstSlots();
}