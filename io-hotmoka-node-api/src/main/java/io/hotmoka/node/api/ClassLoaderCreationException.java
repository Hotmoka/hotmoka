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

import java.util.Objects;

/**
 * An exception stating that the creation of a class loader failed
 * because the parameters for its creation (for instance, the jars) contain
 * something illegal. This is meant to represent that the data in the
 * transaction request, from which the classloader is being built, is
 * inconsistent and prevented the classloader from being built.
 * In such cases, the transaction should be rejected since it cannot
 * even start without a classloader.
 */
@SuppressWarnings("serial")
public class ClassLoaderCreationException extends Exception {

	/**
	 * Creates a new exception with the given message.
	 * 
	 * @param message the message
	 */
	public ClassLoaderCreationException(String message) {
		super(Objects.requireNonNull(message, "message cannot be null"));
	}
}