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

package io.hotmoka.node.api;

import io.hotmoka.node.api.signatures.ConstructorSignature;
import io.hotmoka.node.api.signatures.MethodSignature;

/**
 * An exception thrown when a the method or constructor
 * called by a transaction cannot be found and accessed.
 */
@SuppressWarnings("serial")
public class UnmatchedTargetException extends HotmokaException {

	/**
	 * Creates the exception with the given message.
	 * 
	 * @param message the message
	 */
	public UnmatchedTargetException(String message) {
		super(message);
	}

	/**
	 * Creates an exception about a call to a constructor that cannot be found.
	 * 
	 * @param constructor the constructor
	 */
	public UnmatchedTargetException(ConstructorSignature constructor) {
		super("Cannot find constructor " + constructor);
	}

	/**
	 * Creates an exception about a call to a method that cannot be found.
	 * 
	 * @param method the method
	 */
	public UnmatchedTargetException(MethodSignature method) {
		super("Cannot find method " + method);
	}
}