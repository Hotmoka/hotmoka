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

package io.hotmoka.helpers;

import io.hotmoka.helpers.api.UnexpectedCodeException;
import io.hotmoka.node.api.signatures.NonVoidMethodSignature;

/**
 * An exception stating that a method was expected to return a value, instead
 * of being declared as a {@code void} method.
 */
@SuppressWarnings("serial")
public class UnexpectedVoidMethodException extends UnexpectedCodeException {

	/**
	 * Builds the exception.
	 * 
	 * @param method the method that was expected to return a value, not to be {@code void}
	 */
	public UnexpectedVoidMethodException(NonVoidMethodSignature method) {
		super(method + " should not return void");
	}
}