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

package io.hotmoka.verification.api;

import java.util.Objects;

/**
 * An exception thrown when a jar file refers to an unknown type, that cannot be resolved from the classpath.
 */
@SuppressWarnings("serial")
public class UnknownTypeException extends Exception {

	/**
	 * Creates an exception.
	 * 
	 * @param name the name of the unknown type
	 */
	public UnknownTypeException(String name) {
		super("Unknown type " + Objects.requireNonNull(name));
	}
}