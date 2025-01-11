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

package io.hotmoka.verification.api;

import java.lang.reflect.Executable;
import java.lang.reflect.Field;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.InvokeInstruction;

/**
 * A class that passed the static Takamaka verification tests.
 * They are ordered wrt their name.
 */
public interface VerifiedClass extends Comparable<VerifiedClass> {

	/**
	 * This prefix is forbidden in the name of fields and methods of a Takamaka class,
	 * since it will be used for instrumentation. Java compilers do not allow one to
	 * use this character in the name of fields or methods, but it is still possible if
	 * Java bytecode is produced in other ways. Hence it is necessary to check that it is not used.
	 */
	String FORBIDDEN_PREFIX = "§";

	/**
	 * Yields the white-listing model for the field accessed by the given instruction.
	 * This means that that instruction accesses that field but that access is white-listed
	 * only if the resulting model is verified.
	 * 
	 * @param fi the instruction that accesses the field
	 * @return the model. This must exist, since the class is verified and all accesses have been proved
	 *         to be white-listed (up to possible proof obligations contained in the model).
	 * @throws ClassNotFoundException if some class of the Takamaka program cannot be loaded
	 */
	Field whiteListingModelOf(FieldInstruction fi) throws ClassNotFoundException;

	/**
	 * Yields the white-listing model for the method called by the given instruction.
	 * This means that that instruction calls that method but that call is white-listed
	 * only if the resulting model is verified.
	 * 
	 * @param invoke the instruction that calls the method
	 * @return the model. This must exist, since the class is verified and all calls have been proved
	 *         to be white-listed (up to possible proof obligations contained in the model).
	 * @throws ClassNotFoundException if some class of the Takamaka program cannot be loaded
	 */
	Executable whiteListingModelOf(InvokeInstruction invoke) throws ClassNotFoundException;

	/**
	 * Yields the jar this class belongs to.
	 * 
	 * @return the jar
	 */
	VerifiedJar getJar();

	/**
	 * Yields a deep copy of the utility object that knows about the bootstraps of this class.
	 * It yields a deep copy since it is a modifiable object.
	 * 
	 * @return the utility object
	 */
	Bootstraps getBootstraps();

	/**
	 * Yields the utility that allows one to compute the pushers of values on the stack
	 * for the code in this class.
	 * 
	 * @return the utility object
	 */
	Pushers getPushers();

	/**
	 * Yields the fully-qualified name of this class.
	 * 
	 * @return the fully-qualified name
	 */
	String getClassName();

	/**
	 * Builds a Java class from this object.
	 * 
	 * @return the Java class
	 */
	JavaClass toJavaClass();

	/**
	 * Determines if this class has been verified during the initialization of the node.
	 * 
	 * @return true if and only if that condition holds
	 */
	boolean isDuringInitialization();

	/**
	 * Determines if this class is annotated as white-listed during the initialization of the node.
	 * 
	 * @return true if and only if that condition holds
	 */
	boolean isWhiteListedDuringInitialization();
}