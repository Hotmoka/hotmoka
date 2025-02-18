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

import org.apache.bcel.classfile.JavaClass;

/**
 * A class that passed the static Takamaka verification tests.
 * Such classes are ordered wrt their name.
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
	 * Yields the jar this class belongs to.
	 * 
	 * @return the jar
	 */
	VerifiedJar getJar();

	/**
	 * Yields the container of the bootstrap methods of this class.
	 * It yields a deep copy since it is a modifiable object.
	 * 
	 * @return the container
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
}