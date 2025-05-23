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

package io.hotmoka.node.api.responses;

import java.util.stream.Stream;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.node.api.values.StorageReference;

/**
 * A response for a transaction that might contain events.
 */
@Immutable
public interface TransactionResponseWithEvents extends TransactionResponse {
	
	/**
	 * Yields the events induced by the execution of this transaction.
	 * 
	 * @return the events
	 */
    Stream<StorageReference> getEvents();

    /**
     * Determines if this responses contains at least an event.
     * 
     * @return true if and only if that condition holds
     */
    boolean hasEvents();
}