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

package io.hotmoka.beans.api.updates;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.api.types.ClassType;

/**
 * An update that states that an object belongs to a given class.
 */
@Immutable
public interface ClassTag extends Update {

	/**
	 * Yields the class of the object.
	 * 
	 * @return the class of the object
	 */
	ClassType getClazz();

	/**
	 * Yields the reference of the transaction that installed the jar from which the class was resolved.
	 * 
	 * @return the reference
	 */
	TransactionReference getJar();
}