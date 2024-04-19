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

package io.hotmoka.node.api.updates;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.marshalling.api.Marshallable;
import io.hotmoka.node.api.values.StorageReference;

/**
 * An update states that a property of an object has been
 * modified to a given value. Updates are stored in blockchain and
 * describe the state of storage objects.
 */
@Immutable
public interface Update extends Marshallable, Comparable<Update> {

	/**
	 * Yields the storage reference of the object whose field is modified.
	 * 
	 * @return the storage reference of the object whose field is modified
	 */
	StorageReference getObject();

	/**
	 * Determines if the information expressed by this update is set immediately
	 * when a storage object is deserialized from blockchain. Otherwise, the
	 * information will only be set on-demand.
	 * 
	 * @return true if and only if the information is eager
	 */
	boolean isEager();

	/**
	 * Determines if this update is for the same property of the {@code other},
	 * although possibly for a different object. For instance, they are both class tags
	 * or they are both updates to the same field signature.
	 * 
	 * @param other the other update
	 * @return true if and only if that condition holds
	 */
	boolean sameProperty(Update other);

	@Override
	boolean equals(Object other);

	@Override
	int hashCode();

	@Override
	String toString();
}