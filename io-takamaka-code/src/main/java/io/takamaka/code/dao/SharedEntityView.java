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

package io.takamaka.code.dao;

import java.math.BigInteger;
import java.util.stream.Stream;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.View;
import io.takamaka.code.util.StorageMapView;

/**
 * A view of a shared entity. It contains only methods for reading its state.
 * 
 * @param <S> the type of the shareholders
 */
public interface SharedEntityView<S extends Contract> {

	/**
	 * Yields the current shares, for each current shareholder.
	 * 
	 * @return the shares
	 */
	@View StorageMapView<S, BigInteger> getShares();

	/**
	 * Yields the shareholders.
	 * 
	 * @return the shareholders
	 */
	Stream<S> getShareholders();

	/**
	 * Determine if the given object is a shareholder of this entity.
	 * 
	 * @param who the potential shareholder
	 * @return true if and only if {@code who} is a shareholder of this entity
	 */
	@View boolean isShareholder(Object who);

	/**
	 * Yields the current shares of the given shareholder.
	 * 
	 * @param shareholder the shareholder
	 * @return the shares. Yields zero if {@code shareholder} is currently not a shareholder
	 */
	@View BigInteger sharesOf(S shareholder);

	/**
	 * Yields the total amount of shares, that is split among all shareholders.
	 * 
	 * @return the total amount of shares
	 */
	@View BigInteger getTotalShares();

	/**
	 * Yields a snapshot of this shared entity. The snapshot contains the shares in this entity
	 * but is independent from this entity: any future modification of this enoty will
	 * not be seen through the snapshot. A snapshot is always {@link io.takamaka.code.lang.Exported}.
	 * 
	 * @return a snapshot of this entity
	 */
	SharedEntityView<S> snapshot();
}