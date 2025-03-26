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

package io.hotmoka.node.local.internal;

import java.util.Arrays;

import io.hotmoka.crypto.Hex;
import io.hotmoka.node.local.api.StateId;

/**
 * Implementation of the identifier of the state of a trie-based store.
 */
public class StateIdImpl implements StateId {
	private final byte[] bytes;

	/**
	 * Creates an identifier represented by the given bytes.
	 * 
	 * @param id the bytes of the identifier
	 */
	public StateIdImpl(byte[] id) {
		this.bytes = id.clone();
	}

	@Override
	public byte[] getBytes() {
		return bytes.clone();
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof StateIdImpl sii)
			return Arrays.equals(bytes, sii.bytes); // optimization
		else
			return other instanceof StateId si && Arrays.equals(bytes, si.getBytes());
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(bytes);
	}

	@Override
	public String toString() {
		return Hex.toHexString(bytes);
	}

	@Override
	public int compareTo(StateId other) {
		if (other instanceof StateIdImpl sii)
			return Arrays.compare(bytes, sii.bytes); // optimization
		else
			return Arrays.compare(bytes, other.getBytes());
	}
}