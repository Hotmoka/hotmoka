/*
Copyright 2024 Fausto Spoto

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

package io.hotmoka.patricia;

/**
 * A function that marshals values into their byte representation.
 *
 * @param <Value> the type of the marshalled values
 */
public interface ToBytes<Value> {

	/**
	 * Marshals a value into its byte representation.
	 * 
	 * @param value the value to marshals
	 * @return the resulting bytes
	 */
	byte[] get(Value value);
}