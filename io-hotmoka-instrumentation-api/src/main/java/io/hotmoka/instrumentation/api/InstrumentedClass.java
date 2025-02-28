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

package io.hotmoka.instrumentation.api;

import org.apache.bcel.classfile.JavaClass;

/**
 * An instrumented class file. For instance, storage classes are instrumented
 * by adding the serialization support; contracts are instrumented in order
 * to deal with payable calls. Instrumented classes are ordered by name.
 */
public interface InstrumentedClass extends Comparable<InstrumentedClass> {

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