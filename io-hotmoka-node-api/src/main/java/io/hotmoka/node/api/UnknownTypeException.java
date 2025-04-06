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

import io.hotmoka.node.api.types.StorageType;

/**
 * An exception thrown when a the method or constructor
 * called by a transaction refers to an unknown type.
 */
@SuppressWarnings("serial")
public class UnknownTypeException extends HotmokaException {

	/**
	 * Creates an exception about an unknown type.
	 * 
	 * @param type the type
	 */
	public UnknownTypeException(StorageType type) {
		super("Unknown type " + type);
	}
}