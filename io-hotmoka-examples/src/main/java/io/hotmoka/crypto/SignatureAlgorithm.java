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

package io.hotmoka.crypto;

/**
 * A class with the same name as a Hotmoka class. Once installed
 * in a node, a call should lead here, not inside its implementation
 * of the node!
 *
 * @param <T> the type of values that get signed
 */
public interface SignatureAlgorithm<T> {

	/**
	 * Yields an empty signature algorithm that signs everything with an empty array of bytes.
	 * 
	 * @param <T> the type of values that get signed
	 * @return the algorithm
	 */
	static <T> SignatureAlgorithm<T> empty() {
		// the real implementation does not return null; this will allow our test case
		// to understand that this implementation has been called
		return null;
	}
}