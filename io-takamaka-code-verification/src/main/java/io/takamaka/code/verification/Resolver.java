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

package io.takamaka.code.verification;

import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.util.Optional;

import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.InvokeInstruction;

/**
 * An utility that implements resolving algorithms for field and methods.
 */
public interface Resolver {

	/**
	 * Yields the field signature that would be accessed by the given instruction.
	 * 
	 * @param fi the instruction
	 * @return the signature, if any
	 */
	Optional<Field> resolvedFieldFor(FieldInstruction fi);

	/**
	 * Yields the method or constructor signature that would be accessed by the given instruction.
	 * At run time, that signature or one of its redefinitions (for non-private non-final methods) will be called.
	 * 
	 * @param invoke the instruction
	 * @return the signature
	 */
	Optional<? extends Executable> resolvedExecutableFor(InvokeInstruction invoke);
}