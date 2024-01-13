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

package io.hotmoka.beans.api.values;

import io.hotmoka.marshalling.api.Marshallable;

/**
 * A value that can be stored in blockchain, passed as argument or returned
 * between the outside world and the blockchain.
 */
public interface StorageValue extends Marshallable, Comparable<StorageValue> {

	@Override
	boolean equals(Object other);

	@Override
	int hashCode();

	@Override
	String toString();
}