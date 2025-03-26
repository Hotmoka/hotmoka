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

package io.hotmoka.node.local.api;

import java.util.Optional;

import io.hotmoka.annotations.Immutable;

/**
 * A store that can be checked out, that is, its view of the world can be moved
 * back and forth in time. Different moments of the store are identified by
 * state identifiers, that can be checked out when needed.
 * 
 * @param <S> the type of this store
 * @param <T> the type of the store transformations that can be started from this store
 */
@Immutable
public interface CheckableStore<S extends CheckableStore<S, T>, T extends StoreTransformation<S, T>> extends Store<S, T> {

	/**
	 * Yields the identifier of the currently checked-out state of this store.
	 * 
	 * @return the identifier; this is for instance the hash of the store
	 * @throws StoreException if the operation cannot be completed correctly
	 */
	StateId getStateId() throws StoreException;

	/**
	 * Yields a store derived from this by resetting the view of the world to that expressed
	 * by the given identifier. This assumes that this store was derived by a chain of transformations
	 * passing through a store with that identifier, that has not been garbage-collected yet.
	 * It allows one to specify the cache to use for the store.
	 * 
	 * @param stateId the identifier of the resulting store
	 * @param cache the cache to use for the resulting store; if empty, it will be extracted from the store
	 *              itself, which might be expensive
	 * @return the resulting store
	 * @throws UnknownStateIdException if {@code stateId} cannot be found
	 * @throws StoreException if the operation cannot be completed correctly
	 * @throws InterruptedException if the current thread is interrupted while performing the operation
	 */
	S checkedOutAt(StateId stateId, Optional<StoreCache> cache) throws UnknownStateIdException, StoreException, InterruptedException;
}