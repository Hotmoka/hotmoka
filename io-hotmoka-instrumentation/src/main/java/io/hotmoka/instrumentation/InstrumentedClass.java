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

package io.hotmoka.instrumentation;

import org.apache.bcel.classfile.JavaClass;

import io.hotmoka.instrumentation.internal.InstrumentedClassImpl;
import io.hotmoka.verification.VerifiedClass;

/**
 * An instrumented class file. For instance, it instruments storage
 * classes, by adding the serialization support, and contracts, to deal with entries.
 * They are ordered by their name.
 */
public interface InstrumentedClass extends Comparable<InstrumentedClass> {

	/**
	 * Yields an instrumented class from a verified class.
	 * 
	 * @param clazz the class to instrument
	 * @param gasCostModel the gas cost model used for the instrumentation
	 */
	static InstrumentedClass of(VerifiedClass clazz, GasCostModel gasCostModel) {
		return new InstrumentedClassImpl(clazz, gasCostModel);
	}

	/**
	 * Yields the fully-qualified name of this class.
	 * 
	 * @return the fully-qualified name
	 */
	String getClassName();

	/**
	 * Yields a Java class from this object.
	 * 
	 * @return the Java class
	 */
	JavaClass toJavaClass();
}