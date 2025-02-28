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

import org.apache.bcel.generic.Type;

/**
 * A utility that transforms a BCEL type into its corresponding class tag.
 */
public interface BcelToClassTransformer {

	/**
	 * Computes the Java class tag for the given BCEL type.
	 * 
	 * @param type the BCEL type
	 * @return the class tag corresponding to {@code type}
	 * @throws ClassNotFoundException if {@code type} is a class that cannot be found in the Takamaka program 
	 */
	Class<?> of(Type type) throws ClassNotFoundException;

	/**
	 * Computes the Java class tags for the given BCEL types.
	 * 
	 * @param types the BCEL types
	 * @return the class tags corresponding to {@code types}
	 * @throws ClassNotFoundException if some of the {@code types} is a class that cannot be found in the Takamaka program 
	 */
	Class<?>[] of(Type[] types) throws ClassNotFoundException;
}