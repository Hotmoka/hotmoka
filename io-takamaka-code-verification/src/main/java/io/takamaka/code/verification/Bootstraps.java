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
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.bcel.classfile.BootstrapMethod;
import org.apache.bcel.generic.INVOKEDYNAMIC;
import org.apache.bcel.generic.MethodGen;

/**
 * An object that provides utility methods about the lambda bootstraps
 * contained in a class.
 */
public interface Bootstraps {

	/**
	 * Determines if the given bootstrap method is a method reference to an entry.
	 * 
	 * @param bootstrap the bootstrap method
	 * @return true if and only if that condition holds
	 */
	boolean lambdaIsEntry(BootstrapMethod bootstrap);

	/**
	 * Determines if the given bootstrap method is a method reference to a red payable method or constructor.
	 * 
	 * @param bootstrap the bootstrap method
	 * @return true if and only if that condition holds
	 */
	boolean lambdaIsRedPayable(BootstrapMethod bootstrap);

	/**
	 * Yields the bootstrap methods in this class.
	 * 
	 * @return the bootstrap methods
	 */
	Stream<BootstrapMethod> getBootstraps();

	/**
	 * Yields the subset of the bootstrap methods of this class that lead to an entry,
	 * possibly indirectly.
	 * 
	 * @return the bootstrap methods that lead to an entry
	 */
	Stream<BootstrapMethod> getBootstrapsLeadingToEntries();

	/**
	 * Yields the bootstrap method associated with the given instruction.
	 * 
	 * @param invokedynamic the instruction
	 * @return the bootstrap method
	 */
	BootstrapMethod getBootstrapFor(INVOKEDYNAMIC invokedynamic);

	/**
	 * Yields the target method or constructor called by the given bootstrap. It can also be outside
	 * the class that we are processing.
	 * 
	 * @param bootstrap the bootstrap
	 * @return the target called method or constructor
	 */
	Optional<? extends Executable> getTargetOf(BootstrapMethod bootstrap);

	/**
	 * Determines if the given lambda method is a part of an {@code @@Entry} method,
	 * that the compiler moved into a synthetic method implementing the code of a lambda expression.
	 * As such, it can be considered as part of the {@code @@Entry} method itself.
	 * 
	 * @param lambda the lambda method
	 * @return true if and only if that condition holds
	 */
	boolean isPartOfFromContract(MethodGen lambda);
}