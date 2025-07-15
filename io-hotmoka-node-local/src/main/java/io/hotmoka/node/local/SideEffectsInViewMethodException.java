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

package io.hotmoka.node.local;

import io.hotmoka.node.api.signatures.MethodSignature;

/**
 * An exception thrown when a transaction for the execution of a
 * {@code @@View} method has side-effects different
 * from the modification of the balance of the caller.
 */
@SuppressWarnings("serial")
public class SideEffectsInViewMethodException extends HotmokaTransactionException {

	/**
	 * Creates an exception about a {@code @@View} method that caused side-effects.
	 * 
	 * @param method the method
	 */
	public SideEffectsInViewMethodException(MethodSignature method) {
		super("@View method " + method + " induced side-effects");
	}
}