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

package io.hotmoka.node.api;

import io.hotmoka.node.api.signatures.ConstructorSignature;
import io.hotmoka.node.api.signatures.MethodSignature;

/**
 * An exception thrown when a non-white-listed method is called.
 */
@SuppressWarnings("serial")
public class NonWhiteListedCallException extends HotmokaException {

	/**
	 * Creates the exception about a call to a non-white-listed constructor.
	 * 
	 * @param constructor the constructor
	 */
	public NonWhiteListedCallException(ConstructorSignature constructor) {
		super("Illegal call to non-white-listed constructor " + constructor);
	}

	/**
	 * Creates the exception about a call to a non-white-listed method.
	 * 
	 * @param method the method
	 */
	public NonWhiteListedCallException(MethodSignature method) {
		super("Illegal call to non-white-listed method " + method);
	}
}